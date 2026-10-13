package com.powers.allegiance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Objects;

/** Immutable, data-authored relationship policy shared by every aligned system. */
public final class AllegianceMatrix {
	public enum Faction { RADIANT, DARKNESS, NEUTRAL }

	public enum Role { PLAYER, SHADOW, ARTIFACT, FORCE, BOSS, FOOD, PARTY, AMETHYST }

	public enum Relationship {
		ALLY,
		HOSTILE,
		NEUTRALITY,
		SUPPRESSED;

		private static Relationship decode(String value) {
			try {
				return valueOf(value.toUpperCase(Locale.ROOT));
			} catch (RuntimeException error) {
				throw new IllegalArgumentException("Unknown allegiance relationship: " + value, error);
			}
		}
	}

	public record Profile(Faction faction, Role role) {
		public Profile {
			Objects.requireNonNull(faction, "faction");
			Objects.requireNonNull(role, "role");
		}
	}

	private final Relationship same;
	private final Relationship opposed;
	private final Relationship neutral;
	private final Relationship party;
	private final Relationship amethyst;

	private AllegianceMatrix(Relationship same, Relationship opposed,
			Relationship neutral, Relationship party, Relationship amethyst) {
		this.same = Objects.requireNonNull(same, "same");
		this.opposed = Objects.requireNonNull(opposed, "opposed");
		this.neutral = Objects.requireNonNull(neutral, "neutral");
		this.party = Objects.requireNonNull(party, "party");
		this.amethyst = Objects.requireNonNull(amethyst, "amethyst");
	}

	/** Built-in policy used before datapacks publish their first complete snapshot. */
	public static AllegianceMatrix defaults() {
		return new AllegianceMatrix(Relationship.ALLY, Relationship.HOSTILE,
				Relationship.NEUTRALITY, Relationship.ALLY, Relationship.SUPPRESSED);
	}

	/** Parses one complete policy document; partial policy cannot silently inherit stale rows. */
	public static AllegianceMatrix parse(JsonObject json) {
		Objects.requireNonNull(json, "json");
		return new AllegianceMatrix(read(json, "same"), read(json, "opposed"),
				read(json, "neutral"), read(json, "party"), read(json, "amethyst"));
	}

	/** Resolves one interaction without knowing or branching on the caller's subsystem. */
	public Relationship resolve(Profile source, Profile target, boolean sameParty) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(target, "target");
		if (source.role() == Role.AMETHYST || target.role() == Role.AMETHYST) return amethyst;
		if (sameParty) return party;
		if (source.faction() == Faction.NEUTRAL || target.faction() == Faction.NEUTRAL) return neutral;
		return source.faction() == target.faction() ? same : opposed;
	}

	private static Relationship read(JsonObject json, String key) {
		JsonElement element = json.get(key);
		if (element == null || !element.isJsonPrimitive()
				|| !element.getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Missing allegiance relationship: " + key);
		}
		return Relationship.decode(element.getAsString());
	}
}
