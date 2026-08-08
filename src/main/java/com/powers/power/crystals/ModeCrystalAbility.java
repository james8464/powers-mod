package com.powers.power.crystals;

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
	private final CrystalModeState state = new CrystalModeState();

	public ModeCrystalAbility(String crystalPath, List<Ability> modes) {
		super(PowersMod.id(crystalPath + "_convergence"),
				Component.translatable("ability.powers." + crystalPath + "_convergence"), 0, false);
		if (modes.isEmpty()) throw new IllegalArgumentException("A crystal needs at least one ability");
		this.modes = List.copyOf(modes);
	}

	private Ability selected(ServerPlayer player) {
		return modes.get(state.current(player.getUUID(), modes.size()));
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
			Ability next = modes.get(state.advance(player.getUUID(), modes.size()));
			player.sendSystemMessage(Component.translatable("crystal.powers.mode_selected", next.name()));
			return true;
		}
		// Track the underlying ability as well as this crystal surface so a
		// player cannot bypass a rare power's cooldown by swapping artifacts.
		if (!ActivationCooldowns.isReady(player, selected)) {
			PowerMessages.send(player, "ability.powers.cooldown", 4,
					String.valueOf((ActivationCooldowns.remainingTicks(player, selected) + 19) / 20));
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
		state.clear(player);
	}

	public void clearAll() {
		state.clearAll();
	}
}
