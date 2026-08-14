package com.powers.player;

import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactSelectionMigration;
import com.powers.item.artifact.ArtifactFavouriteRules;
import com.powers.item.artifact.ArtifactSelectionRules;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Persistent, server-validated selection for each opposed mythic artifact. */
public final class ArtifactSelectionState {
	private ArtifactSelectionState() {
	}

	/** Reads the persisted selection without migration or attachment writes. */
	public static String peekSelected(ServerPlayer player, ArtifactAlignment alignment) {
		String fallback = "innate/lightning_strike";
		return alignment == ArtifactAlignment.DARKNESS
				? player.getAttachedOrElse(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, fallback)
				: player.getAttachedOrElse(PlayerPowerAttachments.HEAVENLY_PARTISAN_SELECTION, fallback);
	}

	public static String selected(ServerPlayer player, ArtifactAlignment alignment) {
		String stored = peekSelected(player, alignment);
		int rank = alignment == ArtifactAlignment.DARKNESS
				? PlayerPowers.get(player).darknessLevel() : PlayerPowers.get(player).skillLevel();
		String migrated = ArtifactSelectionMigration.migrate(alignment, stored, rank);
		if (!migrated.equals(stored)) select(player, alignment, migrated);
		return migrated;
	}

	public static boolean select(ServerPlayer player, ArtifactAlignment alignment, String key) {
		if (key == null || key.length() > 96) return false;
		int rank = alignment == ArtifactAlignment.DARKNESS
				? PlayerPowers.get(player).darknessLevel() : PlayerPowers.get(player).skillLevel();
		String canonical = ArtifactSelectionMigration.migrate(alignment, key, rank);
		if (ArtifactActionCatalogue.find(alignment, canonical) == null) return false;
		if (alignment == ArtifactAlignment.DARKNESS) {
			player.setAttached(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, canonical);
		} else {
			player.setAttached(PlayerPowerAttachments.HEAVENLY_PARTISAN_SELECTION, canonical);
		}
		return true;
	}

	/** Returns and save-migrates the authoritative eight-slot combat loadout. */
	public static List<String> favourites(ServerPlayer player, ArtifactAlignment alignment) {
		List<String> stored = alignment == ArtifactAlignment.DARKNESS
				? player.getAttachedOrElse(PlayerPowerAttachments.SHADOW_SWORD_FAVOURITES, List.of())
				: player.getAttachedOrElse(PlayerPowerAttachments.HEAVENLY_PARTISAN_FAVOURITES, List.of());
		int rank = alignment == ArtifactAlignment.DARKNESS
				? PlayerPowers.get(player).darknessLevel() : PlayerPowers.get(player).skillLevel();
		List<String> reconciled = ArtifactFavouriteRules.reconcile(stored,
				ArtifactActionCatalogue.forAlignment(alignment), alignment, rank, selected(player, alignment));
		if (!reconciled.equals(stored)) setFavourites(player, alignment, reconciled);
		return reconciled;
	}

	/** Server-validates and persists one direct library-to-wheel binding. */
	public static boolean bindFavourite(ServerPlayer player, ArtifactAlignment alignment,
			int slot, String key) {
		int rank = alignment == ArtifactAlignment.DARKNESS
				? PlayerPowers.get(player).darknessLevel() : PlayerPowers.get(player).skillLevel();
		String canonical = ArtifactSelectionMigration.migrate(alignment, key, rank);
		var definition = ArtifactActionCatalogue.find(alignment, canonical);
		if (slot < 0 || slot >= ArtifactFavouriteRules.SLOT_COUNT || definition == null
				|| !ArtifactSelectionRules.maySelect(definition, alignment, rank)) return false;
		setFavourites(player, alignment,
				ArtifactFavouriteRules.assign(favourites(player, alignment), slot, canonical));
		return true;
	}

	private static void setFavourites(ServerPlayer player, ArtifactAlignment alignment,
			List<String> favourites) {
		if (alignment == ArtifactAlignment.DARKNESS) {
			player.setAttached(PlayerPowerAttachments.SHADOW_SWORD_FAVOURITES, List.copyOf(favourites));
		} else {
			player.setAttached(PlayerPowerAttachments.HEAVENLY_PARTISAN_FAVOURITES, List.copyOf(favourites));
		}
	}
}
