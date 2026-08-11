package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastTransactionTest {
	@Test
	void successfulCastCommitsEveryPhaseOnce() {
		Map<CastTransaction.Phase, Integer> resources = resources();
		CastTransaction transaction = transaction(resources, null);

		assertTrue(transaction.execute().committed());
		resources.forEach((phase, count) -> assertEquals(1, count, phase.name()));
	}

	@Test
	void faultAtEveryPhaseRollsBackCurrentAndEarlierResourcesExactlyOnce() {
		for (CastTransaction.Phase fault : CastTransaction.Phase.values()) {
			Map<CastTransaction.Phase, Integer> resources = resources();
			CastTransaction.Result result = transaction(resources, fault).execute();

			assertFalse(result.committed(), fault.name());
			assertEquals(fault, result.failedPhase());
			resources.forEach((phase, count) -> assertEquals(0, count,
					fault + " leaked " + phase));
		}
	}

	@Test
	void explicitRejectionUsesTheSameRollbackPathAsAnException() {
		Map<CastTransaction.Phase, Integer> resources = resources();
		CastTransaction transaction = new CastTransaction()
				.stage(CastTransaction.Phase.VALIDATION, () -> increment(resources,
						CastTransaction.Phase.VALIDATION), () -> decrement(resources,
						CastTransaction.Phase.VALIDATION))
				.stage(CastTransaction.Phase.COST, () -> false, () -> decrement(resources,
						CastTransaction.Phase.COST));

		CastTransaction.Result result = transaction.execute();
		assertFalse(result.committed());
		assertEquals(CastTransaction.Phase.COST, result.failedPhase());
		resources.forEach((phase, count) -> assertEquals(0, count, phase.name()));
	}

	private static CastTransaction transaction(Map<CastTransaction.Phase, Integer> resources,
			CastTransaction.Phase fault) {
		CastTransaction transaction = new CastTransaction();
		for (CastTransaction.Phase phase : CastTransaction.Phase.values()) {
			transaction.stage(phase, () -> {
				increment(resources, phase);
				if (phase == fault) throw new IllegalStateException("injected " + phase);
				return true;
			}, () -> decrement(resources, phase));
		}
		return transaction;
	}

	private static Map<CastTransaction.Phase, Integer> resources() {
		Map<CastTransaction.Phase, Integer> resources = new EnumMap<>(CastTransaction.Phase.class);
		for (CastTransaction.Phase phase : CastTransaction.Phase.values()) resources.put(phase, 0);
		return resources;
	}

	private static boolean increment(Map<CastTransaction.Phase, Integer> values,
			CastTransaction.Phase phase) {
		values.compute(phase, (ignored, value) -> value + 1);
		return true;
	}

	private static void decrement(Map<CastTransaction.Phase, Integer> values,
			CastTransaction.Phase phase) {
		values.compute(phase, (ignored, value) -> Math.max(0, value - 1));
	}
}
