package com.powers.companion.combat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded LRU contextual action values; contains no player/entity UUIDs. */
public final class ShadowLearningState {
	public static final int MAX_CONTEXTS = 64;
	public static final int MAX_TYPES = 32;
	private static final String VERSION = "v1";
	private final LinkedHashMap<String, Profile> contexts = lru();
	private final LinkedHashMap<String, Profile> types = lru();

	private static final class Stat {
		private int count;
		private double mean;

		private void add(double reward) {
			count = Math.min(65_535, count + 1);
			double alpha = Math.max(0.02, 1.0 / Math.min(10, count));
			mean = Math.clamp(mean * (1.0 - alpha) + Math.clamp(reward, -1.0, 1.0) * alpha,
					-1.0, 1.0);
		}
	}

	private static final class Profile {
		private final Map<String, Stat> actions = new LinkedHashMap<>();
	}

	public void adjust(String context, String type, String action, double reward) {
		profile(contexts, safe(context), MAX_CONTEXTS).actions
				.computeIfAbsent(safe(action), ignored -> new Stat()).add(reward);
		profile(types, safe(type), MAX_TYPES).actions
				.computeIfAbsent(safe(action), ignored -> new Stat()).add(reward);
	}

	public double modifier(String context, String type, String action) {
		double value = mean(contexts.get(safe(context)), action) * 0.125
				+ mean(types.get(safe(type)), action) * 0.125;
		return Math.clamp(value, -0.25, 0.25);
	}

	/** Small optimism bonus used only as safe exploration, never as authorization. */
	public double confidenceBonus(String context, String type, String action) {
		int observations = count(contexts.get(safe(context)), action)
				+ count(types.get(safe(type)), action);
		return Math.min(BoundedCombatLearner.MAX_EXPLORATION,
				BoundedCombatLearner.MAX_EXPLORATION / Math.sqrt(1.0 + observations));
	}

	public int contextCount() { return contexts.size(); }
	public int typeCount() { return types.size(); }

	public String encode() {
		StringBuilder raw = new StringBuilder(VERSION);
		append(raw, 'C', contexts);
		append(raw, 'T', types);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				raw.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static ShadowLearningState decode(String encoded) {
		ShadowLearningState state = new ShadowLearningState();
		if (encoded == null || encoded.isBlank() || encoded.length() > 32_768) return state;
		try {
			String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
			if (!raw.startsWith(VERSION)) return state;
			for (String entry : raw.substring(VERSION.length()).split(";")) {
				String[] fields = entry.split(",", 6);
				if (fields.length != 6) continue;
				LinkedHashMap<String, Profile> target = fields[0].equals("C")
						? state.contexts : fields[0].equals("T") ? state.types : null;
				if (target == null) continue;
				int cap = target == state.contexts ? MAX_CONTEXTS : MAX_TYPES;
				Profile profile = profile(target, safe(fields[1]), cap);
				Stat stat = profile.actions.computeIfAbsent(safe(fields[2]), ignored -> new Stat());
				stat.count = Math.clamp(Integer.parseInt(fields[3]), 0, 65_535);
				stat.mean = Math.clamp(Double.parseDouble(fields[4]), -1.0, 1.0);
			}
		} catch (RuntimeException ignored) {
			return new ShadowLearningState();
		}
		return state;
	}

	private static void append(StringBuilder out, char kind,
			LinkedHashMap<String, Profile> source) {
		source.forEach((key, profile) -> profile.actions.forEach((action, stat) -> out
				.append(';').append(kind).append(',').append(key).append(',').append(action)
				.append(',').append(stat.count).append(',').append(stat.mean).append(",0")));
	}

	private static Profile profile(LinkedHashMap<String, Profile> profiles, String key, int cap) {
		Profile existing = profiles.get(key);
		if (existing != null) return existing;
		if (profiles.size() >= cap) profiles.remove(profiles.keySet().iterator().next());
		Profile created = new Profile();
		profiles.put(key, created);
		return created;
	}

	private static double mean(Profile profile, String action) {
		if (profile == null) return 0.0;
		Stat stat = profile.actions.get(safe(action));
		return stat == null ? 0.0 : stat.mean;
	}

	private static int count(Profile profile, String action) {
		if (profile == null) return 0;
		Stat stat = profile.actions.get(safe(action));
		return stat == null ? 0 : stat.count;
	}

	private static <K, V> LinkedHashMap<K, V> lru() {
		return new LinkedHashMap<>(16, .75F, true);
	}

	private static String safe(String value) {
		if (value == null) return "none";
		String safe = value.replaceAll("[^a-zA-Z0-9_:-]", "_");
		return safe.substring(0, Math.min(48, safe.length()));
	}
}
