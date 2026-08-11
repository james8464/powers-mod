package com.powers.quality;

import com.powers.item.ArtifactEnergyReservoir;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactCooldownRules;
import com.powers.power.PowerEnergy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic property checks across hostile numeric energy and cooldown inputs. */
class EnergyCooldownPropertyTest {
	private static final int CASES = 50_000;

	@Test
	void capacitiesAndCooldownsNeverBecomeNegativeOrWrap() {
		SplittableRandom random = new SplittableRandom(0x504F57455253504CL);
		for (int index = 0; index < CASES; index++) {
			int level = random.nextInt();
			int ordinary = PowerEnergy.maxCapacity(level);
			int darkness = PowerEnergy.darknessMaxCapacity(level);
			assertTrue(ordinary >= PowerEnergy.BASE_MAX);
			assertTrue(darkness >= PowerEnergy.DARKNESS_BASE_MAX);

			int baseTicks = random.nextInt();
			int bounded = Math.max(0, baseTicks);
			int rank = random.nextInt(-100, 101);
			for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
				int cooldown = ArtifactCooldownRules.cooldownTicks(alignment, rank, baseTicks);
				assertTrue(cooldown >= 0 && cooldown <= bounded);
				if (rank >= 10 && alignment == ArtifactAlignment.DARKNESS) {
					assertEquals(0, cooldown);
				}
			}
		}
	}

	@Test
	void reservoirDebitIsAtomicConservativeAndNeverDuplicatesEnergy() {
		SplittableRandom random = new SplittableRandom(0x454E455247595052L);
		for (int run = 0; run < CASES; run++) {
			List<Integer> input = new ArrayList<>();
			int entries = random.nextInt(0, 17);
			for (int entry = 0; entry < entries; entry++) input.add(random.nextInt(-2_000, 8_001));
			int requested = random.nextInt();
			List<Integer> normalized = input.stream().map(value -> Math.max(0, value)).toList();
			long before = normalized.stream().mapToLong(Integer::longValue).sum();
			long cost = Math.max(0L, requested);

			ArtifactEnergyReservoir.Debit debit = ArtifactEnergyReservoir.debit(input, requested);
			long after = debit.balances().stream().mapToLong(Integer::longValue).sum();
			assertTrue(debit.balances().stream().allMatch(balance -> balance >= 0));
			assertEquals(before >= cost, debit.paid());
			if (debit.paid()) assertEquals(before - cost, after);
			else assertEquals(normalized, debit.balances());
		}
	}
}
