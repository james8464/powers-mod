package com.powers.progression;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.CastAdjustment;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Progression formula for innate player powers. Crystal and grimoire callers
 * use {@link #unranked(String)} so equipment magic remains independent of the
 * wielder's rank, while collision adjustments still compose safely.
 */
public final class PowerScalingService {
	private static final MagicActionCatalogue CATALOGUE = MagicActionCatalogue.defaults();
	private static final RankProfileService PROFILES = new RankProfileService();
	private static final PowerScalingService INSTANCE = new PowerScalingService();

	/** Scales a definition from an already aggregated profile; useful to pure callers and tests. */
	public ScaledMagicValues scale(MagicActionDefinition action, RankProfile profile, int legacyLevel) {
		int level = Math.max(0, Math.min(10, legacyLevel));
		InnatePowerLevel authored = InnatePowerLevels.forPower(action.id().value(), level);
		double costReduction = Math.min(0.30, level * 0.025);
		double cooldownReduction = Math.min(0.35, level * 0.03);
		Set<String> unlocked = new LinkedHashSet<>(authored.variants());
		unlocked.addAll(variants(profile));
		return new ScaledMagicValues(
				scaledInt(action.basePotency(), authored.damageMultiplier()),
				action.baseRange() * authored.rangeMultiplier(),
				scaledInt(action.baseDurationTicks(), authored.durationMultiplier()),
				reducedInt(action.baseEnergy(), costReduction, 0.35),
				reducedInt(action.baseCooldownTicks(), cooldownReduction, 0.40),
				action.priority() + authored.destructionTier() / 3,
				Set.copyOf(unlocked), Math.max(0.65, 1.0 - level * 0.035),
				authored.damageMultiplier(), authored.rangeMultiplier(),
				authored.durationMultiplier());
	}

	/** Scales an action only when the authoritative invocation route is directly innate. */
	public ScaledMagicValues scaleForSource(MagicActionDefinition action, RankProfile profile,
			int legacyLevel, CastSource source) {
		return source.appliesPlayerRank(true)
				? scale(action, profile, legacyLevel)
				: scale(action, RankProfile.EMPTY, 0);
	}

	/** Returns the active light or darkness profile for a server player. */
	public static RankProfile profile(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean darkness = SkillSystem.hasDarknessTag(player);
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		if (graph == null) return RankProfile.EMPTY;
		return PROFILES.profile(graph, data.rankProgress(darkness));
	}

	/** Returns the scaled canonical values for a player and stable action ID. */
	public static ScaledMagicValues forPlayer(ServerPlayer player, String actionId) {
		MagicActionDefinition action = requireAction(actionId);
		CastSource source = CastScalingContext.currentSource();
		RankProfile activeProfile = source.appliesPlayerRank(true) ? profile(player) : RankProfile.EMPTY;
		int activeLevel = source.appliesPlayerRank(true) ? SkillSystem.effectiveLevel(player) : 0;
		ScaledMagicValues ranked = INSTANCE.scaleForSource(action, activeProfile, activeLevel, source);
		return applyInteraction(action, ranked, CastScalingContext.current());
	}

	/** Returns the authored innate-only profile for ability-specific capacity/destruction rules. */
	public static InnatePowerLevel innateLevel(ServerPlayer player, String actionId) {
		return CastScalingContext.currentSource().appliesPlayerRank(true)
				? InnatePowerLevels.forPower(actionId, SkillSystem.effectiveLevel(player))
				: InnatePowerLevels.forPower(actionId, 0);
	}

	/** Returns a canonical crystal/spell baseline with only live collision adjustments. */
	public static ScaledMagicValues unranked(String actionId) {
		MagicActionDefinition action = requireAction(actionId);
		ScaledMagicValues baseline = INSTANCE.scale(action, RankProfile.EMPTY, 0);
		return applyInteraction(action, baseline, CastScalingContext.current());
	}

	/** Applies the canonical potency ratio to an implementation-specific base value. */
	public static float damage(ServerPlayer player, String actionId, float baseDamage) {
		return (float) (Math.max(0, baseDamage) * forPlayer(player, actionId).potencyMultiplier());
	}

	/** Applies the canonical range ratio to an implementation-specific base value. */
	public static double range(ServerPlayer player, String actionId, double baseRange) {
		return Math.max(0, baseRange) * forPlayer(player, actionId).rangeMultiplier();
	}

	/** Applies rank duration to an implementation-specific baseline. */
	public static int duration(ServerPlayer player, String actionId, int baseTicks) {
		return scaledInt(Math.max(0, baseTicks), forPlayer(player, actionId).durationMultiplier());
	}

	/** Reduces a bespoke ability cost using its canonical action profile. */
	public static int energyCost(ServerPlayer player, String actionId, int baseCost) {
		if (!CastScalingContext.currentSource().appliesPlayerRank(true)) return Math.max(0, baseCost);
		double ratio = Math.min(0.30, SkillSystem.effectiveLevel(player) * 0.025);
		return reducedInt(Math.max(0, baseCost), ratio, 0.35);
	}

	/** Reduces a bespoke ability cooldown using its canonical action profile. */
	public static int cooldown(ServerPlayer player, String actionId, int baseTicks) {
		if (!CastScalingContext.currentSource().appliesPlayerRank(true)) return Math.max(0, baseTicks);
		double ratio = Math.min(0.35, SkillSystem.effectiveLevel(player) * 0.03);
		return reducedInt(Math.max(0, baseTicks), ratio, 0.40);
	}

	/** Numeric light/dark rank ladders already own capacity; maze paths cannot multiply it again. */
	public static int energyCapacity(ServerPlayer player, int baseCapacity) {
		return Math.max(0, baseCapacity);
	}

	/** Regeneration is authored by alignment/environment and is not a generic branch percentage. */
	public static int regeneration(ServerPlayer player, int baseAmount) {
		return Math.max(0, baseAmount);
	}

	/** Returns whether the player's active maze profile unlocks a named mechanical variant. */
	public static boolean hasVariant(ServerPlayer player, String variant) {
		if (!CastScalingContext.currentSource().appliesPlayerRank(true)) return false;
		return variants(profile(player)).contains(variant);
	}

	private static MagicActionDefinition requireAction(String actionId) {
		MagicActionDefinition definition = CATALOGUE.definition(new MagicActionId(actionId));
		if (definition == null) throw new IllegalArgumentException("Unknown magic action: " + actionId);
		return definition;
	}

	private static Set<String> variants(RankProfile profile) {
		Set<String> variants = new LinkedHashSet<>();
		addVariant(variants, profile, "might", "empowered_impact");
		addVariant(variants, profile, "motion", "second_step");
		addVariant(variants, profile, "insight", "true_sight");
		addVariant(variants, profile, "wardcraft", "reflective_ward");
		addVariant(variants, profile, "communion", "soul_echo");
		addVariant(variants, profile, "veil", "afterimage");
		addVariant(variants, profile, "dominion", "ancient_mastery");
		addVariant(variants, profile, "abyss", "dark_resurgence");
		return Set.copyOf(variants);
	}

	private static void addVariant(Set<String> variants, RankProfile profile, String branch, String variant) {
		if (profile.branchWeight(branch) > 0) variants.add(variant);
	}

	private static int scaledInt(int base, double multiplier) {
		return base <= 0 ? 0 : Math.max(1, (int) Math.round(base * multiplier));
	}

	private static int reducedInt(int base, double reduction, double cap) {
		return base <= 0 ? 0 : Math.max(1, (int) Math.ceil(
				base * (1.0 - Math.max(0, Math.min(cap, reduction)))));
	}

	static ScaledMagicValues applyInteraction(MagicActionDefinition action, ScaledMagicValues ranked,
			CastAdjustment adjustment) {
		double potencyMultiplier = ranked.potencyMultiplier() * adjustment.potencyMultiplier();
		double rangeMultiplier = ranked.rangeMultiplier() * adjustment.rangeMultiplier();
		double durationMultiplier = ranked.durationMultiplier() * adjustment.durationMultiplier();
		return new ScaledMagicValues(scaledInt(action.basePotency(), potencyMultiplier),
				action.baseRange() * rangeMultiplier, scaledInt(action.baseDurationTicks(), durationMultiplier),
				ranked.energyCost(), ranked.cooldownTicks(), ranked.interactionPriority(),
				ranked.unlockedVariants(), ranked.backlashMultiplier(), potencyMultiplier,
				rangeMultiplier, durationMultiplier);
	}
}
