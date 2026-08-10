package com.powers.item;

import com.powers.fx.PowerFx;
import com.powers.fx.ShadowSwordFx;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactAuthorizationRules;
import com.powers.item.artifact.ArtifactCooldownRules;
import com.powers.item.artifact.ArtifactSelectionRules;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.PowersPackets;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AbilityActivationService;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.artifact.AbyssalSingularityAbility;
import com.powers.power.artifact.AlignedArtifactAbility;
import com.powers.power.artifact.AnnihilationBeamAbility;
import com.powers.power.artifact.NightfallDominionAbility;
import com.powers.power.artifact.OblivionPulseAbility;
import com.powers.power.artifact.SoulRequiemAbility;
import com.powers.power.artifact.SpreadDarknessAbility;
import com.powers.power.artifact.SummonDarknessAbility;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resolves, authorizes, and invokes both mythic artifact catalogues. */
public final class ArtifactWeaponManager {
	public record Action(ArtifactActionDefinition definition, Ability ability) {
		public Component name() {
			return ability.name();
		}
	}

	private static final Map<ArtifactAlignment, List<Action>> ACTIONS =
			new EnumMap<>(ArtifactAlignment.class);

	private ArtifactWeaponManager() {
	}

	public static void initialize() {
		ACTIONS.clear();
		for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
			ACTIONS.put(alignment, ArtifactActionCatalogue.forAlignment(alignment).stream()
					.map(definition -> new Action(definition, resolve(definition))).toList());
		}
	}

	public static List<Action> actions(ArtifactAlignment alignment) {
		if (ACTIONS.isEmpty()) initialize();
		return ACTIONS.getOrDefault(alignment, List.of());
	}

	public static Action selected(ServerPlayer player, ArtifactAlignment alignment) {
		return find(alignment, ArtifactSelectionState.selected(player, alignment));
	}

	public static boolean select(ServerPlayer player, ArtifactAlignment alignment,
			String key, int option) {
		if (!holds(player, alignment) || !authorized(player, alignment)) return false;
		Action action = find(alignment, key);
		int rank = rank(player, alignment);
		if (action == null || !ArtifactSelectionRules.maySelect(action.definition(), alignment, rank)
				|| !ShadowSwordSelectionRules.validOption(option, action.ability().selectionOptionCount())) {
			PowerMessages.overlay(player, Component.translatable("item.powers.artifact.locked",
					action == null ? 1 : action.definition().requiredRank()));
			return false;
		}
		boolean selected = ArtifactSelectionState.select(player, alignment, key);
		if (selected && option >= 0) {
			selected = action.ability().selectOption(player, PlayerPowers.get(player), option);
		}
		if (selected) PowerMessages.overlay(player,
				Component.translatable("item.powers.artifact.selected", action.name()));
		if (selected) PowersPackets.syncTo(player);
		return selected;
	}

	public static void activateSelected(ServerPlayer player, ArtifactAlignment alignment) {
		if (!holds(player, alignment) || !authorized(player, alignment)) {
			refuse(player, alignment);
			return;
		}
		Action action = selected(player, alignment);
		if (action == null || !ArtifactSelectionRules.maySelect(
				action.definition(), alignment, rank(player, alignment))) return;
		if (action.ability().requiresInput()) {
			ShadowSwordPackets.openTeleport(player, alignment);
			return;
		}
		int cooldown = ArtifactCooldownRules.cooldownTicks(alignment, rank(player, alignment),
				action.ability().cooldownTicksFor(player, PlayerPowers.get(player)));
		if (AbilityActivationService.activateWithCooldown(player, action.ability(),
				toggleKey(action), cooldown) == AbilityActivationService.Result.ACTIVATED) {
			castFx(player, alignment, action);
		}
	}

	public static void openMenu(ServerPlayer player, ArtifactAlignment alignment) {
		if (!holds(player, alignment) || !authorized(player, alignment)) {
			refuse(player, alignment);
			return;
		}
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		ShadowSwordPackets.openMenu(player, alignment, ArtifactSelectionState.selected(player, alignment),
				rank(player, alignment), data.getPhase(), data.getSizeMorphOption());
	}

	public static boolean authorized(ServerPlayer player, ArtifactAlignment alignment) {
		return ArtifactAuthorizationRules.mayUse(alignment, SkillSystem.hasDarknessTag(player));
	}

	public static boolean holds(ServerPlayer player, ArtifactAlignment alignment) {
		return isAlignment(player.getMainHandItem(), alignment)
				|| isAlignment(player.getOffhandItem(), alignment);
	}

	public static boolean carries(ServerPlayer player, ArtifactAlignment alignment) {
		return player.getInventory().contains(stack -> isAlignment(stack, alignment));
	}

	public static ArtifactAlignment alignment(ItemStack stack) {
		return stack.getItem() instanceof MythicArtifactItem artifact ? artifact.alignment() : null;
	}

	public static int rank(ServerPlayer player, ArtifactAlignment alignment) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		return alignment == ArtifactAlignment.DARKNESS ? data.darknessLevel() : data.skillLevel();
	}

	public static String toggleKey(Action action) {
		return "artifact/" + action.definition().alignment().serializedName()
				+ "/" + action.definition().key();
	}

	public static int cooldown(ServerPlayer player, ArtifactAlignment alignment, Action action) {
		return ArtifactCooldownRules.cooldownTicks(alignment, rank(player, alignment),
				action.ability().cooldownTicksFor(player, PlayerPowers.get(player)));
	}

	public static void castFx(ServerPlayer player, ArtifactAlignment alignment, Action action) {
		ServerLevel level = (ServerLevel) player.level();
		if (alignment == ArtifactAlignment.DARKNESS) {
			ShadowSwordFx.corruptedCast(level, player.position(), action.definition().key().hashCode(),
				sourceColor(action));
		} else {
			PowerFx.rune(level, player.position(), 2.0, 0xFFF2B2, 30,
					player.level().getGameTime() * 0.08);
			PowerFx.spiral(level, player.position(), 0.9, 3.2, 0xFFFFFF, 24, 0.0);
			PowerFx.burst(level, player.position().add(0.0, 1.0, 0.0),
					ParticleTypes.END_ROD, 12, 0.55, 0.05);
		}
	}

	private static boolean isAlignment(ItemStack stack, ArtifactAlignment alignment) {
		return stack.getItem() instanceof MythicArtifactItem artifact && artifact.alignment() == alignment;
	}

	private static Action find(ArtifactAlignment alignment, String key) {
		return actions(alignment).stream().filter(action -> action.definition().key().equals(key))
				.findFirst().orElse(null);
	}

	private static Ability resolve(ArtifactActionDefinition definition) {
		if (definition.category() == ArtifactActionCategory.ROUTED_POWER) {
			Power power = PowerRegistry.get(definition.abilityId());
			if (power == null || power.ability() == null) throw missing(definition);
			return power.ability();
		}
		if (definition.category() == ArtifactActionCategory.ROUTED_CRYSTAL) {
			Ability ability = CrystalPowerRegistry.getAbility(definition.abilityId());
			if (ability == null) throw missing(definition);
			return ability;
		}
		return switch (definition.abilityId()) {
			case "summon_darkness" -> new SummonDarknessAbility();
			case "spread_darkness" -> new SpreadDarknessAbility();
			case "abyssal_singularity" -> new AbyssalSingularityAbility();
			case "oblivion_pulse" -> new OblivionPulseAbility();
			case "annihilation_beam" -> new AnnihilationBeamAbility();
			case "soul_requiem" -> new SoulRequiemAbility();
			case "nightfall_dominion" -> new NightfallDominionAbility();
			default -> new AlignedArtifactAbility(definition);
		};
	}

	private static IllegalStateException missing(ArtifactActionDefinition definition) {
		return new IllegalStateException("Missing artifact action: " + definition.abilityId());
	}

	private static int sourceColor(Action action) {
		Power power = PowerRegistry.get(action.definition().abilityId());
		return power == null ? 0x55265F : power.color();
	}

	private static void refuse(ServerPlayer player, ArtifactAlignment alignment) {
		PowerMessages.overlay(player, Component.translatable(alignment == ArtifactAlignment.DARKNESS
				? "item.powers.shadow_sword.refuses" : "item.powers.heavenly_partisan.refuses"));
	}
}
