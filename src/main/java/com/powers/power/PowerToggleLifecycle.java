package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Separates innate slot replacement from artifact-owned toggle lifecycles. */
public final class PowerToggleLifecycle {
	private PowerToggleLifecycle() {
	}

	/** Deactivates canonical innate toggles and returns artifact keys unchanged. */
	public static List<String> deactivateInnate(ServerPlayer player,
			PlayerPowers.PlayerPowersData data, Iterable<String> activeKeys) {
		return reconcileInnate(player, data, activeKeys, List.of());
	}

	/** Deactivates only innate toggles whose logical power left the new loadout. */
	public static List<String> reconcileInnate(ServerPlayer player,
			PlayerPowers.PlayerPowersData data, Iterable<String> activeKeys,
			Iterable<String> retainedPowerIds) {
		Set<String> retainedIds = new HashSet<>();
		for (String id : retainedPowerIds) {
			Power power = PowerRegistry.get(id);
			if (power != null) retainedIds.add(power.id().toString());
		}
		List<String> retainedToggles = new ArrayList<>();
		for (String key : activeKeys) {
			Power power = PowerRegistry.get(key);
			if (power != null && power.ability().isToggle()) {
				if (retainedIds.contains(power.id().toString())) retainedToggles.add(key);
				else power.ability().activateToggleOff(player, data);
			} else if (power == null && ToggleKeyRules.isArtifactOwned(key)) {
				retainedToggles.add(key);
			}
		}
		return List.copyOf(retainedToggles);
	}
}
