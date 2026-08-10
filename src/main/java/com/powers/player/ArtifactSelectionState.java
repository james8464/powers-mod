package com.powers.player;

import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.server.level.ServerPlayer;

/** Persistent, server-validated selection for each opposed mythic artifact. */
public final class ArtifactSelectionState {
	private ArtifactSelectionState() {
	}

	public static String selected(ServerPlayer player, ArtifactAlignment alignment) {
		String fallback = "innate/lightning_strike";
		String stored = alignment == ArtifactAlignment.DARKNESS
				? player.getAttachedOrElse(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, fallback)
				: player.getAttachedOrElse(PlayerPowerAttachments.HEAVENLY_PARTISAN_SELECTION, fallback);
		return ArtifactActionCatalogue.find(alignment, stored) == null ? fallback : stored;
	}

	public static boolean select(ServerPlayer player, ArtifactAlignment alignment, String key) {
		if (key == null || key.length() > 96 || ArtifactActionCatalogue.find(alignment, key) == null) return false;
		if (alignment == ArtifactAlignment.DARKNESS) {
			player.setAttached(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, key);
		} else {
			player.setAttached(PlayerPowerAttachments.HEAVENLY_PARTISAN_SELECTION, key);
		}
		return true;
	}
}
