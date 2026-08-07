package com.powers.client;

import com.powers.network.PowersPackets;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;

import java.util.List;

public final class ClientPowerState {
	private static List<String> powerIds = List.of();
	private static List<String> activeToggles = List.of();
	private static int energy;
	private static int energyCapacity;
	public static int markingSlot = -1;
	public static int markingTicks;

	private ClientPowerState() {
	}

	public static void update(PowersPackets.PowerStatePayload payload) {
		powerIds = payload.powerIds();
		activeToggles = payload.activeToggles();
		energy = payload.energy();
		energyCapacity = payload.energyCapacity();
	}

	public static void reset() {
		powerIds = List.of();
		activeToggles = List.of();
		energy = 0;
		energyCapacity = 0;
		markingSlot = -1;
		markingTicks = 0;
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

	public static int energyCapacity() {
		return energyCapacity > 0 ? energyCapacity : com.powers.power.PowerEnergy.BASE_MAX;
	}

	public static boolean isMarking() {
		return markingSlot >= 0;
	}
}
