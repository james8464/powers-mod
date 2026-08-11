package com.powers.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.companion.combat.ShadowRequestRange;

import java.util.Optional;
import java.util.UUID;

/** Small owner-keyed save record; runtime entity references never enter it. */
public record ShadowCompanionData(int schemaVersion, int energy, ShadowStance stance,
		boolean revealed, String bodyId, long recallReadyAt, ShadowConversationMemory memory,
		String combatRange, String learnedCombat) {
	public static final int CURRENT_SCHEMA_VERSION = 3;
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
							.forGetter(ShadowCompanionData::recallReadyAt),
					ShadowConversationMemory.CODEC.optionalFieldOf("memory",
							ShadowConversationMemory.empty()).forGetter(ShadowCompanionData::memory),
					Codec.STRING.optionalFieldOf("combat_range", "auto")
							.forGetter(ShadowCompanionData::combatRange),
					Codec.STRING.optionalFieldOf("learned_combat", "")
							.forGetter(ShadowCompanionData::learnedCombat))
					.apply(instance, ShadowCompanionData::new));

	public ShadowCompanionData {
		schemaVersion = CURRENT_SCHEMA_VERSION;
		energy = ShadowCompanionRules.energy(energy);
		stance = stance == null ? ShadowStance.FOLLOW : stance;
		bodyId = sanitizeUuid(bodyId);
		recallReadyAt = Math.max(0L, recallReadyAt);
		memory = memory == null ? ShadowConversationMemory.empty() : memory;
		combatRange = parseRange(combatRange).name().toLowerCase(java.util.Locale.ROOT);
		learnedCombat = learnedCombat == null || learnedCombat.length() > 32_768 ? "" : learnedCombat;
	}

	public static ShadowCompanionData defaults() {
		return new ShadowCompanionData(CURRENT_SCHEMA_VERSION, ShadowCompanionRules.MAX_ENERGY,
				ShadowStance.FOLLOW, false, "", 0L, ShadowConversationMemory.empty(), "auto", "");
	}

	public Optional<UUID> bodyUuid() {
		if (bodyId.isEmpty()) return Optional.empty();
		return Optional.of(UUID.fromString(bodyId));
	}

	public ShadowCompanionData withEnergy(int value) {
		return copy(value, stance, revealed, bodyId, recallReadyAt, memory, combatRange, learnedCombat);
	}

	public ShadowCompanionData withStance(ShadowStance value) {
		return copy(energy, value, revealed, bodyId, recallReadyAt, memory, combatRange, learnedCombat);
	}

	public ShadowCompanionData withRevealed(boolean value) {
		return copy(energy, stance, value, bodyId, recallReadyAt, memory, combatRange, learnedCombat);
	}

	public ShadowCompanionData withBodyId(UUID value) {
		return copy(energy, stance, revealed, value == null ? "" : value.toString(),
				recallReadyAt, memory, combatRange, learnedCombat);
	}

	public ShadowCompanionData withoutBody() {
		return withBodyId(null);
	}

	public ShadowCompanionData withRecallReadyAt(long value) {
		return copy(energy, stance, revealed, bodyId, value, memory, combatRange, learnedCombat);
	}

	public ShadowCompanionData withMemory(ShadowConversationMemory value) {
		return copy(energy, stance, revealed, bodyId, recallReadyAt, value,
				combatRange, learnedCombat);
	}

	public ShadowCompanionData withCombatRange(ShadowRequestRange value) {
		return copy(energy, stance, revealed, bodyId, recallReadyAt, memory,
				(value == null ? ShadowRequestRange.AUTO : value).name().toLowerCase(java.util.Locale.ROOT),
				learnedCombat);
	}

	public ShadowCompanionData withLearnedCombat(String value) {
		return copy(energy, stance, revealed, bodyId, recallReadyAt, memory, combatRange, value);
	}

	public ShadowRequestRange preferredCombatRange() {
		return parseRange(combatRange);
	}

	private ShadowCompanionData copy(int nextEnergy, ShadowStance nextStance,
			boolean nextRevealed, String nextBody, long nextRecall,
			ShadowConversationMemory nextMemory, String nextRange, String nextLearned) {
		return new ShadowCompanionData(schemaVersion, nextEnergy, nextStance, nextRevealed,
				nextBody, nextRecall, nextMemory, nextRange, nextLearned);
	}

	private static ShadowRequestRange parseRange(String value) {
		try {
			return ShadowRequestRange.valueOf(value == null ? "AUTO"
					: value.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return ShadowRequestRange.AUTO;
		}
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
