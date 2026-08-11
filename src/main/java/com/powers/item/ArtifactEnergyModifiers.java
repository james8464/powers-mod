package com.powers.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** Explicit, non-rank energy modifiers granted by carried imported artifacts. */
public final class ArtifactEnergyModifiers {
	public record Quote(boolean eligible, int cost, int saved) {
	}

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
		return quote(hasMalignember, actionId, authoredCost).cost();
	}

	public static Quote quote(boolean hasMalignember, String actionId, int authoredCost) {
		int authored = Math.max(0, authoredCost);
		boolean eligible = DESTRUCTIVE_ACTIONS.contains(actionId);
		int cost = !hasMalignember || !eligible || authored == 0
				? authored : Math.max(1, (int) Math.ceil(authored * 0.8));
		return new Quote(eligible, cost, authored - cost);
	}

	/** Stable action identifiers used by both payment and player-facing presentation. */
	public static Set<String> eligibleActionIds() {
		return DESTRUCTIVE_ACTIONS;
	}

	public static boolean carries(ServerPlayer player, ArtifactRole role) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.getItem() instanceof ImportedArtifactItem relic && relic.role() == role) return true;
		}
		return false;
	}
}
