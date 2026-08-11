package com.powers.item;

import com.powers.ImportedItemRules;

/** Single deterministic catalogue for imported item purpose and UI documentation. */
public final class ArtifactRoleCatalogue {
	private ArtifactRoleCatalogue() {
	}

	public static ArtifactRole role(String texture) {
		if (texture == null || texture.isBlank() || texture.startsWith("food_")
				|| ImportedItemRules.isHiddenCompatibilityItem(texture)) return ArtifactRole.NONE;
		if (texture.startsWith("imported_")) texture = texture.substring("imported_".length());
		if (texture.startsWith("book_grimoire")) return ArtifactRole.GRIMOIRE;
		if (texture.startsWith("book_")) return ArtifactRole.LORE_FRAGMENT;
		if (texture.contains("runestone") || texture.contains("rune")) return ArtifactRole.ENERGY_RUNE;
		if (texture.startsWith("magic_essence_") || texture.startsWith("blood_salts")) {
			return ArtifactRole.ARCANE_ENERGY_DUST;
		}
		if (texture.contains("soulstone") || texture.contains("soulmatrix")) {
			return ArtifactRole.ENERGY_RESERVOIR;
		}
		if (texture.contains("ritualdagger")) return ArtifactRole.HEALTH_TO_ENERGY;
		if (texture.contains("ring") || texture.contains("amulet")) return ArtifactRole.ATTUNEMENT;
		if (texture.contains("heart")) return ArtifactRole.VITALITY_RELIC;
		if (texture.contains("philosopherstone")) return ArtifactRole.TRANSMUTER;
		if (texture.contains("lodestone") || texture.startsWith("device_miniportal")) {
			return ArtifactRole.TRAVEL_RELIC;
		}
		if (texture.contains("flute")) return ArtifactRole.CREATURE_COMMAND;
		if (texture.contains("emperyeanjewel")) return ArtifactRole.CONSENT_OVERRIDE;
		if (texture.contains("malignember")) return ArtifactRole.DESTRUCTIVE_FOCUS;
		if (texture.contains("star") || texture.contains("ammolite")) return ArtifactRole.CELESTIAL_FOCUS;
		if (texture.contains("bloodstone")) return ArtifactRole.VITALITY_RELIC;
		if (texture.contains("bowl") || texture.contains("smallpot") || texture.contains("dripping_orb")) {
			return ArtifactRole.RITUAL_CONTAINER;
		}
		if (texture.contains("fossil")) return ArtifactRole.ARCHAEOLOGY;
		if (texture.startsWith("artifact_")) return ArtifactRole.ARCANE_CATALYST;
		return ArtifactRole.NONE;
	}
}
