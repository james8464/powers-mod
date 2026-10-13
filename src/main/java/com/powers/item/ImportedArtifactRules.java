package com.powers.item;

import com.powers.ImportedItemRules;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Deterministic family classification for previously decorative imported items. */
public final class ImportedArtifactRules {
	public enum HeartSpecialization { NONE, LIVING, WILDWOOD, GHOUL, CLOCKWORK, BLOOD_WARD }
	public enum AttunementSchool {
		NONE("tooltip.powers.attunement.school.none"),
		WARDING("tooltip.powers.attunement.school.warding"),
		VERDANCY("tooltip.powers.attunement.school.verdancy"),
		CHANNELING("tooltip.powers.attunement.school.channeling"),
		RECOVERY("tooltip.powers.attunement.school.recovery");

		private final String translationKey;

		AttunementSchool(String translationKey) {
			this.translationKey = translationKey;
		}

		public String translationKey() {
			return translationKey;
		}
	}

	public enum AttunementEffect {
		NONE("tooltip.powers.attunement.effect.none"),
		WOUNDED_RESISTANCE("tooltip.powers.attunement.effect.wounded_resistance"),
		WOUNDED_REGENERATION("tooltip.powers.attunement.effect.wounded_regeneration"),
		CHANNEL_RECOVERY("tooltip.powers.attunement.effect.channel_recovery"),
		LOW_ENERGY_RECOVERY("tooltip.powers.attunement.effect.low_energy_recovery");

		private final String translationKey;

		AttunementEffect(String translationKey) {
			this.translationKey = translationKey;
		}

		public String translationKey() {
			return translationKey;
		}
	}

	public record AttunementProfile(String texture, AttunementSchool school,
			AttunementEffect effect, int magnitude) {
		private static final AttunementProfile NONE = new AttunementProfile(
				"", AttunementSchool.NONE, AttunementEffect.NONE, 0);

		public boolean present() {
			return school != AttunementSchool.NONE && effect != AttunementEffect.NONE && magnitude > 0;
		}
	}

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

	/** One visible, save-stable school and bounded situational modifier per ring or amulet. */
	public static AttunementProfile attunementProfile(String texture) {
		if (texture == null) return AttunementProfile.NONE;
		return switch (texture) {
			case "artifact_diamond_ring" -> new AttunementProfile(texture,
					AttunementSchool.WARDING, AttunementEffect.WOUNDED_RESISTANCE, 2);
			case "artifact_emerald_ring" -> new AttunementProfile(texture,
					AttunementSchool.VERDANCY, AttunementEffect.WOUNDED_REGENERATION, 1);
			case "artifact_amulet" -> new AttunementProfile(texture,
					AttunementSchool.CHANNELING, AttunementEffect.CHANNEL_RECOVERY, 2);
			case "artifact_plain_copper_ring" -> new AttunementProfile(texture,
					AttunementSchool.RECOVERY, AttunementEffect.LOW_ENERGY_RECOVERY, 2);
			case "artifact_corroded_copper_ring" -> new AttunementProfile(texture,
					AttunementSchool.RECOVERY, AttunementEffect.LOW_ENERGY_RECOVERY, 1);
			default -> AttunementProfile.NONE;
		};
	}

	/** Duplicate copies and weaker relics in the same school never stack. */
	public static Map<AttunementSchool, AttunementProfile> strongestAttunements(
			Collection<String> textures) {
		EnumMap<AttunementSchool, AttunementProfile> result = new EnumMap<>(AttunementSchool.class);
		if (textures == null) return Map.of();
		for (String texture : textures) {
			AttunementProfile candidate = attunementProfile(texture);
			if (!candidate.present()) continue;
			AttunementProfile current = result.get(candidate.school());
			if (current == null || candidate.magnitude() > current.magnitude()
					|| candidate.magnitude() == current.magnitude()
					&& candidate.texture().compareTo(current.texture()) < 0) {
				result.put(candidate.school(), candidate);
			}
		}
		return Map.copyOf(result);
	}

	public static int attunementEnergy(Map<AttunementSchool, AttunementProfile> strongest,
			boolean lowEnergy, boolean channeling) {
		if (strongest == null || strongest.isEmpty()) return 0;
		int total = 0;
		for (AttunementProfile profile : strongest.values()) {
			total += switch (profile.effect()) {
				case LOW_ENERGY_RECOVERY -> lowEnergy ? profile.magnitude() : 0;
				case CHANNEL_RECOVERY -> channeling ? profile.magnitude() : 0;
				default -> 0;
			};
		}
		return Math.min(6, total);
	}

	public static int cappedPassiveEnergy(int attunementEnergy, boolean celestialFocus,
			boolean ghoulHeart) {
		return Math.min(6, Math.max(0, attunementEnergy)
				+ (celestialFocus ? 1 : 0) + (ghoulHeart ? 1 : 0));
	}

	public static boolean wounded(float health, float maximumHealth) {
		return maximumHealth > 0.0F && health <= maximumHealth * 0.5F;
	}

	public static boolean lowEnergy(int energy, int capacity) {
		return capacity > 0 && energy <= capacity / 4;
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
