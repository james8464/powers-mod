package com.powers.item;

import com.powers.ImportedItemRules;

/** Deterministic family classification for previously decorative imported items. */
public final class ImportedArtifactRules {
	public enum HeartSpecialization { NONE, LIVING, WILDWOOD, GHOUL, CLOCKWORK, BLOOD_WARD }

	private ImportedArtifactRules() {
	}

	public static ImportedArtifactKind kind(String texture) {
		return switch (ArtifactRoleCatalogue.role(texture)) {
			case ATTUNEMENT -> ImportedArtifactKind.ATTUNEMENT;
			case ENERGY_RESERVOIR -> ImportedArtifactKind.ENERGY_RESERVOIR;
			case HEALTH_TO_ENERGY -> ImportedArtifactKind.RITUAL_CATALYST;
			case VITALITY_RELIC -> ImportedArtifactKind.HEART_RELIC;
			case TRANSMUTER -> ImportedArtifactKind.TRANSMUTER;
			case TRAVEL_RELIC -> ImportedArtifactKind.TRAVEL_RELIC;
			case CREATURE_COMMAND -> ImportedArtifactKind.COMMAND_RELIC;
			case CONSENT_OVERRIDE, DESTRUCTIVE_FOCUS, CELESTIAL_FOCUS,
					ARCANE_ENERGY_DUST, RITUAL_CONTAINER, ARCANE_CATALYST ->
					ImportedArtifactKind.ARCANE_CATALYST;
			case ARCHAEOLOGY, LORE_FRAGMENT -> ImportedArtifactKind.LORE_RELIC;
			default -> ImportedArtifactKind.NONE;
		};
	}

	/** Staggered passive energy rate for an attuned inventory, capped for servers. */
	public static int attunementEnergy(int relicCount) {
		return Math.clamp(relicCount, 0, 3) * 2;
	}

	/** Per-second restoration weight; the runtime still caps the combined inventory at six. */
	public static int attunementEnergy(String texture) {
		if (texture == null) return 0;
		return switch (texture) {
			case "artifact_diamond_ring" -> 3;
			case "artifact_emerald_ring", "artifact_amulet" -> 2;
			case "artifact_plain_copper_ring", "artifact_corroded_copper_ring" -> 1;
			default -> 0;
		};
	}

	/** Save-stable heart identity used by active and passive relic behavior. */
	public static HeartSpecialization heartSpecialization(String texture) {
		if (texture == null) return HeartSpecialization.NONE;
		return switch (texture) {
			case "artifact_beating_heart" -> HeartSpecialization.LIVING;
			case "artifact_woodheart" -> HeartSpecialization.WILDWOOD;
			case "artifact_ghoul_heart" -> HeartSpecialization.GHOUL;
			case "artifact_heart_mechanism" -> HeartSpecialization.CLOCKWORK;
			case "artifact_bloodstone" -> HeartSpecialization.BLOOD_WARD;
			default -> HeartSpecialization.NONE;
		};
	}

}
