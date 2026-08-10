package com.powers.progression;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import com.powers.magic.MagicAspect;
import com.powers.magic.MagicIntent;
import com.powers.magic.runtime.CastAdjustment;
import com.powers.magic.runtime.CastScalingContext;
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
		double potencyBonus = Math.min(0.90, level * 0.05 + potencyBonus(action, profile));
		double rangeBonus = Math.min(0.55, level * 0.02 + rangeBonus(action, profile));
		double durationBonus = Math.min(0.65, durationBonus(action, profile, level));
		double costReduction = Math.min(0.35, level * 0.01
				+ bonus(action, profile, RankPerkType.ENERGY_COST_REDUCTION));
		double cooldownReduction = Math.min(0.40, level * 0.015
				+ bonus(action, profile, RankPerkType.COOLDOWN_REDUCTION));
		double priorityBonus = bonus(action, profile, RankPerkType.INTERACTION_PRIORITY);
		double backlashReduction = bonus(action, profile, RankPerkType.BACKLASH_REDUCTION);

		double potencyMultiplier = 1.0 + potencyBonus;
		double rangeMultiplier = 1.0 + rangeBonus;
		double durationMultiplier = 1.0 + durationBonus;
		return new ScaledMagicValues(
				scaledInt(action.basePotency(), potencyMultiplier),
				action.baseRange() * rangeMultiplier,
				scaledInt(action.baseDurationTicks(), durationMultiplier),
				reducedInt(action.baseEnergy(), costReduction, 0.35),
				reducedInt(action.baseCooldownTicks(), cooldownReduction, 0.40),
				Math.max(0, action.priority() + (int) Math.round(priorityBonus * 10.0)),
				variants(profile), Math.max(0.50, 1.0 - backlashReduction),
				potencyMultiplier, rangeMultiplier, durationMultiplier);
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
		ScaledMagicValues ranked = INSTANCE.scale(action, profile(player), SkillSystem.effectiveLevel(player));
		return applyInteraction(action, ranked, CastScalingContext.current());
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
		double ratio = Math.min(0.35, SkillSystem.effectiveLevel(player) * 0.01
				+ actionReduction(player, actionId, RankPerkType.ENERGY_COST_REDUCTION));
		return reducedInt(Math.max(0, baseCost), ratio, 0.35);
	}

	/** Reduces a bespoke ability cooldown using its canonical action profile. */
	public static int cooldown(ServerPlayer player, String actionId, int baseTicks) {
		double ratio = Math.min(0.40, SkillSystem.effectiveLevel(player) * 0.015
				+ actionReduction(player, actionId, RankPerkType.COOLDOWN_REDUCTION));
		return reducedInt(Math.max(0, baseTicks), ratio, 0.40);
	}

	/** Applies rank energy-capacity perks after the legacy numeric ladder. */
	public static int energyCapacity(ServerPlayer player, int baseCapacity) {
		return scaledInt(Math.max(0, baseCapacity), 1.0 + profile(player).value(RankPerkType.ENERGY_CAPACITY));
	}

	/** Applies capped regeneration perks while preserving a minimum positive pulse. */
	public static int regeneration(ServerPlayer player, int baseAmount) {
		if (baseAmount <= 0) return 0;
		return Math.max(1, scaledInt(baseAmount, 1.0 + profile(player).value(RankPerkType.ENERGY_REGEN)));
	}

	/** Returns whether the player's active maze profile unlocks a named mechanical variant. */
	public static boolean hasVariant(ServerPlayer player, String variant) {
		return variants(profile(player)).contains(variant);
	}

	private static double actionReduction(ServerPlayer player, String actionId, RankPerkType type) {
		return INSTANCE.bonus(requireAction(actionId), profile(player), type);
	}

	private static MagicActionDefinition requireAction(String actionId) {
		MagicActionDefinition definition = CATALOGUE.definition(new MagicActionId(actionId));
		if (definition == null) throw new IllegalArgumentException("Unknown magic action: " + actionId);
		return definition;
	}

	private double potencyBonus(MagicActionDefinition action, RankProfile profile) {
		RankPerkType type = switch (action.intent()) {
			case HARM -> RankPerkType.POWER_DAMAGE;
			case CONTROL, WORLD_INTERACTION -> RankPerkType.CONTROL;
			case MOVEMENT -> RankPerkType.MOVEMENT;
			case DEFENCE -> RankPerkType.WARD_INTEGRITY;
			case SUPPORT -> RankPerkType.HEALING;
			case INFORMATION -> RankPerkType.REVEAL;
		};
		return bonus(action, profile, type);
	}

	private double rangeBonus(MagicActionDefinition action, RankProfile profile) {
		double result = bonus(action, profile, RankPerkType.RANGE);
		if (action.intent() == MagicIntent.MOVEMENT) {
			result += bonus(action, profile, RankPerkType.MOVEMENT);
		} else if (action.intent() == MagicIntent.INFORMATION) {
			result += bonus(action, profile, RankPerkType.REVEAL) * 0.5;
		}
		return Math.min(RankPerkType.RANGE.cap(), result);
	}

	private double durationBonus(MagicActionDefinition action, RankProfile profile, int legacyLevel) {
		double result = legacyLevel * 0.025 + bonus(action, profile, RankPerkType.DURATION);
		if (action.intent() == MagicIntent.CONTROL || action.intent() == MagicIntent.SUPPORT
				|| action.intent() == MagicIntent.DEFENCE) {
			result += legacyLevel * 0.015;
		}
		return Math.min(RankPerkType.DURATION.cap(), result);
	}

	private double bonus(MagicActionDefinition action, RankProfile profile, RankPerkType type) {
		double total = profile.value(type) + profile.scopedValue(type, action.id().value());
		for (MagicAspect aspect : action.aspects()) total += profile.scopedValue(type, aspect.name().toLowerCase());
		return Math.min(type.cap(), total);
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

	private static ScaledMagicValues applyInteraction(MagicActionDefinition action, ScaledMagicValues ranked,
			CastAdjustment adjustment) {
		double potencyMultiplier = Math.min(2.0,
				ranked.potencyMultiplier() * adjustment.potencyMultiplier());
		double rangeMultiplier = Math.min(2.0, ranked.rangeMultiplier() * adjustment.rangeMultiplier());
		double durationMultiplier = Math.min(2.0,
				ranked.durationMultiplier() * adjustment.durationMultiplier());
		return new ScaledMagicValues(scaledInt(action.basePotency(), potencyMultiplier),
				action.baseRange() * rangeMultiplier, scaledInt(action.baseDurationTicks(), durationMultiplier),
				ranked.energyCost(), ranked.cooldownTicks(), ranked.interactionPriority(),
				ranked.unlockedVariants(), ranked.backlashMultiplier(), potencyMultiplier,
				rangeMultiplier, durationMultiplier);
	}
}
