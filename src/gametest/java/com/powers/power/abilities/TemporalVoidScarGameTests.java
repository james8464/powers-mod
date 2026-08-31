package com.powers.power.abilities;

import com.powers.magic.InteractionContext;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicCastContext;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PresenceAnchor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

/** Covers the scar producer/shared-index clock boundary on a mature world. */
@SuppressWarnings("removal")
public final class TemporalVoidScarGameTests {
	@GameTest(environment = "powers:temporal_void_scar_isolated", maxTicks = 20)
	public void scarPresenceSurvivesWhenWorldClockLeadsControlClock(GameTestHelper helper) {
		var level = helper.getLevel();
		var data = (ServerLevelData) level.getLevelData();
		long restoreWorldTime = level.getGameTime();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Vec3 center = helper.absoluteVec(new Vec3(3.0, 3.0, 3.0));
		MagicRuntime runtime = MagicRuntime.global();
		long started = 2_000_000L + level.getServer().getTickCount();
		try {
			data.setGameTime(started);
			helper.assertTrue(VoidScarManager.create(owner, center, 2.0, 80, 1.5F, 0, 20, false),
					"Real Void Scar fixture was rejected");
			data.setGameTime(started + 1);
			runtime.tick(level.getGameTime());
			helper.assertTrue(hasScar(runtime, owner, center),
					"World-clock expiry removed a still-live control-created Void Scar presence");
			data.setGameTime(started + 79);
			runtime.tick(level.getGameTime());
			helper.assertTrue(hasScar(runtime, owner, center), "Void Scar presence expired before its deadline");
			data.setGameTime(started + 80);
			runtime.tick(level.getGameTime());
			helper.assertFalse(hasScar(runtime, owner, center), "Void Scar presence survived its world deadline");
			VoidScarManager.clear(owner.getUUID());
			helper.assertTrue(VoidScarManager.create(owner, center, 2.0, 80, 1.5F, 0, 20, false),
					"Lifecycle cleanup fixture was rejected");
			VoidScarManager.clear(owner.getUUID());
			helper.assertFalse(hasScar(runtime, owner, center), "Owner cleanup retained the scar presence");
		} finally {
			VoidScarManager.clear(owner.getUUID());
			data.setGameTime(restoreWorldTime);
		}
		helper.succeed();
	}

	private static boolean hasScar(MagicRuntime runtime, ServerPlayer owner, Vec3 center) {
		var cast = new MagicCastContext(MagicRuntime.catalogue().definition(new MagicActionId("energy_beam")),
				owner.getUUID(), owner.level().dimension().identifier().toString(),
				PresenceAnchor.fixed(center.x, center.y, center.z), 4.0,
				owner.level().getGameTime(), InteractionContext.DEFAULT);
		return runtime.previewCast(cast).reactions().stream().anyMatch(event ->
				event.existing().owner().equals(owner.getUUID())
						&& event.existing().action().equals(new MagicActionId("void_beam")));
	}
}
