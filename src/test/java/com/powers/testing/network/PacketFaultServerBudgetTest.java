package com.powers.testing.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class PacketFaultServerBudgetTest {
	@Test
	void simultaneousSessionsShareOneQueueAndFairTickCap() {
		PacketFaultServerBudget budget = new PacketFaultServerBudget();
		PacketFaultProfile profile = new PacketFaultProfile("shared-server", 91L,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				10, 0, 0, 0, 20_000, 40, 20_000);
		PacketFaultEngine first = new PacketFaultEngine(profile, budget);
		PacketFaultEngine second = new PacketFaultEngine(profile, budget);
		AtomicInteger firstDelivered = new AtomicInteger();
		AtomicInteger secondDelivered = new AtomicInteger();
		AtomicInteger failed = new AtomicInteger();
		for (int index = 0; index < 20_000; index++) {
			first.offer(connection(1), PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.MAGIC_FX,
					0L, ignored -> { firstDelivered.incrementAndGet(); return true; }, failed::incrementAndGet, index);
			second.offer(connection(2), PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.MAGIC_FX,
					0L, ignored -> { secondDelivered.incrementAndGet(); return true; }, failed::incrementAndGet, index);
		}

		assertEquals(PacketFaultServerBudget.GLOBAL_QUEUE_LIMIT, budget.queueDepth());
		assertEquals(40_000 - PacketFaultServerBudget.GLOBAL_QUEUE_LIMIT, failed.get());
		budget.tick(List.of(first, second), 10L);
		assertEquals(PacketFaultServerBudget.GLOBAL_WORK_PER_TICK,
				firstDelivered.get() + secondDelivered.get());
		assertTrue(firstDelivered.get() > 0 && secondDelivered.get() > 0,
				"One scoped session starved another");
		assertEquals(PacketFaultServerBudget.GLOBAL_QUEUE_LIMIT
				- PacketFaultServerBudget.GLOBAL_WORK_PER_TICK, budget.queueDepth());
	}

	private static PacketFaultConnection connection(long id) {
		return new PacketFaultConnection(new UUID(0L, id), 1L);
	}
}
