package com.powers.testing;

import com.powers.player.DarknessQuestRules;
import com.powers.player.DarknessDeed;
import com.powers.player.QuestRoute;
import com.powers.player.SkillDeed;
import com.powers.player.SkillQuestRules;
import com.powers.progression.QuestTelemetryLedger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic human-equivalent deed cadences for the live quest balance campaign. */
public final class QuestTelemetryCampaignPlan {
	/** One anonymous multiplayer session's independent deed timings. */
	public record Profile(int sample, Map<String, Integer> intervals) {
		public Profile {
			if (sample < 1 || sample > 10 || intervals == null || intervals.isEmpty()
					|| intervals.entrySet().stream().anyMatch(entry -> entry.getKey() == null
							|| entry.getKey().isBlank() || entry.getValue() == null
							|| entry.getValue() <= 0)) {
				throw new IllegalArgumentException("Invalid quest campaign profile");
			}
			intervals = Map.copyOf(intervals);
		}

		public int interval(String deed) {
			Integer interval = intervals.get(deed);
			if (interval == null) throw new IllegalArgumentException("Unknown deed " + deed);
			return interval;
		}

		public boolean due(String deed, long elapsedTicks) {
			return elapsedTicks > 0L && elapsedTicks % interval(deed) == 0L;
		}

		/** Earliest cumulative tick at which a legal route can satisfy this level. */
		public long expectedCompletionTick(QuestTelemetryLedger.Alignment alignment, int level) {
			if (level < 1 || level > 10) throw new IllegalArgumentException("Invalid quest level");
			long previous = 0L;
			for (int current = 1; current <= level; current++) {
				long raw = alignment == QuestTelemetryLedger.Alignment.LIGHT
						? earliestIndependent(SkillQuestRules.routes(current))
						: earliestDarkness(DarknessQuestRules.routes(current));
				previous = Math.max(previous, raw);
			}
			return previous;
		}

		public long maximumCompletionTick(QuestTelemetryLedger.Alignment alignment) {
			return expectedCompletionTick(alignment, 10);
		}

		private <T extends Enum<T>> long earliestIndependent(List<QuestRoute<T>> routes) {
			return routes.stream().mapToLong(route -> route.thresholds().entrySet().stream()
					.mapToLong(entry -> (long) interval(deedKey(entry.getKey())) * entry.getValue())
					.max().orElseThrow()).min().orElseThrow();
		}

		private long earliestDarkness(List<QuestRoute<DarknessDeed>> routes) {
			return routes.stream().mapToLong(this::earliestDarkness).min().orElseThrow();
		}

		private long earliestDarkness(QuestRoute<DarknessDeed> route) {
			long upper = 1L;
			while (!completed(route, upper)) upper = Math.multiplyExact(upper, 2L);
			long lower = 0L;
			while (lower + 1L < upper) {
				long candidate = lower + (upper - lower) / 2L;
				if (completed(route, candidate)) upper = candidate;
				else lower = candidate;
			}
			return upper;
		}

		private boolean completed(QuestRoute<DarknessDeed> route, long elapsedTicks) {
			return route.thresholds().entrySet().stream().allMatch(entry ->
					darknessCount(entry.getKey(), elapsedTicks) >= entry.getValue());
		}

		private long darknessCount(DarknessDeed deed, long elapsedTicks) {
			long count = elapsedTicks / interval(deedKey(deed));
			if (deed == DarknessDeed.VILLAGER) {
				count += elapsedTicks / interval(DarknessDeed.BABY_VILLAGER.key());
			}
			return count;
		}
	}

	private QuestTelemetryCampaignPlan() {
	}

	public static List<Profile> profiles(QuestTelemetryLedger.Alignment alignment) {
		List<Profile> profiles = new ArrayList<>(10);
		for (int index = 0; index < 10; index++) {
			Map<String, Integer> intervals = new LinkedHashMap<>();
			if (alignment == QuestTelemetryLedger.Alignment.LIGHT) {
				intervals.put("power_use", 40 + index * 2);
				intervals.put("power_kill", 1_800 + index * 80);
				intervals.put("boss_kill", 7_200 + index * 240);
				intervals.put("light_memory", 12_000 + index * 600);
			} else {
				intervals.put("passive", 900 + index * 30);
				intervals.put("villager", 800 + index * 40);
				intervals.put("wolf", 900 + index * 40);
				intervals.put("baby_villager", 4_000 + index * 160);
				intervals.put("iron_golem", 4_800 + index * 180);
			}
			profiles.add(new Profile(index + 1, intervals));
		}
		return List.copyOf(profiles);
	}

	private static String deedKey(Enum<?> deed) {
		if (deed instanceof SkillDeed skill) return skill.key();
		if (deed instanceof DarknessDeed darkness) return darkness.key();
		throw new IllegalArgumentException("Unsupported quest deed " + deed);
	}
}
