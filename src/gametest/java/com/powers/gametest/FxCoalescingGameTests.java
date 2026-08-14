package com.powers.gametest;

import com.powers.config.PowersConfigLoader;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.fx.PowerFx;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.network.FxPacketCoalescer;
import com.powers.network.EventAudioPackets;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Live proof that visual transport suppression cannot suppress physical resolution. */
public final class FxCoalescingGameTests {
	public FxCoalescingGameTests() {
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void semanticBeamAndShapeFanoutReachTheLiveTransport(GameTestHelper helper) {
		ServerPlayer observer = helper.makeMockServerPlayerInLevel();
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		observer.teleportTo(center.x, center.y, center.z);
		helper.runAfterDelay(1, () -> {
			int before = ServerRuntimeMetrics.snapshot(helper.getLevel().getServer()).particles();
			PowerFx.beam(helper.getLevel(), center, center.add(12.0, 0.0, 0.0),
					ParticleTypes.ELECTRIC_SPARK, 48);
			PowerFx.rune(helper.getLevel(), center, 3.0, 0xB36BFF, 48, 0.25);

			var work = ServerRuntimeMetrics.snapshot(helper.getLevel().getServer());
			helper.assertTrue(work.particles() >= before,
					"Production semantic fan-out decremented tick accounting");
			helper.assertTrue(work.particles() <= PowersConfigLoader.get().maxParticlesPerTick(),
					"Semantic fan-out bypassed the global visual budget");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void duplicateVisualUpdatesLeaveBeamCollisionAuthoritative(GameTestHelper helper) {
		UUID firstOwner = new UUID(0L, 101L);
		UUID secondOwner = new UUID(0L, 202L);
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		long tick = helper.getLevel().getServer().getTickCount();
		var first = PhysicalMagicPresences.registerFixed(new MagicActionId("energy_beam"),
				firstOwner, helper.getLevel(), center.add(96.0, 0.0, 0.0), 1.0, tick + 100,
				MagicPresenceHandle.Kind.BEAM);
		var second = PhysicalMagicPresences.registerFixed(new MagicActionId("void_beam"),
				secondOwner, helper.getLevel(), center, 1.0, tick + 100,
				MagicPresenceHandle.Kind.BEAM);
		try {
			FxPacketCoalescer coalescer = new FxPacketCoalescer(128);
			for (int attempt = 0; attempt < 64; attempt++) {
				coalescer.allow(tick, firstOwner,
						helper.getLevel().dimension().identifier().toString(),
						(int) Math.floor(center.x) >> 4, (int) Math.floor(center.z) >> 4,
						"energy_beam", "sustain", 55);
			}
			helper.assertTrue(coalescer.trafficSnapshot().deliveredPackets() == 1,
					"Duplicate visual beam updates were not coalesced");
			helper.assertTrue(MagicRuntime.global().movePresence(first.presenceId(),
					helper.getLevel().dimension().identifier().toString(),
					PresenceAnchor.fixed(center.x, center.y, center.z)),
					"Could not move the live beam presence into collision");
			helper.assertTrue(PhysicalMagicPresences.collideNearby(
					first, helper.getLevel(), center, tick) == 1,
					"Visual coalescing suppressed the physical beam collision");
			helper.assertTrue(PhysicalMagicPresences.collideNearby(
					first, helper.getLevel(), center, tick) == 0,
					"Physical collision resolved more than once");
		} finally {
			PhysicalMagicPresences.remove(first);
			PhysicalMagicPresences.remove(second);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	@SuppressWarnings("removal")
	public void eventScaleLodReachesNearMidAndFarObservers(GameTestHelper helper) {
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		ServerPlayer observer = helper.makeMockServerPlayerInLevel();
		observer.teleportTo(center.x + 32.0, center.y, center.z);
		PowerFx.resetLodMetrics(helper.getLevel().getServer());
		helper.runAfterDelay(2, () -> {
			PowerFx.eventRune(helper.getLevel(), center, 12.0, 0xFFF2A8, 64, 0.25);
			observer.teleportTo(center.x + 144.0, center.y, center.z);
		});
		helper.runAfterDelay(3, () ->
				PowerFx.eventRune(helper.getLevel(), center, 12.0, 0xFFF2A8, 64, 0.25));
		helper.runAfterDelay(4, () -> observer.teleportTo(center.x + 1_800.0, center.y, center.z));
		helper.runAfterDelay(5, () -> {
			PowerFx.eventRune(helper.getLevel(), center, 12.0, 0xFFF2A8, 64, 0.25);
			PowerFx.eventSound(helper.getLevel(), center,
					EventAudioPackets.Cue.LIGHT_HERALD, 3.0F, 0.65F);
			var snapshot = PowerFx.lodSnapshot(helper.getLevel().getServer());
			helper.assertTrue(snapshot.nearDeliveries() >= 1,
					"Near observer did not receive the full event silhouette");
			helper.assertTrue(snapshot.midDeliveries() >= 1,
					"Mid observer did not receive the reduced-density event silhouette");
			helper.assertTrue(snapshot.farDeliveries() >= 1,
					"Far observer did not receive the bounded event silhouette");
			helper.assertTrue(snapshot.nearSamples() > 0 && snapshot.midSamples() > 0
					&& snapshot.farSamples() > 0,
					"One visible tier consumed no bounded presentation budget");
			helper.assertTrue(snapshot.farAudio() == 1,
					"Far observer did not receive exactly one restrained event-audio layer");
			helper.succeed();
		});
	}
}
