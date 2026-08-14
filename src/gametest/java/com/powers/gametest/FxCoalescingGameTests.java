package com.powers.gametest;

import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.fx.PowerFx;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.network.FxPacketCoalescer;
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
			ServerRuntimeMetrics.clear();
			PowerFx.beam(helper.getLevel(), center, center.add(12.0, 0.0, 0.0),
					ParticleTypes.ELECTRIC_SPARK, 48);
			PowerFx.rune(helper.getLevel(), center, 3.0, 0xB36BFF, 48, 0.25);

			var work = ServerRuntimeMetrics.snapshot(helper.getLevel().getServer());
			helper.assertTrue(work.particles() > 0,
					"Production semantic beam/shape fan-out did not claim viewer work");
			helper.assertTrue(work.particles() <= 128,
					"Semantic fan-out bypassed the per-viewer visual budget");
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
}
