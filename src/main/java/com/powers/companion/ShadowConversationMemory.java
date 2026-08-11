package com.powers.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Immutable, owner-local conversation facts with strict count and text bounds. */
public record ShadowConversationMemory(List<Turn> turns, Map<ReferentType, String> referents,
		String recentFailure, int relationship, int influence) {
	public static final int MAX_TURNS = 24;
	private static final int MAX_LINE = 160;
	private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
	private static final Pattern SECRET = Pattern.compile(
			"(?i)\\b(?:token|password|secret|session|address)\\s*[=:]\\s*\\S+");

	public enum ReferentType { ENTITY, ITEM, POWER, TASK }
	public record Turn(String owner, String shadow) {
		public static final Codec<Turn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("owner", "").forGetter(Turn::owner),
				Codec.STRING.optionalFieldOf("shadow", "").forGetter(Turn::shadow))
				.apply(instance, Turn::new));

		public Turn {
			owner = bounded(owner);
			shadow = bounded(shadow);
		}
	}

	public static final Codec<ShadowConversationMemory> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Turn.CODEC.listOf().optionalFieldOf("turns", List.of())
							.forGetter(ShadowConversationMemory::turns),
					Codec.STRING.optionalFieldOf("entity", "")
							.forGetter(memory -> memory.recent(ReferentType.ENTITY)),
					Codec.STRING.optionalFieldOf("item", "")
							.forGetter(memory -> memory.recent(ReferentType.ITEM)),
					Codec.STRING.optionalFieldOf("power", "")
							.forGetter(memory -> memory.recent(ReferentType.POWER)),
					Codec.STRING.optionalFieldOf("task", "")
							.forGetter(memory -> memory.recent(ReferentType.TASK)),
					Codec.STRING.optionalFieldOf("failure", "")
							.forGetter(ShadowConversationMemory::recentFailure),
					Codec.INT.optionalFieldOf("relationship", 0)
							.forGetter(ShadowConversationMemory::relationship),
					Codec.INT.optionalFieldOf("influence", 0)
							.forGetter(ShadowConversationMemory::influence))
					.apply(instance, ShadowConversationMemory::decoded));

	public ShadowConversationMemory {
		turns = List.copyOf(turns == null ? List.of()
				: turns.subList(Math.max(0, turns.size() - MAX_TURNS), turns.size()));
		EnumMap<ReferentType, String> safe = new EnumMap<>(ReferentType.class);
		if (referents != null) referents.forEach((type, value) -> safe.put(type, bounded(value)));
		referents = Map.copyOf(safe);
		recentFailure = bounded(recentFailure);
		relationship = Math.clamp(relationship, -100, 100);
		influence = Math.clamp(influence, -100, 100);
	}

	public static ShadowConversationMemory empty() {
		return new ShadowConversationMemory(List.of(), Map.of(), "", 0, 0);
	}

	private static ShadowConversationMemory decoded(List<Turn> turns, String entity, String item,
			String power, String task, String failure, int relationship, int influence) {
		EnumMap<ReferentType, String> referents = new EnumMap<>(ReferentType.class);
		putIfPresent(referents, ReferentType.ENTITY, entity);
		putIfPresent(referents, ReferentType.ITEM, item);
		putIfPresent(referents, ReferentType.POWER, power);
		putIfPresent(referents, ReferentType.TASK, task);
		return new ShadowConversationMemory(turns, referents, failure, relationship, influence);
	}

	public ShadowConversationMemory remember(String owner, String shadow) {
		List<Turn> next = new ArrayList<>(turns);
		next.add(new Turn(owner, shadow));
		return new ShadowConversationMemory(next, referents, recentFailure, relationship, influence);
	}

	public ShadowConversationMemory rememberReferent(ReferentType type, String value) {
		EnumMap<ReferentType, String> next = new EnumMap<>(ReferentType.class);
		next.putAll(referents);
		next.put(type, bounded(value));
		return new ShadowConversationMemory(turns, next, recentFailure, relationship, influence);
	}

	public ShadowConversationMemory rememberFailure(String value) {
		return new ShadowConversationMemory(turns, referents, value, relationship, influence);
	}

	public ShadowConversationMemory withRelationship(int value) {
		return new ShadowConversationMemory(turns, referents, recentFailure, value, influence);
	}

	public ShadowConversationMemory withInfluence(int value) {
		return new ShadowConversationMemory(turns, referents, recentFailure, relationship, value);
	}

	public String recent(ReferentType type) {
		return referents.getOrDefault(type, "");
	}

	/** Runtime bodies/tasks are not stored here, so death deliberately preserves all memory. */
	public ShadowConversationMemory afterBodyDeath() {
		return this;
	}

	public String redactedSummary() {
		return turns.stream().map(turn -> redact(turn.owner()) + " | " + redact(turn.shadow()))
				.reduce((left, right) -> left + "\n" + right).orElse("");
	}

	private static String redact(String value) {
		return SECRET.matcher(IPV4.matcher(value).replaceAll("[redacted]")).replaceAll("[redacted]");
	}

	private static String bounded(String value) {
		if (value == null) return "";
		String stripped = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").strip();
		return stripped.substring(0, Math.min(MAX_LINE, stripped.length()));
	}

	private static void putIfPresent(EnumMap<ReferentType, String> target,
			ReferentType type, String value) {
		if (value != null && !value.isBlank()) target.put(type, value);
	}
}
