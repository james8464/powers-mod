package com.powers.item;

import com.powers.ImportedItemRules;

/** Deterministic family classification for previously decorative imported items. */
public final class ImportedArtifactRules {
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

}
