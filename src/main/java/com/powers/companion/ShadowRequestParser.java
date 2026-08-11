package com.powers.companion;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic bounded parser. It describes intent but can never authorize an action. */
public final class ShadowRequestParser {
	private static final String PREFIX = "shadow,";
	private static final Pattern COUNT_ITEM = Pattern.compile("^(?:please )?(?:bring|get|fetch)(?: me)?(?: (\\d{1,3}))? (.+)$");
	private static final Pattern CONJURE = Pattern.compile("^(?:please )?(?:conjure|create|make)(?: me)?(?: (\\d{1,3}))? (.+)$");
	private static final Set<String> REVEAL = Set.of("reveal yourself", "show yourself", "be seen", "reveal");
	private static final Set<String> HIDE = Set.of("hide yourself", "hide", "be unseen", "conceal yourself");
	private static final Set<String> DISMISS = Set.of("leave me", "dismiss", "vanish", "go away");
	private static final Set<String> SUMMON = Set.of("come to me", "appear", "come", "manifest");

	private ShadowRequestParser() {
	}

	public static ShadowRequest parse(String raw, ShadowConversationMemory memory,
			ShadowNameResolver names) {
		String stripped = raw == null ? "" : raw.strip();
		if (stripped.length() < PREFIX.length()
				|| !stripped.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
			return ShadowRequest.unaddressed();
		}
		String original = stripped.substring(PREFIX.length()).strip();
		if (original.isEmpty()) return request(ShadowRequest.Kind.EMPTY, "", 1, original);
		if (original.length() > ShadowRequest.MAX_MESSAGE_LENGTH) {
			return request(ShadowRequest.Kind.TOO_LONG, "", 1, "");
		}
		String command = normalize(original);
		if (REVEAL.contains(command)) return request(ShadowRequest.Kind.REVEAL, "", 1, original);
		if (HIDE.contains(command)) return request(ShadowRequest.Kind.HIDE, "", 1, original);
		if (DISMISS.contains(command)) return request(ShadowRequest.Kind.DISMISS, "", 1, original);
		if (SUMMON.contains(command)) return request(ShadowRequest.Kind.SUMMON, "", 1, original);
		if (command.equals("follow") || command.equals("follow me")) return request(ShadowRequest.Kind.FOLLOW, "owner", 1, original);
		if (command.equals("stay") || command.equals("stay here") || command.equals("wait here")) return request(ShadowRequest.Kind.STAY, "here", 1, original);
		if (command.startsWith("guard ")) return request(ShadowRequest.Kind.GUARD, after(command, "guard "), 1, original);
		if (command.startsWith("defend ") || command.equals("protect me")) return request(ShadowRequest.Kind.DEFEND, command.endsWith("me") ? "owner" : after(command, "defend "), 1, original);
		if (isDiagnose(command)) return request(ShadowRequest.Kind.DIAGNOSE, memory.recentFailure(), 1, original);
		ShadowRequest range = rangePreference(command, original);
		if (range != null) return range;
		if (command.startsWith("stop ")) return resolvePower(ShadowRequest.Kind.STOP_POWER,
				after(command, "stop "), original, memory, names);
		if (command.equals("stop") || command.equals("cancel") || command.equals("enough")) return request(ShadowRequest.Kind.STOP, "", 1, original);
		if (command.startsWith("attack ") || command.startsWith("fight ")) {
			String target = after(command, command.startsWith("attack ") ? "attack " : "fight ");
			return request(ShadowRequest.Kind.ATTACK, resolvePronoun(target, memory,
					ShadowConversationMemory.ReferentType.ENTITY), 1, original);
		}
		if (command.startsWith("use ") || command.startsWith("cast ")) {
			String query = after(command, command.startsWith("use ") ? "use " : "cast ");
			return resolvePower(ShadowRequest.Kind.USE_POWER, query, original, memory, names);
		}
		Matcher get = COUNT_ITEM.matcher(command);
		if (get.matches()) return resolveItem(ShadowRequest.Kind.GET_ITEM, get, original, names);
		Matcher conjure = CONJURE.matcher(command);
		if (conjure.matches()) return resolveItem(ShadowRequest.Kind.CONJURE_ITEM, conjure, original, names);
		if (command.startsWith("scout") || command.startsWith("search ahead")) return request(ShadowRequest.Kind.SCOUT, afterOptional(command, "scout"), 1, original);
		return request(ShadowRequest.Kind.CONVERSE, "", 1, original);
	}

