package com.powers.loot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Deterministically publishes the statistical acceptance evidence for additive loot pools. */
public final class LootDistributionReport {
	private static final int TRIALS = 200_000;
	private static final int STANDARD_TRIGGERS_PER_HOUR = 60;

	private LootDistributionReport() {
	}

	public static void main(String[] arguments) throws Exception {
		Path root = Path.of(arguments.length == 0 ? "." : arguments[0]).toAbsolutePath().normalize();
		StringBuilder report = new StringBuilder("""
				# Additive loot-distribution acceptance

				Every POWERS injection is a separate one-roll pool, so foreign loot weights cannot dilute it. Results below use 200,000 deterministic triggers per pool. “Items/hour” uses an explicit 60 matching table triggers/hour comparison baseline; actual play rates depend on the structure, mob, block and server.

				| Loot table | Authored chance | Simulated chance | Items / 1,000 triggers | Items / baseline hour | Same-item consecutive-trigger chance | Foreign weight 50,000 |
				| --- | ---: | ---: | ---: | ---: | ---: | --- |
				""");
		long seed = 0x504F57455253L;
		for (LootDropGroup group : LootInjectionCatalog.groups()) {
			var vanilla = LootDistributionSimulator.simulate(group, TRIALS, 0, seed);
			var representativePack = LootDistributionSimulator.simulate(group, TRIALS, 50_000, seed);
			double itemsPerTrigger = vanilla.totalItems() / (double) TRIALS;
			double repeat = group.chance() * group.chance() / group.itemIds().size();
			report.append(String.format(Locale.ROOT,
					"| `%s` | %.3f | %.3f | %.2f | %.2f | %.4f%% | %s |%n",
					group.tableId(), group.chance(), vanilla.dropRate(),
					itemsPerTrigger * 1_000.0, itemsPerTrigger * STANDARD_TRIGGERS_PER_HOUR,
					repeat * 100.0, vanilla.equals(representativePack) ? "identical" : "FAILED"));
		}
		report.append("""

				## Interpretation

				- The simulator uses the same independent chance, inclusive count range and equal item choice as the runtime catalogue.
				- The representative-pack column deliberately adds an extreme foreign weight; every seeded result must remain byte-for-byte identical because POWERS never edits a foreign pool.
				- “Same-item consecutive-trigger chance” is the probability that two adjacent table triggers both drop from this POWERS pool and select the same item. It is not a duplication exploit.
				""");
		Path output = root.resolve("docs/verification/2026-08-11-loot-distribution.md");
		Files.createDirectories(output.getParent());
		Files.writeString(output, report.toString());
	}
}
