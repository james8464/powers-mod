package com.powers.spell;

import java.util.ArrayList;
import java.util.List;

/** Pure bounded rules for Blood Reading's deliberately coarse recent trend. */
public final class BloodReadingTrendRules {
	public static final long WINDOW_TICKS = 600L;
	public static final int MAX_SAMPLES = 8;

	private BloodReadingTrendRules() {
	}

	public enum Kind { DAMAGE, HEALING }
	public enum Trend { STABLE, WORSENING, RECOVERING, VOLATILE }

	public record Sample(long tick, Kind kind, float amount) { }
	public record Summary(float recentDamage, float recentHealing, Trend trend, int sampleCount) { }

	public static List<Sample> record(List<Sample> existing, long currentTick, Kind kind, float amount) {
		List<Sample> retained = new ArrayList<>();
		if (existing != null) {
			for (Sample sample : existing) {
				if (valid(sample, currentTick)) retained.add(sample);
			}
		}
		if (kind != null && Float.isFinite(amount) && amount > 0.0F) {
			retained.add(new Sample(currentTick, kind, amount));
		}
		int from = Math.max(0, retained.size() - MAX_SAMPLES);
		return List.copyOf(retained.subList(from, retained.size()));
	}

	public static Summary summarize(List<Sample> samples, long currentTick) {
		float damage = 0.0F;
		float healing = 0.0F;
		int count = 0;
		if (samples != null) {
			for (Sample sample : samples) {
				if (!valid(sample, currentTick)) continue;
				if (sample.kind() == Kind.DAMAGE) damage = addBounded(damage, sample.amount());
				else healing = addBounded(healing, sample.amount());
				count++;
			}
		}
		Trend trend = count == 0 ? Trend.STABLE
				: damage > healing * 1.25F ? Trend.WORSENING
				: healing > damage * 1.25F ? Trend.RECOVERING : Trend.VOLATILE;
		return new Summary(damage, healing, trend, count);
	}

	private static boolean valid(Sample sample, long currentTick) {
		return sample != null && sample.kind() != null && Float.isFinite(sample.amount())
				&& sample.amount() > 0.0F && sample.tick() <= currentTick
				&& currentTick - sample.tick() <= WINDOW_TICKS;
	}

	private static float addBounded(float left, float right) {
		return (float) Math.min(Float.MAX_VALUE, (double) left + right);
	}
}
