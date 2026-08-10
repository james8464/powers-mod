package com.powers.item;

import com.powers.ImportedItemRules;

/** Deterministic family classification for previously decorative imported items. */
public final class ImportedArtifactRules {
	private ImportedArtifactRules() {
	}

	public static ImportedArtifactKind kind(String texture) {
		if (texture == null || texture.isBlank() || texture.startsWith("food_")
				|| texture.startsWith("book_grimoire") || ImportedItemRules.isLegacyAssetLayer(texture)
				|| texture.contains("runestone")) return ImportedArtifactKind.NONE;
		if (texture.contains("ring") || texture.contains("amulet")) {
			return ImportedArtifactKind.ATTUNEMENT;
		}
		if (texture.contains("soulstone") || texture.contains("soulmatrix")) {
			return ImportedArtifactKind.SOUL_VESSEL;
		}
		if (texture.contains("ritualdagger")) return ImportedArtifactKind.RITUAL_CATALYST;
		if (texture.contains("heart")) return ImportedArtifactKind.HEART_RELIC;
		if (texture.contains("philosopherstone")) return ImportedArtifactKind.TRANSMUTER;
		if (texture.contains("lodestone") || texture.startsWith("device_miniportal")) {
			return ImportedArtifactKind.TRAVEL_RELIC;
		}
		if (texture.contains("flute")) return ImportedArtifactKind.COMMAND_RELIC;
		if (texture.startsWith("magic_essence_") || texture.startsWith("blood_salts")
				|| texture.contains("jewel") || texture.contains("ammolite")
				|| texture.contains("blackpearl") || texture.contains("bloodstone")
				|| texture.contains("malignember") || texture.contains("oddstone")
				|| texture.contains("star") || texture.contains("figurine")) {
			return ImportedArtifactKind.ARCANE_CATALYST;
		}
		if (texture.startsWith("artifact_") || texture.startsWith("book_")
				|| texture.startsWith("device_")) return ImportedArtifactKind.LORE_RELIC;
		return ImportedArtifactKind.NONE;
	}

	/** Staggered passive energy rate for an attuned inventory, capped for servers. */
	public static int attunementEnergy(int relicCount) {
		return Math.clamp(relicCount, 0, 3) * 2;
	}

	/** Soul vessel strength is inferred from the authored size and matrix tiers. */
	public static int soulDrain(String texture) {
		if (texture == null) return 0;
		if (texture.contains("soulmatrix")) return 60;
		if (texture.contains("large")) return 36;
		if (texture.contains("medium")) return 24;
		return texture.contains("soulstone") ? 14 : 0;
	}
}
