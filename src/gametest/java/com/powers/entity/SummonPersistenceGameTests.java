package com.powers.entity;

import com.powers.PowersEntities;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.power.artifact.ArtifactGuardianSummons;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Live save/load proof for compact finite guardian state and derived indexes. */
public final class SummonPersistenceGameTests {
	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void loadedNaturalAndReassignedGuardiansRebindIndexes(GameTestHelper helper) {
		ArtifactGuardianSummons.clear();
		ServerLevel level = helper.getLevel();
		var firstOwner = helper.makeMockServerPlayerInLevel();
		var secondOwner = helper.makeMockServerPlayerInLevel();
		firstOwner.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4))));
		secondOwner.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 2, 4))));

		DarknessCreature natural = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(2, 2, 2));
		natural.configureGuardian(firstOwner.getUUID(), 1_200, false);
		helper.assertTrue(ArtifactGuardianSummons.indexedGuardianCount() == 1
					&& ArtifactGuardianSummons.lifecycleIndexRebuildCount() == 0
					&& ArtifactGuardianSummons.runtimeRebindCount() == 1,
				"A loaded natural guardian did not enter the finite-summon index once");

		natural.configureGuardian(secondOwner.getUUID(), 1_200, true);
		helper.assertTrue(ArtifactGuardianSummons.indexedGuardianCount() == 1
					&& ArtifactGuardianSummons.runtimeRebindCount() == 2,
				"Owner/tier reassignment did not atomically rebuild one index membership");
		natural.setHealth(200.0F);
		natural.configureGuardian(secondOwner.getUUID(), 1_200, false);
		helper.assertTrue(natural.getMaxHealth() == 100.0F && natural.getHealth() <= 100.0F
					&& natural.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
							.getBaseValue() == 12.0
					&& natural.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
							.getBaseValue() == 16.0
					&& ArtifactGuardianSummons.runtimeRebindCount() == 3,
				"Elite-to-normal rebind retained elite attributes or over-cap health");
		natural.configureGuardian(secondOwner.getUUID(), 1_200, true);
		helper.assertTrue(ArtifactGuardianSummons.runtimeRebindCount() == 4,
				"Final elite rebind was not recorded exactly once");
		int additionalElite = ArtifactGuardianSummons.summon(secondOwner,
				ArtifactAlignment.DARKNESS, 2, true, null, true);
		helper.assertTrue(additionalElite == 1,
				"Reassigned elite guardian was absent from its new owner's tier cap");
		ArtifactGuardianSummons.revokeOwner(level.getServer(), firstOwner.getUUID(),
				ArtifactAlignment.DARKNESS);
		helper.assertTrue(!natural.isRemoved(),
				"Old owner membership survived guardian reassignment");
		ArtifactGuardianSummons.revokeOwner(level.getServer(), secondOwner.getUUID(),
				ArtifactAlignment.DARKNESS);
		helper.assertTrue(natural.isRemoved(),
				"New owner could not revoke its reassigned guardian");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void expiredUnloadedSummonCannotConsumeIndexCapacity(GameTestHelper helper) {
		ArtifactGuardianSummons.clear();
		ServerLevel level = helper.getLevel();
		DarknessCreature original = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(original != null, "Darkness guardian entity type did not create");
		original.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
		original.configureGuardian(null, 1, false);
		TagValueOutput output = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING, level.registryAccess());
		original.saveWithoutId(output);

		helper.runAfterDelay(2, () -> {
			DarknessCreature expired = PowersEntities.DARKNESS_CREATURE.create(
					level, EntitySpawnReason.LOAD);
			helper.assertTrue(expired != null, "Guardian could not be recreated for expiry load");
			expired.load(TagValueInput.create(ProblemReporter.DISCARDING,
					level.registryAccess(), output.buildResult()));
			level.addFreshEntity(expired);
			helper.assertTrue(expired.isRemoved(),
					"An expired unloaded guardian remained active after load");
			helper.assertTrue(!ArtifactGuardianSummons.isIndexed(expired.getUUID()),
					"An expired unloaded guardian consumed derived-index capacity");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void unownedSummonsNeverConsumeOwnerTierCapacity(GameTestHelper helper) {
		ArtifactGuardianSummons.clear();
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4))));
		int spawned = ArtifactGuardianSummons.summon(caster, ArtifactAlignment.DARKNESS,
				1, false, caster, false);
		helper.assertTrue(spawned == 1, "Unowned guardian fixture did not summon");
		helper.assertTrue(ArtifactGuardianSummons.ownedGuardianCount(caster.getUUID(), false) == 0,
				"An unowned hostile guardian consumed the caster's owned cap");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void summonRoundTripRebuildsDerivedIndexOnce(GameTestHelper helper) {
		ArtifactGuardianSummons.clear();
		ServerLevel level = helper.getLevel();
		DarknessCreature original = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(original != null, "Darkness guardian entity type did not create");
		original.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
		original.configureGuardian(helper.makeMockServerPlayerInLevel().getUUID(), 1_200, true);
		original.setHealth(90.0F);
		original.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
		original.getAttribute(Attributes.ARMOR).setBaseValue(12.0);
		original.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0);
		helper.assertTrue(level.addFreshEntity(original), "Finite guardian did not enter the level");

		helper.assertTrue(ArtifactGuardianSummons.indexedGuardianCount() == 1,
				"Entity-load lifecycle did not rebuild the guardian index");
		helper.assertTrue(ArtifactGuardianSummons.lifecycleIndexRebuildCount() == 1,
				"A single entity load rebuilt its derived index more than once");
		ArtifactGuardianSummons.trackLoaded(original);
		helper.assertTrue(ArtifactGuardianSummons.lifecycleIndexRebuildCount() == 1,
				"A duplicate lifecycle callback rebuilt the derived index again");

		TagValueOutput output = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING, level.registryAccess());
		original.saveWithoutId(output);
		var tag = output.buildResult();
		var summon = tag.getCompoundOrEmpty("PowersSummon");
		helper.assertTrue(summon.keySet().equals(Set.of("o", "t", "a", "e")),
				"Finite guardian persisted data outside the compact summon contract: "
						+ summon.keySet());
		helper.assertTrue(summon.sizeInBytes() <= 512,
				"Compact summon state exceeded its fixed save-size budget");
		helper.assertTrue(tag.contains("UUID") && !tag.contains("PowersGuardianOwner")
				&& !tag.contains("PowersGuardianLifetime")
				&& !tag.contains("PowersEliteGuardian"),
				"Save omitted vanilla stable identity or retained legacy summon fields");

		var expected = original.summonRecord();
		ArtifactGuardianSummons.untrackLoaded(original);
		original.discard();
		DarknessCreature loaded = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(loaded != null, "Guardian could not be recreated for load");
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), tag));
		helper.assertTrue(expected.equals(loaded.summonRecord()),
				"Compact summon facts changed across save/load");
		helper.assertTrue(loaded.getMaxHealth() == 240.0F && loaded.getHealth() == 90.0F
					&& loaded.getAttribute(Attributes.ARMOR).getBaseValue() == 22.0
					&& loaded.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() == 34.0,
				"Compact elite archetype did not restore authoritative attributes");
		helper.assertTrue(level.addFreshEntity(loaded), "Loaded guardian did not re-enter the level");
		helper.assertTrue(ArtifactGuardianSummons.lifecycleIndexRebuildCount() == 2,
				"Reload did not rebuild the derived guardian index exactly once");
		ArtifactGuardianSummons.trackLoaded(loaded);
		helper.assertTrue(ArtifactGuardianSummons.lifecycleIndexRebuildCount() == 2,
				"Duplicate post-load tracking rebuilt the index twice");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void legacyAndMalformedSummonDataMigrateFailClosed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var owner = helper.makeMockServerPlayerInLevel();
		DarknessCreature seed = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(seed != null, "Guardian seed could not be created");
		TagValueOutput output = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING, level.registryAccess());
		seed.saveWithoutId(output);
		var legacy = output.buildResult();
		legacy.putString("PowersGuardianOwner", owner.getUUID().toString());
		legacy.putInt("PowersGuardianLifetime", 600);
		legacy.putBoolean("PowersEliteGuardian", true);

		DarknessCreature migrated = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(migrated != null, "Legacy guardian could not be recreated");
		long loadedAt = level.getGameTime();
		migrated.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), legacy));
		var migratedRecord = migrated.summonRecord();
		helper.assertTrue(migratedRecord != null
					&& owner.getUUID().equals(migratedRecord.ownerId())
					&& migratedRecord.archetype() == LongLivedSummonRecord.Archetype.ELITE
					&& migratedRecord.expiresAtGameTime() == loadedAt + 600,
				"Legacy owner, tier, or remaining lifetime did not migrate exactly");
		helper.assertTrue(migrated.getMaxHealth() == 240.0F && migrated.getHealth() == 100.0F
					&& migrated.getAttribute(Attributes.ARMOR).getBaseValue() == 22.0
					&& migrated.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() == 34.0,
				"Legacy elite archetype did not restore authoritative attributes");
		TagValueOutput rewritten = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING, level.registryAccess());
		migrated.saveWithoutId(rewritten);
		helper.assertTrue(rewritten.buildResult().contains("PowersSummon")
					&& !rewritten.buildResult().contains("PowersGuardianLifetime"),
				"Migrated guardian did not rewrite only the newest compact schema");

		legacy.putInt("PowersGuardianLifetime", 0);
		DarknessCreature zero = PowersEntities.DARKNESS_CREATURE.create(level, EntitySpawnReason.LOAD);
		helper.assertTrue(zero != null, "Zero-lifetime guardian could not be recreated");
		zero.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), legacy));
		helper.assertTrue(zero.summonRecord() != null
					&& zero.summonRecord().expiredAt(level.getGameTime()),
				"Legacy zero lifetime became an immortal natural guardian");

		legacy.putInt("PowersGuardianLifetime", -1);
		DarknessCreature natural = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(natural != null, "Natural guardian could not be recreated");
		natural.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), legacy));
		helper.assertTrue(natural.summonRecord() == null,
				"Legacy natural guardian was incorrectly converted to a finite summon");

		TagValueOutput malformedOutput = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING, level.registryAccess());
		seed.saveWithoutId(malformedOutput);
		var malformed = malformedOutput.buildResult();
		var compact = new CompoundTag();
		malformed.put("PowersSummon", compact);
		compact.putByte("t", LongLivedSummonRecord.Task.GUARD.id());
		compact.putByte("a", LongLivedSummonRecord.Archetype.NORMAL.id());
		compact.putLong("e", level.getGameTime() + 1_200);
		DarknessCreature rejected = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(rejected != null, "Malformed guardian could not be recreated");
		rejected.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), malformed));
		helper.assertTrue(rejected.summonRecord().expiredAt(level.getGameTime()),
				"Ownerless GUARD data did not fail closed at its load tick");

		var missingTask = rewritten.buildResult().copy();
		missingTask.getCompoundOrEmpty("PowersSummon").remove("t");
		DarknessCreature missing = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(missing != null, "Missing-task guardian could not be recreated");
		missing.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), missingTask));
		helper.assertTrue(missing.summonRecord().expiredAt(level.getGameTime()),
				"Missing compact task ID did not fail closed");

		var unknownTask = rewritten.buildResult().copy();
		unknownTask.getCompoundOrEmpty("PowersSummon").putByte("t", (byte) 99);
		DarknessCreature unknown = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(unknown != null, "Unknown-task guardian could not be recreated");
		unknown.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), unknownTask));
		helper.assertTrue(unknown.summonRecord().expiredAt(level.getGameTime()),
				"Unknown compact task ID did not fail closed");

		var missingArchetype = rewritten.buildResult().copy();
		missingArchetype.getCompoundOrEmpty("PowersSummon").remove("a");
		DarknessCreature missingTier = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(missingTier != null, "Missing-archetype guardian could not be recreated");
		missingTier.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), missingArchetype));
		helper.assertTrue(missingTier.summonRecord().expiredAt(level.getGameTime()),
				"Missing compact archetype ID did not fail closed");

		var unknownArchetype = rewritten.buildResult().copy();
		unknownArchetype.getCompoundOrEmpty("PowersSummon").putByte("a", (byte) 99);
		DarknessCreature unknownTier = PowersEntities.DARKNESS_CREATURE.create(
				level, EntitySpawnReason.LOAD);
		helper.assertTrue(unknownTier != null, "Unknown-archetype guardian could not be recreated");
		unknownTier.load(TagValueInput.create(ProblemReporter.DISCARDING,
				level.registryAccess(), unknownArchetype));
		helper.assertTrue(unknownTier.summonRecord().expiredAt(level.getGameTime()),
				"Unknown compact archetype ID did not fail closed");
		helper.succeed();
	}
}
