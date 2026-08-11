package com.powers.mind;

import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FatalResolutionGateTest {
	@RepeatedTest(500)
	void simultaneousBodyAndAvatarFatalHitsProduceOneTerminalOwner() throws InterruptedException {
		FatalResolutionGate gate = new FatalResolutionGate();
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger accepted = new AtomicInteger();
		Thread body = Thread.ofVirtual().start(() -> claim(start, gate,
				FatalResolutionGate.Cause.BODY, accepted));
		Thread avatar = Thread.ofVirtual().start(() -> claim(start, gate,
				FatalResolutionGate.Cause.AVATAR, accepted));
		start.countDown();
		body.join();
		avatar.join();

		assertEquals(1, accepted.get());
		assertNotNull(gate.winner());
	}

	private static void claim(CountDownLatch start, FatalResolutionGate gate,
			FatalResolutionGate.Cause cause, AtomicInteger accepted) {
		try {
			start.await();
			if (gate.claim(cause)) accepted.incrementAndGet();
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
