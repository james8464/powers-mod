package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Separates innate slot replacement from artifact-owned toggle lifecycles. */
public final class PowerToggleLifecycle {
	private PowerToggleLifecycle() {
	}

	/** Deactivates canonical innate toggles and returns artifact keys unchanged. */
	public static List<String> deactivateInnate(ServerPlayer player,
			PlayerPowers.PlayerPowersData data, Iterable<String> activeKeys) {
		List<String> retainedArtifactToggles = new ArrayList<>();
		for (String key : activeKeys) {
			Power power = PowerRegistry.get(key);
			if (power != null && power.ability().isToggle()) {
				power.ability().activateToggleOff(player, data);
			} else if (power == null && ToggleKeyRules.isArtifactOwned(key)) {
				retainedArtifactToggles.add(key);
			}
		}
		return List.copyOf(retainedArtifactToggles);
	}
}
