package com.powers.gametest;

import com.mojang.authlib.GameProfile;
import com.powers.api.v1.CastContext;
import com.powers.api.v1.CastSource;
import com.powers.api.v1.PhysicalPresence;
import com.powers.api.v1.PowersApiRuntime;
import com.powers.api.v1.PresenceHandle;
import com.powers.api.v1.PresenceKind;
import com.powers.example.ExamplePowersExtension;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.protection.ProtectionAction;
import com.powers.protection.ProtectionQuery;
import com.powers.player.PlayerPowers;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Dedicated-server proof that the independently compiled v1 extension reaches production owners. */
public final class ApiCompatibilityGameTests {
	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void exampleExtensionUsesActionPresenceProtectionAndLifecycleProductionPaths(GameTestHelper helper) {
		helper.assertTrue(ExamplePowersExtension.started(), "Example extension missed SERVER_STARTED");
		helper.assertTrue(MagicRuntime.catalogue().definition(
				new MagicActionId(ExamplePowersExtension.ACTION_ID)) != null,
				"Example action did not reach the canonical production catalogue");
		ServerPlayer actor = helper.makeMockServerPlayerInLevel();
		var initialContext = PowersApiRuntime.global().api().castContext(actor,
				ExamplePowersExtension.ACTION_ID);
		helper.assertTrue(initialContext.actor() == actor && initialContext.source() == CastSource.EXTENSION,
				"Public cast context was not authored by the bound production server");
		BlockPos denied = new BlockPos(13, 64, 0);
		helper.assertTrue(!PowerProtectionAdapters.allows(new ProtectionQuery(
				ProtectionAction.RITUAL, helper.getLevel(), denied, null, null)),
				"Example protection service did not reach the fail-closed production chain");
		Vec3 originalPosition = actor.position();
		actor.snapTo(13.5, 64, 0.5);
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(initialContext,
				new PhysicalPresence(helper.getLevel(), 13.5, 64.5, 0.5, 1.0,
						helper.getLevel().getServer().getTickCount() + 5, PresenceKind.FIELD))),
				"Protected location entered the physical collision runtime");
		actor.snapTo(originalPosition.x, originalPosition.y, originalPosition.z);
		CastContext forged = new CastContext() {
			@Override public ServerPlayer actor() { return actor; }
			@Override public String actionId() { return ExamplePowersExtension.ACTION_ID; }
			@Override public CastSource source() { return CastSource.EXTENSION; }
		};
		Vec3 center = actor.position().add(0, 1, 0);
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(forged,
				new PhysicalPresence(helper.getLevel(), center.x, center.y, center.z, 2.0,
						helper.getLevel().getServer().getTickCount() + 10, PresenceKind.FIELD))),
				"Forged public context entered the physical collision runtime");
		var synthetic = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new GameProfile(actor.getUUID(), "replacement"), ClientInformation.createDefault());
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().castContext(synthetic,
				ExamplePowersExtension.ACTION_ID)), "Synthetic replacement actor received cast authority");
		var outOfRange = PowersApiRuntime.global().api().castContext(actor,
				ExamplePowersExtension.ACTION_ID);
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(outOfRange,
				new PhysicalPresence(helper.getLevel(), center.x + 7, center.y, center.z, 2.0,
						helper.getLevel().getServer().getTickCount() + 10, PresenceKind.FIELD))),
				"Out-of-range external presence bypassed action bounds");
		var acceptedContext = PowersApiRuntime.global().api().castContext(actor, ExamplePowersExtension.ACTION_ID);
		int energyBefore = PlayerPowers.get(actor).energy();
		var handle = PowersApiRuntime.global().api().registerPresence(acceptedContext, new PhysicalPresence(
				helper.getLevel(),
				center.x, center.y, center.z, 2.0,
				helper.getLevel().getServer().getTickCount() + 10, PresenceKind.FIELD));
		helper.assertTrue(MagicRuntime.global().activePresenceCount() > 0,
				"Example presence did not reach the production spatial runtime");
		helper.assertTrue(PowersApiRuntime.global().api().removePresence(handle),
				"Example presence could not be removed through its public lifecycle token");
		helper.assertTrue(PlayerPowers.get(actor).energy() == energyBefore - 10,
				"Accepted external presence did not pay authoritative action energy");
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(acceptedContext,
				new PhysicalPresence(helper.getLevel(), center.x, center.y, center.z, 1.0,
						helper.getLevel().getServer().getTickCount() + 5, PresenceKind.FIELD))),
				"Consumed cast context was reusable");
		var cooldownContext = PowersApiRuntime.global().api().castContext(actor,
				ExamplePowersExtension.ACTION_ID);
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(cooldownContext,
				new PhysicalPresence(helper.getLevel(), center.x, center.y, center.z, 1.0,
						helper.getLevel().getServer().getTickCount() + 5, PresenceKind.FIELD))),
				"External action cooldown was bypassed");
		TestingOverrides.setAll(actor.getUUID(), true);
		try {
			for (int index = 0; index < 3; index++) {
				var boundedContext = PowersApiRuntime.global().api().castContext(actor,
						ExamplePowersExtension.ACTION_ID);
				var boundedHandle = PowersApiRuntime.global().api().registerPresence(boundedContext,
						new PhysicalPresence(helper.getLevel(), center.x, center.y, center.z, 1.0,
								helper.getLevel().getServer().getTickCount() + 5, PresenceKind.FIELD));
				helper.assertTrue(PowersApiRuntime.global().api().removePresence(boundedHandle),
						"Bounded accepted presence could not be removed");
			}
			var overBudget = PowersApiRuntime.global().api().castContext(actor,
					ExamplePowersExtension.ACTION_ID);
			helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(overBudget,
					new PhysicalPresence(helper.getLevel(), center.x, center.y, center.z, 1.0,
							helper.getLevel().getServer().getTickCount() + 5, PresenceKind.FIELD))),
					"Per-player presence work budget was unbounded");
		} finally {
			TestingOverrides.clear(actor.getUUID());
		}
		actor.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().castContext(actor,
				ExamplePowersExtension.ACTION_ID)), "Removed player retained cast authority");
		helper.succeed();
	}

	@GameTest(maxTicks = 42)
	@SuppressWarnings("removal")
	public void expiredApiPresencesReclaimCapacityWithoutAliasingHandles(GameTestHelper helper) {
		ServerPlayer actor = helper.makeMockServerPlayerInLevel();
		actor.snapTo(100.0, 64.0, 0.0);
		List<PresenceHandle> expiredHandles = new ArrayList<>();
		long expiresAt = helper.getLevel().getServer().getTickCount() + 35L;
		registerBatch(helper, actor, 4, expiresAt, expiredHandles);
		for (long tick = 1L; tick < 32L; tick++) {
			helper.runAtTickTime(tick, () -> registerBatch(helper, actor, 4, expiresAt, expiredHandles));
		}
		helper.runAtTickTime(32L, () -> {
			CastContext context = PowersApiRuntime.global().api().castContext(actor,
					ExamplePowersExtension.ACTION_ID);
			Vec3 point = actor.position().add(0.0, 1.0, 0.0);
			helper.assertTrue(rejected(() -> PowersApiRuntime.global().api().registerPresence(context,
					new PhysicalPresence(helper.getLevel(), point.x, point.y, point.z, 1.0,
							expiresAt, PresenceKind.FIELD))),
					"Active API presence cap was not enforced");
		});
		helper.runAtTickTime(37L, () -> {
			TestingOverrides.setAll(actor.getUUID(), true);
			try {
				CastContext context = PowersApiRuntime.global().api().castContext(actor,
						ExamplePowersExtension.ACTION_ID);
				Vec3 point = actor.position().add(0.0, 1.0, 0.0);
				PresenceHandle replacement = PowersApiRuntime.global().api().registerPresence(context,
						new PhysicalPresence(helper.getLevel(), point.x, point.y, point.z, 1.0,
								helper.getLevel().getServer().getTickCount() + 5L, PresenceKind.FIELD));
				helper.assertTrue(!PowersApiRuntime.global().api().removePresence(expiredHandles.getFirst()),
						"Expired handle still referred to active API state");
				helper.assertTrue(PowersApiRuntime.global().api().removePresence(replacement),
						"Expired handle removed or aliased the replacement presence");
			} finally {
				TestingOverrides.clear(actor.getUUID());
			}
			helper.succeed();
		});
	}

	private static void registerBatch(GameTestHelper helper, ServerPlayer actor,
			int registrations, long expiresAt, List<PresenceHandle> handles) {
		TestingOverrides.setAll(actor.getUUID(), true);
		try {
			for (int registration = 0; registration < registrations; registration++) {
				CastContext context = PowersApiRuntime.global().api().castContext(actor,
						ExamplePowersExtension.ACTION_ID);
				Vec3 point = actor.position().add(registration * 0.25, 1.0, 0.0);
				handles.add(PowersApiRuntime.global().api().registerPresence(context,
						new PhysicalPresence(helper.getLevel(), point.x, point.y, point.z, 1.0,
								expiresAt, PresenceKind.FIELD)));
			}
		} finally {
			TestingOverrides.clear(actor.getUUID());
		}
	}

	private static boolean rejected(Runnable operation) {
		try { operation.run(); return false; }
		catch (IllegalArgumentException | IllegalStateException expected) { return true; }
	}
}
