package com.powers.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/** Paces the shared test server at at most 20 TPS while asynchronous generation is exercised. */
final class GameTestClock {
	private static final long TICK_NANOS = 50_000_000L;
	private long previousTick = System.nanoTime();

	private GameTestClock() { }

	static void atNormalServerRate(GameTestHelper helper, int ticks) {
		GameTestClock clock = new GameTestClock();
		for (int tick = 0; tick < ticks; tick++) {
			// GameTestInfo keys scheduled work by Runnable identity. A stateless
			// lambda can be reused by the JVM and silently collapse this schedule.
			helper.runAtTickTime(tick, new Runnable() {
				@Override
				public void run() {
					clock.waitForNextTick();
				}
			});
		}
	}

	private void waitForNextTick() {
		long remaining = previousTick + TICK_NANOS - System.nanoTime();
		if (remaining > 0) {
			try {
				Thread.sleep(java.time.Duration.ofNanos(remaining));
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Normal-rate GameTest clock was interrupted", interrupted);
			}
		}
		// Never catch up by sprinting ticks after slow setup or generation work.
		previousTick = System.nanoTime();
	}
}
