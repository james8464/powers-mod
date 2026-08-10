package com.powers.item;

import com.powers.fx.ShadowSwordFx;
import com.powers.network.ShadowSwordPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.ShadowSwordState;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AbilityActivationService;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.artifact.AbyssalSingularityAbility;
import com.powers.power.artifact.AnnihilationBeamAbility;
import com.powers.power.artifact.NightfallDominionAbility;
import com.powers.power.artifact.SpreadDarknessAbility;
import com.powers.power.artifact.SummonDarknessAbility;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves, validates, and invokes the persistent Shadow Sword selection. */
public final class ShadowSwordPowerManager {
	public record Action(ShadowSwordCatalogue.Definition definition, Ability ability) {
		public Component name() {
			return ability.name();
		}
	}

	private static final Map<String, Ability> ARTIFACT_ABILITIES = List.<Ability>of(
			new SummonDarknessAbility(), new SpreadDarknessAbility(),
			new AbyssalSingularityAbility(), new AnnihilationBeamAbility(),
			new NightfallDominionAbility()).stream()
			.collect(Collectors.toUnmodifiableMap(ability -> ability.id().getPath(), Function.identity()));
	private static List<Action> cachedActions = List.of();

	private ShadowSwordPowerManager() {
	}

	/** Returns every currently registered action in stable menu order. */
	public static List<Action> actions() {
		if (cachedActions.isEmpty()) initialize();
		return cachedActions;
	}

	/** Resolves every registry reference once and fails server startup on a missing action. */
	public static void initialize() {
		cachedActions = ShadowSwordCatalogue.definitions().stream().map(definition ->
				new Action(definition, resolveAbility(definition))).toList();
	}

	public static Action selected(ServerPlayer player) {
		return find(ShadowSwordState.selected(player));
	}

	public static boolean select(ServerPlayer player, String key) {
		if (!holdsSword(player) || !authorized(player)) return false;
		Action action = find(key);
		if (action == null || !unlocked(player, action)) {
			PowerMessages.overlay(player, Component.translatable("item.powers.shadow_sword.locked",
					action == null ? 1 : action.definition().requiredDarknessRank()));
			return false;
		}
		boolean selected = ShadowSwordState.select(player, key);
		if (selected) PowerMessages.overlay(player,
				Component.translatable("item.powers.shadow_sword.selected", action.name()));
		return selected;
	}

	/** Selects an action and, when supplied, one of its server-validated nested modes. */
	public static boolean select(ServerPlayer player, String key, int option) {
		if (!select(player, key)) return false;
		if (option < 0) return true;
		Action action = find(key);
		if (action == null || option >= action.ability().selectionOptionCount()) return false;
		boolean changed = action.ability().selectOption(player, PlayerPowers.get(player), option);
		if (changed) com.powers.network.PowersPackets.syncTo(player);
		return changed;
	}

	/** Right-click cast entry point after the item has established the interaction hand. */
	public static void activateSelected(ServerPlayer player) {
		if (!holdsSword(player) || !authorized(player)) {
			refuse(player);
			return;
		}
		Action action = selected(player);
		if (action == null || !unlocked(player, action)) {
			PowerMessages.overlay(player, Component.translatable("item.powers.shadow_sword.locked",
					action == null ? 1 : action.definition().requiredDarknessRank()));
			return;
		}
		if (action.ability().requiresInput()) {
			ShadowSwordPackets.openTeleport(player);
			return;
		}
		boolean apotheosis = ShadowSwordRules.bypassesCooldown(PlayerPowers.get(player).darknessLevel());
		AbilityActivationService.Result result = AbilityActivationService.activate(
				player, action.ability(), toggleKey(action), apotheosis);
		if (result == AbilityActivationService.Result.ACTIVATED) {
			ShadowSwordFx.corruptedCast((ServerLevel) player.level(), player.position(),
					action.definition().key().hashCode());
		}
	}

	public static void openMenu(ServerPlayer player) {
		if (!authorized(player)) {
			refuse(player);
			return;
		}
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		ShadowSwordPackets.openMenu(player, ShadowSwordState.selected(player), data.darknessLevel(),
				data.getPhase(), data.getSizeMorphOption());
	}

	public static boolean authorized(ServerPlayer player) {
		return ShadowSwordRules.mayUse(SkillSystem.hasDarknessTag(player));
	}

	public static boolean holdsSword(ServerPlayer player) {
		return isSword(player.getMainHandItem()) || isSword(player.getOffhandItem());
	}

	public static boolean carriesSword(ServerPlayer player) {
		return player.getInventory().contains(ShadowSwordPowerManager::isSword);
	}

	public static boolean isSword(ItemStack stack) {
		return stack.getItem() instanceof ShadowSwordItem;
	}

	public static String toggleKey(Action action) {
		return "shadow_sword/" + action.definition().key();
	}

	public static boolean unlocked(ServerPlayer player, Action action) {
		return PlayerPowers.get(player).darknessLevel() >= action.definition().requiredDarknessRank();
	}

	private static Action find(String key) {
		return actions().stream().filter(action -> action.definition().key().equals(key))
				.findFirst().orElse(null);
	}

	private static Ability resolveAbility(ShadowSwordCatalogue.Definition definition) {
		return switch (definition.source()) {
			case INNATE -> {
				Power power = PowerRegistry.get(definition.abilityId());
				if (power == null || power.ability() == null) {
					throw new IllegalStateException("Missing Shadow Sword innate action: " + definition.abilityId());
				}
				yield power.ability();
			}
			case CRYSTAL -> {
				Ability ability = CrystalPowerRegistry.getAbility(definition.abilityId());
				if (ability == null) {
					throw new IllegalStateException("Missing Shadow Sword crystal action: " + definition.abilityId());
				}
				yield ability;
			}
			case COMMAND, DARKNESS -> {
				Ability ability = ARTIFACT_ABILITIES.get(definition.abilityId());
				if (ability == null) {
					throw new IllegalStateException("Missing Shadow Sword artifact action: " + definition.abilityId());
				}
				yield ability;
			}
		};
	}

	private static void refuse(ServerPlayer player) {
		PowerMessages.overlay(player, Component.translatable("item.powers.shadow_sword.refuses"));
	}
}
