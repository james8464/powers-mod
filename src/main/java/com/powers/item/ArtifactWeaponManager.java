package com.powers.item;

import com.powers.fx.PowerFx;
import com.powers.fx.ShadowSwordFx;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactAuthorizationRules;
import com.powers.item.artifact.ArtifactCooldownRules;
import com.powers.item.artifact.ArtifactSelectionRules;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.PowersPackets;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.PowerEnergy;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.artifact.AlignedArtifactAbility;
import com.powers.power.artifact.NightfallDominionAbility;
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
		return action(alignment, ArtifactSelectionState.selected(player, alignment));
	}

	/** Resolves one canonical menu key to its live ability adapter. */
	public static Action action(ArtifactAlignment alignment, String key) {
		return actions(alignment).stream().filter(candidate -> candidate.definition().key().equals(key))
				.findFirst().orElse(null);
	}

	public static boolean select(ServerPlayer player, ArtifactAlignment alignment,
			String key, int option) {
		if (!holds(player, alignment) || !authorized(player, alignment)) return false;
		Action action = action(alignment, key);
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

	public static AbilityActivationService.Result activateSelected(ServerPlayer player,
			ArtifactAlignment alignment) {
		if (!holds(player, alignment) || !authorized(player, alignment)) {
			refuse(player, alignment);
			return AbilityActivationService.Result.FAILED;
		}
		Action action = selected(player, alignment);
		if (action == null || !ArtifactSelectionRules.maySelect(
				action.definition(), alignment, rank(player, alignment))) {
			return AbilityActivationService.Result.FAILED;
		}
		if (action.ability().requiresInput()) {
			ShadowSwordPackets.openTeleport(player, alignment);
			return AbilityActivationService.Result.REQUIRES_INPUT;
		}
		int cooldown = cooldown(player, alignment, action);
		AbilityActivationService.Result result = AbilityActivationService.activateWithCooldown(
				player, action.ability(), toggleKey(action), cooldown, CastSource.ARTIFACT);
		if (result == AbilityActivationService.Result.ACTIVATED) {
			castFx(player, alignment, action);
		}
		return result;
	}

	public static void openMenu(ServerPlayer player, ArtifactAlignment alignment) {
		if (!holds(player, alignment) || !authorized(player, alignment)) {
			refuse(player, alignment);
			return;
		}
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		List<Action> menuActions = actions(alignment);
		List<ArtifactActionSnapshot> snapshots = menuActions.stream().map(action -> {
			int authoredCost = CastScalingContext.withSource(CastSource.ARTIFACT,
					() -> PowerEnergy.costBeforeArtifact(player, action.ability()));
			ArtifactEnergyModifiers.Quote quote = ArtifactEnergyModifiers.quote(
					ArtifactEnergyModifiers.carries(player, ArtifactRole.DESTRUCTIVE_FOCUS),
					action.ability().id().getPath(), authoredCost);
			int maximum = action.ability().isToggle() ? 0 : cooldown(player, alignment, action);
			int remaining = maximum <= 0 ? 0 : Math.min(maximum,
					ActivationCooldowns.remainingTicks(player, action.ability()));
			boolean active = action.ability().isToggle() && data.isToggleActive(toggleKey(action));
			boolean locked = !ArtifactSelectionRules.maySelect(action.definition(), alignment,
					rank(player, alignment));
			int variant = com.powers.item.artifact.ArtifactMenuRules.selectionVariant(
					action.ability().id().getPath(), data.getSizeMorphOption(),
					com.powers.power.abilities.GravityDisplacementAbility.selectedModeOption(
							player.getUUID()));
			return new ArtifactActionSnapshot(action.definition().key(), action.definition().category(),
					quote.cost(), quote.saved(), remaining, maximum, active, locked, variant);
		}).toList();
		ShadowSwordPackets.openMenu(player, alignment, ArtifactSelectionState.selected(player, alignment),
				rank(player, alignment), data.getSizeMorphOption(), data.energy(),
				ArtifactSelectionState.favourites(player, alignment), snapshots);
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

	/** Revalidates a multi-tick artifact invocation without emitting per-tick feedback. */
	public static boolean maySustain(ServerPlayer player, ArtifactAlignment alignment) {
		return player != null && ArtifactAuthorizationRules.maySustain(
				MagicUseGate.ongoingAllowed(player), carries(player, alignment),
				authorized(player, alignment));
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
		return CastScalingContext.withSource(CastSource.ARTIFACT,
				() -> ArtifactCooldownRules.cooldownTicks(alignment, rank(player, alignment),
						action.ability().cooldownTicksFor(player, PlayerPowers.get(player))));
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
					com.powers.PowersParticles.GLYPH, 12, 0.55, 0.05);
		}
	}

	private static boolean isAlignment(ItemStack stack, ArtifactAlignment alignment) {
		return stack.getItem() instanceof MythicArtifactItem artifact && artifact.alignment() == alignment;
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
