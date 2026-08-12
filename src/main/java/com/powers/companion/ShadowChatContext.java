package com.powers.companion;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded, speaker-labelled public-chat awareness for manifested Shadows.
 *
 * <p>Public chat remains public and is never treated as an unprefixed combat command. Addressing
 * Shadow opens a short owner-local conversational focus; natural follow-ups may receive replies,
 * while every active Shadow can still use the recent labelled conversation as answer context.</p>
 */
public final class ShadowChatContext {
	private static final int MAX_ENTRIES = 64;
	private static final int MAX_MESSAGE_LENGTH = 160;
	private static final int MAX_CONTEXT_ENTRIES = 12;
	private static final long MAX_AGE_TICKS = 20L * 60L;
	private static final long FOCUS_TICKS = 20L * 45L;
	private static final long FOLLOW_UP_COOLDOWN_TICKS = 10L;
	private static final Map<MinecraftServer, Timeline> TIMELINES = new IdentityHashMap<>();

	/** One immutable public line. UUID identity prevents equal display names crossing conversations. */
	public record Entry(UUID speaker, String speakerName, String message, long tick) {
		public Entry {
			if (speaker == null) throw new IllegalArgumentException("Chat speaker is required");
			speakerName = bounded(speakerName, 48);
			message = bounded(message, MAX_MESSAGE_LENGTH);
		}
	}

	private static final class Timeline {
		private final ArrayDeque<Entry> entries = new ArrayDeque<>();
		private final Map<UUID, Long> focusedUntil = new LinkedHashMap<>();
		private final Map<UUID, Long> lastFollowUp = new LinkedHashMap<>();
	}

	private ShadowChatContext() {
	}

	/** Opens an owner-local natural-dialogue window after an explicit Shadow address. */
	public static void markAddressed(ServerPlayer owner, long tick) {
		if (owner == null) return;
		timeline(owner.level().getServer()).focusedUntil.put(owner.getUUID(), tick + FOCUS_TICKS);
	}

	/** Records public chat once and returns whether this speaker's Shadow should answer naturally. */
	public static boolean observe(ServerPlayer speaker, String rawMessage, long tick) {
		if (speaker == null) return false;
		String message = bounded(rawMessage, MAX_MESSAGE_LENGTH);
		if (message.isEmpty()) return false;
		Timeline timeline = timeline(speaker.level().getServer());
		prune(timeline, tick);
		if (timeline.entries.size() >= MAX_ENTRIES) timeline.entries.removeFirst();
		timeline.entries.addLast(new Entry(speaker.getUUID(), speaker.getScoreboardName(), message, tick));
		long focusedUntil = timeline.focusedUntil.getOrDefault(speaker.getUUID(), Long.MIN_VALUE);
		Long last = timeline.lastFollowUp.get(speaker.getUUID());
		if (tick > focusedUntil || last != null && tick - last < FOLLOW_UP_COOLDOWN_TICKS) return false;
		timeline.lastFollowUp.put(speaker.getUUID(), tick);
		return true;
	}

	/** Recent global context remains speaker-labelled and chronologically ordered. */
	public static List<Entry> snapshot(ServerPlayer owner, long tick) {
		if (owner == null) return List.of();
		Timeline timeline = timeline(owner.level().getServer());
		prune(timeline, tick);
		List<Entry> entries = new ArrayList<>(timeline.entries);
		return List.copyOf(entries.subList(Math.max(0, entries.size() - MAX_CONTEXT_ENTRIES),
				entries.size()));
	}

	/** Adds bounded public context for the offline/opt-in provider without changing persisted turns. */
	static String contextualize(ServerPlayer owner, String question) {
		List<Entry> entries = snapshot(owner, owner.level().getServer().getTickCount());
		if (entries.isEmpty()) return question;
		StringBuilder result = new StringBuilder(bounded(question, MAX_MESSAGE_LENGTH));
		result.append("\nRecent public chat (speaker-labelled):");
		for (Entry entry : entries) {
			result.append("\n<").append(entry.speakerName()).append('#')
					.append(entry.speaker().toString(), 0, 8).append("> ")
					.append(entry.message());
		}
		return result.toString();
	}

	public static void forget(UUID owner) {
		if (owner == null) return;
		for (Timeline timeline : TIMELINES.values()) {
			timeline.focusedUntil.remove(owner);
			timeline.lastFollowUp.remove(owner);
		}
	}

	public static void clear() {
		TIMELINES.clear();
	}

	private static Timeline timeline(MinecraftServer server) {
		return TIMELINES.computeIfAbsent(server, ignored -> new Timeline());
	}

	private static void prune(Timeline timeline, long tick) {
		long oldest = tick - MAX_AGE_TICKS;
		while (!timeline.entries.isEmpty() && timeline.entries.getFirst().tick() < oldest) {
			timeline.entries.removeFirst();
		}
		timeline.focusedUntil.entrySet().removeIf(entry -> entry.getValue() < tick);
		timeline.lastFollowUp.entrySet().removeIf(entry -> entry.getValue() < oldest);
	}

	private static String bounded(String value, int limit) {
		if (value == null) return "";
		String clean = value.codePoints().filter(codePoint -> !Character.isISOControl(codePoint))
				.limit(limit).collect(StringBuilder::new, StringBuilder::appendCodePoint,
						StringBuilder::append).toString().strip();
		return clean;
	}
}
