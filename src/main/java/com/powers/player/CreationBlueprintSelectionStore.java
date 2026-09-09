package com.powers.player;

import com.powers.power.crystals.CreationBlueprintSelection;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Persistent stable-key selection for the Orange Crystal blueprint catalogue. */
public final class CreationBlueprintSelectionStore {
	private static final String DEFAULT = "powers:orange_chamber";
	private CreationBlueprintSelectionStore() { }

	public static String selected(ServerPlayer player, List<String> available) {
		String stored = player.getAttachedOrElse(PlayerPowerAttachments.CREATION_BLUEPRINT_SELECTION, "");
		if (available.contains(stored)) return stored;
		if (available.contains(DEFAULT)) return DEFAULT;
		return available.isEmpty() ? "" : available.getFirst();
	}

	public static String advance(ServerPlayer player, List<String> available) {
		String next = CreationBlueprintSelection.next(selected(player, available), available);
		player.setAttached(PlayerPowerAttachments.CREATION_BLUEPRINT_SELECTION, next);
		return next;
	}
}
