package com.powers.loot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** Deterministic additive-pool simulator used to audit pack-weight compatibility. */
public final class LootDistributionSimulator {
	/** Compact statistics for one authored pool over a fixed number of table rolls. */
	public record Result(int trials, int drops, long totalItems, Map<String, Long> itemCounts) {
		public Result {
			itemCounts = Map.copyOf(itemCounts);
		}

		public double dropRate() {
			return drops / (double) trials;
		}

		public double standardError() {
			double probability = dropRate();
			return Math.sqrt(probability * (1.0 - probability) / trials);
		}
	}

	private LootDistributionSimulator() {
	}

	/**
	 * Simulates the POWERS pool independently from arbitrary foreign entries.
	 * {@code foreignWeight} is validated but deliberately cannot influence this
	 * result because the runtime injects a separate additive loot pool.
	 */
	public static Result simulate(LootDropGroup group, int trials, int foreignWeight, long seed) {
		Objects.requireNonNull(group, "group");
		if (trials <= 0 || trials > 10_000_000) {
			throw new IllegalArgumentException("Trials must be within 1..10000000");
		}
		if (foreignWeight < 0) throw new IllegalArgumentException("Foreign weight cannot be negative");
		RandomGenerator random = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
		Map<String, Long> counts = new LinkedHashMap<>();
		group.itemIds().forEach(item -> counts.put(item, 0L));
		int drops = 0;
		long totalItems = 0;
		for (int trial = 0; trial < trials; trial++) {
			if (random.nextFloat() >= group.chance()) continue;
			drops++;
			int count = random.nextInt(group.minCount(), group.maxCount() + 1);
			String item = group.itemIds().get(random.nextInt(group.itemIds().size()));
			counts.compute(item, (ignored, current) -> current + count);
			totalItems += count;
		}
		return new Result(trials, drops, totalItems, counts);
	}
}