	private static ShadowRequest resolvePower(ShadowRequest.Kind kind, String query, String original,
			ShadowConversationMemory memory, ShadowNameResolver names) {
		query = resolvePronoun(query, memory, ShadowConversationMemory.ReferentType.POWER);
		return resolve(kind, query, 1, original, names.resolve(ShadowNameResolver.Type.POWER, query));
	}

	private static ShadowRequest resolveItem(ShadowRequest.Kind kind, Matcher matcher,
			String original, ShadowNameResolver names) {
		int count = matcher.group(1) == null ? 1 : Integer.parseInt(matcher.group(1));
		String query = itemQuery(matcher.group(2));
		return resolve(kind, query, count, original, names.resolve(ShadowNameResolver.Type.ITEM, query));
	}

	private static ShadowRequest resolve(ShadowRequest.Kind kind, String query, int count,
			String original, ShadowNameResolver.Resolution resolution) {
		return switch (resolution.status()) {
			case FOUND -> new ShadowRequest(true, kind, resolution.value(), count,
					ShadowRequest.Range.AUTO, original, "");
			case AMBIGUOUS -> new ShadowRequest(true, ShadowRequest.Kind.CLARIFY, query, count,
					ShadowRequest.Range.AUTO, original, String.join(", ", resolution.candidates()));
			case MISSING -> new ShadowRequest(true, ShadowRequest.Kind.CLARIFY, query, count,
					ShadowRequest.Range.AUTO, original, "unknown_name");
		};
	}

	private static ShadowRequest rangePreference(String command, String original) {
		if (!(command.contains("fight") || command.contains("range") || command.contains("distance"))) return null;
		ShadowRequest.Range range = command.matches(".*\\b(?:far|farther|away|distance|ranged)\\b.*")
				? ShadowRequest.Range.FAR
				: command.matches(".*\\b(?:close|closer|melee|near)\\b.*") ? ShadowRequest.Range.CLOSE
				: command.matches(".*\\b(?:middle|mid|balanced)\\b.*") ? ShadowRequest.Range.MID : null;
		return range == null ? null : new ShadowRequest(true, ShadowRequest.Kind.RANGE_PREFERENCE,
				"combat", 1, range, original, "");
	}

	private static boolean isDiagnose(String command) {
		return command.startsWith("why did") || command.contains("why didn't")
				|| command.contains("why didnt") || command.startsWith("diagnose");
	}

	private static String resolvePronoun(String value, ShadowConversationMemory memory,
			ShadowConversationMemory.ReferentType type) {
		String stripped = value.replaceFirst("^(?:that|the) ", "").strip();
		if (Set.of("it", "him", "her", "them", "power", "that power").contains(stripped)) {
			String recent = memory.recent(type);
			return recent.isEmpty() ? stripped : recent;
		}
		return stripped;
	}

	private static String itemQuery(String value) {
		String query = value.replaceFirst("^(?:a|an|some) ", "").strip();
		if (query.endsWith("torches")) return query.substring(0, query.length() - 2);
		if (!query.contains(":") && query.endsWith("s") && query.length() > 2) return query.substring(0, query.length() - 1);
		return query;
	}

	private static ShadowRequest request(ShadowRequest.Kind kind, String subject, int count, String original) {
		return new ShadowRequest(true, kind, subject, count, ShadowRequest.Range.AUTO, original, "");
	}

	private static String after(String value, String prefix) {
		return value.substring(Math.min(prefix.length(), value.length())).strip();
	}

	private static String afterOptional(String value, String prefix) {
		String suffix = after(value, prefix);
		return suffix.isEmpty() ? "ahead" : suffix;
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$", "")
				.replaceAll("\\s+", " ").strip();
	}
}
