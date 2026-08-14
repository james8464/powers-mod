package com.powers.gametest;

import com.powers.PowersMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Live lifecycle proof for owned delayed server callbacks. */
public final class DelayedCallbackGameTests {
	@GameTest(maxTicks = 20)
	public void ownerCancellationPreventsLogoutOrDeathCallback(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();
		AtomicBoolean executed = new AtomicBoolean();
		var token = PowersMod.scheduleDelayed(helper.getLevel().getServer(), 4, owner,
				helper.getLevel().dimension(), owner, "gametest_owner_cancel",
				(server, task) -> executed.set(true));
		helper.assertTrue(token.accepted(), "Owned callback was rejected by an empty scheduler");
		helper.assertTrue(PowersMod.delayedTasks().stream()
				.anyMatch(task -> owner.equals(task.cancellationOwner())),
				"Owned callback did not publish lifecycle diagnostics");
		helper.assertTrue(PowersMod.cancelDelayedTasks(owner) == 1,
				"Logout/death cancellation did not claim the owned callback");
		helper.runAfterDelay(6, () -> {
			helper.assertFalse(executed.get(), "Cancelled callback executed after lifecycle exit");
			helper.assertFalse(PowersMod.delayedTasks().stream()
					.anyMatch(task -> owner.equals(task.cancellationOwner())),
					"Cancelled callback remained in active diagnostics");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void missingDimensionCancelsCallbackWithoutWorldReference(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();
		AtomicBoolean executed = new AtomicBoolean();
		ResourceKey<Level> missing = ResourceKey.create(Registries.DIMENSION,
				PowersMod.id("missing_callback_fixture"));
		PowersMod.scheduleDelayed(helper.getLevel().getServer(), 2, owner, missing, owner,
				"gametest_missing_dimension", (server, task) -> executed.set(true));
		helper.runAfterDelay(4, () -> {
			helper.assertFalse(executed.get(), "Unloaded dimension callback executed");
			helper.succeed();
		});
	}
}
