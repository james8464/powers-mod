package com.powers.power.crystals;

import com.powers.cooldown.CooldownPresentation;
import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.ActivationCooldowns;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** One crystal surface that safely selects among several fully fledged abilities. */
public final class ModeCrystalAbility extends Ability {
	private final List<Ability> modes;
	private final String crystalPath;

	public ModeCrystalAbility(String crystalPath, List<Ability> modes) {
		super(PowersMod.id(crystalPath + "_convergence"),
				Component.translatable("ability.powers." + crystalPath + "_convergence"),
				0, false, false);
		if (modes.isEmpty()) throw new IllegalArgumentException("A crystal needs at least one ability");
		this.crystalPath = crystalPath;
		this.modes = List.copyOf(modes);
	}

	private Ability selected(ServerPlayer player) {
		return modes.get(PlayerPowers.get(player).selectedCrystalMode(crystalPath, modes.size()));
	}

	/** Returns the canonical underlying action used for interaction resolution. */
	public String selectedActionId(ServerPlayer player) {
		return selected(player).id().getPath();
	}

	/** Returns the selected ability for energy, cooldown, and rank accounting. */
	public Ability selectedAbility(ServerPlayer player) {
		return selected(player);
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return selected(player).isSelectionAction(player) || player.isCrouching();
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		Ability selected = selected(player);
		// Returning from an active dream/realm must remain free even when the
		// convergence itself is cooling down.
		if (selected.isSelectionAction(player)) return selected.activate(player, data);
		if (player.isCrouching()) {
			if (radialSelector()) {
				com.powers.network.CrystalSelectorPackets.open(player, this);
				return true;
			}
			int nextIndex = CrystalModeState.advance(
					PlayerPowers.get(player).selectedCrystalMode(crystalPath, modes.size()), modes.size());
			PlayerPowers.get(player).setSelectedCrystalMode(crystalPath, nextIndex);
			Ability next = modes.get(nextIndex);
			PowerMessages.overlay(player,
					Component.translatable("crystal.powers.mode_selected", next.name()));
			return true;
		}
		// Track the underlying ability as well as this crystal surface so a
		// player cannot bypass a rare power's cooldown by swapping artifacts.
		if (!ActivationCooldowns.isReady(player, selected)) {
			PowerMessages.send(player, "ability.powers.cooldown", 4,
					Long.toString(CooldownPresentation.wholeSeconds(
							ActivationCooldowns.remainingTicks(player, selected))));
			return false;
		}
		boolean activated = selected.activate(player, data);
		if (activated) {
			ActivationCooldowns.start(player, selected, selected.cooldownTicksFor(player, data));
		}
		return activated;
	}

	@Override
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return selected(player).cooldownTicksFor(player, data);
	}

	public void clear(UUID player) {
		// Selections are persistent player data and intentionally survive reconnect.
	}

	public void clearAll() {
		// Persistent selections are owned by player attachments.
	}

	public boolean radialSelector() {
		return crystalPath.equals("rainbow_crystal");
	}

	public List<String> modeIds() {
		return modes.stream().map(ability -> ability.id().getPath()).toList();
	}

	public int selectedIndex(ServerPlayer player) {
		return PlayerPowers.get(player).selectedCrystalMode(crystalPath, modes.size());
	}

	public boolean selectMode(ServerPlayer player, int selected) {
		if (!CrystalSelectorRules.validSelection(modes.size(), selected)) return false;
		PlayerPowers.get(player).setSelectedCrystalMode(crystalPath, selected);
		PowerMessages.overlay(player, Component.translatable(
				"crystal.powers.mode_selected", modes.get(selected).name()));
		return true;
	}
}
