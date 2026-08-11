package com.powers.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.UUID;

/** Small owner-keyed save record; runtime entity references never enter it. */
public record ShadowCompanionData(int schemaVersion, int energy, ShadowStance stance,
		boolean revealed, String bodyId, long recallReadyAt) {
	public static final int CURRENT_SCHEMA_VERSION = 1;
	public static final Codec<ShadowCompanionData> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Codec.INT.optionalFieldOf("schema", CURRENT_SCHEMA_VERSION)
							.forGetter(ShadowCompanionData::schemaVersion),
					Codec.INT.optionalFieldOf("energy", ShadowCompanionRules.MAX_ENERGY)
							.forGetter(ShadowCompanionData::energy),
					ShadowStance.CODEC.optionalFieldOf("stance", ShadowStance.FOLLOW)
							.forGetter(ShadowCompanionData::stance),
					Codec.BOOL.optionalFieldOf("revealed", false)
							.forGetter(ShadowCompanionData::revealed),
					Codec.STRING.optionalFieldOf("body_id", "")
							.forGetter(ShadowCompanionData::bodyId),
					Codec.LONG.optionalFieldOf("recall_ready_at", 0L)
							.forGetter(ShadowCompanionData::recallReadyAt))
					.apply(instance, ShadowCompanionData::new));

	public ShadowCompanionData {
		schemaVersion = CURRENT_SCHEMA_VERSION;
		energy = ShadowCompanionRules.energy(energy);
		stance = stance == null ? ShadowStance.FOLLOW : stance;
		bodyId = sanitizeUuid(bodyId);
		recallReadyAt = Math.max(0L, recallReadyAt);
	}

	public static ShadowCompanionData defaults() {
		return new ShadowCompanionData(CURRENT_SCHEMA_VERSION, ShadowCompanionRules.MAX_ENERGY,
				ShadowStance.FOLLOW, false, "", 0L);
	}

	public Optional<UUID> bodyUuid() {
		if (bodyId.isEmpty()) return Optional.empty();
		return Optional.of(UUID.fromString(bodyId));
	}

	public ShadowCompanionData withEnergy(int value) {
		return new ShadowCompanionData(schemaVersion, value, stance, revealed, bodyId, recallReadyAt);
	}

	public ShadowCompanionData withStance(ShadowStance value) {
		return new ShadowCompanionData(schemaVersion, energy, value, revealed, bodyId, recallReadyAt);
	}

	public ShadowCompanionData withRevealed(boolean value) {
		return new ShadowCompanionData(schemaVersion, energy, stance, value, bodyId, recallReadyAt);
	}

	public ShadowCompanionData withBodyId(UUID value) {
		return new ShadowCompanionData(schemaVersion, energy, stance, revealed,
				value == null ? "" : value.toString(), recallReadyAt);
	}

	public ShadowCompanionData withoutBody() {
		return withBodyId(null);
	}

	public ShadowCompanionData withRecallReadyAt(long value) {
		return new ShadowCompanionData(schemaVersion, energy, stance, revealed, bodyId, value);
	}

	private static String sanitizeUuid(String value) {
		if (value == null || value.isBlank()) return "";
		try {
			return UUID.fromString(value).toString();
		} catch (IllegalArgumentException ignored) {
			return "";
		}
	}
}
