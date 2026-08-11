package com.powers.performance;

import com.powers.diagnostics.TickWorkMetrics;
import com.powers.force.ForceAuraWorkBudget;
import com.powers.fx.ParticleBudget;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.ActiveMagicIndex;
import com.powers.magic.runtime.MagicPresence;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.mind.BodyProxyTicketRules;
import com.powers.network.UniqueNameIndex;
import com.powers.network.PacketRateLimiter;
import com.powers.power.AmethystWardIndex;
import com.powers.power.artifact.ArtifactFieldPulseRules;
import com.powers.util.BoundedRoundRobinQueue;
import com.powers.util.ChunkSpatialIndex;
import net.minecraft.core.BlockPos;
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
		ActiveMagicIndex magic = new ActiveMagicIndex(16);
		ChunkSpatialIndex<UUID, Integer> fields = new ChunkSpatialIndex<>(16);
		AmethystWardIndex wards = new AmethystWardIndex();
		UniqueNameIndex<UUID> names = new UniqueNameIndex<>();
		BoundedRoundRobinQueue<UUID> rotatingWork = new BoundedRoundRobinQueue<>();
		for (int index = 0; index < playerCount; index++) {
			UUID player = new UUID(0L, index + 1L);
			double x = index * 64.0;
			players.add(player);
			fields.put(player, "minecraft:overworld", x, 0.0, 4.0, index);
			wards.add(BlockPos.containing(x, 64.0, 0.0));
			names.upsert(player, "soak_player_" + index);
			rotatingWork.offer(player);
			magic.register(new MagicPresence(new MagicPresenceId(player),
					new MagicActionId("soak_field"), player, "minecraft:overworld",
					PresenceAnchor.fixed(x, 64.0, 0.0), 4.0, 1_201L));
		}
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
				double x = index * 64.0;
				assertTrue(magic.nearby("minecraft:overworld", x, 64.0, 0.0, 12.0, tick).size() <= 1);
				assertTrue(fields.nearby("minecraft:overworld", x, 0.0, 12.0).size() <= 1);
				assertTrue(wards.nearby(BlockPos.containing(x, 64.0, 0.0), 12).size() <= 1);
			}
			List<UUID> queueBatch = rotatingWork.pollBatch(32);
			assertTrue(queueBatch.size() <= 32);
			queueBatch.forEach(rotatingWork::offer);
			int granted = particles.claim(tick, playerCount * 64);
			metrics.recordPackets(tick, acceptedPackets);
			metrics.recordParticles(tick, granted);
			metrics.recordEntityInspections(tick, scans.inspected());
			TickWorkMetrics.Snapshot snapshot = metrics.snapshot(tick);
			assertTrue(snapshot.particles() <= 512);
			assertTrue(snapshot.entityInspections() <= 2_048);
			assertTrue(snapshot.packets() <= playerCount);
		}
		assertEquals(List.of(players.getLast()), names.candidates("SOAK_PLAYER_" + (playerCount - 1), 2));
		assertEquals(playerCount, magic.expire(1_201L));
		assertEquals(0, magic.cellCount());
		assertEquals(playerCount, fields.size());
		assertEquals(playerCount, wards.size());
		fields.clear();
		wards.clear();
		names.clear();
		rotatingWork.clear();
		assertEquals(0, fields.cellCount());
		assertEquals(0, wards.size());
		assertEquals(0, rotatingWork.size());
		assertEquals(1, BodyProxyTicketRules.maximumChunksPerBody());
	}
}
