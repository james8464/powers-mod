package com.powers.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** Explicit, non-rank energy modifiers granted by carried imported artifacts. */
public final class ArtifactEnergyModifiers {
	private static final Set<String> DESTRUCTIVE_ACTIONS = Set.of(
			"lightning_strike", "fireball", "energy_beam", "void_beam",
			"thunderclap", "breezy_bash", "gravity_displacement", "starfall",
			"energy_drain", "celestial_ruin", "ward_breaking_ritual");

	private ArtifactEnergyModifiers() {
	}

	public static int forPlayer(ServerPlayer player, String actionId, int authoredCost) {
		return activationCost(carries(player, ArtifactRole.DESTRUCTIVE_FOCUS), actionId, authoredCost);
	}

	public static int activationCost(boolean hasMalignember, String actionId, int authoredCost) {
		int cost = Math.max(0, authoredCost);
		if (!hasMalignember || !DESTRUCTIVE_ACTIONS.contains(actionId) || cost == 0) return cost;
		return Math.max(1, (int) Math.ceil(cost * 0.8));
	}

	public static boolean carries(ServerPlayer player, ArtifactRole role) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.getItem() instanceof ImportedArtifactItem relic && relic.role() == role) return true;
		}
		return false;
	}
}
