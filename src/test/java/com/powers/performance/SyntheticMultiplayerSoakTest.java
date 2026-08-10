package com.powers.performance;

import com.powers.diagnostics.TickWorkMetrics;
import com.powers.force.ForceAuraWorkBudget;
import com.powers.fx.ParticleBudget;
import com.powers.mind.BodyProxyTicketRules;
import com.powers.network.PacketRateLimiter;
import com.powers.power.artifact.ArtifactFieldPulseRules;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic 10/50/100-player budget soak without requiring wall-clock Minecraft clients. */
class SyntheticMultiplayerSoakTest {
	@Test
	void serverBudgetsRemainBoundedAcrossAdvertisedPlayerCounts() {
		for (int players : List.of(10, 50, 100)) {
			assertTimeoutPreemptively(Duration.ofSeconds(3), () -> soak(players),
					players + "-player synthetic workload exceeded its tick-time envelope");
		}
	}

	private static void soak(int playerCount) {
		List<UUID> players = new ArrayList<>();
		for (int index = 0; index < playerCount; index++) players.add(UUID.randomUUID());
		PacketRateLimiter packets = new PacketRateLimiter();
		ParticleBudget particles = new ParticleBudget(512);
		TickWorkMetrics metrics = new TickWorkMetrics();
		for (int tick = 0; tick < 1_200; tick++) {
			ForceAuraWorkBudget scans = new ForceAuraWorkBudget(2_048, 32);
			int acceptedPackets = 0;
			for (int index = 0; index < players.size(); index++) {
				if (packets.allow(players.get(index), PacketRateLimiter.Lane.ACTIVATION, tick)) {
					acceptedPackets++;
				}
				if (ArtifactFieldPulseRules.shouldPulse(tick, index)) {
					int allowance = scans.allowanceForPlayer();
					scans.recordInspections(allowance);
				}
			}
			int granted = particles.claim(tick, playerCount * 64);
			metrics.recordPackets(tick, acceptedPackets);
			metrics.recordParticles(tick, granted);
			metrics.recordEntityInspections(tick, scans.inspected());
			TickWorkMetrics.Snapshot snapshot = metrics.snapshot(tick);
			assertTrue(snapshot.particles() <= 512);
			assertTrue(snapshot.entityInspections() <= 2_048);
			assertTrue(snapshot.packets() <= playerCount);
		}
		assertEquals(1, BodyProxyTicketRules.maximumChunksPerBody());
	}
}
