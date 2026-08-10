package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersDataComponents;
import com.powers.PowersWeapons;
import com.powers.PowersBlocks;
import com.powers.forge.ArcaneCrucibleBlockEntity;
import com.powers.forge.CrucibleEligibility;
import com.powers.forge.CrucibleTransactionEngine;
import com.powers.forge.CrucibleTransactionResult;
import com.powers.forge.CrucibleWeaponData;
import com.powers.entity.DarknessCreature;
import com.powers.entity.RadiantSentinel;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.SkillSystem;
import com.powers.power.artifact.ArtifactDeathWardManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small runtime suite for mechanics that pure unit tests cannot exercise. */
public final class PowersGameTests {
	public PowersGameTests() {
	}

	@GameTest
	public void darknessCreatureHonorsRuntimeFactionTags(GameTestHelper helper) {
		DarknessCreature creature = helper.spawn(PowersEntities.DARKNESS_CREATURE, new BlockPos(1, 1, 1));
		Player target = helper.makeMockPlayer(GameType.SURVIVAL);
		target.addTag(SkillSystem.DARKNESS_TAG);
		helper.assertFalse(creature.canAttack(target), "Darkness guardians attacked their own faction");
		target.removeTag(SkillSystem.DARKNESS_TAG);
		helper.assertTrue(creature.canAttack(target), "Darkness guardians ignored a non-dark target");
		helper.succeed();
	}

	@GameTest
	public void radiantSentinelHonorsOwnerAndOpposedFaction(GameTestHelper helper) {
		RadiantSentinel sentinel = helper.spawn(PowersEntities.RADIANT_SENTINEL, new BlockPos(1, 1, 1));
		Player target = helper.makeMockPlayer(GameType.SURVIVAL);
		sentinel.configureGuardian(target.getUUID(), 200, false);
		target.addTag(SkillSystem.DARKNESS_TAG);
		helper.assertFalse(sentinel.canAttack(target), "Radiant sentinel attacked its owner");
		Player opposed = helper.makeMockPlayer(GameType.SURVIVAL);
		opposed.addTag(SkillSystem.DARKNESS_TAG);
		helper.assertTrue(sentinel.canAttack(opposed), "Radiant sentinel ignored darkness");
		helper.succeed();
	}

