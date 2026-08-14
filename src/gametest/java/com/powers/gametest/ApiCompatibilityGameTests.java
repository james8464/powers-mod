package com.powers.gametest;

import com.powers.api.v1.PhysicalPresence;
import com.powers.api.v1.CastSource;
import com.powers.api.v1.PowersApiRuntime;
import com.powers.api.v1.PresenceKind;
import com.powers.example.ExamplePowersExtension;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.protection.ProtectionAction;
import com.powers.protection.ProtectionQuery;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Dedicated-server proof that the independently compiled v1 extension reaches production owners. */
public final class ApiCompatibilityGameTests {
	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void exampleExtensionUsesActionPresenceProtectionAndLifecycleProductionPaths(GameTestHelper helper) {
		helper.assertTrue(ExamplePowersExtension.started(), "Example extension missed SERVER_STARTED");
		helper.assertTrue(MagicRuntime.catalogue().definition(
				new MagicActionId(ExamplePowersExtension.ACTION_ID)) != null,
				"Example action did not reach the canonical production catalogue");
		var actor = helper.makeMockServerPlayerInLevel();
		var context = PowersApiRuntime.global().api().castContext(actor,
				ExamplePowersExtension.ACTION_ID);
		helper.assertTrue(context.actor() == actor && context.source() == CastSource.EXTENSION,
				"Public cast context was not authored by the bound production server");
		BlockPos denied = new BlockPos(13, 64, 0);
		helper.assertTrue(!PowerProtectionAdapters.allows(new ProtectionQuery(
				ProtectionAction.RITUAL, helper.getLevel(), denied, null, null)),
				"Example protection service did not reach the fail-closed production chain");
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		var handle = PowersApiRuntime.global().api().registerPresence(new PhysicalPresence(
				ExamplePowersExtension.ACTION_ID, UUID.randomUUID(), helper.getLevel(),
				center.x, center.y, center.z, 2.0,
				helper.getLevel().getServer().getTickCount() + 10, PresenceKind.FIELD));
		helper.assertTrue(MagicRuntime.global().activePresenceCount() > 0,
				"Example presence did not reach the production spatial runtime");
		helper.assertTrue(PowersApiRuntime.global().api().removePresence(handle),
				"Example presence could not be removed through its public lifecycle token");
		helper.succeed();
	}
}
