package com.powers.client;

import com.powers.network.PowerStatePayload;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;

import java.util.List;

/** the client's mirror of your loadout, toggles and energy, kept in sync from the server so the hud can draw it */
public final class ClientPowerState {
	private static List<String> powerIds = List.of();
	private static List<String> activeToggles = List.of();
	private static List<Integer> cooldownTicks = List.of();
	private static List<Integer> cooldownMaximums = List.of();
	private static List<Integer> reactivationTicks = List.of();
	private static int energy;
	private static int energyCapacity;
	private static boolean canSeeDarkRealm;
	private static boolean darkness;
	private static boolean projection;
	private static int elementalPhase;
	private static List<String> rankNodes = List.of();
	private static String rankFocus = "";
	private static int rankDepth;
	public static int markingSlot = -1;
	public static int markingTicks;

	private ClientPowerState() {
	}

	public static void update(PowerStatePayload payload) {
		powerIds = payload.powerIds();
		activeToggles = payload.activeToggles();
		cooldownTicks = payload.cooldownTicks();
		cooldownMaximums = payload.cooldownMaximums();
		reactivationTicks = payload.reactivationTicks();
		energy = payload.energy();
		energyCapacity = payload.energyCapacity();
		canSeeDarkRealm = payload.canSeeDarkRealm();
		darkness = payload.darkness();
		projection = payload.projection();
		elementalPhase = payload.elementalPhase();
		rankNodes = List.copyOf(payload.rankNodes());
		rankFocus = payload.rankFocus();
		rankDepth = payload.rankDepth();
	}

	// wipe everything on disconnect so the hud shows nothing instead of stale powers
	public static void reset() {
		powerIds = List.of();
		activeToggles = List.of();
		cooldownTicks = List.of();
		cooldownMaximums = List.of();
		reactivationTicks = List.of();
		energy = 0;
		energyCapacity = 0;
		canSeeDarkRealm = false;
		darkness = false;
		projection = false;
		elementalPhase = 0;
		rankNodes = List.of();
		rankFocus = "";
		rankDepth = 0;
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

	public static int cooldownTicks(int slot) {
		return slot >= 0 && slot < cooldownTicks.size() ? Math.max(0, cooldownTicks.get(slot)) : 0;
	}

	/** Returns the rank-adjusted cooldown that armed the displayed ring. */
	public static int cooldownMaximum(int slot) {
		return slot >= 0 && slot < cooldownMaximums.size() ? Math.max(0, cooldownMaximums.get(slot)) : 0;
	}

	/** Remaining server-authorized follow-up time for a slot, or zero. */
	public static int reactivationTicks(int slot) {
		return slot >= 0 && slot < reactivationTicks.size()
				? Math.max(0, reactivationTicks.get(slot)) : 0;
	}

	public static void tickCooldowns() {
		if (cooldownTicks.isEmpty()) return;
		java.util.ArrayList<Integer> updated = new java.util.ArrayList<>(cooldownTicks.size());
		for (int ticks : cooldownTicks) updated.add(Math.max(0, ticks - 1));
		cooldownTicks = List.copyOf(updated);
		if (!reactivationTicks.isEmpty()) {
			java.util.ArrayList<Integer> reactivations = new java.util.ArrayList<>(reactivationTicks.size());
			for (int ticks : reactivationTicks) reactivations.add(Math.max(0, ticks - 1));
			reactivationTicks = List.copyOf(reactivations);
		}
	}

	public static boolean darkness() {
		return darkness;
	}

	public static boolean projection() {
		return projection;
	}

	/** Returns the server-authoritative Elemental Blast phase. */
	public static int elementalPhase() {
		return elementalPhase;
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

	public static List<String> rankNodes() {
		return rankNodes;
	}

	public static String rankFocus() {
		return rankFocus;
	}

	public static int rankDepth() {
		return rankDepth;
	}
}
