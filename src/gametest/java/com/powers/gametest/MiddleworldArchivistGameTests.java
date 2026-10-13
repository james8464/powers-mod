package com.powers.gametest;

import com.powers.realm.MiddleworldOutpostManager;
import com.powers.realm.MiddleworldOutpostRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Exercises the real outpost spawn boundary without a synthetic NPC implementation. */
public final class MiddleworldArchivistGameTests {

	@GameTest(maxTicks = 60)
	public void displacedArchivistIsNotDuplicated(GameTestHelper helper) {
		fixture(helper, "displacement_fixture", (level, outpost, original) -> {
			original.setNoGravity(true);
			original.setPos(15.5, original.getY(), 8.5);
			for (int pulse = 0; pulse < 4; pulse++) ensure(level, outpost);
			List<Villager> after = archivists(level, outpost);
			helper.assertTrue(after.size() == 1 && after.getFirst().getUUID().equals(original.getUUID()),
					"Moving an Archivist outside the library spawned a duplicate");
		});
	}

	@GameTest(maxTicks = 60)
	public void unloadedArchivistRetainsItsOutpost(GameTestHelper helper) {
		fixture(helper, "unload_fixture", (level, outpost, original) -> {
			original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
			for (int pulse = 0; pulse < 4; pulse++) ensure(level, outpost);
			helper.assertTrue(archivists(level, outpost).isEmpty(),
					"Unloading an Archivist spawned a replacement before its saved owner returned");
		});
	}

	@GameTest(maxTicks = 60)
	public void destroyedArchivistCanBeReplacedExactlyOnce(GameTestHelper helper) {
		fixture(helper, "destruction_fixture", (level, outpost, original) -> {
			original.remove(Entity.RemovalReason.KILLED);
			for (int pulse = 0; pulse < 4; pulse++) ensure(level, outpost);
			List<Villager> after = archivists(level, outpost);
			helper.assertTrue(after.size() == 1 && !after.getFirst().getUUID().equals(original.getUUID()),
					"Destroyed Archivist did not release its identity for exactly one replacement");
		});
	}

	private static void fixture(GameTestHelper helper, String id, Scenario scenario) {
		ServerLevel level = helper.getLevel();
		var outpost = new MiddleworldOutpostRules.Outpost(id, 8, 8);
		GameTestClock.atNormalServerRate(helper, 40);
		level.setChunkForced(0, 0, true);
		level.getChunk(0, 0);
		helper.runAtTickTime(30, () -> {
			try {
				ensure(level, outpost);
				List<Villager> initial = archivists(level, outpost);
				helper.assertTrue(initial.size() == 1, "Fixture did not spawn exactly one Archivist");
				scenario.run(level, outpost, initial.getFirst());
				helper.succeed();
			} finally {
				level.setChunkForced(0, 0, false);
			}
		});
	}

	private static void ensure(ServerLevel level, MiddleworldOutpostRules.Outpost outpost) {
		try {
			Method method = MiddleworldOutpostManager.class.getDeclaredMethod("ensureArchivist",
					ServerLevel.class, MiddleworldOutpostRules.Outpost.class);
			method.setAccessible(true);
			method.invoke(null, level, outpost);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Cannot invoke outpost spawn boundary", exception);
		}
	}

	private static List<Villager> archivists(ServerLevel level, MiddleworldOutpostRules.Outpost outpost) {
		List<Villager> result = new ArrayList<>();
		for (var entity : level.getAllEntities()) {
			if (entity instanceof Villager villager
					&& villager.entityTags().contains("powers_middleworld_archivist_" + outpost.id())) {
				result.add(villager);
			}
		}
		return result;
	}

	@FunctionalInterface
	private interface Scenario {
		void run(ServerLevel level, MiddleworldOutpostRules.Outpost outpost, Villager original);
	}
}
