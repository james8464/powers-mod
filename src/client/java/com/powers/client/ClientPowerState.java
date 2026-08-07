package com.powers.client;

import com.powers.network.PowersPackets;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;

import java.util.List;

/** the client's mirror of your loadout, toggles and energy, kept in sync from the server so the hud can draw it */
public final class ClientPowerState {
	private static List<String> powerIds = List.of();
	private static List<String> activeToggles = List.of();
	private static int energy;
	private static int energyCapacity;
	private static boolean canSeeDarkRealm;
	public static int markingSlot = -1;
	public static int markingTicks;

	private ClientPowerState() {
	}

	public static void update(PowersPackets.PowerStatePayload payload) {
		powerIds = payload.powerIds();
		activeToggles = payload.activeToggles();
		energy = payload.energy();
		energyCapacity = payload.energyCapacity();
		canSeeDarkRealm = payload.canSeeDarkRealm();
	}

	// wipe everything on disconnect so the hud shows nothing instead of stale powers
	public static void reset() {
		powerIds = List.of();
		activeToggles = List.of();
		energy = 0;
		energyCapacity = 0;
		canSeeDarkRealm = false;
		markingSlot = -1;
		markingTicks = 0;
	}

	// the dark realm only shows as a destination for the darkness-marked at rank 5+
	public static boolean canSeeDarkRealm() {
		return canSeeDarkRealm;
	}

	public static Power getPower(int slot) {
		if (slot < 0 || slot >= powerIds.size()) return null;
		return PowerRegistry.get(powerIds.get(slot));
	}

	public static boolean isToggleActive(String powerId) {
		return activeToggles.contains(powerId);
	}

	public static int energy() {
		return energy;
	}

	// fall back to the base max until the first payload arrives, so the meter has a scale to draw against
	public static int energyCapacity() {
		return energyCapacity > 0 ? energyCapacity : com.powers.power.PowerEnergy.BASE_MAX;
	}

	public static boolean isMarking() {
		return markingSlot >= 0;
	}
}
