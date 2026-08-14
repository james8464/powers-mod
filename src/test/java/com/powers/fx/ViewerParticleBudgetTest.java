package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewerParticleBudgetTest {
	private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void chargesEveryActualRecipientAgainstServerAndViewerCaps() {
		ViewerParticleBudget budget = new ViewerParticleBudget(70, 50, 128.0);

		assertEquals(40, budget.claim(10, FIRST, 40, 4.0));
		assertEquals(30, budget.claim(10, SECOND, 40, 4.0));
		assertEquals(0, budget.claim(10, FIRST, 10, 4.0));
		assertEquals(70, budget.serverUsed());
		assertEquals(40, budget.viewerUsed(FIRST));
		assertEquals(30, budget.viewerUsed(SECOND));
	}

	@Test
	void enforcesPerViewerCapBeforeTheServerIsFull() {
		ViewerParticleBudget budget = new ViewerParticleBudget(500, 50, 128.0);

		assertEquals(45, budget.claim(20, FIRST, 45, 16.0));
		assertEquals(5, budget.claim(20, FIRST, 45, 16.0));
		assertEquals(45, budget.claim(20, SECOND, 45, 16.0));
		assertEquals(95, budget.serverUsed());
	}

	@Test
	void cullsDistantViewersAndResetsAllCountersOnTheNextTick() {
		ViewerParticleBudget budget = new ViewerParticleBudget(70, 50, 128.0);

		assertEquals(0, budget.claim(30, FIRST, 40, 128.01 * 128.01));
		assertEquals(50, budget.claim(30, FIRST, 60, 0.0));
		assertEquals(50, budget.claim(31, FIRST, 60, 0.0));
		assertEquals(50, budget.serverUsed());
	}

	@Test
	void rejectsInvalidRequestsWithoutConsumingBudget() {
		ViewerParticleBudget budget = new ViewerParticleBudget(70, 50, 128.0);

		assertEquals(0, budget.claim(40, FIRST, 0, 0.0));
		assertEquals(0, budget.claim(40, FIRST, -10, 0.0));
		assertEquals(0, budget.claim(40, FIRST, 10, Double.NaN));
		assertEquals(0, budget.serverUsed());
	}

	@Test
	void duplicateProfilesRetainIndependentLiveViewerAllowances() {
		ViewerParticleBudget budget = new ViewerParticleBudget(150, 50, 128.0);

		assertEquals(50, budget.claim(50, FIRST, 101, 60, 0.0));
		assertEquals(50, budget.claim(50, FIRST, 102, 60, 0.0));
		assertEquals(100, budget.serverUsed());
	}
}
