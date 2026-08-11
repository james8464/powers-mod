package com.powers.performance;

import com.powers.companion.combat.ShadowCombatFacts;
import com.powers.companion.combat.ShadowLearningState;
import com.powers.companion.combat.ShadowPowerAction;
import com.powers.companion.combat.ShadowPowerCatalogue;
import com.powers.companion.combat.ShadowRequestRange;
import com.powers.companion.combat.ShadowTacticalPlanner;
import com.powers.diagnostics.TickWorkMetrics;
import com.powers.force.ForceAuraWorkBudget;
import com.powers.fx.ParticleBudget;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.ActiveMagicIndex;
import com.powers.magic.runtime.MagicPresence;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.MagicRayCollisionIndex;
import com.powers.magic.runtime.MagicRayCollisionRules;
import com.powers.magic.runtime.MagicRaySegment;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.mind.BodyProxyTicketRules;
import com.powers.network.UniqueNameIndex;
import com.powers.network.PacketRateLimiter;
import com.powers.power.AmethystWardIndex;
import com.powers.power.PowerRegistry;
import com.powers.power.artifact.ArtifactFieldPulseRules;
import com.powers.util.BoundedRoundRobinQueue;
import com.powers.util.ChunkSpatialIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic 10/50/100-player budget soak without requiring wall-clock Minecraft clients. */
class SyntheticMultiplayerSoakTest {
	private static final long HUNDRED_PLAYER_ALLOCATION_BUDGET_BYTES = 2_750_000_000L;

	@Test
	void serverBudgetsRemainBoundedAcrossAdvertisedPlayerCounts() {
		for (int players : List.of(10, 50, 100)) {
			assertTimeoutPreemptively(Duration.ofSeconds(3), () -> soak(players),
					players + "-player synthetic workload exceeded its tick-time envelope");
		}
	}

	@Test
	void representativeHundredPlayerWorkloadHasAnAllocationRegressionBudget() {
		var bean = ManagementFactory.getThreadMXBean();
		assertTrue(bean instanceof com.sun.management.ThreadMXBean,
				"Java 25 CI must expose per-thread allocation accounting");
		var allocations = (com.sun.management.ThreadMXBean) bean;
		assertTrue(allocations.isThreadAllocatedMemorySupported(),
				"Java 25 CI must support per-thread allocation accounting");
		if (!allocations.isThreadAllocatedMemoryEnabled()) {
			allocations.setThreadAllocatedMemoryEnabled(true);
		}

		// Warm class loading and JIT bookkeeping before measuring only the workload.
		soak(10);
		long thread = Thread.currentThread().threadId();
		long before = allocations.getThreadAllocatedBytes(thread);
		soak(100);
		long allocated = allocations.getThreadAllocatedBytes(thread) - before;
		assertTrue(allocated <= HUNDRED_PLAYER_ALLOCATION_BUDGET_BYTES,
				() -> "100-player synthetic allocation budget exceeded: " + allocated + " bytes");
	}

	private static void soak(int playerCount) {
		PowerRegistry.initialize();
		List<UUID> players = new ArrayList<>();
		List<ShadowLearningState> shadowLearning = new ArrayList<>();
		List<ShadowPowerAction> shadowActions = ShadowPowerCatalogue.actions();
		ActiveMagicIndex magic = new ActiveMagicIndex(16);
		ChunkSpatialIndex<UUID, Integer> fields = new ChunkSpatialIndex<>(16);
		AmethystWardIndex wards = new AmethystWardIndex();
		UniqueNameIndex<UUID> names = new UniqueNameIndex<>();
		BoundedRoundRobinQueue<UUID> rotatingWork = new BoundedRoundRobinQueue<>();
		MagicRayCollisionIndex rays = new MagicRayCollisionIndex();
		for (int index = 0; index < playerCount; index++) {
			UUID player = new UUID(0L, index + 1L);
			double x = index * 64.0;
			players.add(player);
			shadowLearning.add(new ShadowLearningState());
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
			rays.tick(tick);
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
				if (tick % 10 == 0) {
					ShadowCombatFacts facts = new ShadowCombatFacts(4.0 + index % 24,
							0.75, index % 3 == 0 ? 0.8 : 0.3, index % 2 == 0,
							index % 20 == 0, 0.9, 0.8, 0.7, false, false,
							ShadowRequestRange.AUTO);
					var decision = ShadowTacticalPlanner.choose(
							shadowActions, facts, shadowLearning.get(index));
					assertTrue(decision.action() != null && decision.evaluatedCount() <= 26);
					if (tick % 100 == 0) shadowLearning.get(index).adjust(
							facts.contextKey(decision.mode()), facts.archetype().name(),
							decision.action().id(), 0.25);
				}
			}
			if (tick % 20 == 0) {
				for (int index = 0; index < players.size(); index++) {
					double height = 64.0 + index / 2;
					boolean horizontal = (index & 1) == 0;
					rays.submit(new MagicRaySegment(players.get(index),
							horizontal ? "energy_beam" : "void_beam", "minecraft:overworld",
							horizontal ? new Vec3(-4, height, 0) : new Vec3(0, height, -4),
							horizontal ? new Vec3(4, height, 0) : new Vec3(0, height, 4), tick));
				}
				assertTrue(rays.collisionsThisTick() <= MagicRayCollisionRules.MAX_COLLISIONS_PER_TICK);
				assertTrue(rays.activeSegmentCount() <= MagicRayCollisionRules.MAX_SEGMENTS_PER_DIMENSION);
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
		for (int index = 0; index < 96; index++) {
			shadowLearning.getFirst().adjust("context_" + index, "general",
					shadowActions.get(index % shadowActions.size()).id(), 0.1);
		}
		assertEquals(ShadowLearningState.MAX_CONTEXTS,
				shadowLearning.getFirst().contextCount());
		assertTrue(shadowLearning.stream().allMatch(state -> state.typeCount()
				<= ShadowLearningState.MAX_TYPES && state.encode().length() <= 32_768));
		fields.clear();
		wards.clear();
		names.clear();
		rotatingWork.clear();
		rays.clear();
		assertEquals(0, fields.cellCount());
		assertEquals(0, wards.size());
		assertEquals(0, rotatingWork.size());
		assertEquals(0, rays.activeSegmentCount());
		assertEquals(1, BodyProxyTicketRules.maximumChunksPerBody());
	}
}