	@GameTest
	public void mythicArtifactsCarryUnbreakableRegisteredIdentity(GameTestHelper helper) {
		var shadow = PowersWeapons.WEAPONS.get("lycanbane").getDefaultInstance();
		var partisan = PowersWeapons.WEAPONS.get("heavenly_partisan").getDefaultInstance();
		helper.assertTrue(shadow.has(DataComponents.UNBREAKABLE), "Shadow Sword was damageable");
		helper.assertTrue(partisan.has(DataComponents.UNBREAKABLE), "Heavenly Partisan was damageable");
		helper.assertTrue(shadow.get(PowersDataComponents.ARTIFACT_IDENTITY).alignment()
				== ArtifactAlignment.DARKNESS, "Shadow Sword identity was missing");
		helper.assertTrue(partisan.get(PowersDataComponents.ARTIFACT_IDENTITY).alignment()
				== ArtifactAlignment.LIGHT, "Partisan identity was missing");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void deathWardConsumesExactlyOnceAndVoidBypasses(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ArtifactDeathWardManager.arm(player, ArtifactAlignment.DARKNESS);
		helper.assertTrue(ArtifactDeathWardManager.preventDeath(player, player.damageSources().fall()),
				"Legal lethal source did not consume the ward");
		helper.assertFalse(ArtifactDeathWardManager.preventDeath(player, player.damageSources().fall()),
				"One ward prevented death twice");
		ArtifactDeathWardManager.arm(player, ArtifactAlignment.LIGHT);
		helper.assertFalse(ArtifactDeathWardManager.preventDeath(
				player, player.damageSources().fellOutOfWorld()), "Void damage was incorrectly prevented");
		ArtifactDeathWardManager.clear();
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void arcaneCrucibleCommitsOneDelayedAtomicResult(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, PowersBlocks.ARCANE_CRUCIBLE);
		ArcaneCrucibleBlockEntity crucible = helper.getBlockEntity(pos, ArcaneCrucibleBlockEntity.class);
		crucible.setItem(ArcaneCrucibleBlockEntity.WEAPON_SLOT, new ItemStack(Items.IRON_SWORD));
		crucible.setItem(ArcaneCrucibleBlockEntity.CATALYST_SLOT, new ItemStack(PowersBlocks.DARKNESS));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var choice = crucible.choices().getFirst();
		helper.assertTrue(crucible.begin(player, choice, crucible.version()),
				"Crucible rejected a valid conversion");
		helper.assertTrue(crucible.isMutating(), "Crucible did not hold its ritual lock");
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(crucible.getItem(ArcaneCrucibleBlockEntity.WEAPON_SLOT).isEmpty(),
					"Crucible failed to consume the base weapon");
			helper.assertTrue(player.getInventory().contains(stack ->
					stack.has(PowersDataComponents.CRUCIBLE_WEAPON)),
					"Crucible did not deliver exactly one transformed weapon");
			helper.succeed();
		});
	}

	@GameTest
	public void crucibleBindingAndInfusionPreserveWeaponIdentity(GameTestHelper helper) {
		ItemStack base = new ItemStack(Items.DIAMOND_SWORD);
		ItemStack darkness = new ItemStack(PowersBlocks.DARKNESS);
		var conversionChoice = com.powers.forge.CrucibleTransformationCatalogue
				.choices(base, darkness).getFirst();
		CrucibleTransactionResult conversion = CrucibleTransactionEngine.prepare(
				base, darkness, conversionChoice);
		helper.assertTrue(conversion.success(), "Valid conversion plan failed");
		ItemStack star = new ItemStack(com.powers.ImportedPackItems.ITEMS.get(
				"imported_artifact_star_animated"));
		var bindingChoice = com.powers.forge.CrucibleTransformationCatalogue
				.choices(conversion.result(), star).getFirst();
		CrucibleTransactionResult binding = CrucibleTransactionEngine.prepare(
				conversion.result(), star, bindingChoice);
		helper.assertTrue(binding.success(), "Animated-star binding failed");
		CrucibleWeaponData bound = binding.result().get(PowersDataComponents.CRUCIBLE_WEAPON);
		helper.assertTrue(bound != null && bound.starBound(), "Binding data was not persisted");
		ItemStack rune = new ItemStack(com.powers.ImportedPackItems.ITEMS.get(
				"imported_artifact_runestone_dark_inscribed_large"));
		var infusionChoice = com.powers.forge.CrucibleTransformationCatalogue
				.choices(binding.result(), rune).getFirst();
		CrucibleTransactionResult infusion = CrucibleTransactionEngine.prepare(
				binding.result(), rune, infusionChoice);
		helper.assertTrue(infusion.success(), "Rune infusion failed");
		helper.assertTrue(infusion.result().getItem() == binding.result().getItem(),
				"Infusion changed the transformed weapon item");
		helper.assertTrue(infusion.result().get(PowersDataComponents.CRUCIBLE_WEAPON).xp() == 675,
				"Ancient rune granted the wrong XP");
		helper.succeed();
	}

	@GameTest
	public void mythicArtifactsAreHardExcludedFromTheCrucible(GameTestHelper helper) {
		helper.assertFalse(CrucibleEligibility.isBaseWeapon(
				PowersWeapons.WEAPONS.get("lycanbane").getDefaultInstance()),
				"Shadow Sword entered the Crucible");
		helper.assertFalse(CrucibleEligibility.isBaseWeapon(
				PowersWeapons.WEAPONS.get("heavenly_partisan").getDefaultInstance()),
				"Heavenly Partisan entered the Crucible");
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void breakingCrucibleDuringRitualCannotDuplicateResult(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, PowersBlocks.ARCANE_CRUCIBLE);
		ArcaneCrucibleBlockEntity crucible = helper.getBlockEntity(pos, ArcaneCrucibleBlockEntity.class);
		crucible.setItem(0, new ItemStack(Items.IRON_SWORD));
		crucible.setItem(1, new ItemStack(PowersBlocks.DARKNESS));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var choice = crucible.choices().getFirst();
		helper.assertTrue(crucible.begin(player, choice, crucible.version()), "Ritual did not start");
		helper.assertFalse(crucible.begin(player, choice, crucible.version()),
				"Concurrent transaction bypassed the mutation lock");
		helper.assertFalse(crucible.canTakeItemThroughFace(0, crucible.getItem(0),
				net.minecraft.core.Direction.UP), "Hopper extracted during a commit");
		var target = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(choice.targetItem());
		helper.destroyBlock(pos);
		helper.runAfterDelay(45, () -> {
			helper.assertItemEntityNotPresent(target, pos, 4.0);
			helper.assertItemEntityPresent(Items.IRON_SWORD, pos, 4.0);
			helper.succeed();
		});
	}

}
