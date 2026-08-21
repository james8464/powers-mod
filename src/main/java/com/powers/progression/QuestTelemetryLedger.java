package com.powers.progression;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded anonymous quest-duration ledger. Player UUIDs exist only in active
 * start markers; completed samples retain alignment, level, route and elapsed
 * ticks, so published balance evidence cannot identify individual players.
 */
public final class QuestTelemetryLedger {
	public enum Alignment { LIGHT, DARK }

	public record Sample(Alignment alignment, int level, String route, long elapsedTicks) {
		public Sample {
			if (alignment == null || level < 1 || level > 10 || !validRoute(route)
					|| elapsedTicks < 0L) {
				throw new IllegalArgumentException("Invalid quest telemetry sample");
			}
		}
	}

	/** One level reached by the same authoritative deed in a batched completion. */
	public record Completion(int level, String route) {
		public Completion {
			if (level < 1 || level > 10) throw new IllegalArgumentException("Invalid quest level");
			route = normalizeRoute(route);
		}
	}

	private record ActiveKey(UUID player, Alignment alignment) { }

	public record Summary(int samples, long medianTicks, long p90Ticks, List<String> routes) {
		public Summary {
			routes = List.copyOf(routes);
		}

		public boolean sufficient(int minimumSamples) {
			return samples >= Math.max(1, minimumSamples);
		}
	}

	private final int maximumSamples;
	private final Map<ActiveKey, Long> starts = new HashMap<>();
	private final ArrayDeque<Sample> samples = new ArrayDeque<>();

	public QuestTelemetryLedger(int maximumSamples) {
		this.maximumSamples = Math.clamp(maximumSamples, 1, 100_000);
	}

	/** Starts the current level timer once; repeated deeds do not reset it. */
	public boolean noteActivity(UUID player, Alignment alignment, long worldTick) {
		if (player == null || alignment == null) return false;
		return starts.putIfAbsent(new ActiveKey(player, alignment), Math.max(0L, worldTick)) == null;
	}

	/** Completes one level, advances its timer, and stores no player identity. */
	public Sample complete(UUID player, Alignment alignment, int level, String route, long worldTick) {
		return completeBatch(player, alignment, List.of(new Completion(level, route)), worldTick).getFirst();
	}

	/**
	 * Completes all levels earned by one deed against the same elapsed interval.
	 * This prevents a legitimate multi-level completion from manufacturing zero-time samples.
	 */
	public List<Sample> completeBatch(UUID player, Alignment alignment,
			List<Completion> completions, long worldTick) {
		if (player == null || alignment == null || completions == null) {
			throw new IllegalArgumentException("Missing quest completion data");
		}
		if (completions.isEmpty()) return List.of();
		int previousLevel = 0;
		for (Completion completion : completions) {
			if (completion == null || completion.level() <= previousLevel) {
				throw new IllegalArgumentException("Quest completions must be strictly ordered");
			}
			previousLevel = completion.level();
		}
		long now = Math.max(0L, worldTick);
		long started = starts.getOrDefault(new ActiveKey(player, alignment), now);
		long elapsed = Math.max(0L, now - started);
		List<Sample> added = new ArrayList<>(completions.size());
		for (Completion completion : completions) {
			Sample sample = new Sample(alignment, completion.level(), completion.route(), elapsed);
			if (samples.size() >= maximumSamples) samples.removeFirst();
			samples.addLast(sample);
			added.add(sample);
		}
		starts.put(new ActiveKey(player, alignment), now);
		return List.copyOf(added);
	}

	public List<Sample> samples() {
		return List.copyOf(samples);
	}

	/** Stable rows used by world SavedData without exposing completed identities. */
	public List<String> encodedStarts() {
		return starts.entrySet().stream()
				.sorted(Comparator.comparing((Map.Entry<ActiveKey, ?> entry) ->
						entry.getKey().player().toString()).thenComparing(entry -> entry.getKey().alignment()))
				.map(entry -> entry.getKey().player() + ";" + entry.getKey().alignment()
						+ ";" + entry.getValue()).toList();
	}

	public List<String> encodedSamples() {
		return samples.stream().map(sample -> sample.alignment() + ";" + sample.level()
				+ ";" + sample.route() + ";" + sample.elapsedTicks()).toList();
	}

	public Summary summary(Alignment alignment, int level) {
		List<Sample> selected = samples.stream()
				.filter(sample -> sample.alignment() == alignment && sample.level() == level)
				.toList();
		if (selected.isEmpty()) return new Summary(0, 0L, 0L, List.of());
		List<Long> durations = selected.stream().map(Sample::elapsedTicks).sorted().toList();
		long median = durations.size() % 2 == 0
				? safeAverage(durations.get(durations.size() / 2 - 1), durations.get(durations.size() / 2))
				: durations.get(durations.size() / 2);
		int p90Index = Math.max(0, (int) Math.ceil(durations.size() * 0.90) - 1);
		LinkedHashSet<String> routes = new LinkedHashSet<>();
		selected.forEach(sample -> routes.add(sample.route()));
		return new Summary(selected.size(), median, durations.get(p90Index), List.copyOf(routes));
	}

	public static QuestTelemetryLedger decode(int maximumSamples,
			List<String> encodedStarts, List<String> encodedSamples) {
		QuestTelemetryLedger ledger = new QuestTelemetryLedger(maximumSamples);
		for (String row : safeRows(encodedStarts)) {
			try {
				String[] parts = row.split(";", -1);
				if (parts.length != 3) continue;
				ledger.starts.put(new ActiveKey(UUID.fromString(parts[0]), Alignment.valueOf(parts[1])),
						Math.max(0L, Long.parseLong(parts[2])));
			} catch (IllegalArgumentException ignored) {
				// One corrupt row must not discard the remaining world telemetry.
			}
		}
		for (String row : safeRows(encodedSamples)) {
			try {
				String[] parts = row.split(";", -1);
				if (parts.length != 4) continue;
				Sample sample = new Sample(Alignment.valueOf(parts[0]), Integer.parseInt(parts[1]),
						parts[2], Long.parseLong(parts[3]));
				if (ledger.samples.size() >= ledger.maximumSamples) ledger.samples.removeFirst();
				ledger.samples.addLast(sample);
			} catch (IllegalArgumentException ignored) {
				// Drop invalid historic samples independently so one corrupt row cannot erase later evidence.
			}
		}
		return ledger;
	}

	private static List<String> safeRows(List<String> rows) {
		return rows == null ? List.of() : rows;
	}

	private static String normalizeRoute(String route) {
		return validRoute(route) ? route : "unknown";
	}

	private static boolean validRoute(String route) {
		return route != null && route.matches("[a-z0-9_.-]{1,40}");
	}

	private static long safeAverage(long first, long second) {
		return first / 2L + second / 2L + (first % 2L + second % 2L) / 2L;
	}
}
