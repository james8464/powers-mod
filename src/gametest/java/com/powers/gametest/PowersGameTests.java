package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersDataComponents;
import com.powers.PowersWeapons;
import com.powers.PowersBlocks;
import com.powers.allegiance.AllegianceMatrix;
import com.powers.allegiance.AllegianceReloadListener;
import com.powers.forge.ArcaneCrucibleBlockEntity;
import com.powers.forge.CrucibleEligibility;
import com.powers.forge.CrucibleTransactionEngine;
import com.powers.forge.CrucibleTransactionResult;
import com.powers.forge.CrucibleWeaponData;
import com.powers.entity.DarknessCreature;
import com.powers.entity.RadiantSentinel;
import com.powers.entity.FirstVessel;
import com.powers.entity.PowerTestActor;
import com.powers.entity.TestActorPowerState;
import com.powers.entity.RealmHerald;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.ShadowAgendaPhase;
import com.powers.companion.ShadowCompanionStore;
import com.powers.boss.FirstVesselPowerCatalogue;
import com.powers.boss.FirstVesselRitual;
import com.powers.boss.BossReactionManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.FantasyWeaponItem;
import com.powers.player.SkillSystem;
import com.powers.player.PlayerGuide;
import com.powers.player.PlayerPowers;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.power.PowerDamage;
import com.powers.item.ArtifactWeaponManager;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.FireballAbility;
import com.powers.power.abilities.IceManipulationAbility;
import com.powers.power.abilities.BrittleArmorManager;
import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.AstralReturnIndicator;
import com.powers.power.abilities.LightningConductanceRuntime;
import com.powers.power.abilities.LightningStrikeRules;
import com.powers.power.abilities.PlantHealingAbility;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.EnergyDrainRules;
import com.powers.power.abilities.TeleportAbility;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.network.VesselControlPackets;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.power.AbilityActivationService;
import com.powers.power.travel.MindscapeMobReturnTracker;
import com.powers.mind.MindSessionManager;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.network.NamedTargetRules;
import com.powers.testing.TestingOverrides;
import com.powers.companion.PrivateCompanionManager;
import com.powers.knowledge.KnowledgeService;
import com.powers.power.abilities.CombatTerrainImpact;
import com.powers.power.state.MagicShieldManager;
import com.powers.power.ConcordCastManager;
import com.powers.power.PowerRegistry;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.MagicRayCollisionRuntime;
import com.powers.spell.SpellRegistry;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.CartographerQuery;
import com.powers.spell.CartographerSearch;
import com.powers.protection.ConsentKind;
import com.powers.protection.ConsentOverrideRuntime;
import com.powers.progression.RankAttributeManager;
import com.powers.progression.RankPerkType;
import com.powers.progression.RankProfile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Small runtime suite for mechanics that pure unit tests cannot exercise. */
public final class PowersGameTests {
	public PowersGameTests() {
	}

	@GameTest
	@SuppressWarnings("removal")
	public void bossReactionsRewardOppositionWithoutRequiringOneAlignment(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		caster.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
		caster.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(caster).setDarknessLevel(caster, 10);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				PowersWeapons.weapon("lycanbane").getDefaultInstance());
		TestingOverrides.setEnergyDisabled(caster.getUUID(), true);
		RealmHerald lightHerald = helper.spawn(PowersEntities.LIGHT_HERALD,
				new BlockPos(4, 2, 2));
		RealmHerald darkHerald = helper.spawn(PowersEntities.DARK_HERALD,
				new BlockPos(6, 2, 2));
		FirstVessel vessel = helper.spawn(PowersEntities.FIRST_VESSEL,
				new BlockPos(8, 2, 2));
		lightHerald.setNoAi(true);
		darkHerald.setNoAi(true);
		vessel.setNoAi(true);

		helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS,
				"innate/forcefield", -1), "Shadow Sword rejected Forcefield");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Dark artifact did not activate through its authoritative owner");
		helper.assertTrue(lightHerald.hasEffect(MobEffects.GLOWING),
				"Opposing artifact did not expose the Light Herald");
		helper.assertFalse(darkHerald.hasEffect(MobEffects.GLOWING),
				"Matching artifact incorrectly became mandatory boss counterplay");
		helper.assertTrue(vessel.hasEffect(MobEffects.WEAKNESS),
				"First Vessel did not accept the dark-aligned setup");

		TestingOverrides.clear(caster.getUUID());

		ConcordCastManager.clear();
		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		BlockPos concordCenter = helper.absolutePos(new BlockPos(10, 2, 2));
		first.setPos(concordCenter.getX() - 1.0, concordCenter.getY(), concordCenter.getZ());
		second.setPos(concordCenter.getX() + 1.0, concordCenter.getY(), concordCenter.getZ());
		FirstVessel radiantVessel = helper.spawn(PowersEntities.FIRST_VESSEL,
				new BlockPos(10, 2, 4));
		radiantVessel.setNoAi(true);
		var ability = new ForcefieldAbility();
		helper.assertFalse(ConcordCastManager.record(first, ability),
				"First complementary cast incorrectly concorded alone");
		helper.assertTrue(ConcordCastManager.record(second, ability),
				"Radiant Concord did not form against the First Vessel");
		helper.assertTrue(radiantVessel.hasEffect(MobEffects.GLOWING),
				"Radiant Concord reaction was not readable");
		ConcordCastManager.clear();
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void innateSizeMorphActivationTogglesScaleBackToNormal(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var data = com.powers.player.PlayerPowers.get(player);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		data.setSizeMorphOption(1);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		helper.assertTrue(com.powers.power.AbilityActivationService.activate(
				player, ability, ability.id().toString())
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Size Morphing did not activate through the authoritative pipeline");
		helper.assertTrue(player.getScale() < 0.75F,
				"Size Morphing did not apply the selected half-scale body");
		data.setSlots(player, java.util.List.of(
				"powers:size_shift", "powers:double_health", "powers:starfall"));
		helper.assertTrue(data.isToggleActive(ability.id().toString()) && player.getScale() < 0.75F,
				"Replacing another slot silently deactivated a retained toggle");
		helper.assertTrue(com.powers.power.AbilityActivationService.activate(
				player, ability, ability.id().toString())
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Size Morphing did not deactivate through the authoritative pipeline");
		helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F,
				"Size Morphing retained its scale after toggle-off");
		TestingOverrides.clear(player.getUUID());
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void innateSizeMorphRejectsMountedActivationWithoutChangingEitherBody(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var horse = net.minecraft.world.entity.EntityTypes.HORSE.create(helper.getLevel(),
				net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(horse != null, "Mount fixture could not create a horse");
		horse.snapTo(player.getX(), player.getY(), player.getZ());
		helper.getLevel().addFreshEntity(horse);
		helper.assertTrue(player.startRiding(horse, true, true),
				"Mount fixture could not attach its player passenger");
		var data = com.powers.player.PlayerPowers.get(player);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		data.setSizeMorphOption(1);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.FAILED,
					"Mounted Size Morphing entered the authoritative toggle pipeline");
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F,
					"Rejected mounted activation changed the passenger scale");
			helper.assertFalse(data.isToggleActive(ability.id().toString()),
					"Rejected mounted activation retained toggle ownership");
			helper.assertTrue(player.getVehicle() == horse,
					"Rejected mounted activation silently dismounted the player");
		} finally {
			TestingOverrides.clear(player.getUUID());
			player.stopRiding();
			horse.discard();
		}
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void innateSizeMorphRelinquishesItsBodyWhenThePlayerMounts(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 4));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		var data = com.powers.player.PlayerPowers.get(player);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		data.setSizeMorphOption(1);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		var horse = net.minecraft.world.entity.EntityTypes.HORSE.create(helper.getLevel(),
				net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(horse != null, "Mount fixture could not create a horse");
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Unmounted half-scale form did not activate");
			horse.snapTo(player.getX(), player.getY(), player.getZ());
			helper.getLevel().addFreshEntity(horse);
			helper.assertTrue(player.startRiding(horse, true, true),
					"Mount fixture could not attach its morphed passenger");
			player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
			helper.getLevel().setBlockAndUpdate(feet.above(), Blocks.STONE.defaultBlockState());
			ability.tickActive(player, data);
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F,
					"Mounted player retained the transformed body");
			helper.assertFalse(data.isToggleActive(ability.id().toString()),
					"Mounted player retained stale Size Morphing toggle ownership");
			helper.assertTrue(player.getVehicle() == horse,
					"Size Morphing cleanup silently dismounted the player");
			helper.assertTrue(player.getPose() == net.minecraft.world.entity.Pose.SWIMMING
					&& helper.getLevel().noCollision(player, player.getBoundingBox()),
					"Mounted normalization expanded into the one-block ceiling");
		} finally {
			TestingOverrides.clear(player.getUUID());
			player.stopRiding();
			horse.discard();
		}
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void innateSizeMorphRejectsMountWhenNoVanillaPoseFits(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 4));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		var data = com.powers.player.PlayerPowers.get(player);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		data.setSizeMorphOption(0);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		var horse = net.minecraft.world.entity.EntityTypes.HORSE.create(helper.getLevel(),
				net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(horse != null, "Mount-rejection fixture could not create a horse");
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Quarter-scale mount-rejection fixture did not activate");
			horse.snapTo(player.getX(), player.getY(), player.getZ());
			helper.getLevel().addFreshEntity(horse);
			helper.assertTrue(player.startRiding(horse, true, true),
					"Mount-rejection fixture could not attach its passenger");
			player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
			helper.getLevel().setBlockAndUpdate(feet,
					Blocks.STONE_SLAB.defaultBlockState().setValue(
							net.minecraft.world.level.block.SlabBlock.TYPE,
							net.minecraft.world.level.block.state.properties.SlabType.TOP));
			ability.tickActive(player, data);
			helper.assertFalse(player.isPassenger(),
					"Impossible vanilla-scale transition retained the unsafe mount");
			helper.assertTrue(data.isToggleActive(ability.id().toString())
					&& Math.abs(player.getScale() - 0.25F) < 0.001F
					&& helper.getLevel().noCollision(player, player.getBoundingBox()),
					"Rejected mount lost the collision-safe transformed body");
		} finally {
			helper.getLevel().setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
			if (data.isToggleActive(ability.id().toString())) {
				com.powers.power.AbilityActivationService.activate(
						player, ability, ability.id().toString(), true);
			}
			TestingOverrides.clear(player.getUUID());
			player.stopRiding();
			horse.discard();
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void innateSizeMorphUsesDoorwayFallbackThenRestoresAuthoritativeReachAndMass(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 2));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		for (int x = -5; x <= 5; x++) for (int y = 0; y <= 9; y++) for (int z = -2; z <= 12; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		for (int x = -3; x <= 3; x++) {
			for (int y = 0; y <= 7; y++) {
				helper.getLevel().setBlockAndUpdate(feet.offset(x, y, 2),
						x == 0 && y < 2 ? Blocks.AIR.defaultBlockState()
								: Blocks.STONE.defaultBlockState());
			}
		}
		var data = com.powers.player.PlayerPowers.get(player);
		data.setSkillLevel(player, 10);
		data.setSizeMorphOption(11);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Doorway prediction rejected the safe fallback with the selected form");
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F,
					"Fourfold form did not fall back before the vanilla doorway");
			helper.assertTrue(com.powers.power.PowerEnergy.ongoingCost(player, ability) == 0,
					"Recurring drain did not follow the actually applied doorway form");
			helper.assertTrue(Math.abs(player.blockInteractionRange() - 4.5) < 0.001,
					"Doorway fallback desynchronized authoritative block reach");
			helper.assertTrue(Math.abs(player.entityInteractionRange() - 3.0) < 0.001,
					"Doorway fallback desynchronized authoritative entity reach");
			player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 2.5);
			ability.tickActive(player, data);
			helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()),
					"Fallback body suffocated inside the standard doorway fixture");

			for (int x = -3; x <= 3; x++) {
				for (int y = 0; y <= 7; y++) {
					helper.getLevel().setBlockAndUpdate(feet.offset(x, y, 2), Blocks.AIR.defaultBlockState());
				}
			}
			player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 4.5);
			ability.tickActive(player, data);
			helper.assertTrue(Math.abs(player.getScale() - 4.0F) < 0.001F,
					"Clear space did not restore the selected fourfold form: scale="
							+ player.getScale() + ", pose=" + player.getPose()
							+ ", box=" + player.getBoundingBox());
			helper.assertTrue(Math.abs(player.blockInteractionRange() - 12.0) < 0.001,
					"Fourfold block reach exceeded or missed its authoritative cap");
			helper.assertTrue(Math.abs(player.entityInteractionRange() - 10.0) < 0.001,
					"Fourfold entity reach exceeded or missed its authoritative cap");
			helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.8) < 0.001,
					"Fourfold body mass did not apply bounded knockback resistance");
			helper.assertTrue(com.powers.power.PowerEnergy.ongoingCost(player, ability) == 9,
					"Rank-adjusted recurring drain did not restore with the selected fourfold form");
		} finally {
			if (data.isToggleActive(ability.id().toString())) {
				com.powers.power.AbilityActivationService.activate(
						player, ability, ability.id().toString(), true);
			}
			TestingOverrides.clear(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void innateSizeMorphToggleOffNormalizesThroughASafeCompactPose(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 4));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		for (int z = -1; z <= 2; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(0, 1, z), Blocks.STONE.defaultBlockState());
		}
		var data = com.powers.player.PlayerPowers.get(player);
		data.setSizeMorphOption(0);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Quarter-scale tunnel fixture did not activate");
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString(), true)
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Confined toggle-off did not complete");
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F,
					"Confined toggle-off retained the quarter-scale modifier");
			helper.assertTrue(player.getPose() == net.minecraft.world.entity.Pose.SWIMMING,
					"Confined toggle-off did not choose the collision-free vanilla crawl pose");
			helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()),
					"Confined toggle-off expanded the player into the tunnel ceiling");
		} finally {
			TestingOverrides.clear(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void innateSizeMorphRejectsToggleOffWhenNoVanillaPoseFits(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 4));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		helper.getLevel().setBlockAndUpdate(feet,
				Blocks.STONE_SLAB.defaultBlockState().setValue(
						net.minecraft.world.level.block.SlabBlock.TYPE,
						net.minecraft.world.level.block.state.properties.SlabType.TOP));
		var data = com.powers.player.PlayerPowers.get(player);
		data.setSizeMorphOption(0);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Half-block cavity did not accept the collision-free quarter-scale body");
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString(), true)
					== com.powers.power.AbilityActivationService.Result.FAILED,
					"Toggle-off succeeded where no vanilla-scale pose could fit");
			helper.assertTrue(data.isToggleActive(ability.id().toString())
					&& Math.abs(player.getScale() - 0.25F) < 0.001F
					&& helper.getLevel().noCollision(player, player.getBoundingBox()),
					"Rejected toggle-off lost safe morph ownership or entered collision");
		} finally {
			helper.getLevel().setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
			if (data.isToggleActive(ability.id().toString())) {
				com.powers.power.AbilityActivationService.activate(
						player, ability, ability.id().toString(), true);
			}
			TestingOverrides.clear(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void innateSizeMorphCrawlPoseIsReleasedWhenTheToggleEnds(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 4));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		player.setYRot(0.0F);
		for (int z = -1; z <= 3; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(0, 1, z), Blocks.STONE.defaultBlockState());
		}
		var data = com.powers.player.PlayerPowers.get(player);
		data.setSizeMorphOption(com.powers.power.abilities.SizeMorphRules.normalOption());
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"One-block tunnel rejected the volume-aware crawl body");
			helper.assertTrue(player.getPose() == net.minecraft.world.entity.Pose.SWIMMING,
					"One-block tunnel did not enter the compact crawl pose");
			helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()),
					"Compact crawl body collided with the one-block tunnel");
			for (int z = -1; z <= 3; z++) {
				helper.getLevel().setBlockAndUpdate(feet.offset(0, 1, z), Blocks.AIR.defaultBlockState());
			}
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString(), true)
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Clear-space toggle-off was rejected after crawling");
			helper.assertTrue(player.getPose() == net.minecraft.world.entity.Pose.STANDING,
					"Toggle-off retained the forced crawl pose in clear space");
		} finally {
			if (data.isToggleActive(ability.id().toString())) {
				com.powers.power.AbilityActivationService.activate(
						player, ability, ability.id().toString(), true);
			}
			TestingOverrides.clear(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void innateSizeMorphDisconnectForgetsForcedCrawlerPreference(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(4, 2, 4));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		for (int z = -1; z <= 2; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(0, 1, z), Blocks.STONE.defaultBlockState());
		}
		var data = com.powers.player.PlayerPowers.get(player);
		data.setSizeMorphOption(com.powers.power.abilities.SizeMorphRules.normalOption());
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		helper.assertTrue(com.powers.power.AbilityActivationService.activate(
				player, ability, ability.id().toString())
				== com.powers.power.AbilityActivationService.Result.ACTIVATED
				&& player.getPose() == net.minecraft.world.entity.Pose.SWIMMING,
				"Disconnect fixture did not acquire forced crawler state");
		for (int z = -1; z <= 2; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(0, 1, z), Blocks.AIR.defaultBlockState());
		}
		com.powers.power.PowerAbilityRuntime.onDisconnect(helper.getLevel().getServer(), player);
		ability.tickActive(player, data);
		helper.assertTrue(player.getPose() == net.minecraft.world.entity.Pose.SWIMMING,
				"Disconnect leaked the prior entity's forced-crawler standing preference");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void innateSizeMorphPreservesForeignReachAndRankMassModifiers(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var data = com.powers.player.PlayerPowers.get(player);
		var ability = new com.powers.power.abilities.SizeMorphAbility();
		data.setSizeMorphOption(4);
		var foreignId = net.minecraft.resources.Identifier.fromNamespaceAndPath(
				"example_mod", "size_morph_composition");
		for (var attribute : java.util.List.of(Attributes.BLOCK_INTERACTION_RANGE,
				Attributes.ENTITY_INTERACTION_RANGE, Attributes.KNOCKBACK_RESISTANCE)) {
			player.getAttribute(attribute).addTransientModifier(new AttributeModifier(
					foreignId, 0.1, AttributeModifier.Operation.ADD_VALUE));
		}
		RankAttributeManager.reconcile(player, new RankProfile(
				java.util.Map.of(RankPerkType.WARD_INTEGRITY, 0.25),
				java.util.Map.of(), java.util.Map.of(), "size-morph-composition"));
		double blockBefore = player.blockInteractionRange();
		double entityBefore = player.entityInteractionRange();
		double knockbackBefore = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		var morphProfile = com.powers.power.abilities.SizeMorphRules.profile(1.25);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString())
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Composed Size Morphing fixture did not activate");
			helper.assertTrue(Math.abs(player.blockInteractionRange()
					- (blockBefore + morphProfile.blockReach() - 4.5)) < 0.001,
					"Size Morphing replaced a foreign block-reach contribution");
			helper.assertTrue(Math.abs(player.entityInteractionRange()
					- (entityBefore + morphProfile.entityReach() - 3.0)) < 0.001,
					"Size Morphing replaced a foreign entity-reach contribution");
			helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)
					> knockbackBefore + morphProfile.knockbackResistance(),
					"Size Morphing replaced a rank or foreign mass contribution");
			for (var attribute : java.util.List.of(Attributes.BLOCK_INTERACTION_RANGE,
					Attributes.ENTITY_INTERACTION_RANGE, Attributes.KNOCKBACK_RESISTANCE)) {
				helper.assertTrue(player.getAttribute(attribute).hasModifier(foreignId),
						"Size Morphing removed a foreign attribute owner");
			}
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(
					player, ability, ability.id().toString(), true)
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Composed Size Morphing fixture did not deactivate");
			helper.assertTrue(Math.abs(player.blockInteractionRange() - blockBefore) < 0.001
					&& Math.abs(player.entityInteractionRange() - entityBefore) < 0.001
					&& Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)
							- knockbackBefore) < 0.001,
					"Toggle-off did not restore the unrelated attribute totals");
		} finally {
			RankAttributeManager.clear(player);
			TestingOverrides.clear(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void doubleHealthHasARealBaselineAndCleansUpOnToggleOff(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var data = com.powers.player.PlayerPowers.get(player);
		var ability = new com.powers.power.abilities.DoubleHealthAbility();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		float baseline = player.getMaxHealth();
		helper.assertTrue(com.powers.power.AbilityActivationService.activate(
				player, ability, ability.id().toString())
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Double Health did not activate through the authoritative pipeline");
		helper.assertTrue(player.getMaxHealth() >= baseline * 2.0F,
				"Double Health did not provide its promised baseline second heart row");
		helper.assertTrue(com.powers.power.AbilityActivationService.activate(
				player, ability, ability.id().toString())
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Double Health did not deactivate through the authoritative pipeline");
		helper.assertTrue(Math.abs(player.getMaxHealth() - baseline) < 0.001F,
				"Double Health retained its modifier after toggle-off");
		TestingOverrides.clear(player.getUUID());
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void rankReconciliationPreservesForeignAttributeOwners(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var foreignId = net.minecraft.resources.Identifier.fromNamespaceAndPath("example_mod", "persistent_buff");
		for (var attribute : java.util.List.of(Attributes.SCALE, Attributes.MOVEMENT_SPEED,
				Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE)) {
			var instance = player.getAttribute(attribute);
			helper.assertTrue(instance != null, "Test player lacked a required vanilla attribute");
			instance.addTransientModifier(new AttributeModifier(foreignId, 0.05,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		RankProfile profile = new RankProfile(java.util.Map.of(
				RankPerkType.MOVEMENT, 0.20, RankPerkType.RESISTANCE, 0.20,
				RankPerkType.WARD_INTEGRITY, 0.20), java.util.Map.of(), java.util.Map.of(), "test");
		RankAttributeManager.reconcile(player, profile);
		RankAttributeManager.clear(player);
		for (var attribute : java.util.List.of(Attributes.SCALE, Attributes.MOVEMENT_SPEED,
				Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE)) {
			helper.assertTrue(player.getAttribute(attribute).hasModifier(foreignId),
					"POWERS removed a foreign modifier from " + attribute.getRegisteredName());
		}
		helper.succeed();
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
	@SuppressWarnings("removal")
	public void twoAlignedInnateCastsCreateACombatConcord(GameTestHelper helper) {
		ConcordCastManager.clear();
		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(5, 1, 5));
		first.setPos(origin.getX(), origin.getY(), origin.getZ());
		second.setPos(origin.getX() + 3.0, origin.getY(), origin.getZ());
		DarknessCreature target = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(6, 1, 7));
		target.setNoAi(true);
		var ability = new ForcefieldAbility();
		helper.assertFalse(ConcordCastManager.record(first, ability),
				"The first cast incorrectly concorded alone");
		helper.assertTrue(ConcordCastManager.record(second, ability),
				"A nearby aligned matching cast did not concord");
		helper.assertTrue(first.hasEffect(MobEffects.ABSORPTION)
				&& second.hasEffect(MobEffects.ABSORPTION), "Concord did not protect both casters");
		helper.assertTrue(target.getHealth() < target.getMaxHealth(),
				"Concord pulse did not damage the opposed faction");
		ConcordCastManager.clear();
		helper.succeed();
	}

	@GameTest
	public void realmHeraldsAreBossCapableAndDefendOpposedFactions(GameTestHelper helper) {
		RealmHerald dark = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(1, 1, 1));
		RealmHerald light = helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(5, 1, 1));
		Player ordinary = helper.makeMockPlayer(GameType.SURVIVAL);
		Player infected = helper.makeMockPlayer(GameType.SURVIVAL);
		infected.addTag(SkillSystem.DARKNESS_TAG);
		helper.assertTrue(dark.getMaxHealth() >= 1_024.0F && light.getMaxHealth() >= 1_024.0F,
				"A realm Herald lacked boss-scale vitality");
		helper.assertTrue(dark.canAttack(ordinary) && !dark.canAttack(infected),
				"Dark Herald violated its faction boundary");
		helper.assertTrue(light.canAttack(infected) && !light.canAttack(ordinary),
				"Light Herald violated its faction boundary");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void heraldCourtPhaseAppliesExpiringHazardAndCleansItsBoundedSummon(
			GameTestHelper helper) {
		RealmHerald herald = helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(2, 2, 2));
		herald.beginCourtEncounter();
		ServerPlayer opposed = helper.makeMockServerPlayerInLevel();
		opposed.setGameMode(GameType.CREATIVE);
		opposed.addTag(SkillSystem.DARKNESS_TAG);
		opposed.snapTo(helper.absoluteVec(new Vec3(4.0, 2.0, 2.0)), 0.0F, 0.0F);
		herald.setHealth(herald.getMaxHealth() * 0.5F);
		herald.tickCount = 0;
		try {
			var ai = RealmHerald.class.getDeclaredMethod("customServerAiStep",
					net.minecraft.server.level.ServerLevel.class);
			ai.setAccessible(true);
			ai.invoke(herald, helper.getLevel());
			helper.assertTrue(herald.courtPhase()
					== com.powers.realm.RealmHeraldCourtRules.Phase.ASCENDANT,
					"Herald health did not advance the persistent three-phase court");
			helper.assertTrue(opposed.hasEffect(MobEffects.SLOWNESS)
					&& opposed.hasEffect(MobEffects.GLOWING),
					"Light Court did not apply its finite, non-terrain hazard");
			var summons = com.powers.util.BoundedEntityCandidates.ofClass(helper.getLevel(),
					RadiantSentinel.class,
					new net.minecraft.world.phys.AABB(herald.blockPosition()).inflate(24.0),
					com.powers.realm.RealmHeraldCourtRules.MAX_SUMMONS + 1,
					RadiantSentinel::temporaryGuardian);
			helper.assertTrue(summons.size() == 1,
					"Herald Court did not create exactly one bounded first summon");
			herald.die(PowerDamage.source(opposed));
			helper.assertTrue(summons.getFirst().isRemoved(),
					"Defeated Herald left a loaded court summon behind");
			helper.succeed();
		} catch (ReflectiveOperationException failure) {
			helper.fail("Herald Court production seam missing: " + failure.getMessage());
		}
	}

	@GameTest
	public void datapackAllegiancePolicyOwnsEveryAlignedCategory(GameTestHelper helper) {
		var snapshot = AllegianceReloadListener.snapshot();
		helper.assertTrue(snapshot.revision() > 0L,
				"The server-data reload listener never published the allegiance policy");
		AllegianceMatrix matrix = snapshot.matrix();
		var radiantPlayer = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.RADIANT, AllegianceMatrix.Role.PLAYER);
		var darknessShadow = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.DARKNESS, AllegianceMatrix.Role.SHADOW);
		var radiantArtifact = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.RADIANT, AllegianceMatrix.Role.ARTIFACT);
		var darknessForce = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.DARKNESS, AllegianceMatrix.Role.FORCE);
		var radiantBoss = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.RADIANT, AllegianceMatrix.Role.BOSS);
		var darknessFood = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.DARKNESS, AllegianceMatrix.Role.FOOD);
		var alliedParty = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.DARKNESS, AllegianceMatrix.Role.PARTY);
		var amethyst = new AllegianceMatrix.Profile(
				AllegianceMatrix.Faction.NEUTRAL, AllegianceMatrix.Role.AMETHYST);
		helper.assertTrue(matrix.resolve(radiantPlayer, darknessShadow, false)
				== AllegianceMatrix.Relationship.HOSTILE,
				"Player-to-Shadow opposition bypassed the shared policy");
		helper.assertTrue(matrix.resolve(radiantArtifact, darknessForce, false)
				== AllegianceMatrix.Relationship.HOSTILE,
				"Artifact-to-force opposition bypassed the shared policy");
		helper.assertTrue(matrix.resolve(radiantBoss, darknessFood, false)
				== AllegianceMatrix.Relationship.HOSTILE,
				"Boss-to-food opposition bypassed the shared policy");
		helper.assertTrue(matrix.resolve(radiantPlayer, alliedParty, true)
				== AllegianceMatrix.Relationship.ALLY,
				"Party allegiance did not override opposing factions");
		helper.assertTrue(matrix.resolve(radiantPlayer, amethyst, true)
				== AllegianceMatrix.Relationship.SUPPRESSED,
				"Amethyst suppression did not override party allegiance");
		helper.succeed();
	}

	@GameTest
	public void mythicArtifactsCarryUnbreakableRegisteredIdentity(GameTestHelper helper) {
		var shadow = PowersWeapons.weapon("lycanbane").getDefaultInstance();
		var partisan = PowersWeapons.weapon("heavenly_partisan").getDefaultInstance();
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
	public void nestedMythicArtifactNeverGrantsOwnership(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.addTag(SkillSystem.DARKNESS_TAG);
		ItemStack nestedSword = PowersWeapons.weapon("lycanbane").getDefaultInstance();
		ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
		shulker.set(DataComponents.CONTAINER,
				net.minecraft.world.item.component.ItemContainerContents.fromItems(
						java.util.List.of(nestedSword)));
		player.getInventory().add(shulker);
		boolean nestedCarried = ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS);
		helper.assertFalse(nestedCarried,
				"A mythic artifact nested in a container granted ghost ownership");
		helper.assertFalse(com.powers.item.artifact.ArtifactAuthorizationRules.mayOwn(
				nestedCarried, ArtifactWeaponManager.authorized(player, ArtifactAlignment.DARKNESS)),
				"Alignment alone authorized a nested mythic artifact");

		player.getInventory().clearContent();
		player.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				"A direct top-level mythic artifact did not grant ownership");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void ordinaryFantasyWeaponExecutesItsVisibleArchetype(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 1, 3));
		target.setNoAi(true);
		ItemStack winterthorn = PowersWeapons.weapon("winterthorn").getDefaultInstance();
		helper.assertTrue(winterthorn.getItem() instanceof FantasyWeaponItem,
				"Ordinary fantasy weapon remained a generic Item");
		winterthorn.getItem().hurtEnemy(winterthorn, target, player);
		helper.assertTrue(target.hasEffect(MobEffects.SLOWNESS)
				&& target.hasEffect(MobEffects.WEAKNESS), "Frostbound proc did not execute live");
		helper.succeed();
	}

	@GameTest
	public void firstJoinGuideIsAResolvedVanillaWrittenBook(GameTestHelper helper) {
		ItemStack guide = PlayerGuide.create();
		var content = guide.get(DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(guide.is(Items.WRITTEN_BOOK), "Guide did not use the vanilla written book");
		helper.assertTrue(content != null && content.resolved() && content.pages().size() >= 5,
				"Guide pages were incomplete or unresolved");
		helper.succeed();
	}

	@GameTest(maxTicks = 180)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void shadowChatOwnsVisibilityAndFormerBookKnowledge(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.addTag(SkillSystem.DARKNESS_TAG);
		player.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		com.powers.knowledge.MagicAttemptReporter.failure(player, "fireball",
				com.powers.knowledge.MagicFailureReason.INSUFFICIENT_ENERGY,
				java.util.Map.of("required", 40L, "available", 12L));
		helper.assertTrue(PrivateCompanionManager.handleChat(player,
				"shadow, reveal yourself"), "Explicit Shadow chat leaked into ordinary chat");
		PrivateCompanionManager.tickPlayer(player, 0);
		helper.assertTrue(PrivateCompanionManager.body(player.getUUID()).isPresent(),
				"Shadow address did not create this owner's lightweight session");
		helper.assertTrue(PrivateCompanionManager.revealedBodyId(player.getUUID()).isPresent(),
				"Revealed Shadow did not create this owner's mortal server body");
		helper.assertTrue(PrivateCompanionManager.isRevealed(player.getUUID()),
				"Reveal command did not change global visibility");
		var answer = KnowledgeService.answer(player, "How do the crystals work?");
		helper.assertTrue(!answer.answer().isBlank() && KnowledgeService.entryCount() > 0,
				"Shadow could not answer the former curated-book question offline");
		helper.runAfterDelay(1, () -> {
			var bodyId = PrivateCompanionManager.revealedBodyId(player.getUUID());
			helper.assertTrue(bodyId.isPresent(),
					"Revealed Shadow diagnostic count had no body identity");
			var worldBody = helper.getLevel().getEntity(bodyId.orElseThrow());
			helper.assertTrue(worldBody instanceof ShadowCompanionEntity,
					"Revealed Shadow body identity was absent from the live world");
			ShadowCompanionEntity body = (ShadowCompanionEntity) worldBody;
			helper.assertTrue(body.getMainHandItem().isEmpty() && body.getOffhandItem().isEmpty(),
					"Shadow copied equipment despite owning only the user's skin");
			PrivateCompanionManager.handleChat(player, "shadow, hide yourself");
			helper.assertTrue(PrivateCompanionManager.bodyId(player.getUUID()).orElseThrow()
					.equals(body.getUUID()), "Hiding Shadow replaced its authoritative body");
			helper.assertTrue(body.isInvisible() && body.isInvulnerable(),
					"Hidden Shadow remained externally visible or vulnerable");
			PrivateCompanionManager.handleChat(player, "shadow, reveal yourself");
			helper.assertTrue(PrivateCompanionManager.bodyId(player.getUUID()).orElseThrow()
					.equals(body.getUUID()), "Revealing Shadow replaced its authoritative body");
			helper.assertTrue(body.hurtServer(helper.getLevel(), body.damageSources().generic(), 10_000.0F),
					"Revealed Shadow body could not be killed");
			helper.assertTrue(PrivateCompanionManager.body(player.getUUID()).isEmpty()
					&& PrivateCompanionManager.revealedBodyId(player.getUUID()).isEmpty(),
					"Killed Shadow leaked a session or body");
			var remembered = KnowledgeService.answer(player, "Shadow, why did my fireball fail?");
			helper.assertTrue(remembered.answer().contains("required 40 energy"),
					"Shadow death erased player-keyed magic memories");
			helper.runAfterDelay(102, () -> {
				PrivateCompanionManager.handleChat(player, "shadow, reveal yourself");
				PrivateCompanionManager.tickPlayer(player, 120);
				helper.assertTrue(PrivateCompanionManager.revealedBodyId(player.getUUID()).isPresent(),
						"Shadow Sword could not manifest a new remembered body after its delay");
				PrivateCompanionManager.handleChat(player, "shadow, hide yourself");
				helper.assertFalse(PrivateCompanionManager.isRevealed(player.getUUID())
						|| PrivateCompanionManager.revealedBodyId(player.getUUID()).isPresent(),
						"Hide command left the global mortal body visible");
				PrivateCompanionManager.handleChat(player, "shadow, leave me");
				helper.assertTrue(PrivateCompanionManager.body(player.getUUID()).isEmpty(),
						"Dismiss command leaked a Shadow session");
				helper.assertFalse(PrivateCompanionManager.handleChat(player, "ordinary chat"),
						"Unrelated signed chat was intercepted");
				helper.succeed();
			});
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void generatedShadowKnowledgeLoadsAndAnswersEveryRequiredDomain(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(KnowledgeService.entryCount() == 449,
				"Shadow did not load fourteen authored plus 435 generated knowledge entries");
		var item = KnowledgeService.answer(player,
				"How do I get powers:imported_food_wisdomfruit?");
		var action = KnowledgeService.answer(player,
				"Explain the magic action fireball");
		var rank = KnowledgeService.answer(player,
				"Explain the Light rank Worldsinger");
		var realm = KnowledgeService.answer(player,
				"What is the powers:middleworld realm?");
		var interaction = KnowledgeService.answer(player,
				"Explain the magic interaction Cinderheart reflection");
		helper.assertTrue(item.entryId().equals(
				"powers:generated/items/imported_food_wisdomfruit"),
				"Shadow did not retrieve the generated item catalogue entry");
		helper.assertTrue(action.entryId().equals(
				"powers:generated/actions/fireball"),
				"Shadow did not retrieve the generated action catalogue entry: " + action.entryId());
		helper.assertTrue(rank.entryId().equals(
				"powers:generated/ranks/light_worldsinger_8"),
				"Shadow did not retrieve the generated rank catalogue entry: " + rank.entryId());
		helper.assertTrue(realm.entryId().equals(
				"powers:generated/realms/middleworld"),
				"Shadow did not retrieve the generated realm catalogue entry: " + realm.entryId());
		helper.assertTrue(interaction.entryId().equals(
				"powers:generated/interaction_rules/27_cinderheart_reflection"),
				"Shadow did not retrieve the generated interaction-rule entry: " + interaction.entryId());
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void shadowAgendaAdvancesPersistentlyWithoutRewritingCommandFacts(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.addTag(SkillSystem.DARKNESS_TAG);
		player.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		for (int turn = 0; turn < 18; turn++) {
			helper.assertTrue(PrivateCompanionManager.handleChat(player,
					"shadow, guard this place"), "Explicit Shadow command was not consumed");
		}
		var memory = ShadowCompanionStore.get(player).memory();
		helper.assertTrue(memory.influence() == 72
				&& memory.agendaPhase() == ShadowAgendaPhase.COVENANT,
				"Owner-local agenda did not advance through all three bounded phases");
		helper.assertTrue(memory.turns().get(0).shadow().startsWith("I will guard it.")
				&& memory.turns().get(0).shadow().contains("Remember:"),
				"Whisper phase rewrote the command fact or lost its distinct tone");
		helper.assertTrue(memory.turns().get(9).shadow().startsWith("I will guard it.")
				&& memory.turns().get(9).shadow().contains("Verify it yourself"),
				"Invitation phase rewrote the command fact or lost its distinct tone");
		helper.assertTrue(memory.turns().get(17).shadow().startsWith("I will guard it.")
				&& memory.turns().get(17).shadow().contains("safety still permit"),
				"Covenant phase rewrote the command fact or safety qualifier");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void shadowConjuresAndExecutesItsBoundedCombatArsenal(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself"),
				"Shadow summon chat was not consumed");
		PrivateCompanionManager.tickPlayer(owner, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();

		var conjured = com.powers.companion.ShadowConjurationManager.begin(
				owner, shadow, Items.TORCH, 4);
		helper.assertTrue(conjured.accepted() && !conjured.pending()
				&& conjured.count() == 4 && owner.getInventory().contains(new ItemStack(Items.TORCH)),
				"Shadow did not deliver a bounded ordinary-item conjuration");
		shadow.setEnergy(com.powers.companion.ShadowCompanionRules.MAX_ENERGY);
		owner.getEnderChestInventory().setItem(0, new ItemStack(PowersItems.DARK_CRYSTAL));
		var duplicateCrystal = com.powers.companion.ShadowConjurationManager.begin(
				owner, shadow, PowersItems.DARK_CRYSTAL, 1);
		helper.assertTrue(!duplicateCrystal.accepted()
				&& duplicateCrystal.reason().equals("dark_crystal_already_carried"),
				"Shadow ignored a Dark Crystal stored in the owner's ender chest");
		owner.getEnderChestInventory().setItem(0, ItemStack.EMPTY);
		PrivateCompanionManager.handleChat(owner, "shadow, hide yourself");
		var hiddenRite = com.powers.companion.ShadowConjurationManager.begin(
				owner, shadow, PowersItems.DARK_CRYSTAL, 1);
		helper.assertTrue(!hiddenRite.accepted()
				&& hiddenRite.reason().equals("rite_requires_reveal"),
				"A hidden Shadow could begin the globally visible Dark Crystal rite");
		PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself");

		long tick = helper.getLevel().getServer().getTickCount();
		var flight = com.powers.companion.combat.ShadowPowerCatalogue.find("flight");
		var flightResult = com.powers.companion.combat.ShadowPowerExecutor.execute(
				helper.getLevel(), shadow, null, flight,
				new com.powers.companion.combat.ShadowPowerExecutor.ExecutionContext(owner, false, tick));
		helper.assertTrue(flightResult.success() && shadow.isNoGravity()
				&& shadow.getDeltaMovement().lengthSqr() > 0.1,
				"Shadow could not use a self-directed mobility power without a target");

		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 7));
		target.setNoAi(true);
		float before = target.getHealth();
		var lightning = com.powers.companion.combat.ShadowPowerCatalogue.find("lightning_strike");
		var strike = com.powers.companion.combat.ShadowPowerExecutor.execute(
				helper.getLevel(), shadow, target, lightning,
				new com.powers.companion.combat.ShadowPowerExecutor.ExecutionContext(owner, false, tick));
		helper.assertTrue(strike.success() && target.getHealth() < before,
				"Shadow's named lightning executor did not damage a player-like target");
		helper.assertTrue(!helper.getLevel().getEntitiesOfClass(LightningBolt.class,
				target.getBoundingBox().inflate(2.0), Entity -> true).isEmpty(),
				"Shadow lightning did not create a live server bolt");
		com.powers.companion.combat.ShadowPowerCatalogue.requireComplete();
		PrivateCompanionManager.handleChat(owner, "shadow, leave me");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void shadowCombatRolesAreInspectableAndAreaAttacksSpareDarknessAllies(
			GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(4, 1, 4));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself");
		PrivateCompanionManager.tickPlayer(owner, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		shadow.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		shadow.setEnergy(com.powers.companion.ShadowCompanionRules.MAX_ENERGY);

		PowerTestActor hostile = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(4, 1, 8));
		PowerTestActor ally = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(7, 1, 4));
		hostile.setNoAi(true);
		ally.setNoAi(true);
		ally.addTag(SkillSystem.DARKNESS_TAG);
		float hostileBefore = hostile.getHealth();
		float allyBefore = ally.getHealth();
		var lightning = com.powers.companion.combat.ShadowPowerCatalogue.find("lightning_strike");
		var rejected = com.powers.companion.combat.ShadowPowerExecutor.execute(
				helper.getLevel(), shadow, ally, lightning,
				new com.powers.companion.combat.ShadowPowerExecutor.ExecutionContext(
						owner, false, helper.getLevel().getServer().getTickCount()));
		helper.assertTrue(!rejected.success() && rejected.reason().equals("friendly_target")
				&& ally.getHealth() == allyBefore,
				"Shadow did not reject a direct attack against a Darkness-aligned ally");
		var thunderclap = com.powers.companion.combat.ShadowPowerCatalogue.find("thunderclap");
		var cast = com.powers.companion.combat.ShadowPowerExecutor.execute(
				helper.getLevel(), shadow, hostile, thunderclap,
				new com.powers.companion.combat.ShadowPowerExecutor.ExecutionContext(
						owner, false, helper.getLevel().getServer().getTickCount()));
		helper.assertTrue(cast.success() && hostile.getHealth() < hostileBefore,
				"Shadow area attack did not affect the hostile encounter target");
		helper.assertTrue(ally.getHealth() == allyBefore,
				"Shadow area attack damaged a Darkness-aligned ally");

		var facts = new com.powers.companion.combat.ShadowCombatFacts(
				4, .8, .8, false, false, .9, .9, .8,
				false, false, true, com.powers.companion.combat.ShadowRequestRange.AUTO);
		var plan = com.powers.companion.combat.ShadowTacticalPlanner.choose(
				com.powers.companion.combat.ShadowPowerCatalogue.actions(), facts,
				new com.powers.companion.combat.ShadowLearningState());
		helper.assertTrue(plan.role() == com.powers.companion.combat.ShadowEncounterRole.PEEL
				&& plan.evaluatedCount() <= 26 && plan.action() != null,
				"Live planner did not expose its bounded peel-role decision");
		PrivateCompanionManager.handleChat(owner, "shadow, leave me");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void shadowExplicitFeedbackIsOwnerLocalResettableAndCannotOverrideSafety(
			GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		other.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 1, 12))));

		var learning = new com.powers.companion.combat.ShadowLearningState();
		learning.rememberChoice("far:boss", "boss", "fireball");
		com.powers.companion.ShadowCompanionStore.update(owner,
				state -> state.withLearnedCombat(learning.encode()));
		helper.assertTrue(PrivateCompanionManager.handleChat(owner, "shadow, good"),
				"Explicit feedback was not consumed");
		var learned = com.powers.companion.combat.ShadowLearningState.decode(
				com.powers.companion.ShadowCompanionStore.get(owner).learnedCombat());
		helper.assertTrue(learned.feedbackModifier("fireball") > 0.0
				&& learned.rewardModifier("far:boss", "boss", "fireball") == 0.0,
				"Explicit feedback leaked into the combat-reward profile");
		helper.assertTrue(com.powers.companion.ShadowCompanionStore.get(other)
				.learnedCombat().isEmpty(), "One owner's feedback leaked to another owner");

		PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself");
		PrivateCompanionManager.tickPlayer(owner, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		BlockPos amethyst = shadow.blockPosition().offset(1, -1, 0);
		helper.getLevel().setBlock(amethyst, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
		com.powers.power.AmethystDampening.blockChanged(helper.getLevel(), amethyst,
				Blocks.AMETHYST_BLOCK.defaultBlockState());
		com.powers.companion.ShadowMagicState.tick(owner, shadow);
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 1, 7));
		int energyBefore = shadow.energy();
		var result = com.powers.companion.combat.ShadowPowerExecutor.execute(
				helper.getLevel(), shadow, target,
				com.powers.companion.combat.ShadowPowerCatalogue.find("fireball"),
				new com.powers.companion.combat.ShadowPowerExecutor.ExecutionContext(
						owner, false, helper.getLevel().getServer().getTickCount()));
		helper.assertTrue(!result.success() && result.reason().equals("amethyst_suppressed")
				&& shadow.energy() == energyBefore,
				"Owner praise overrode the hard amethyst safety gate");

		PrivateCompanionManager.handleChat(owner, "shadow, reset what you learned");
		var reset = com.powers.companion.combat.ShadowLearningState.decode(
				com.powers.companion.ShadowCompanionStore.get(owner).learnedCombat());
		helper.assertTrue(reset.feedbackActionCount() == 0 && reset.contextCount() == 0
				&& !reset.hasRememberedChoice(), "Owner reset retained learned combat state");
		helper.getLevel().setBlock(amethyst, Blocks.AIR.defaultBlockState(), 3);
		com.powers.power.AmethystDampening.blockChanged(helper.getLevel(), amethyst,
				Blocks.AIR.defaultBlockState());
		PrivateCompanionManager.handleChat(owner, "shadow, leave me");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal")
	public void shadowNamedPlacesStayOwnerLocalAndUseOnlyLoadedNavigation(GameTestHelper helper) {
		for (int x = 1; x <= 11; x++) {
			for (int z = 0; z <= 3; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		}
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Vec3 camp = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		owner.setPos(camp);
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		other.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 1, 12))));

		helper.assertTrue(PrivateCompanionManager.handleChat(owner,
				"shadow, remember this place as camp"), "Named-place command was not consumed");
		var saved = com.powers.companion.ShadowCompanionStore.get(owner).places();
		helper.assertTrue(saved.size() == 1 && saved.find("camp").isPresent(),
				"Owner-designated place was not stored");
		helper.assertTrue(com.powers.companion.ShadowCompanionStore.get(other).places().size() == 0,
				"Named place leaked to another owner");

		owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 1, 2))));
		owner.setYRot(-90.0F);
		owner.setYHeadRot(-90.0F);
		PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself");
		PrivateCompanionManager.tickPlayer(owner, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		double before = shadow.distanceToSqr(camp);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(shadow.onGround(),
						"Manifested Shadow did not settle on the prepared floor"))
				.thenExecute(() -> {
					helper.assertTrue(PrivateCompanionManager.handleChat(owner, "shadow, go to camp"),
							"Loaded-place navigation command was not consumed");
					helper.assertTrue(shadow.getNavigation().isInProgress(),
							"Loaded local place did not create a navigation path: shadow="
									+ shadow.position() + ", target=" + camp + ", onGround="
									+ shadow.onGround() + ", noPhysics=" + shadow.noPhysics
									+ ", below=" + shadow.level().getBlockState(
											shadow.blockPosition().below()));
					helper.assertTrue(PrivateCompanionManager.diagnostics().forcedChunks() == 0,
							"Named-place navigation created a forced chunk");
				})
				.thenIdle(20)
				.thenExecute(() -> {
					helper.assertTrue(shadow.distanceToSqr(camp) < before,
							"Shadow did not move toward the owner-designated loaded place");
					PrivateCompanionManager.handleChat(owner, "shadow, forget place camp");
					helper.assertTrue(com.powers.companion.ShadowCompanionStore.get(owner)
							.places().size() == 0, "Explicit delete retained the named place");
					PrivateCompanionManager.handleChat(owner, "shadow, leave me");
				})
				.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	@SuppressWarnings("removal")
	public void shadowCalloutsPreserveCriticalWarningsPrivacyAndFrequencyModes(
			GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		PrivateCompanionManager.handleChat(owner, "shadow, come");
		PrivateCompanionManager.tickPlayer(owner, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		var before = PrivateCompanionManager.diagnostics();

		PrivateCompanionManager.handleChat(owner, "shadow, callouts critical");
		owner.setHealth(4.0F);
		PrivateCompanionManager.tickPlayer(owner, 1);
		var danger = PrivateCompanionManager.diagnostics();
		helper.assertTrue(danger.criticalWarnings() == before.criticalWarnings() + 1
				&& danger.chatterCallouts() == before.chatterCallouts()
				&& danger.privateCallouts() == before.privateCallouts() + 1,
				"Critical-only mode suppressed or publicly leaked a hidden danger warning");

		owner.setHealth(owner.getMaxHealth());
		PrivateCompanionManager.handleChat(owner, "shadow, callouts verbose");
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(2, 1, 7));
		target.setNoAi(true);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000.0);
		target.setHealth(target.getMaxHealth());
		shadow.setTarget(target);
		PrivateCompanionManager.tickPlayer(owner, 6);
		var engaged = PrivateCompanionManager.diagnostics();
		helper.assertTrue(engaged.chatterCallouts() == danger.chatterCallouts() + 1,
				"Verbose mode did not emit the combat-state transition");

		PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself");
		BlockPos amethyst = helper.absolutePos(new BlockPos(2, 1, 2));
		helper.getLevel().setBlock(amethyst, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
		com.powers.power.AmethystDampening.blockChanged(helper.getLevel(), amethyst,
				Blocks.AMETHYST_BLOCK.defaultBlockState());
		helper.assertTrue(PrivateCompanionManager.isRevealed(owner.getUUID()),
				"Reveal command did not expose the live Shadow session");
		PrivateCompanionManager.tickPlayer(owner, 11);
		var revealed = PrivateCompanionManager.diagnostics();
		helper.assertTrue(revealed.criticalWarnings() == engaged.criticalWarnings() + 1
				&& revealed.globalCallouts() == engaged.globalCallouts() + 1,
				"A revealed critical warning did not survive chatter or global delivery: before="
						+ engaged.summary() + ", after=" + revealed.summary());

		PrivateCompanionManager.handleChat(owner, "shadow, leave me");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void crossingEnergyAndVoidRaysCreatePhysicalConsequences(GameTestHelper helper) {
		MagicRayCollisionRuntime.clearAll();
		ServerPlayer sun = helper.makeMockServerPlayerInLevel();
		ServerPlayer voidCaster = helper.makeMockServerPlayerInLevel();
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 2, 5)));
		sun.setPos(center.add(-5.0, 0.0, 0.0));
		voidCaster.setPos(center.add(0.0, 0.0, -5.0));
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(5, 2, 5));
		target.setNoAi(true);
		float before = target.getHealth();
		long tick = helper.getLevel().getServer().getTickCount();
		helper.assertTrue(MagicRayCollisionRuntime.publish(helper.getLevel(), "energy_beam",
				sun.getUUID(), center.add(-4.0, 0.0, 0.0), center.add(4.0, 0.0, 0.0), tick).isEmpty(),
				"First ray collided without another presence");
		helper.assertTrue(MagicRayCollisionRuntime.publish(helper.getLevel(), "void_beam",
				voidCaster.getUUID(), center.add(0.0, 0.0, -4.0), center.add(0.0, 0.0, 4.0), tick).isPresent(),
				"Crossing physical rays did not collide");
		helper.assertTrue(target.getHealth() < before,
				"Beam collision pressure blast did not damage a nearby entity");
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(LightningBolt.class,
				new net.minecraft.world.phys.AABB(center.add(-8, -4, -8), center.add(8, 8, 8))).size() >= 2,
				"Beam collision did not strike both caster omens");
		MagicRayCollisionRuntime.clearAll();
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void deadPossessedVesselReturnsItsControllerWithDivineWrath(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		PowerTestActor host = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		host.setNoAi(false);
		helper.assertTrue(new VesselPossessionAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)), "Possession setup failed");
		int beforeEnergy = com.powers.player.PlayerPowers.get(caster).energy();
		host.hurtServer(helper.getLevel(), host.damageSources().generic(), 10_000.0F);
		VesselPossessionAbility.tickAll(helper.getLevel().getServer());
		helper.assertTrue(caster.isAlive() && com.powers.player.PlayerPowers.get(caster).mindBody() == null,
				"Dead vessel killed or stranded its controller");
		helper.assertTrue(com.powers.player.PlayerPowers.get(caster).energy() < beforeEnergy
				&& caster.hasEffect(MobEffects.WEAKNESS) && caster.hasEffect(MobEffects.DARKNESS),
				"Dead vessel did not invoke energy-draining particle-hidden wrath");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void losingShadowSwordDeactivatesItsRoutedFlight(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.addTag(SkillSystem.DARKNESS_TAG);
		ItemStack sword = PowersWeapons.weapon("lycanbane").getDefaultInstance();
		player.getInventory().add(sword);
		var flight = ArtifactWeaponManager.actions(ArtifactAlignment.DARKNESS).stream()
				.filter(action -> action.definition().key().equals("innate/flight")).findFirst().orElseThrow();
		String key = ArtifactWeaponManager.toggleKey(flight);
		helper.assertTrue(flight.ability().activateToggleOn(player,
				com.powers.player.PlayerPowers.get(player)), "Artifact flight setup failed");
		com.powers.player.PlayerPowers.get(player).setToggleActive(player, key, true);
		player.getInventory().clearContent();
		com.powers.item.ArtifactInventoryRuntime.reconcileOwnership(player);
		helper.assertFalse(com.powers.player.PlayerPowers.get(player).isToggleActive(key),
				"Artifact-routed flight survived loss of its owning sword");
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
			ItemStack output = crucible.getItem(ArcaneCrucibleBlockEntity.WEAPON_SLOT);
			helper.assertTrue(output.getCount() == 1
					&& output.has(PowersDataComponents.CRUCIBLE_WEAPON),
					"Crucible did not commit exactly one persistent transformed weapon");
			helper.assertFalse(player.getInventory().contains(stack ->
					stack.has(PowersDataComponents.CRUCIBLE_WEAPON)),
					"Crucible duplicated its output across two ownership boundaries");
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
		ItemStack star = new ItemStack(com.powers.ImportedPackItems.item(
				"imported_artifact_star_animated"));
		var bindingChoice = com.powers.forge.CrucibleTransformationCatalogue
				.choices(conversion.result(), star).getFirst();
		CrucibleTransactionResult binding = CrucibleTransactionEngine.prepare(
				conversion.result(), star, bindingChoice);
		helper.assertTrue(binding.success(), "Animated-star binding failed");
		CrucibleWeaponData bound = binding.result().get(PowersDataComponents.CRUCIBLE_WEAPON);
		helper.assertTrue(bound != null && bound.starBound(), "Binding data was not persisted");
		ItemStack rune = new ItemStack(com.powers.ImportedPackItems.item(
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
				PowersWeapons.weapon("lycanbane").getDefaultInstance()),
				"Shadow Sword entered the Crucible");
		helper.assertFalse(CrucibleEligibility.isBaseWeapon(
				PowersWeapons.weapon("heavenly_partisan").getDefaultInstance()),
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

	@GameTest
	public void firstVesselSpawnsAsPersistentCompletePowerBoss(GameTestHelper helper) {
		FirstVessel boss = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 1, 2));
		Player target = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(boss.isPersistenceRequired(), "First Vessel was allowed to despawn");
		helper.assertTrue(boss.effectiveMaximumHealth() == 5_000.0F,
				"First Vessel effective health was not 5000");
		float vitality = boss.effectiveHealth();
		helper.assertTrue(boss.hurtServer(helper.getLevel(), boss.damageSources().generic(), 100.0F),
				"First Vessel rejected ordinary damage");
		helper.assertTrue(boss.effectiveHealth() < vitality && boss.isAlive(),
				"First Vessel vitality layer did not absorb damage");
		helper.assertTrue(FirstVesselPowerCatalogue.actions().size() == 23,
				"First Vessel did not adapt every innate power");
		helper.assertTrue(boss.canAttack(target), "First Vessel could not target a survival player");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void completedFirstVesselAltarConsumesAnchorsAndSpawnsBoss(GameTestHelper helper) {
		BlockPos altar = new BlockPos(5, 1, 5);
		helper.setBlock(altar, PowersBlocks.ARCANE_CRUCIBLE);
		for (BlockPos offset : java.util.List.of(new BlockPos(3, 0, 0), new BlockPos(-3, 0, 0),
				new BlockPos(0, 0, 3), new BlockPos(0, 0, -3))) {
			helper.setBlock(altar.offset(offset), PowersBlocks.DARKNESS);
		}
		for (BlockPos offset : java.util.List.of(new BlockPos(2, 0, 2), new BlockPos(-2, 0, 2),
				new BlockPos(2, 0, -2), new BlockPos(-2, 0, -2))) {
			helper.setBlock(altar.offset(offset), PowersBlocks.PURE_LIGHT);
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(player).setDarknessLevel(player, 10);
		helper.assertTrue(FirstVesselRitual.invoke(player, helper.absolutePos(altar)),
				"Completed First Vessel ritual was rejected");
		helper.assertEntityPresent(PowersEntities.FIRST_VESSEL, altar.above(), 3.0);
		helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR,
				altar.offset(3, 0, 0));
		helper.succeed();
	}

	@GameTest
	public void celestialRuinOverwhelmsTheFirstVesselsLayeredVitality(GameTestHelper helper) {
		FirstVessel boss = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 1, 2));
		float before = boss.effectiveHealth();

		boss.hurtServer(helper.getLevel(), PowerDamage.celestialRuin(helper.getLevel()), 50_000.0F);

		helper.assertTrue(before >= 5_000.0F, "Test boss did not start at full layered vitality");
		helper.assertTrue(boss.effectiveHealth() <= 0.0F || !boss.isAlive(),
				"Celestial Ruin barely damaged the First Vessel");
		helper.succeed();
	}

	@GameTest
	public void celestialRuinAtmosphereAndScarsStayBoundedAcrossSixThousandBlocks(
			GameTestHelper helper) {
		var near = com.powers.spell.CelestialRuinPresentation.aftermathDensity(
				com.powers.fx.FxLodTier.NEAR);
		var far = com.powers.spell.CelestialRuinPresentation.aftermathDensity(
				com.powers.fx.FxLodTier.FAR);
		helper.assertTrue(near.particleCount() <= 56 && far.particleCount() <= 16,
				"Celestial fallout exceeded its per-event client budget");
		helper.assertTrue(com.powers.spell.CelestialRuinPresentation.MAX_AFTERMATH_EVENTS
				* near.particleCount() <= 224,
				"Overlapping Celestial fallout exceeded its global client budget");
		double[] distances = {0.0, 1_200.0, 3_600.0, 5_999.0};
		int previousCap = Integer.MAX_VALUE;
		for (int index = 0; index < distances.length; index++) {
			double distance = distances[index];
			int depth = com.powers.spell.CelestialRuinRules.structuralScarDepth(distance, index * 31);
			int cap = distance < 1_200.0 ? 6 : distance < 3_600.0 ? 4
					: distance < 5_400.0 ? 2 : 1;
			helper.assertTrue(depth >= 1 && depth <= cap && cap <= previousCap,
					"Structural scar depth escaped its distance band");
			previousCap = cap;
		}
		var last = com.powers.spell.CelestialRuinRules.aftershockOffset(
				com.powers.spell.CelestialRuinRules.aftershockTotalSteps() - 1);
		helper.assertTrue(Math.hypot(last.x(), last.z())
				<= com.powers.spell.CelestialRuinRules.DAMAGE_RADIUS + 1.0,
				"A distant scar escaped the authored event radius");
		helper.assertTrue(com.powers.spell.CelestialRuinRules.AFTERSHOCK_WORK_PER_TICK == 2_048,
				"Server scar work no longer has the authored hard tick budget");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void crystalDiscoveryMigratesLegacySelectionAndPersistsFirstUse(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var convergence = (com.powers.power.crystals.ModeCrystalAbility)
				com.powers.power.crystals.CrystalPowerRegistry.get(PowersItems.RAINBOW_CRYSTAL);
		List<String> modes = convergence.modeIds();
		helper.assertTrue(convergence.discoveredModes(player).isEmpty(),
				"new player crystal identities were revealed before use");
		helper.assertTrue(convergence.selectMode(player, 1), "new selector rejected a valid unknown mode");
		helper.assertTrue(convergence.discoveredModes(player).isEmpty(),
				"selecting an unknown mode revealed it before successful use");
		helper.assertTrue(com.powers.player.CrystalDiscoveryStore.discover(player,
				"rainbow_crystal", modes, modes.get(1)), "first use was not discovered");

		ServerPlayer legacyPlayer = helper.makeMockServerPlayerInLevel();
		PlayerPowers.get(legacyPlayer).setSelectedCrystalMode("rainbow_crystal", 2);
		List<String> migrated = convergence.discoveredModes(legacyPlayer);
		helper.assertTrue(migrated.equals(List.of(modes.get(2))),
				"legacy crystal selection did not migrate to one stable discovery");
		helper.assertTrue(com.powers.player.CrystalDiscoveryStore.discover(legacyPlayer,
				"rainbow_crystal", modes, modes.get(4)), "first use was not discovered");
		helper.assertTrue(convergence.discoveredModes(legacyPlayer).equals(
				List.of(modes.get(2), modes.get(4))), "discovery did not persist in canonical order");
		helper.assertTrue(!com.powers.player.CrystalDiscoveryStore.discover(legacyPlayer,
				"rainbow_crystal", modes, modes.get(4)), "duplicate use rewrote discovery state");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void forcefieldSharesAndSacrificesItselfAgainstOverkill(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		ServerPlayer ally = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		ally.setPos(origin.getX() + 1.5, origin.getY(), origin.getZ() + 0.5);
		helper.assertTrue(new ForcefieldAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)), "Forcefield activation failed");
		long tick = helper.getLevel().getServer().getTickCount();
		helper.assertTrue(MagicShieldManager.global().active(caster.getUUID(), tick),
				"Caster did not receive shared integrity");
		helper.assertTrue(MagicShieldManager.global().active(ally.getUUID(), tick),
				"Nearby ally did not receive shared integrity");
		helper.assertTrue(ForcefieldAbility.absorbDamage(ally, ally.damageSources().generic(), 50_000.0F),
				"Overkill impact bypassed the sacrificial shield");
		helper.assertFalse(MagicShieldManager.global().active(ally.getUUID(), tick),
				"Broken shield retained integrity after sacrificing itself");
		MagicShieldManager.global().clear();
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal")
	public void layeredDamageUsesWardThenSoulLinkThenHealthExactlyOnce(GameTestHelper helper) {
		MagicShieldManager.global().clear();
		com.powers.power.crystals.SoulLinkAbility.clearAll();
		com.powers.power.damage.LayeredDamageLedger.global().clear();
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var first = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(3, 1, 2));
		var second = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(4, 1, 2));
		first.setNoAi(true);
		second.setNoAi(true);
		MagicShieldManager.global().raise(first.getUUID(), 40.0F, Long.MAX_VALUE);
		helper.assertTrue(com.powers.magic.runtime.CastScalingContext.withSource(
				com.powers.magic.runtime.CastSource.CRYSTAL,
				() -> new com.powers.power.crystals.SoulLinkAbility().activate(
						caster, com.powers.player.PlayerPowers.get(caster))),
				"Soul Link activation failed");
		float firstBefore = first.getHealth();
		float secondBefore = second.getHealth();

		helper.assertFalse(first.hurtServer(helper.getLevel(), first.damageSources().generic(), 50_000.0F),
				"Forcefield overkill reached health before Soul Link");
		CrystalPowerRegistry.tick(helper.getLevel().getServer());
		helper.assertTrue(first.getHealth() == firstBefore && second.getHealth() == secondBefore,
				"A shielded wound leaked into Soul Link or health");
		helper.assertFalse(MagicShieldManager.global().active(first.getUUID(),
				helper.getLevel().getServer().getTickCount()), "Overkill did not sacrifice the ward");

		MagicShieldManager.global().raise(second.getUUID(), 2.0F, Long.MAX_VALUE);
		boolean primaryDamaged = first.hurtServer(
				helper.getLevel(), first.damageSources().generic(), 3.0F);
		helper.assertTrue(primaryDamaged && first.getHealth() < firstBefore,
				"The first unshielded wound never reached health: result=" + primaryDamaged
						+ "; firstHealth=" + first.getHealth() + "; links="
						+ com.powers.power.crystals.SoulLinkAbility.snapshots());
		CrystalPowerRegistry.tick(helper.getLevel().getServer());
		helper.assertTrue(second.getHealth() == secondBefore,
				"Mirrored damage reached health before the recipient's forcefield");
		helper.assertFalse(MagicShieldManager.global().active(second.getUUID(),
				helper.getLevel().getServer().getTickCount()),
				"The mirrored wound did not sacrifice the recipient ward: "
						+ com.powers.power.damage.LayeredDamageLedger.global().snapshot().line()
						+ "; links=" + com.powers.power.crystals.SoulLinkAbility.snapshots());

		// This GameTest deliberately exercises two independent wounds in one server
		// tick, so remove vanilla's post-hit immunity between those stimuli.
		first.invulnerableTime = 0;
		boolean secondPrimaryDamaged = first.hurtServer(
				helper.getLevel(), first.damageSources().generic(), 3.0F);
		helper.assertTrue(secondPrimaryDamaged,
				"The second independent wound was rejected before layer ordering");
		CrystalPowerRegistry.tick(helper.getLevel().getServer());
		float secondAfterMirror = second.getHealth();
		helper.assertTrue(secondAfterMirror < secondBefore,
				"Soul Link did not forward the unshielded health wound");
		CrystalPowerRegistry.tick(helper.getLevel().getServer());
		helper.assertTrue(second.getHealth() == secondAfterMirror,
				"The production crystal tick recursively bounced mirrored damage through Soul Link");

		var accounting = com.powers.power.damage.LayeredDamageLedger.global().snapshot();
		helper.assertTrue(accounting.wardBlockedDamage() >= 50_000.0,
				"Diagnostics omitted sacrificial no-overflow absorption");
		helper.assertTrue(accounting.wardIntegritySpent() == 42.0,
				"Diagnostics double-consumed or invented ward integrity");
		helper.assertTrue(accounting.mirrorForwardedDamage() > accounting.mirrorActualHealthRemoved()
				&& accounting.mirrorActualHealthRemoved() > 0.0,
				"Diagnostics did not distinguish shielded forwarding from health damage");

		double healthBeforeMitigationChecks = accounting.actualHealthRemoved();
		var armored = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(7, 1, 2));
		armored.setNoAi(true);
		armored.getAttribute(Attributes.ARMOR).setBaseValue(10.0);
		var armorAttacker = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(9, 1, 2));
		armorAttacker.setNoAi(true);
		float armoredBefore = armored.getHealth();
		helper.assertTrue(armored.hurtServer(helper.getLevel(),
				armored.damageSources().mobAttack(armorAttacker), 8.0F),
				"Armored diagnostic stimulus was rejected");
		float armorReducedHealthRemoved = armoredBefore - armored.getHealth();
		helper.assertTrue(armorReducedHealthRemoved > 0.0F && armorReducedHealthRemoved < 8.0F,
				"Armor did not reduce the diagnostic stimulus: removed=" + armorReducedHealthRemoved);
		var afterArmor = com.powers.power.damage.LayeredDamageLedger.global().snapshot();
		helper.assertTrue(Math.abs(afterArmor.actualHealthRemoved()
				- (healthBeforeMitigationChecks + armorReducedHealthRemoved)) < 0.0001,
				"Armor-reduced accounting differed from actual health removed: " + afterArmor.line());

		var lethal = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(8, 1, 2));
		lethal.setNoAi(true);
		lethal.setHealth(3.0F);
		double healthBeforeLethal = afterArmor.actualHealthRemoved();
		helper.assertTrue(lethal.hurtServer(helper.getLevel(), lethal.damageSources().generic(), 50.0F),
				"Lethal diagnostic stimulus was rejected");
		helper.assertTrue(lethal.getHealth() == 0.0F,
				"Lethal diagnostic stimulus did not remove the remaining three health");
		var afterLethal = com.powers.power.damage.LayeredDamageLedger.global().snapshot();
		helper.assertTrue(Math.abs(afterLethal.actualHealthRemoved()
				- (healthBeforeLethal + 3.0)) < 0.0001,
				"Lethal accounting omitted final health removed: " + afterLethal.line());
		com.powers.power.crystals.SoulLinkAbility.clearAll();
		MagicShieldManager.global().clear();
		com.powers.power.damage.LayeredDamageLedger.global().clear();
		helper.succeed();
	}

	@GameTest(maxTicks = 90)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void hearthSanctuaryRaisesIndependentSacrificialWards(GameTestHelper helper) {
		MagicShieldManager.global().clear();
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		DarknessCreature ally = helper.spawn(PowersEntities.DARKNESS_CREATURE, new BlockPos(4, 1, 2));
		ally.setNoAi(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_wild").getDefaultInstance());
		com.powers.player.PlayerPowers.get(caster).setSelectedSpell("book_grimoire_wild", 2);

		SpellCastingManager.use(caster, "book_grimoire_wild");
		helper.runAfterDelay(50, () -> {
			long tick = helper.getLevel().getServer().getTickCount();
			helper.assertTrue(MagicShieldManager.global().active(caster.getUUID(), tick),
					"Hearth Sanctuary did not ward its caster");
			helper.assertTrue(MagicShieldManager.global().active(ally.getUUID(), tick),
					"Hearth Sanctuary did not ward a living entity inside three blocks");
			helper.assertTrue(ForcefieldAbility.absorbDamage(
					ally, ally.damageSources().generic(), 50_000.0F),
					"An oversized hit bypassed Hearth Sanctuary's sacrificial ward");
			helper.assertTrue(MagicShieldManager.global().active(caster.getUUID(), tick),
					"One recipient's broken ward consumed another recipient's integrity");
			MagicShieldManager.global().clear();
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void forcefieldFollowsTheMindBodyTetherAndProtectsThePhysicalBody(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.setHealth(20.0F);
		helper.assertTrue(BodyProxyManager.start(owner, BodyProxyKind.ASTRAL),
				"Could not create a physical body for the shield collision test");
		Mannequin body = helper.getLevel().getEntitiesOfClass(Mannequin.class,
				owner.getBoundingBox().inflate(2.0), BodyProxyManager::isProxy).getFirst();
		MagicShieldManager.global().raise(owner.getUUID(), 40.0F, Long.MAX_VALUE);

		helper.assertFalse(body.hurtServer(helper.getLevel(), body.damageSources().generic(), 50_000.0F),
				"The sacrificial shield let an overkill impact reach the physical body");
		helper.assertTrue(owner.getHealth() == 20.0F,
				"A shielded physical-body hit leaked to the detached mind");
		helper.assertFalse(MagicShieldManager.global().active(owner.getUUID(),
				helper.getLevel().getServer().getTickCount()),
				"The sacrificed tethered shield retained integrity");
		BodyProxyManager.finish(owner);
		MagicShieldManager.global().clear();
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void astralEntrypointOwnsTheParticipantAndRefusesNestedSoulLink(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		com.powers.knowledge.MagicAttemptJournal.global().forget(owner.getUUID());
		owner.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 5));

		helper.assertTrue(BodyProxyManager.start(owner, BodyProxyKind.ASTRAL),
				"Astral production entrypoint could not create its vulnerable body");
		MindSessionManager.Session astral = MindSessionManager.activeFor(owner.getUUID()).orElse(null);
		helper.assertTrue(astral != null && astral.kind() == MindSessionManager.Kind.ASTRAL_PROJECTION,
				"Astral production entrypoint did not acquire authoritative participant ownership");
		helper.assertFalse(new com.powers.power.crystals.SoulLinkAbility().activate(owner,
				com.powers.player.PlayerPowers.get(owner)),
				"Soul Link nested inside an existing Astral session");
		helper.assertTrue(MindSessionManager.activeFor(owner.getUUID())
				.map(session -> session.id().equals(astral.id())
						&& session.kind() == MindSessionManager.Kind.ASTRAL_PROJECTION).orElse(false),
				"Rejected nested activation disturbed the original Astral session");
		helper.assertTrue(com.powers.power.crystals.SoulLinkAbility.snapshots().stream()
				.noneMatch(snapshot -> snapshot.owner().equals(owner.getUUID())),
				"Rejected nested Soul Link left runtime state behind");
		var refusal = com.powers.knowledge.MagicAttemptJournal.global().latestFailure(
				owner.getUUID(), "Why did Soul Link fail?", helper.getLevel().getGameTime()).orElse(null);
		helper.assertTrue(refusal != null
				&& refusal.reason() == com.powers.knowledge.MagicFailureReason.MIND_SESSION_ACTIVE,
				"Nested refusal did not retain the precise authoritative diagnostic reason");
		BodyProxyManager.finish(owner);
		helper.assertFalse(MindSessionManager.isParticipating(owner.getUUID()),
				"Astral return leaked authoritative participant ownership");
		com.powers.knowledge.MagicAttemptJournal.global().forget(owner.getUUID());
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactAstralReturnsOnSourceLossNearBosses(GameTestHelper helper) {
		var source = helper.getLevel();
		var server = source.getServer();
		var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "astral-caster");
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
		var caster = new ServerPlayer(server, source, profile, cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, caster, cookie);
		caster.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		caster.setGameMode(GameType.SURVIVAL);
		var origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
		caster.snapTo(origin, 0, 0);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		var data = PlayerPowers.get(caster);
		data.setDarknessLevel(caster, 1);
		data.forceRestoreEnergy();
		int energy = data.energy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(2, 1, 4)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(4, 1, 2)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(7, 1, 4)));
		bosses.forEach(boss -> boss.setNoAi(true));
		var health = bosses.stream().map(boss -> boss.getHealth()).toList();
		var alignment = ArtifactAlignment.DARKNESS;
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "innate/astral_projection", -1),
				"Artifact refused Astral Projection selection");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment) == AbilityActivationService.Result.ACTIVATED
				&& data.energy() < energy && caster.isSpectator() && caster.distanceToSqr(origin) > 1
				&& BodyProxyManager.hasSession(caster, BodyProxyKind.ASTRAL)
				&& AstralProjectionAbility.isActive(caster.getUUID()), "Artifact projection did not create an active remote form");
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(AstralProjectionAbility.isActive(caster.getUUID()), "Artifact projection ended while still owned");
			caster.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(!AstralProjectionAbility.isActive(caster.getUUID()) && !caster.isSpectator()
					&& caster.gameMode() == GameType.SURVIVAL && caster.level() == source && caster.distanceToSqr(origin) < 0.01
					&& !BodyProxyManager.hasSession(caster, BodyProxyKind.ASTRAL)
					&& !MindSessionManager.isParticipating(caster.getUUID()), "Source loss did not return the caster and clear ownership");
			helper.assertTrue(bosses.stream().map(boss -> boss.getHealth()).toList().equals(health),
					"Projection changed boss health");
			server.getPlayerList().remove(caster);
			channel.finishAndReleaseAll();
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void fatalAstralDamageReturnsToThePhysicalBodyBeforeDeath(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.setHealth(20.0F);
		helper.assertTrue(BodyProxyManager.start(owner, BodyProxyKind.ASTRAL),
				"Could not create an astral body for the fatal-return test");
		owner.setPos(origin.getX() + 6.5, origin.getY() + 2.0, origin.getZ() + 0.5);
		owner.setHealth(0.0F); // ALLOW_DEATH observes post-mitigation vanilla health.

		helper.assertFalse(BodyProxyManager.allowsAvatarDeath(
				owner, owner.damageSources().generic()),
				"Fatal astral death was allowed to remain at the remote camera");
		helper.assertTrue(com.powers.player.PlayerPowers.get(owner).mindBody() == null,
				"Fatal astral death did not clear the physical-body session before replay");
		helper.assertTrue(owner.position().distanceToSqr(Vec3.atBottomCenterOf(origin)) < 1.0,
				"Fatal astral death did not recall the player to their physical body");
		helper.assertTrue(BodyProxyManager.bodyIdForOwner(owner.getUUID()) == null,
				"Fatal astral damage leaked a body proxy or forced-chunk ticket");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(owner.isAlive(),
					"Recalled fatal damage was not replayed through vanilla death");
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void astralReturnIndicatorUsesOnlyTheOwnedAnchorWithoutForcingChunks(GameTestHelper helper) {
		AstralProjectionAbility.clearAll(helper.getLevel().getServer());
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.setGameMode(GameType.SURVIVAL);
		com.powers.player.PlayerPowers.get(owner).setSlots(owner,
				java.util.List.of("powers:astral_projection"));
		helper.assertTrue(new AstralProjectionAbility().activate(owner,
				com.powers.player.PlayerPowers.get(owner)), "Astral Projection did not start");
		owner.setPos(origin.getX() + 12.5, origin.getY() + 20.0, origin.getZ() + 12.5);
		int travelTicketsBefore = com.powers.power.travel.TravelChunkLoader.diagnostics().active();
		int bodyTicketsBefore = BodyProxyManager.activeProxyCount();

		AstralReturnIndicator.Cue cue = AstralProjectionAbility.returnIndicatorFor(owner);
		helper.assertTrue(cue != null && cue.sameDimension()
				&& "NW".equals(cue.direction()) && cue.distanceBlocks() == 17,
				"Return cue did not derive bounded owner-local direction and distance");
		helper.assertTrue(com.powers.power.travel.TravelChunkLoader.diagnostics().active()
				== travelTicketsBefore && BodyProxyManager.activeProxyCount() == bodyTicketsBefore,
				"Deriving the return cue created a travel or body chunk ticket");
		AstralProjectionAbility.clearAll(helper.getLevel().getServer());
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void fatalPhysicalProxyDamageRecallsItsDetachedOwnerBeforeDeath(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.setHealth(20.0F);
		helper.assertTrue(BodyProxyManager.start(owner, BodyProxyKind.ASTRAL),
				"Could not create a physical proxy for the fatal-body test");
		Mannequin body = helper.getLevel().getEntitiesOfClass(Mannequin.class,
				owner.getBoundingBox().inflate(2.0), BodyProxyManager::isProxy).getFirst();
		owner.setPos(origin.getX() + 6.5, origin.getY() + 2.0, origin.getZ() + 0.5);

		helper.assertTrue(body.hurtServer(helper.getLevel(), body.damageSources().generic(), 10_000.0F),
				"Fatal post-mitigation body damage was rejected before lifecycle handling");
		helper.assertTrue(com.powers.player.PlayerPowers.get(owner).mindBody() == null
				&& owner.position().distanceToSqr(Vec3.atBottomCenterOf(origin)) < 1.0,
				"A killed physical proxy did not recall its detached owner");
		helper.assertTrue(BodyProxyManager.bodyIdForOwner(owner.getUUID()) == null
				&& !BodyProxyManager.isProxy(body),
				"Killed physical proxy leaked its entity or forced-chunk ticket");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(owner.isAlive(),
					"Fatal physical-body damage was not replayed through vanilla death");
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void crouchingPlantHealingRestoresPlayersInsideTwoBlocksOnly(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		ServerPlayer ally = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		ally.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		ally.setPos(origin.getX() + 2.5, origin.getY(), origin.getZ() + 0.5);
		caster.setHealth(1.0F);
		ally.setHealth(1.0F);
		caster.setShiftKeyDown(true);
		caster.setPose(net.minecraft.world.entity.Pose.CROUCHING);

		helper.assertTrue(new PlantHealingAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)),
				"Crouching Plant Healing did not activate for injured nearby players");
		helper.assertTrue(caster.getHealth() > 1.0F, "Plant Healing did not heal its caster");
		helper.assertTrue(ally.getHealth() > 1.0F, "Plant Healing excluded the inclusive two-block boundary");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void testActorAcceptsPlayerTargetStateAndSharedShield(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PowerTestActor actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 1, 2));
		actor.setNoAi(true);

		helper.assertTrue(DimensionalAnchorAbility.apply(caster, actor),
				"Player-compatible actor rejected Dimensional Anchor");
		helper.assertTrue(DimensionalAnchorAbility.isAnchored(actor),
				"Actor did not retain its dimensional anchor");
		TestActorPowerState.drain(actor.getUUID(), 375);
		helper.assertTrue(TestActorPowerState.energy(actor.getUUID()) == 625,
				"Actor simulated energy did not drain like a player well");
		helper.assertTrue(new ForcefieldAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)), "Forcefield activation failed");
		long tick = helper.getLevel().getServer().getTickCount();
		helper.assertTrue(MagicShieldManager.global().active(actor.getUUID(), tick),
				"Nearby actor did not receive shared forcefield integrity");
		helper.assertTrue(ForcefieldAbility.absorbDamage(actor,
				actor.damageSources().generic(), 50_000.0F),
				"Actor shield failed to sacrifice itself against overkill");
		MagicShieldManager.global().clear();
		TestActorPowerState.clear(actor.getUUID());
		helper.succeed();
	}

	@GameTest(padding = 64)
	@SuppressWarnings("removal")
	public void dimensionalAnchorStabilizesOnlyLoadedExistingLocalState(GameTestHelper helper) {
		com.powers.power.artifact.ArtifactGateManager.clear();
		com.powers.power.abilities.LocalBodyAnchorRegistry.global().clear();
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		helper.assertTrue(com.powers.power.artifact.ArtifactGateManager.open(
				caster, ArtifactAlignment.LIGHT), "Could not create the existing local gate fixture");

		var gate = com.powers.power.artifact.ArtifactGateManager.stabilizeExisting(
				caster, caster.getUUID(), caster.position(), 400);
		helper.assertTrue(gate.stabilized() && gate.deadline() > gate.previousDeadline(),
				"Dimensional Anchor did not extend the already-existing loaded legal gate");
		var missing = com.powers.power.artifact.ArtifactGateManager.stabilizeExisting(
				caster, java.util.UUID.randomUUID(), caster.position(), 400);
		helper.assertTrue(!missing.stabilized()
				&& missing.refusal() == com.powers.power.abilities.DimensionalStabilizationRules.Refusal.MISSING,
				"Stabilization invented a missing gate");

		ServerPlayer traveller = helper.makeMockServerPlayerInLevel();
		traveller.setPos(origin.getX() + 2.5, origin.getY(), origin.getZ() + 0.5);
		helper.assertTrue(BodyProxyManager.start(traveller, BodyProxyKind.ASTRAL),
				"Could not create the loaded local body fixture");
		java.util.UUID bodyId = BodyProxyManager.bodyIdForOwner(traveller.getUUID());
		var body = (net.minecraft.world.entity.LivingEntity) helper.getLevel().getEntity(bodyId);
		helper.assertTrue(body != null && DimensionalAnchorAbility.apply(caster, body),
				"Dimensional Anchor rejected a loaded local body proxy");
		helper.assertTrue(DimensionalAnchorAbility.anchorDimension(body)
				== helper.getLevel().dimension(), "Body proxy did not retain its local dimension");
		BodyProxyManager.finish(traveller);
		helper.assertTrue(com.powers.power.abilities.LocalBodyAnchorRegistry.global().size() == 0,
				"Body cleanup retained a stabilization lease");
		com.powers.power.artifact.ArtifactGateManager.clear();
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void energyDrainUsesTheTestActorsPlayerEnergyWell(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(caster).setSlots(caster, java.util.List.of(
				"powers:energy_drain", "powers:flight", "powers:void_beam"));
		PowerTestActor actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		actor.setNoAi(true);
		helper.assertTrue(new EnergyDrainAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)), "Energy Drain did not acquire the actor");
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(TestActorPowerState.energy(actor.getUUID()) == 0,
					"Energy Drain used the ordinary-mob damage branch for a player-compatible actor");
			TestActorPowerState.clear(actor.getUUID());
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void energyDrainTargetBreaksTheLiveTetherByHoldingCrouch(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		caster.snapTo(origin, 0.0F, 0.0F);
		target.snapTo(origin.add(0.0, 0.0, 4.0), 180.0F, 0.0F);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(caster).setSlots(caster,
				java.util.List.of("powers:energy_drain", "powers:flight", "powers:void_beam"));
		helper.assertTrue(new EnergyDrainAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)), "Energy Drain did not acquire its player target");
		target.setShiftKeyDown(true);
		helper.runAfterDelay(2, () -> helper.assertTrue(EnergyDrainAbility.isChanneling(caster.getUUID()),
				"The live tether ended before the eight-tick crouch contest completed"));

		helper.runAfterDelay(EnergyDrainRules.RESISTANCE_TICKS + 2, () -> {
			helper.assertFalse(EnergyDrainAbility.isChanneling(caster.getUUID()),
					"Eight consecutive crouch ticks did not break the live soul tether");
			EnergyDrainAbility.clear(caster.getUUID());
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void energyDrainMobConversionCannotExceedItsHealthScaledBudget(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		caster.snapTo(origin, 0.0F, 0.0F);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		var data = com.powers.player.PlayerPowers.get(caster);
		data.setSlots(caster, java.util.List.of(
				"powers:energy_drain", "powers:flight", "powers:void_beam"));
		data.drainEnergy(data.energyCapacity());
		net.minecraft.world.entity.animal.golem.IronGolem golem = helper.spawn(
				net.minecraft.world.entity.EntityTypes.IRON_GOLEM, new BlockPos(2, 1, 6));
		golem.setNoAi(true);
		int cap = EnergyDrainRules.mobConversionCap(golem.getMaxHealth());
		long refundedBefore = com.powers.player.PlayerEnergyHistory.snapshot(caster).amount(
				com.powers.player.EnergyHistorySource.REFUND);
		helper.assertTrue(new EnergyDrainAbility().activate(caster, data),
				"Energy Drain did not acquire its non-player target");

		helper.runAfterDelay(45, () -> {
			long refunded = com.powers.player.PlayerEnergyHistory.snapshot(caster).amount(
					com.powers.player.EnergyHistorySource.REFUND) - refundedBefore;
			helper.assertTrue(refunded > 0 && refunded <= cap,
					"Live mob conversion exceeded its immutable health-scaled budget: "
							+ refunded + "/" + cap);
			EnergyDrainAbility.clear(caster.getUUID());
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void rankZeroAndRankTenCombatImpactsLeaveDistinctBoundedScars(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos center = new BlockPos(7, 2, 7);
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) helper.setBlock(center.offset(x, 0, z), Blocks.STONE);
		}
		Vec3 impact = Vec3.atCenterOf(helper.absolutePos(center));
		int rankZero = CombatTerrainImpact.crater(helper.getLevel(), caster, impact, 0);

		helper.assertTrue(rankZero > 0, "A rank-zero offensive impact left no terrain damage");
		helper.assertTrue(rankZero <= com.powers.power.abilities.CombatTerrainRules.craterBudget(0),
				"Combat crater exceeded its rank-zero work budget");
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) helper.setBlock(center.offset(x, 0, z), Blocks.STONE);
		}
		int rankTen = CombatTerrainImpact.crater(helper.getLevel(), caster, impact, 10);
		helper.assertTrue(rankTen > rankZero, "Rank ten did not leave a visibly larger scar");
		helper.assertTrue(rankTen <= com.powers.power.abilities.CombatTerrainRules.craterBudget(10),
				"Combat crater exceeded its rank-ten work budget");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void cinderheartBlockImpactCannotDereferenceAnAbsentLivingTarget(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		helper.setBlock(new BlockPos(2, 1, 6), Blocks.STONE);
		TestingOverrides.setEnergyDisabled(caster.getUUID(), true);
		FireballAbility ability = new FireballAbility();
		helper.assertTrue(ability.activate(caster, com.powers.player.PlayerPowers.get(caster)),
				"Cinderheart did not create its server-owned projectile");
		LargeFireball projectile = helper.getLevel().getEntitiesOfClass(LargeFireball.class,
				caster.getBoundingBox().inflate(8.0), entity -> entity.isAlive()).stream()
				.findFirst().orElse(null);
		helper.assertTrue(projectile != null, "Cinderheart projectile was absent");
		BlockPos impactPos = helper.absolutePos(new BlockPos(2, 1, 6));
		FireballAbility.resolveImpact(projectile, new BlockHitResult(
				Vec3.atCenterOf(impactPos), Direction.NORTH, impactPos, false));
		helper.assertTrue(projectile.isRemoved(), "Resolved Cinderheart impact remained active");
		TestingOverrides.clear(caster.getUUID());
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void fireballFrostImpactUsesFiniteThermalTableAndKeepsRealmBlocks(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockPos impactPos = helper.absolutePos(new BlockPos(2, 2, 6));
		BlockPos lightPos = impactPos.east();
		BlockPos darknessPos = impactPos.west();
		caster.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		helper.getLevel().setBlockAndUpdate(impactPos, Blocks.FROSTED_ICE.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(lightPos,
				com.powers.PowersBlocks.PURE_LIGHT.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(darknessPos,
				com.powers.PowersBlocks.DARKNESS.defaultBlockState());
		TestingOverrides.setEnergyDisabled(caster.getUUID(), true);
		try {
			FireballAbility ability = new FireballAbility();
			helper.assertTrue(ability.activate(caster, com.powers.player.PlayerPowers.get(caster)),
					"Cinderheart did not create its server-owned projectile");
			LargeFireball projectile = helper.getLevel().getEntitiesOfClass(LargeFireball.class,
					caster.getBoundingBox().inflate(8.0), entity -> entity.isAlive()).stream()
					.findFirst().orElse(null);
			helper.assertTrue(projectile != null, "Cinderheart projectile was absent");
			FireballAbility.resolveImpact(projectile, new BlockHitResult(
					Vec3.atCenterOf(impactPos), Direction.NORTH, impactPos, false));
			helper.assertTrue(helper.getLevel().getBlockState(impactPos).is(Blocks.WATER),
					"Fireball frost impact bypassed the shared ICE-to-WATER transition");
			helper.assertTrue(helper.getLevel().getBlockState(lightPos).is(com.powers.PowersBlocks.PURE_LIGHT)
					&& helper.getLevel().getBlockState(darknessPos).is(com.powers.PowersBlocks.DARKNESS),
					"Thermal impact changed an immutable realm block");
			helper.runAfterDelay(10, () -> {
				helper.assertTrue(helper.getLevel().getBlockState(impactPos).is(Blocks.WATER),
						"Thermal result changed after scheduled fluid ticks");
				helper.succeed();
			});
		} finally {
			TestingOverrides.clear(caster.getUUID());
		}
	}

	@GameTest(maxTicks = 130)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void iceHitAppliesFiniteBrittleArmourAndFireBreaksIt(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
		caster.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		var target = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(2, 2, 6));
		target.setNoAi(true);
		var armour = target.getAttribute(Attributes.ARMOR);
		helper.assertTrue(armour != null, "Brittle-armour target had no armour attribute");
		armour.setBaseValue(20.0);
		var data = com.powers.player.PlayerPowers.get(caster);
		data.setSlots(caster, java.util.List.of("powers:ice_manipulation"));
		TestingOverrides.setAll(caster.getUUID(), true);
		try {
			helper.assertTrue(com.powers.power.AbilityActivationService.activate(caster,
					new IceManipulationAbility(), "powers:ice_manipulation", true)
					== com.powers.power.AbilityActivationService.Result.ACTIVATED,
					"Ice production entrypoint did not hit its armoured target");
			helper.assertTrue(BrittleArmorManager.active(target.getUUID())
					&& armour.getValue() == 15.0,
					"Ice hit did not apply the exact finite 25% armour fracture");
			target.invulnerableTime = 0;
			helper.assertTrue(target.hurtServer(helper.getLevel(), target.damageSources().inFire(), 2.0F),
					"Fire counterplay did not produce a successful damage event");
			helper.assertTrue(!BrittleArmorManager.active(target.getUUID())
					&& armour.getValue() == 20.0,
					"Successful fire damage did not clear only the Ice-owned armour fracture");
			BrittleArmorManager.apply(target);
			helper.assertTrue(BrittleArmorManager.active(target.getUUID())
					&& armour.getValue() == 15.0,
					"Brittle armour could not begin a fresh finite lifecycle after fire cleanup");
		} catch (RuntimeException | Error failure) {
			BrittleArmorManager.clear(target);
			TestingOverrides.clear(caster.getUUID());
			target.discard();
			caster.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
			throw failure;
		}
		helper.runAfterDelay(101, () -> {
			try {
				helper.assertTrue(!BrittleArmorManager.active(target.getUUID())
						&& armour.getValue() == 20.0,
						"Production ticking did not expire and remove the finite armour fracture");
			} finally {
				BrittleArmorManager.clear(target);
				TestingOverrides.clear(caster.getUUID());
				target.discard();
				caster.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
			}
			helper.succeed();
		});
	}

	@GameTest
	public void lightningTagsGroundRodsButRelayCopper(GameTestHelper helper) {
		BlockPos support = new BlockPos(4, 1, 4);
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR,
				new BlockPos(4, 2, 4));
		helper.setBlock(support, Blocks.LIGHTNING_ROD.asList().getFirst());
		helper.assertTrue(LightningConductanceRuntime.classify(helper.getLevel(), target)
				== LightningStrikeRules.Conductance.LIGHTNING_ROD,
				"A tagged lightning rod did not safely ground body-local conduction");

		helper.setBlock(support, Blocks.COPPER_BLOCK.asList().getFirst());
		helper.assertTrue(LightningConductanceRuntime.classify(helper.getLevel(), target)
				== LightningStrikeRules.Conductance.BLOCK,
				"Tagged copper did not remain a conductive relay");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void shadowSwordLightningCreatesAVisibleBolt(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 64, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(caster).setDarknessLevel(caster, 10);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.setBlock(new BlockPos(2, 63, 6), Blocks.STONE);
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 64, 6));
		target.setNoAi(true);
		target.setNoGravity(true);
		float targetHealthBefore = target.getHealth();
		helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS,
				"innate/lightning_strike", -1), "Shadow Sword rejected Lightning");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Shadow Sword Lightning activation pipeline failed");
		helper.runAfterDelay(2, () -> {
			caster.getInventory().setItem(1, caster.getMainHandItem().copy());
			caster.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		boolean[] observedBolt = {false};
		for (int delay = 7; delay <= 12; delay++) {
			helper.runAfterDelay(delay, () -> observedBolt[0] |= !helper.getLevel().getEntitiesOfClass(
					LightningBolt.class, caster.getBoundingBox().inflate(12.0), entity -> true).isEmpty());
		}
		helper.runAfterDelay(13, () -> {
			helper.assertTrue(observedBolt[0], "Shadow Sword Lightning did not create a visible bolt");
			helper.assertTrue(target.getHealth() < targetHealthBefore,
					"Shadow Sword Lightning marked its target but dealt no damage");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactLightningHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactLightning(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactLightningHitsLightHerald(GameTestHelper helper) {
		verifyArtifactLightning(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactLightningHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactLightning(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL);
	}

	@GameTest(maxTicks = 40)
	public void lightArtifactLightningHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactLightning(helper, ArtifactAlignment.LIGHT, com.powers.PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void lightArtifactLightningHitsLightHerald(GameTestHelper helper) {
		verifyArtifactLightning(helper, ArtifactAlignment.LIGHT, com.powers.PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void lightArtifactLightningHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactLightning(helper, ArtifactAlignment.LIGHT, com.powers.PowersEntities.FIRST_VESSEL);
	}

	@GameTest(maxTicks = 140)
	public void darknessArtifactStarfallHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "starfall");
	}

	@GameTest(maxTicks = 140)
	public void darknessArtifactStarfallHitsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "starfall");
	}

	@GameTest(maxTicks = 140)
	public void darknessArtifactStarfallHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "starfall");
	}

	@GameTest(maxTicks = 140)
	public void lightArtifactStarfallHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.LIGHT, com.powers.PowersEntities.DARK_HERALD, "starfall");
	}

	@GameTest(maxTicks = 140)
	public void lightArtifactStarfallHitsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.LIGHT, com.powers.PowersEntities.LIGHT_HERALD, "starfall");
	}

	@GameTest(maxTicks = 140)
	public void lightArtifactStarfallHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.LIGHT, com.powers.PowersEntities.FIRST_VESSEL, "starfall");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactVoidBeamHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "void_beam");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactVoidBeamHitsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "void_beam");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactVoidBeamHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "void_beam");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactFireballHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "fireball");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactFireballHitsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "fireball");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactFireballHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "fireball");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactBreezyBashMovesDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "breezy_bash");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactBreezyBashMovesLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "breezy_bash");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactBreezyBashMovesFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "breezy_bash");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityPullsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "gravity_displacement");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityPullsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "gravity_displacement");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityPullsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "gravity_displacement");
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityOrbitMovesDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD,
				"gravity_displacement", 1);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityOrbitMovesLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD,
				"gravity_displacement", 1);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityOrbitMovesFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL,
				"gravity_displacement", 1);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityRepelMovesDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD,
				"gravity_displacement", 2);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityRepelMovesLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD,
				"gravity_displacement", 2);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactGravityRepelMovesFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL,
				"gravity_displacement", 2);
	}

	@GameTest(maxTicks = 80)
	public void darknessArtifactEnergyDrainHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "energy_drain");
	}

	@GameTest(maxTicks = 80)
	public void darknessArtifactEnergyDrainHitsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "energy_drain");
	}

	@GameTest(maxTicks = 80)
	public void darknessArtifactEnergyDrainHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "energy_drain");
	}

	@GameTest
	public void darknessArtifactIceHitsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "ice_manipulation");
	}

	@GameTest
	public void darknessArtifactIceHitsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "ice_manipulation");
	}

	@GameTest
	public void darknessArtifactIceHitsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "ice_manipulation");
	}

	@GameTest
	public void darknessArtifactVeilBreaksOnHitToDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "invisibility");
	}

	@GameTest
	public void darknessArtifactVeilBreaksOnHitToLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "invisibility");
	}

	@GameTest
	public void darknessArtifactVeilBreaksOnHitToFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "invisibility");
	}

	@GameTest(maxTicks = 30)
	public void darknessArtifactSpeedBurstImpactsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "speed_burst");
	}

	@GameTest(maxTicks = 30)
	public void darknessArtifactSpeedBurstImpactsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "speed_burst");
	}

	@GameTest(maxTicks = 30)
	public void darknessArtifactSpeedBurstImpactsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "speed_burst");
	}

	@GameTest(maxTicks = 250)
	public void darknessArtifactSuperSpeedSupportsDarkHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.DARK_HERALD, "super_speed");
	}

	@GameTest(maxTicks = 250)
	public void darknessArtifactSuperSpeedSupportsLightHerald(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.LIGHT_HERALD, "super_speed");
	}

	@GameTest(maxTicks = 250)
	public void darknessArtifactSuperSpeedSupportsFirstVessel(GameTestHelper helper) {
		verifyArtifactBossStrike(helper, ArtifactAlignment.DARKNESS, com.powers.PowersEntities.FIRST_VESSEL, "super_speed");
	}

	private static void verifyArtifactLightning(GameTestHelper helper, ArtifactAlignment alignment,
			net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type) {
		verifyArtifactBossStrike(helper, alignment, type, "lightning_strike");
	}

	private static void verifyArtifactBossStrike(GameTestHelper helper, ArtifactAlignment alignment,
			net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, String action) {
		verifyArtifactBossStrike(helper, alignment, type, action, action.equals("gravity_displacement") ? 0 : -1);
	}

	@SuppressWarnings("removal")
	private static void verifyArtifactBossStrike(GameTestHelper helper, ArtifactAlignment alignment,
			net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, String action, int option) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 64, 2));
		caster.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
		caster.setNoGravity(true);
		var data = PlayerPowers.get(caster);
		boolean darkness = alignment == ArtifactAlignment.DARKNESS;
		if (darkness) caster.addTag(SkillSystem.DARKNESS_TAG);
		data.setSkillLevel(caster, 1);
		data.setDarknessLevel(caster, 1);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				PowersWeapons.weapon(darkness ? "lycanbane" : "heavenly_partisan").getDefaultInstance());
		data.forceRestoreEnergy();
		int energy = data.energy();
		var boss = helper.spawn(type, new BlockPos(2, 64, 6));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(caster.getX(), caster.getY(), caster.getZ() + 4);
		if (action.equals("speed_burst")) {
			boss.setPos(caster.getX() + 1.5, caster.getY(), caster.getZ() + 1);
			BlockPos wall = caster.blockPosition().offset(0, 0, 2);
			helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());
		}
		var floor = BlockPos.containing(boss.position()).below();
		helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
		double initial = lightningVitality(boss);
		boolean starfall = action.equals("starfall");
		if (starfall) {
			for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
				helper.getLevel().setBlockAndUpdate(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
			}
		}
		boolean gravity = action.equals("gravity_displacement");
		var movementSpeed = net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
		double initialSpeed = caster.getAttributeValue(movementSpeed);
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "innate/" + action, option),
				"Artifact refused selection: " + action);
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
				== AbilityActivationService.Result.ACTIVATED && (action.equals("invisibility") || data.energy() < energy),
				"Artifact did not activate and spend energy: " + action);
		if (action.equals("invisibility")) {
			String toggle = "artifact/darkness/innate/invisibility";
			var invisibility = net.minecraft.world.effect.MobEffects.INVISIBILITY;
			helper.assertTrue(caster.hasEffect(invisibility) && data.isToggleActive(toggle) && lightningVitality(boss) == initial,
					"Artifact veil did not apply its invisibility effect without harming the boss");
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(caster.isInvisible(), "Artifact veil did not update visible entity state");
				boss.hurtServer(helper.getLevel(), caster.damageSources().playerAttack(caster), 8);
				helper.assertTrue(lightningVitality(boss) < initial && !caster.hasEffect(invisibility) && !data.isToggleActive(toggle),
						"A successful hit on the boss did not break the artifact veil");
				helper.succeed();
			});
			return;
		}
		if (action.equals("ice_manipulation")) {
			helper.assertTrue(lightningVitality(boss) < initial && boss.getTicksFrozen() > 0,
					"Ice Manipulation did not damage and chill the aimed boss");
			helper.succeed();
			return;
		}
		helper.assertTrue(lightningVitality(boss) == initial, action + " bypassed its warning");
		if (action.equals("super_speed")) {
			helper.assertTrue(caster.getAttributeValue(movementSpeed) > initialSpeed,
					"Super Speed did not increase movement speed");
			helper.runAfterDelay(200, () -> helper.assertTrue(caster.getAttributeValue(movementSpeed) > initialSpeed,
					"Super Speed ended before its artifact lease"));
			helper.runAfterDelay(230, () -> {
				try {
					helper.assertTrue(caster.getAttributeValue(movementSpeed) == initialSpeed
							&& lightningVitality(boss) == initial, "Super Speed expiry retained its modifier or harmed the boss");
					helper.succeed();
				} finally {
					com.powers.power.abilities.SuperSpeedAbility.clear(helper.getLevel().getServer(), caster.getUUID());
				}
			});
			return;
		}
		if (action.equals("speed_burst")) {
			helper.assertTrue(caster.getDeltaMovement().z > 0, "Speed Burst did not propel the caster forward");
			helper.runAfterDelay(12, () -> {
				try {
					helper.assertTrue(lightningVitality(boss) < initial, "Speed Burst endpoint did not damage the boss");
					helper.succeed();
				} finally {
					com.powers.power.abilities.SpeedBurstAbility.clear(caster.getUUID());
				}
			});
			return;
		}
		if (action.equals("energy_drain")) {
			var refund = com.powers.player.EnergyHistorySource.REFUND;
			long refundedBefore = com.powers.player.PlayerEnergyHistory.snapshot(caster).amount(refund);
			int cap = EnergyDrainRules.mobConversionCap(boss.getMaxHealth());
			caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.powers.PowersEffects.EXHAUSTION, 100));
			helper.assertFalse(data.regenerateEnergy(1), "Exhaustion did not block passive regeneration");
			helper.runAfterDelay(65, () -> {
				try {
					long converted = com.powers.player.PlayerEnergyHistory.snapshot(caster).amount(refund) - refundedBefore;
					helper.assertTrue(caster.hasEffect(com.powers.PowersEffects.EXHAUSTION)
							&& !EnergyDrainAbility.isChanneling(caster.getUUID())
							&& lightningVitality(boss) < initial && converted > 0 && converted <= cap,
							"Energy Drain completion: vitality=" + lightningVitality(boss) + "/" + initial
									+ ", converted=" + converted + "/" + cap + ", energy=" + data.energy()
									+ "/" + data.energyCapacity());
					helper.succeed();
				} finally {
					com.powers.power.abilities.EnergyDrainAbility.clear(caster.getUUID());
				}
			});
			return;
		}
		if (gravity) {
			boss.setNoAi(false);
			double startDistance = boss.distanceToSqr(caster);
			var startPosition = boss.position();
			var impulse = boss.getDeltaMovement();
			helper.assertTrue(switch (option) {
				case 0 -> impulse.z < 0;
				case 1 -> impulse.lengthSqr() > 0;
				case 2 -> impulse.z > 0;
				default -> false;
			}, "Gravity mode " + option + " did not apply its initial steering: " + impulse);
			helper.runAfterDelay(8, () -> {
				try {
					double distance = boss.distanceToSqr(caster);
					helper.assertTrue(switch (option) {
						case 0 -> distance < startDistance;
						case 1 -> Math.abs(boss.getX() - startPosition.x) > 0.01;
						case 2 -> distance > startDistance;
						default -> false;
					}, "Gravity mode " + option + " did not move the boss as expected");
					helper.succeed();
				} finally {
					com.powers.power.abilities.GravityDisplacementAbility.clear(helper.getLevel().getServer(), caster.getUUID());
				}
			});
			return;
		}
		if (action.equals("breezy_bash")) {
			boss.setNoAi(false);
			double startY = boss.getY();
			int startAge = boss.tickCount;
			helper.assertTrue(boss.getDeltaMovement().y > 0, "Breezy Bash did not apply its upward impulse");
			helper.runAfterDelay(8, () -> helper.assertTrue(boss.tickCount > startAge && boss.getY() > startY,
					"Breezy Bash did not lift a ticking boss: age=" + boss.tickCount + "/" + startAge
							+ ", y=" + boss.getY() + "/" + startY));
			helper.runAfterDelay(19, () -> {
				try {
					helper.assertTrue(boss.getDeltaMovement().y < 0, "Breezy Bash did not slam the boss");
					helper.succeed();
				} finally {
					com.powers.power.abilities.BreezyBashAbility.clear(helper.getLevel().getServer(), caster.getUUID());
				}
			});
			return;
		}
		if (action.equals("fireball")) {
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(lightningVitality(boss) == initial, "Hovering Fireball damaged the boss");
				caster.setShiftKeyDown(true);
				caster.setPose(net.minecraft.world.entity.Pose.CROUCHING);
				helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
						== AbilityActivationService.Result.ACTIVATED, "Artifact refused crouch-release");
			});
		}
		if (action.equals("void_beam")) {
			helper.runAfterDelay(10, () -> helper.assertTrue(lightningVitality(boss) == initial,
					"Void Beam damaged its target before its charge finished"));
		}
		helper.runAfterDelay(starfall ? 100 : 16, () -> {
			try {
				helper.assertTrue(lightningVitality(boss) < initial, action + " did not damage the boss");
				helper.succeed();
			} finally {
				if (starfall) {
					com.powers.power.abilities.StarfallAbility.clear(helper.getLevel().getServer(), caster.getUUID());
				} else if (action.equals("void_beam")) {
					com.powers.power.abilities.VoidBeamAbility.clear(caster.getUUID());
				} else if (action.equals("fireball")) {
					com.powers.power.abilities.FireballAbility.clear(helper.getLevel().getServer(), caster.getUUID());
				} else {
					com.powers.power.abilities.LightningStrikeAbility.clear(helper.getLevel().getServer(), caster.getUUID());
				}
			}
		});
	}

	private static double lightningVitality(net.minecraft.world.entity.Mob boss) {
		return boss instanceof com.powers.entity.FirstVessel vessel ? vessel.effectiveHealth() : boss.getHealth();
	}

	@GameTest(maxTicks = 70)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void heavenlyPartisanEnergyBeamDamagesItsAimedTarget(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 4, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		com.powers.player.PlayerPowers.get(caster).setSkillLevel(caster, 10);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				PowersWeapons.weapon("heavenly_partisan").getDefaultInstance());
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 4, 8));
		target.setNoAi(true);
		target.setNoGravity(true);
		float targetHealthBefore = target.getHealth();
		helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.LIGHT,
				"innate/energy_beam", -1), "Heavenly Partisan rejected Energy Beam");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.LIGHT)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Heavenly Partisan Energy Beam activation pipeline failed");
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(target.getHealth() < targetHealthBefore,
					"Heavenly Partisan Energy Beam channel dealt no damage");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 20, setupTicks = 10)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void artifactAstralProjectionRequiresInventoryOwnership(GameTestHelper helper) {
		AstralProjectionAbility.clearAll(helper.getLevel().getServer());
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setGameMode(GameType.SURVIVAL);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(caster).setDarknessLevel(caster, 10);
		GameType originalMode = caster.gameMode();
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS,
				"innate/astral_projection", -1), "Shadow Sword rejected Astral Projection");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Artifact Astral Projection did not start");
		helper.assertTrue(AstralProjectionAbility.isActive(caster.getUUID()),
				"Artifact Astral Projection was not registered");

		helper.runAfterDelay(2, () -> {
			caster.getInventory().setItem(1, caster.getMainHandItem().copy());
			caster.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		helper.runAfterDelay(4, () -> helper.assertTrue(
				AstralProjectionAbility.isActive(caster.getUUID()),
				"Artifact Astral Projection ended when the sword remained in inventory"));
		helper.runAfterDelay(6, () -> caster.getInventory().clearContent());
		helper.runAfterDelay(9, () -> {
			helper.assertFalse(AstralProjectionAbility.isActive(caster.getUUID()),
					"Artifact Astral Projection survived loss of its owning sword");
			helper.assertFalse(BodyProxyManager.hasSession(caster, BodyProxyKind.ASTRAL),
					"Ended artifact projection leaked its vulnerable body session");
			helper.assertTrue(caster.gameMode() == originalMode,
					"Ended artifact projection restored " + caster.gameMode()
							+ " instead of " + originalMode);
			AstralProjectionAbility.clearAll(helper.getLevel().getServer());
			helper.succeed();
		});
	}

	@GameTest
	public void namedTestActorResolvesLikeAPlayerUsername(GameTestHelper helper) {
		PowerTestActor actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 2));
		actor.setTestingUsername("TrainingMage");
		NamedLivingTargetIndex.track(actor);
		var resolution = NamedLivingTargetIndex.resolve(helper.getLevel().getServer(), "trainingmage");
		helper.assertTrue(resolution.status() == NamedTargetRules.Status.FOUND,
				"A unique actor username did not resolve");
		helper.assertTrue(resolution.target() == actor,
				"Actor username resolved to the wrong living target");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void vesselPossessionControlsAndRestoresAMobHost(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		PowerTestActor host = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		host.setNoAi(false);
		double before = host.getZ();

		helper.assertTrue(new VesselPossessionAbility().activate(caster,
				com.powers.player.PlayerPowers.get(caster)), "Vessel Possession rejected a living mob");
		helper.assertTrue(host.isNoAi(), "Possessed mob retained autonomous AI");
		VesselPossessionAbility.applyControl(caster, new VesselControlPackets.InputPayload(
				0L, 1.0F, 0.0F, false, false, 0.0F, 0.0F, 0, -1));
		helper.assertTrue(host.getZ() > before, "Authenticated forward input did not move the host");
		VesselPossessionAbility.clear(caster);
		helper.assertFalse(host.isNoAi(), "Mob AI was not restored after possession");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void blueDreamwalkingUsesTheSharedAuthenticatedControlChannel(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PowerTestActor host = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		host.setNoAi(false);
		double before = host.getZ();

		helper.assertTrue(VesselPossessionAbility.beginDreamwalk(caster, host, 600,
				com.powers.magic.runtime.CastSource.CRYSTAL),
				"Blue Dreamwalking rejected a valid mob host");
		helper.assertTrue(VesselPossessionAbility.isDreamwalking(caster.getUUID()),
				"Dreamwalking did not own the shared control session");
		VesselPossessionAbility.applyControl(caster, new VesselControlPackets.InputPayload(
				0L, 1.0F, 0.0F, false, false, 0.0F, 0.0F, 0, -1));
		helper.assertTrue(host.getZ() > before, "Dreamwalking remained camera-only");
		helper.assertTrue(VesselPossessionAbility.stopDreamwalking(caster),
				"Dreamwalking could not be toggled off");
		helper.assertFalse(host.isNoAi(), "Dreamwalking did not restore the host's AI");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void dreamwalkingReleasePacketReturnsTheSpectatingOwner(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		for (int x = 0; x <= 6; x++) for (int z = 0; z <= 8; z++) {
			helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		}
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PowerTestActor host = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		GameType originalMode = caster.gameMode();

		helper.assertTrue(VesselPossessionAbility.beginDreamwalk(caster, host, 600,
				com.powers.magic.runtime.CastSource.CRYSTAL), "Dreamwalking setup failed");
		helper.assertTrue(originalMode.getName().equals(com.powers.player.PlayerPowers.get(caster)
				.mindBody().gameMode()), "Body proxy did not retain the owner's original mode");
		helper.assertTrue(caster.gameMode.getGameModeForPlayer() == GameType.SPECTATOR,
				"Dreamwalking owner did not enter its remote camera state");
		helper.assertTrue(VesselControlPackets.releaseControlledSession(caster),
				"Authenticated release packet did not end Dreamwalking");
		helper.assertFalse(VesselPossessionAbility.isDreamwalking(caster.getUUID()),
				"Dreamwalking remained active after its release packet");
		helper.succeedWhen(() -> {
			helper.assertFalse(com.powers.mind.BodyProxyManager.hasSession(caster,
					com.powers.mind.BodyProxyKind.DREAMWALK),
					"Release packet left the vulnerable body proxy active");
			helper.assertTrue(caster.gameMode.getGameModeForPlayer() == originalMode,
					"Release packet did not restore the physical body's game mode: "
							+ caster.gameMode.getGameModeForPlayer().getName()
							+ ", alive=" + caster.isAlive());
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void testingOverridesBypassEnergyAndPowerCooldowns(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var data = com.powers.player.PlayerPowers.get(player);
		data.emptyEnergy();
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		helper.assertTrue(data.consumeEnergy(Integer.MAX_VALUE),
				"Testing mode still enforced the energy limit");
		var ability = new ForcefieldAbility();
		TestingOverrides.setCooldownsDisabled(player.getUUID(), false);
		ActivationCooldowns.start(player, ability, 200);
		TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
		helper.assertTrue(ActivationCooldowns.remainingTicks(player, ability) == 0,
				"Testing mode still exposed an active cooldown");
		TestingOverrides.clear(player.getUUID());
		helper.succeed();
	}

	@GameTest(maxTicks = 220)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void darkCrystalMovesItsCasterIntoTheMindscape(GameTestHelper helper) {
		GameTestClock.atNormalServerRate(helper, 150);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
		var darkRealm = helper.getLevel().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						com.powers.PowersMod.id("dark_realm")));
		helper.assertTrue(darkRealm != null, "Dark Crystal travel requires the shipped Dark Realm");
		com.powers.PowersMod.LOGGER.info("Realm travel fixture: realm=dark_realm, destinationInitiallyLoaded={}",
				darkRealm.getChunkSource().getChunkNow(0, 0) != null);

		helper.assertTrue(CrystalPowerRegistry.tryActivate(player, PowersItems.DARK_CRYSTAL),
				"Dark Crystal activation pipeline rejected its caster");
		helper.runAfterDelay(150, () -> {
			helper.assertTrue(player.level().dimension().identifier().toString()
					.equals("powers:dark_realm"), "Dark Crystal never entered the Dark Realm");
			helper.assertTrue(BodyProxyManager.hasSession(player, BodyProxyKind.REALM),
					"Dark Crystal did not leave a vulnerable physical body");
			TestingOverrides.clear(player.getUUID());
			BodyProxyManager.finish(player);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 230)
	public void darkCrystalReturnsItsBossCohort(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.DARK_CRYSTAL, "powers:dark_realm");
	}

	@GameTest(maxTicks = 230)
	public void lightCrystalReturnsItsBossCohort(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.LIGHT_CRYSTAL, "powers:light_realm");
	}

	@GameTest(maxTicks = 230)
	public void indigoCrystalReturnsItsBossCohort(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.INDIGO_CRYSTAL, "powers:middleworld");
	}

	private static void verifyBossRealmRoundTrip(GameTestHelper helper, net.minecraft.world.item.Item crystal,
			String realm) {
		verifyBossRealmRoundTrip(helper, crystal, realm, null, null);
	}

	@GameTest(maxTicks = 230)
	public void darkArtifactReturnsFromDarkRealm(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.DARK_CRYSTAL, "powers:dark_realm",
				ArtifactAlignment.DARKNESS, "crystal/dark_crystal");
	}

	@GameTest(maxTicks = 230)
	public void darkArtifactReturnsFromLightRealm(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.LIGHT_CRYSTAL, "powers:light_realm",
				ArtifactAlignment.DARKNESS, "crystal/light_crystal");
	}

	@GameTest(maxTicks = 230)
	public void darkArtifactReturnsFromMiddleworld(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.INDIGO_CRYSTAL, "powers:middleworld",
				ArtifactAlignment.DARKNESS, "crystal/middleworld");
	}

	@GameTest(maxTicks = 230)
	public void lightArtifactReturnsFromLightRealm(GameTestHelper helper) {
		verifyBossRealmRoundTrip(helper, PowersItems.LIGHT_CRYSTAL, "powers:light_realm",
				ArtifactAlignment.LIGHT, "crystal/light_crystal");
	}

	@SuppressWarnings("removal")
	private static void verifyBossRealmRoundTrip(GameTestHelper helper, net.minecraft.world.item.Item crystal,
			String realm, ArtifactAlignment alignment, String action) {
		GameTestClock.atNormalServerRate(helper, 175);
		var source = helper.getLevel();
		var player = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 1, 4));
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				source.setBlockAndUpdate(origin.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
			}
		}
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var held = alignment == null ? crystal.getDefaultInstance() : PowersWeapons.weapon(
				alignment == ArtifactAlignment.LIGHT ? "heavenly_partisan" : "lycanbane").getDefaultInstance();
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
		if (alignment != ArtifactAlignment.LIGHT) player.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		PlayerPowers.get(player).setSkillLevel(player, com.powers.player.SkillSystem.DARKNESS_GATE_LEVEL);
		PlayerPowers.get(player).setDarknessLevel(player, com.powers.player.SkillSystem.DARKNESS_GATE_LEVEL);
		PlayerPowers.get(player).forceRestoreEnergy();
		var bosses = java.util.List.of(
				helper.spawn(com.powers.PowersEntities.DARK_HERALD, new BlockPos(4, 2, 5)),
				helper.spawn(com.powers.PowersEntities.LIGHT_HERALD, new BlockPos(5, 2, 4)),
				helper.spawn(com.powers.PowersEntities.FIRST_VESSEL, new BlockPos(3, 2, 4)));
		for (int index = 0; index < bosses.size(); index++) {
			var boss = bosses.get(index);
			boss.setNoAi(true);
			boss.setNoGravity(true);
			boss.noPhysics = true;
			double angle = index * Math.TAU / bosses.size();
			boss.setPos(player.getX() + 1.5 * Math.cos(angle), player.getY(),
					player.getZ() + 1.5 * Math.sin(angle));
		}
		if (alignment != null) {
			helper.assertTrue(ArtifactWeaponManager.select(player, alignment, action, -1),
					"Artifact refused selection of " + action);
		}
		java.util.function.BooleanSupplier activate = alignment == null
				? () -> CrystalPowerRegistry.tryActivate(player, crystal)
				: () -> ArtifactWeaponManager.activateSelected(player, alignment) == AbilityActivationService.Result.ACTIVATED;
		var messages = BossControlResponseGameTests.capture(player);
		boolean activated = activate.getAsBoolean();
		helper.assertTrue(activated, "Crystal refused outbound activation for " + realm + ": "
				+ messages.stream().map(packet -> packet.content().getString()).toList());
		helper.runAfterDelay(160, () -> {
			helper.assertTrue(player.level().dimension().identifier().toString().equals(realm),
					"Caster failed to enter " + realm);
			var destination = player.level();
			for (var boss : bosses) {
				var bossId = boss.getUUID();
				helper.assertTrue(destination.getEntity(bossId) instanceof net.minecraft.world.entity.LivingEntity arrived
							&& arrived.isAlive() && MindscapeMobReturnTracker.tracked(arrived)
							&& source.getEntity(bossId) == null,
						boss.getType() + " failed to cross as a tracked cohort member");
			}
			helper.assertTrue(BodyProxyManager.hasSession(player, BodyProxyKind.REALM),
					"Outbound travel left no return body");
			messages.clear();
			boolean returning = activate.getAsBoolean();
			helper.assertTrue(returning, "Crystal refused return activation from " + realm + ": "
					+ messages.stream().map(packet -> packet.content().getString()).toList());
			helper.runAfterDelay(15, () -> {
				helper.assertTrue(player.level() == source, "Caster failed to return to the source world");
				for (var boss : bosses) {
					var bossId = boss.getUUID();
					helper.assertTrue(source.getEntity(bossId) instanceof net.minecraft.world.entity.LivingEntity returned
								&& returned.isAlive() && !MindscapeMobReturnTracker.tracked(returned)
								&& destination.getEntity(bossId) == null,
							"Return did not restore and clear tracking for " + boss.getType());
				}
				helper.assertFalse(BodyProxyManager.hasSession(player, BodyProxyKind.REALM),
						"Return retained the caster's body session");
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 220)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void lightCrystalUsesTheSameAuthenticatedMindscapePipeline(GameTestHelper helper) {
		GameTestClock.atNormalServerRate(helper, 150);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
		var lightRealm = helper.getLevel().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						com.powers.PowersMod.id("light_realm")));
		helper.assertTrue(lightRealm != null, "Light Crystal travel requires the shipped Light Realm");
		com.powers.PowersMod.LOGGER.info("Realm travel fixture: realm=light_realm, destinationInitiallyLoaded={}",
				lightRealm.getChunkSource().getChunkNow(0, 0) != null);

		helper.assertTrue(CrystalPowerRegistry.tryActivate(player, PowersItems.LIGHT_CRYSTAL),
				"Light Crystal activation pipeline rejected its caster");
		helper.runAfterDelay(150, () -> {
			helper.assertTrue(player.level().dimension().identifier().toString()
					.equals("powers:light_realm"), "Light Crystal never entered the Light Realm");
			helper.assertTrue(BodyProxyManager.hasSession(player, BodyProxyKind.REALM),
					"Light Crystal did not leave a vulnerable physical body");
			TestingOverrides.clear(player.getUUID());
			BodyProxyManager.finish(player);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 220)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmCrystalCarriesNearbyMobAndPersistentShadowThenReturnsThem(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		caster.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		PrivateCompanionManager.handleChat(caster, "shadow, reveal yourself");
		PrivateCompanionManager.tickPlayer(caster, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(caster.getUUID()).orElseThrow();
		shadow.setPos(caster.getX() + 0.5, caster.getY(), caster.getZ());
		// This fixture proves transfer and return, not combat. A hostile zombie is
		// legitimately killed by Shadow during the departure warning, cancelling
		// the captured journey before its commit. Keep an ordinary living passenger.
		var mob = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 3));
		mob.setNoAi(true);
		// Realm-arrival tests share one fixed crystal landing zone. Keep this fixture
		// from being collision-pushed by unrelated concurrent mock players so the
		// assertion measures cohort travel, not incidental entity crowding.
		mob.noPhysics = true;
		java.util.UUID mobId = mob.getUUID();
		var netherKey = net.minecraft.world.level.Level.NETHER;
		var nether = helper.getLevel().getServer().getLevel(netherKey);
		helper.assertTrue(nether != null, "GameTest server did not expose the Nether");
		// This live transfer test runs beside up to 49 other tests. Preload only the
		// authored arrival chunk so its assertion measures group travel rather than
		// contention in the separately tested bounded asynchronous loader.
		int arrivalY = com.powers.realm.RealmTerrain.provisionalArrivalY(nether);
		nether.getChunkAt(new BlockPos(8, arrivalY, 8));
		int platformY = 200;
		for (int x = 4; x <= 12; x++) {
			for (int z = 4; z <= 12; z++) {
				nether.setBlockAndUpdate(new BlockPos(x, platformY, z), Blocks.STONE.defaultBlockState());
				nether.setBlockAndUpdate(new BlockPos(x, platformY + 1, z), Blocks.AIR.defaultBlockState());
				nether.setBlockAndUpdate(new BlockPos(x, platformY + 2, z), Blocks.AIR.defaultBlockState());
			}
		}
		var ability = new com.powers.power.crystals.MindscapeCrystalAbility(
				"middleworld", netherKey, com.powers.PowersMod.StormTheme.DARK, 0x301040, 0.6F) { };

		helper.assertTrue(com.powers.magic.runtime.CastScalingContext.withSource(
				com.powers.magic.runtime.CastSource.CRYSTAL,
				() -> ability.activate(caster, com.powers.player.PlayerPowers.get(caster))),
				"Shared crystal journey rejected a valid nearby living cohort");
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(caster.level() == nether
					&& BodyProxyManager.hasSession(caster, BodyProxyKind.REALM),
					"Crystal did not move its caster with a vulnerable body session: caster="
							+ caster.getUUID() + ", level=" + caster.level().dimension().identifier()
							+ ", mindBody=" + com.powers.player.PlayerPowers.get(caster).mindBody()
							+ ", proxies=" + BodyProxyManager.activeProxyCount()
							+ ", travel=" + com.powers.power.travel.TravelChunkLoader.diagnostics()
							+ ", mob=" + mob.getUUID() + ":" + mob.getRemovalReason() + ":" + mob.getHealth()
							+ ", shadow=" + shadow.getUUID() + ":" + shadow.getRemovalReason() + ":" + shadow.getHealth());
			helper.assertTrue(nether.getEntity(mobId) != null
					&& com.powers.power.travel.MindscapeMobReturnTracker.tracked(
							(net.minecraft.world.entity.LivingEntity) nether.getEntity(mobId)),
					"Crystal did not move and track the nearby living mob");
			helper.assertTrue(PrivateCompanionManager.body(caster.getUUID())
					.map(body -> body.level() == nether).orElse(false),
					"Crystal did not rebind persistent Shadow after dimensional travel");
			var returnCohort = com.powers.power.travel.TravelCohort.capture(nether, caster, caster);
			helper.assertTrue(returnCohort.companions().stream()
					.anyMatch(member -> member.entity().getUUID().equals(mobId)),
					"Return cohort could not see the tracked mob near its caster");
			helper.assertTrue(ability.activate(caster, com.powers.player.PlayerPowers.get(caster)),
					"Group crystal return was rejected");
			helper.runAfterDelay(10, () -> helper.succeedWhen(() -> {
				helper.assertTrue(caster.level() == helper.getLevel(),
						"Caster did not return to the vulnerable body");
				var stranded = nether.getEntity(mobId);
				var returnedPassengers = helper.getLevel().getEntitiesOfClass(
						net.minecraft.world.entity.animal.pig.Pig.class,
						net.minecraft.world.phys.AABB.ofSize(
								net.minecraft.world.phys.Vec3.atCenterOf(origin), 16.0, 16.0, 16.0));
				helper.assertTrue(helper.getLevel().getEntity(mobId) != null,
						"Nearby tracked mob did not return to its recorded origin; nether="
								+ (stranded == null ? "missing" : stranded.position())
								+ ", tracked=" + (stranded instanceof net.minecraft.world.entity.LivingEntity living
								&& com.powers.power.travel.MindscapeMobReturnTracker.tracked(living))
								+ ", returned=" + returnedPassengers.stream().map(entity ->
								entity.getUUID() + "@" + entity.position()).toList());
				PrivateCompanionManager.handleChat(caster, "shadow, leave me");
			}));
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmOwnerFirstReturnKeepsDistantMemberExclusiveUntilIndependentReturn(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ServerPlayer member = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		member.setPos(origin.getX() + 1.5, origin.getY(), origin.getZ() + 0.5);
		java.util.UUID sessionId = java.util.UUID.randomUUID();
		helper.assertTrue(MindSessionManager.acquire(sessionId, MindSessionManager.Kind.REALM,
				owner.getUUID(), java.util.List.of(
						new MindSessionManager.Participant(owner.getUUID(), MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(member.getUUID(), MindSessionManager.Role.TARGET)))
				.accepted(), "Realm fixture could not acquire both players atomically");
		helper.assertTrue(BodyProxyManager.startAttached(owner, BodyProxyKind.REALM, sessionId, true),
				"Realm fixture could not create the owner's vulnerable body");
		helper.assertTrue(BodyProxyManager.startAttached(member, BodyProxyKind.REALM, sessionId, false),
				"Realm fixture could not create the member's independent vulnerable body");
		member.setPos(origin.getX() + 20.5, origin.getY(), origin.getZ() + 0.5);

		helper.assertTrue(BodyProxyManager.returnToBody(owner), "Owner-first Realm return failed");
		helper.assertFalse(MindSessionManager.isParticipating(owner.getUUID()),
				"Returned owner retained Realm authority");
		helper.assertTrue(BodyProxyManager.hasSession(member, BodyProxyKind.REALM)
				&& MindSessionManager.activeFor(member.getUUID())
						.filter(session -> session.id().equals(sessionId)).isPresent(),
				"Owner-first return unlocked or recalled the distant detached member");
		MindSessionManager.Admission nested = MindSessionManager.acquire(java.util.UUID.randomUUID(),
				MindSessionManager.Kind.ASTRAL_PROJECTION, member.getUUID(), java.util.List.of(
						new MindSessionManager.Participant(member.getUUID(), MindSessionManager.Role.OWNER)));
		helper.assertTrue(!nested.accepted()
				&& nested.refusal() == MindSessionManager.Refusal.PARTICIPANT_BUSY,
				"Distant Realm member could enter a nested mind session after owner-first return");

		helper.assertTrue(BodyProxyManager.returnToBody(member),
				"Independent Realm member return failed after authority transfer");
		helper.assertTrue(MindSessionManager.session(sessionId).isEmpty()
				&& !MindSessionManager.isParticipating(member.getUUID()),
				"Final Realm member return retained stale session authority");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmNonOwnerDisconnectKeepsSurvivingDetachedOwnerExclusive(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ServerPlayer member = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		member.setPos(origin.getX() + 1.5, origin.getY(), origin.getZ() + 0.5);
		java.util.UUID sessionId = java.util.UUID.randomUUID();
		helper.assertTrue(MindSessionManager.acquire(sessionId, MindSessionManager.Kind.REALM,
				owner.getUUID(), java.util.List.of(
						new MindSessionManager.Participant(owner.getUUID(), MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(member.getUUID(), MindSessionManager.Role.TARGET)))
				.accepted(), "Disconnect fixture could not acquire both Realm players");
		helper.assertTrue(BodyProxyManager.startAttached(owner, BodyProxyKind.REALM, sessionId, true)
				&& BodyProxyManager.startAttached(member, BodyProxyKind.REALM, sessionId, false),
				"Disconnect fixture could not create independent vulnerable bodies");
		java.util.UUID memberId = member.getUUID();
		member.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(
				net.minecraft.network.chat.Component.literal("INT-009 non-owner disconnect")));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(memberId) == null,
					"Disconnected Realm member remained in the player list");
			helper.assertFalse(MindSessionManager.isParticipating(memberId),
					"Disconnected Realm member retained participant authority");
			helper.assertTrue(BodyProxyManager.hasSession(owner, BodyProxyKind.REALM)
					&& MindSessionManager.activeFor(owner.getUUID())
							.filter(session -> session.id().equals(sessionId)).isPresent(),
					"Non-owner disconnect unlocked the surviving detached Realm owner");
			MindSessionManager.Admission nested = MindSessionManager.acquire(java.util.UUID.randomUUID(),
					MindSessionManager.Kind.ASTRAL_PROJECTION, owner.getUUID(), java.util.List.of(
							new MindSessionManager.Participant(owner.getUUID(), MindSessionManager.Role.OWNER)));
			helper.assertTrue(!nested.accepted()
					&& nested.refusal() == MindSessionManager.Refusal.PARTICIPANT_BUSY,
					"Surviving Realm owner entered a nested mind session after member disconnect");
			BodyProxyManager.finish(owner);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmMobAndShadowDeathRemoveOnlyTheirOwnParticipantAuthority(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		var mob = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(2, 1, 3));
		ShadowCompanionEntity shadow = helper.spawn(PowersEntities.SHADOW_COMPANION,
				new BlockPos(3, 1, 2));
		shadow.setRevealed(true);
		java.util.UUID sessionId = java.util.UUID.randomUUID();
		helper.assertTrue(MindSessionManager.acquire(sessionId, MindSessionManager.Kind.REALM,
				owner.getUUID(), java.util.List.of(
						new MindSessionManager.Participant(owner.getUUID(), MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(mob.getUUID(), MindSessionManager.Role.TARGET),
						new MindSessionManager.Participant(shadow.getUUID(), MindSessionManager.Role.SHADOW)))
				.accepted(), "Death fixture could not acquire its Realm cohort");
		helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.track(owner.getUUID(), mob),
				"Death fixture could not record its Realm mob return origin");
		helper.assertTrue(mob.hurtServer(helper.getLevel(), mob.damageSources().generic(), 10_000.0F),
				"Realm mob death fixture rejected fatal damage");
		helper.assertTrue(shadow.hurtServer(helper.getLevel(), shadow.damageSources().generic(), 10_000.0F),
				"Realm Shadow death fixture rejected fatal damage");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(MindSessionManager.isParticipating(mob.getUUID()),
					"Dead Realm mob retained participant authority");
			helper.assertFalse(com.powers.power.travel.MindscapeMobReturnTracker.tracked(mob),
					"Dead Realm mob retained a stale return-origin record");
			helper.assertFalse(MindSessionManager.isParticipating(shadow.getUUID()),
					"Dead Realm Shadow retained participant authority");
			helper.assertTrue(MindSessionManager.activeFor(owner.getUUID())
					.filter(session -> session.id().equals(sessionId)).isPresent(),
					"Mob or Shadow death unlocked the surviving Realm owner");
			MindSessionManager.release(sessionId);
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmShadowIndependentReturnClearsOnlyItsParticipantAuthority(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself");
		PrivateCompanionManager.tickPlayer(owner, 0);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		java.util.UUID shadowId = shadow.getUUID();
		java.util.UUID sessionId = java.util.UUID.randomUUID();
		helper.assertTrue(MindSessionManager.acquire(sessionId, MindSessionManager.Kind.REALM,
				owner.getUUID(), java.util.List.of(
						new MindSessionManager.Participant(owner.getUUID(), MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(shadowId, MindSessionManager.Role.SHADOW)))
				.accepted(), "Shadow-return fixture could not acquire Realm authority");
		var darkRealm = helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.DIMENSION, com.powers.PowersMod.id("dark_realm")));
		helper.assertTrue(darkRealm != null, "GameTest server did not expose the Dark Realm");
		darkRealm.getChunkAt(new BlockPos(8, 80, 8));
		helper.assertTrue(PrivateCompanionManager.travelBody(shadow, darkRealm,
				new net.minecraft.world.phys.Vec3(8.5, 80.0, 8.5)),
				"Shadow could not enter the Realm with its physical body");
		ShadowCompanionEntity travelled = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		helper.assertTrue(MindSessionManager.activeFor(shadowId)
				.filter(session -> session.id().equals(sessionId)).isPresent(),
				"Shadow entry released its Realm participant authority too early");
		helper.assertTrue(PrivateCompanionManager.travelBody(travelled, helper.getLevel(), owner.position()),
				"Shadow could not return independently from the Realm");
		helper.assertFalse(MindSessionManager.isParticipating(shadowId),
				"Returned Shadow retained its Realm participant authority");
		helper.assertTrue(MindSessionManager.activeFor(owner.getUUID())
				.filter(session -> session.id().equals(sessionId)).isPresent(),
				"Independent Shadow return unlocked the surviving Realm owner");
		MindSessionManager.participantUnavailable(owner.getUUID());
		PrivateCompanionManager.forget(owner);
		owner.removeTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().clearContent();
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmTrackedMobReturnClearsOnlyItsParticipantIndex(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var mob = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(2, 1, 3));
		java.util.UUID mobId = mob.getUUID();
		java.util.UUID sessionId = java.util.UUID.randomUUID();
		helper.assertTrue(MindSessionManager.acquire(sessionId, MindSessionManager.Kind.REALM,
				owner.getUUID(), java.util.List.of(
						new MindSessionManager.Participant(owner.getUUID(), MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(mobId, MindSessionManager.Role.TARGET)))
				.accepted(), "Mob-return fixture could not acquire Realm authority");
		helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.track(owner.getUUID(), mob),
				"Mob-return fixture could not record the Realm origin");
		mob.setPos(origin.getX() + 6.5, origin.getY(), origin.getZ() + 0.5);
		helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.returnToOrigin(mob),
				"Tracked Realm mob could not return independently");
		helper.assertFalse(MindSessionManager.isParticipating(mobId),
				"Returned tracked Realm mob retained its participant index");
		helper.assertTrue(MindSessionManager.activeFor(owner.getUUID())
				.filter(session -> session.id().equals(sessionId)).isPresent(),
				"Independent mob return unlocked the surviving Realm owner");
		MindSessionManager.participantUnavailable(owner.getUUID());
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void realmJourneyReturnsOwnedMobAfterItLeavesTheCasterRadius(GameTestHelper helper) {
		ServerPlayer bodyChunkObserver = helper.makeMockServerPlayerInLevel();
		ServerPlayer realmObserver = helper.makeMockServerPlayerInLevel();
		java.util.UUID journeyOwner = java.util.UUID.randomUUID();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		bodyChunkObserver.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var mob = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(2, 1, 3));
		java.util.UUID mobId = mob.getUUID();
		var nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
		helper.assertTrue(nether != null, "GameTest server did not expose the Nether");
		nether.getChunkAt(new BlockPos(8, 80, 8));
		realmObserver.teleport(new net.minecraft.world.level.portal.TeleportTransition(
				nether, new net.minecraft.world.phys.Vec3(8.5, 80.0, 8.5),
				net.minecraft.world.phys.Vec3.ZERO, 0.0F, 0.0F,
				net.minecraft.world.level.portal.TeleportTransition.PLAY_PORTAL_SOUND));
		helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.track(
				journeyOwner, mob), "Mindscape journey did not record its carried mob");
		var moved = mob.teleport(new net.minecraft.world.level.portal.TeleportTransition(
				nether, new net.minecraft.world.phys.Vec3(8.5, 80.0, 8.5),
				net.minecraft.world.phys.Vec3.ZERO, 0.0F, 0.0F,
				net.minecraft.world.level.portal.TeleportTransition.PLAY_PORTAL_SOUND));
		helper.assertTrue(moved instanceof net.minecraft.world.entity.LivingEntity,
				"Fixture could not move its mob into the remote realm");
		helper.runAfterDelay(2, () -> {
			var travelled = (net.minecraft.world.entity.LivingEntity) nether.getEntity(mobId);
			helper.assertTrue(travelled != null, "Remote realm did not register the travelled mob");
			travelled.setPos(14.5, 84.0, 8.5);
			helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.tracked(travelled),
					"Remote body lost its recorded journey ownership");
			helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.trackedCount(
					journeyOwner) == 1, "Journey mob was not indexed under its caster");
			helper.assertTrue(com.powers.power.travel.MindscapeMobReturnTracker.returnOwned(
					helper.getLevel().getServer(), journeyOwner) == 1,
					"Caster return did not recall its separated journey mob");
			helper.succeedWhen(() -> {
				var returned = helper.getLevel().getEntity(mobId);
				helper.assertTrue(returned != null && returned.position().distanceToSqr(
						net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.offset(0, 0, 1))) < 1.0,
						"Separated journey mob did not return to its recorded body location");
			});
		});
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void graveRecallConsumesEnergyOnlyWhenADeathRecordExists(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.recordDeath(player);
		powers.setSelectedSpell("book_grimoire_blight", 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_blight").getDefaultInstance());
		int before = powers.energy();
		SpellCastingManager.use(player, "book_grimoire_blight");
		helper.assertTrue(powers.energy() == before - 10,
				"Grave Recall failed to commit its exact energy payment");
		helper.succeed();
	}

	// Real GameTest realms activate the crystal fixtures in the default batch;
	// keep this independent ritual in its own lifecycle environment.
	@GameTest(environment = "powers:ritual_isolated", maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void auguryCompletesAsAnUnrankedPracticalRitual(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockPos loadedFront = origin.offset(4, 0, 0);
		BlockPos unloadedUnknown = origin.offset(80, 0, 0);
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		helper.getLevel().setBlockAndUpdate(loadedFront, PowersBlocks.DARKNESS.defaultBlockState());
		helper.assertFalse(com.powers.util.LoadedChunks.contains(helper.getLevel(), unloadedUnknown),
				"Augury fixture unexpectedly began with its unknown horizon loaded");
		var report = com.powers.spell.AuguryReport.create(helper.getLevel(), origin);
		helper.assertTrue(report.darknessFront().observedFronts() >= 1
				&& report.darknessFront().nearestDistanceBlocks() <= 4,
				"Augury did not read the authoritative loaded Darkness frontier");
		helper.assertTrue(report.ritualWindow().safety()
				== com.powers.spell.AuguryReport.RitualSafety.LOADED_FRONT_HAZARD,
				"Augury did not turn an observed loaded frontier into a ritual hazard");
		helper.assertTrue(report.uncertainty()
				== com.powers.spell.AuguryReport.Uncertainty.LOADED_CHUNKS_ONLY,
				"Augury did not label its loaded-only uncertainty");
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_celestial").getDefaultInstance());
		var powers = com.powers.player.PlayerPowers.get(player);
		// Mock players reuse one identity across earlier environments, so this fixture owns a clean ritual baseline.
		SpellCastingManager.clear(player);
		com.powers.network.PacketRateLimiter.forgetPlayer(player.getUUID());
		powers.clearCooldown("spell:augury");
		powers.forceRestoreEnergy();
		powers.setSelectedSpell("book_grimoire_celestial", 1);
		SpellCastingManager.use(player, "book_grimoire_celestial");
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()),
				"Augury did not enter its authored ritual channel");
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(powers.cooldownReadyAt("spell:augury") > player.level().getGameTime(),
					"Augury did not commit its authored payment and cooldown");
			helper.assertFalse(SpellCastingManager.isChanneling(player.getUUID()),
					"Augury remained stuck in its ritual channel");
			helper.assertFalse(com.powers.util.LoadedChunks.contains(helper.getLevel(), unloadedUnknown),
					"Augury force-loaded its unknown forecast horizon");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void bloodReadingAcceptsAPlayerCompatibleTestActor(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_blight").getDefaultInstance());
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.setSelectedSpell("book_grimoire_blight", 0);
		SpellCastingManager.use(player, "book_grimoire_blight");
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(powers.cooldownReadyAt("spell:blood_reading") > player.level().getGameTime(),
					"Blood Reading rejected the player-compatible target before payment");
			helper.assertFalse(SpellCastingManager.isChanneling(player.getUUID()),
					"Blood Reading remained stuck in its ritual channel");
			helper.succeed();
		});
	}

	@GameTest
	public void bloodReadingTracksRecentDamageAndHealing(GameTestHelper helper) {
		PowerTestActor target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 2));
		com.powers.spell.BloodReadingHistoryService.clear(target);
		MagicShieldManager.global().remove(target.getUUID());
		helper.assertTrue(target.hurtServer(helper.getLevel(), target.damageSources().generic(), 4.0F),
				"Live damage was not applied to the Blood Reading target");
		target.heal(2.0F);
		long now = helper.getLevel().getServer().getTickCount();
		MagicShieldManager.global().raise(target.getUUID(), 20.0F, now + 100L);
		target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				com.powers.PowersEffects.AMETHYST_POISONING, 100, 0));
		com.powers.spell.BloodReadingReport report =
				com.powers.spell.BloodReadingReport.create(target);
		helper.assertTrue(report.recentDamage() >= 4.0F && report.recentHealing() >= 2.0F,
				"Blood Reading did not summarize live damage and healing");
		helper.assertTrue(report.trend() == com.powers.spell.BloodReadingTrendRules.Trend.WORSENING,
				"Blood Reading did not classify the live history as worsening");
		helper.assertTrue(report.forceState() == com.powers.spell.BloodReadingReport.ForceState.INTACT,
				"Blood Reading leaked or missed the categorical force ward state");
		helper.assertTrue(report.amethystState()
				== com.powers.spell.BloodReadingReport.AmethystState.DAMPENED,
				"Blood Reading missed the categorical amethyst response");
		com.powers.spell.BloodReadingHistoryService.clear(target);
		MagicShieldManager.global().remove(target.getUUID());
		helper.succeed();
	}

	@GameTest(maxTicks = 90)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void purificationUsesEnergyOnlyExclusiveModesAndPreservesAmethyst(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.addEffect(com.powers.PowerStatusEffects.hidden(MobEffects.WITHER, 200, 0, false, true));
		player.addEffect(com.powers.PowerStatusEffects.hidden(com.powers.PowersEffects.EXHAUSTION,
				200, 0, false, true));
		player.addEffect(com.powers.PowerStatusEffects.hidden(
				com.powers.PowersEffects.AMETHYST_POISONING, 200, 0, false, true));
		com.powers.spell.PurificationService.apply(player, 8.0, 0,
				com.powers.spell.PurificationModeRules.Mode.CLEANSE);
		helper.assertFalse(player.hasEffect(MobEffects.WITHER),
				"Cleanse mode left an ordinary harmful effect");
		helper.assertTrue(player.hasEffect(com.powers.PowersEffects.EXHAUSTION),
				"Cleanse mode crossed into corruption relief");
		helper.assertTrue(player.hasEffect(com.powers.PowersEffects.AMETHYST_POISONING),
				"Cleanse mode removed Amethyst Poisoning");
		com.powers.spell.PurificationService.apply(player, 8.0, 0,
				com.powers.spell.PurificationModeRules.Mode.CORRUPTION_RELIEF);
		helper.assertFalse(player.hasEffect(com.powers.PowersEffects.EXHAUSTION),
				"Corruption-relief mode left Exhaustion behind");
		helper.assertTrue(player.hasEffect(com.powers.PowersEffects.AMETHYST_POISONING),
				"Corruption relief removed Amethyst Poisoning");
		player.removeEffect(com.powers.PowersEffects.AMETHYST_POISONING);

		var powers = com.powers.player.PlayerPowers.get(player);
		powers.clearCooldown("spell:purification_circle");
		powers.forceRestoreEnergy();
		powers.setDimensionalAnchor(player.level().dimension().identifier().toString(),
				player.level().getGameTime() + 200L);
		player.addEffect(com.powers.PowerStatusEffects.hidden(MobEffects.WITHER, 200, 0, false, true));
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_wild").getDefaultInstance());
		player.setItemInHand(InteractionHand.OFF_HAND, com.powers.ImportedPackItems.item(
				"imported_magic_essence_blood_dust").getDefaultInstance());
		int energyBefore = powers.energy();
		int catalystBefore = player.getItemInHand(InteractionHand.OFF_HAND).getCount();
		powers.setSelectedSpell("book_grimoire_wild", 0);
		SpellCastingManager.use(player, "book_grimoire_wild");
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()),
				"Purification variant did not enter its authored channel");
		helper.assertTrue(powers.energy() == energyBefore - 20,
				"Purification variant did not reserve its fixed energy payment");
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).getCount() == catalystBefore,
					"Purification consumed its mode catalyst");
			helper.assertTrue(powers.dimensionalAnchor() == null,
					"Corruption-relief ritual did not clear the dimensional anchor");
			helper.assertTrue(player.hasEffect(MobEffects.WITHER),
					"Corruption-relief ritual crossed into ordinary cleanse mode");
			player.removeEffect(MobEffects.WITHER);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void verdantTendingGrowsHydratesAndExtinguishesWithinItsWorkBudget(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(3, 1, 3));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_wild").getDefaultInstance());
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.setSelectedSpell("book_grimoire_wild", 1);
		BlockPos crop = new BlockPos(4, 1, 3);
		BlockPos farmland = new BlockPos(3, 0, 4);
		BlockPos irrigation = new BlockPos(4, 0, 4);
		BlockPos fire = new BlockPos(2, 1, 3);
		helper.setBlock(crop, Blocks.WHEAT);
		helper.setBlock(farmland, Blocks.FARMLAND);
		helper.setBlock(irrigation, Blocks.WATER);
		// Farmland deterministically reverts to dirt when its fixture leaves a
		// solid template block directly above it. Preserve the live-world
		// precondition that Verdant Tending is intended to hydrate.
		helper.setBlock(farmland.above(), Blocks.AIR);
		helper.setBlock(fire, Blocks.FIRE);
		SpellCastingManager.use(player, "book_grimoire_wild");
		helper.runAfterDelay(50, () -> {
			helper.assertFalse(helper.getBlockState(crop).equals(Blocks.WHEAT.defaultBlockState()),
					"Verdant Tending did not grow a valid crop");
			helper.assertTrue(helper.getBlockState(farmland).is(Blocks.FARMLAND),
					"Verdant Tending fixture farmland reverted before verification");
			helper.assertTrue(helper.getBlockState(farmland).getValue(
					net.minecraft.world.level.block.FarmlandBlock.MOISTURE)
					== net.minecraft.world.level.block.FarmlandBlock.MAX_MOISTURE,
					"Verdant Tending did not hydrate farmland");
			helper.assertBlockPresent(Blocks.AIR, fire);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void cartographersStarFindsARegisteredBiomeWithoutKeepingChunksLoaded(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		var biomeRegistry = level.registryAccess().lookupOrThrow(
				net.minecraft.core.registries.Registries.BIOME);
		var biomeId = biomeRegistry.getKey(level.getBiome(origin).value());
		CartographerQuery query = new CartographerQuery(CartographerQuery.Kind.BIOME, biomeId.toString());
		helper.assertTrue(CartographerSearch.isKnownTarget(level, query),
				"Cartographer's Star rejected a biome present in the live registry");
		var result = CartographerSearch.find(level, origin, query);
		helper.assertTrue(result.isPresent() && result.get().registryId().equals(biomeId.toString()),
				"Cartographer's Star did not resolve the live biome around its caster");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void empyreanJewelOverridesEveryConsentCategoryOncePerTick(GameTestHelper helper) {
		ConsentOverrideRuntime.clear();
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		target.setPos(origin.getX() + 2.5, origin.getY(), origin.getZ() + 0.5);
		ItemStack jewel = com.powers.ImportedPackItems.item(
				"imported_artifact_emperyeanjewel").getDefaultInstance();
		jewel.setCount(2);
		caster.setItemInHand(InteractionHand.MAIN_HAND, jewel);
		var powers = com.powers.player.PlayerPowers.get(caster);
		int before = powers.energy();
		for (ConsentKind kind : ConsentKind.values()) {
			helper.assertTrue(ConsentOverrideRuntime.authorize(caster, target, kind, false),
					"Empyrean Jewel did not override " + kind);
		}
		helper.assertTrue(powers.energy() == before - ConsentKind.values().length * 40,
				"Empyrean Jewel did not charge exactly once per consent category");
		int after = powers.energy();
		helper.assertTrue(ConsentOverrideRuntime.authorize(caster, target, ConsentKind.TELEPORT, false)
				&& powers.energy() == after,
				"A duplicate same-tick consent check charged the Empyrean Jewel twice");

		ServerPlayer secondTarget = helper.makeMockServerPlayerInLevel();
		secondTarget.setPos(origin.getX() + 3.5, origin.getY(), origin.getZ() + 0.5);
		powers.emptyEnergy();
		helper.assertFalse(ConsentOverrideRuntime.authorize(
				caster, secondTarget, ConsentKind.TELEPORT, false),
				"Empyrean Jewel bypassed its energy surcharge without testing mode");
		TestingOverrides.setEnergyDisabled(caster.getUUID(), true);
		helper.assertTrue(ConsentOverrideRuntime.authorize(
				caster, secondTarget, ConsentKind.TELEPORT, false),
				"Testing-mode energy bypass did not cover consent-override testing");
		TestingOverrides.clear(caster.getUUID());
		ConsentOverrideRuntime.clear();
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void soulstoneReservoirPaysEnergyShortfallsAtomically(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.emptyEnergy();
		ItemStack stone = com.powers.ImportedPackItems.item(
				"imported_artifact_soulstone_small").getDefaultInstance();
		com.powers.item.ArtifactEnergyReservoir.setStored(stone, 100);
		player.getInventory().setItem(0, stone);

		helper.assertTrue(powers.consumeEnergy(60),
				"A carried Soulstone did not pay the player's energy shortfall");
		helper.assertTrue(com.powers.item.ArtifactEnergyReservoir.stored(stone) == 40,
				"Soulstone payment did not debit the exact stored energy");
		helper.assertFalse(powers.consumeEnergy(41),
				"An underfilled Soulstone paid more energy than it contained");
		helper.assertTrue(com.powers.item.ArtifactEnergyReservoir.stored(stone) == 40,
				"A refused Soulstone payment partially mutated its balance");
		helper.succeed();
	}

	@GameTest
	public void foodAffinityLoadsTagFirstVanillaClassifications(GameTestHelper helper) {
		helper.assertTrue(com.powers.player.FoodAffinity.of(new ItemStack(Items.BREAD))
				== com.powers.player.FoodAffinity.NEUTRAL,
				"Neutral food tag was not authoritative");
		helper.assertTrue(com.powers.player.FoodAffinity.of(new ItemStack(Items.ROTTEN_FLESH))
				== com.powers.player.FoodAffinity.ABNORMAL,
				"Abnormal food tag was not authoritative");
		helper.assertTrue(com.powers.player.FoodAffinity.of(new ItemStack(Items.COOKED_BEEF))
				== com.powers.player.FoodAffinity.NORMAL,
				"Normal food tag was not authoritative");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void runestonesStackAndConsumeOnlyAfterSuccessfulRestoreWithoutCooldown(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		var powers = com.powers.player.PlayerPowers.get(player);
		ItemStack runes = new ItemStack(com.powers.ImportedPackItems.item(
				"imported_artifact_runestone_inert"), 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, runes);
		helper.assertTrue(runes.getMaxStackSize() == 64,
				"Registered runestone did not stack to 64");
		powers.forceRestoreEnergy();
		runes.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(runes.getCount() == 2,
				"A full energy reserve consumed a runestone");

		powers.emptyEnergy();
		runes.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(powers.energy() == 40 && runes.getCount() == 1,
				"Successful restoration did not consume exactly one runestone");
		helper.assertFalse(player.getCooldowns().isOnCooldown(runes),
				"Successful restoration created a runestone cooldown");
		runes.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(powers.energy() == 80 && runes.isEmpty(),
				"Immediate second restoration was cooldown-bound or failed to consume");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void ritualDaggerConvertsHealthDirectlyIntoExistingEnergy(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setHealth(20.0F);
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.emptyEnergy();
		ItemStack dagger = com.powers.ImportedPackItems.item(
				"imported_artifact_ritualdagger").getDefaultInstance();
		player.setItemInHand(InteractionHand.MAIN_HAND, dagger);

		dagger.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		helper.assertTrue(player.getHealth() == 16.0F,
				"Ritual Dagger did not take its fixed health payment");
		helper.assertTrue(powers.energy() == 80,
				"Ritual Dagger created essence instead of refilling the existing energy pool");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void shadowExplainsTheExactLatestServerRecordedMagicFailure(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		com.powers.knowledge.MagicAttemptReporter.failure(player, "fireball",
				com.powers.knowledge.MagicFailureReason.INSUFFICIENT_ENERGY,
				java.util.Map.of("required", 40L, "available", 12L));

		var answer = com.powers.knowledge.KnowledgeService.answer(player,
				"Shadow, why did my fireball fail?");
		helper.assertTrue(answer.answer().equals(
					"Your Fireball failed because it required 40 energy, but only 12 was available. "
							+ "I recorded this at server tick " + player.level().getGameTime() + "."),
				"Shadow replaced an authoritative magic diagnosis with a guess");
		helper.assertTrue(answer.confidence() == 1.0,
				"Authoritative magic diagnostics were not marked fully verified");
		helper.succeed();
	}

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void graveRecallStateCapturesThePlayersCurrentDimensionAndPosition(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos point = helper.absolutePos(new BlockPos(5, 3, 7));
		player.setPos(point.getX() + 0.75, point.getY(), point.getZ() + 0.25);
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.recordDeath(player);
		var death = powers.lastDeath();
		helper.assertTrue(death != null, "No last-death record was stored");
		helper.assertTrue(death.dimension().equals(helper.getLevel().dimension().identifier().toString()),
				"Last-death dimension was not server authoritative");
		helper.assertTrue(death.x() == point.getX() && death.y() == point.getY()
				&& death.z() == point.getZ(), "Last-death coordinates were not floored block coordinates");
		helper.succeed();
	}

	@GameTest
	public void operatorTestingTreeExposesCoverageAndArenaControls(GameTestHelper helper) {
		var powers = helper.getLevel().getServer().getCommands().getDispatcher()
				.getRoot().getChild("powers");
		helper.assertTrue(powers != null, "The /powers root command was not registered");
		var testing = powers.getChild("testing");
		var vfx = testing == null ? null : testing.getChild("vfx");
		helper.assertTrue(testing != null && testing.getChild("coverage") != null
				&& testing.getChild("arena") != null && vfx != null
				&& vfx.getChild("locator-entity") != null
				&& vfx.getChild("advancement-dark") != null,
				"The manual acceptance coverage/arena/VFX commands were not registered");
		helper.succeed();
	}

	@GameTest(maxTicks = 140)
	@SuppressWarnings("removal")
	public void artifactTimeShiftCarriesEveryBoss(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var level = helper.getLevel();
		var origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
		caster.snapTo(origin, 0, 0);
		caster.setNoGravity(true);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		var data = PlayerPowers.get(caster);
		data.setDarknessLevel(caster, 1);
		data.forceRestoreEnergy();
		int energy = data.energy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(3, 1, 4)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(5, 1, 4)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(4, 1, 5)));
		var offsets = java.util.List.of(new Vec3(-1.5, 0, 0), new Vec3(1.5, 0, 0), new Vec3(0, 0, 1.5));
		for (int i = 0; i < bosses.size(); i++) {
			var boss = bosses.get(i);
			boss.setNoAi(true);
			boss.setNoGravity(true);
			boss.noPhysics = true;
			boss.setPos(origin.add(offsets.get(i)));
		}
		var alignment = ArtifactAlignment.DARKNESS;
		var key = "innate/time_shift";
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, key, -1), "Time Shift selection failed");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment) == AbilityActivationService.Result.REQUIRES_INPUT,
				"Artifact Time Shift did not request coordinates");
		var action = ArtifactWeaponManager.action(alignment, key);
		var destination = origin.add(4, 5, 4);
		helper.assertTrue(AbilityActivationService.activateArtifactTeleport(caster, caster, action.ability(),
				level.dimension(), destination.x, destination.y, destination.z,
				ArtifactWeaponManager.cooldown(caster, alignment, action)) == AbilityActivationService.Result.ACTIVATED
				&& data.energy() < energy, "Artifact coordinate activation failed");
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(caster.distanceToSqr(destination) < 0.01, "Time Shift changed the selected coordinates");
			for (int i = 0; i < bosses.size(); i++) {
				var boss = bosses.get(i);
				helper.assertTrue(boss.isAlive() && boss.distanceToSqr(destination.add(offsets.get(i))) < 0.01,
						"Time Shift did not preserve the cohort offset for " + boss.getType());
			}
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 120)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void timeShiftCanMoveANamedTestActor(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PowerTestActor actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(3, 1, 2));
		actor.setTestingUsername("WarpTarget");
		actor.setNoAi(true);
		// This case deliberately places the actor inside a solid destination to prove
		// exact-coordinate travel bypasses safety adjustment. Disable post-teleport
		// collision resolution so the later assertion measures the selected landing
		// coordinate rather than the entity being ejected from that solid block.
		actor.noPhysics = true;
		var nearbyMob = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(2, 1, 3));
		nearbyMob.setNoAi(true);
		Vec3 mobOffset = nearbyMob.position().subtract(caster.position());
		var data = com.powers.player.PlayerPowers.get(caster);
		data.setSlots(caster, java.util.List.of(
				"powers:time_shift", "powers:flight", "powers:forcefield"));
		BlockPos destination = helper.absolutePos(new BlockPos(7, 1, 7));
		// Exact coordinate travel deliberately accepts a solid destination.
		helper.setBlock(new BlockPos(7, 1, 7), Blocks.STONE);
		TeleportAbility ability = new TeleportAbility();
		helper.assertTrue(com.powers.power.AbilityActivationService.activateTeleport(
				caster, actor, ability, helper.getLevel().dimension(),
				destination.getX(), destination.getY(), destination.getZ(), false)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Time Shift rejected a player-compatible test actor");
		helper.runAfterDelay(75, () -> {
			helper.assertTrue(actor.position().distanceToSqr(
					new Vec3(destination.getX(), destination.getY(), destination.getZ())) < 0.01,
					"Time Shift changed or rejected the exact selected destination");
			helper.assertTrue(nearbyMob.position().distanceToSqr(
					new Vec3(destination.getX(), destination.getY(), destination.getZ()).add(mobOffset)) < 0.01,
					"Time Shift did not carry a nearby living mob with the caster");
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal")
	public void orangeCrystalEchoUsesItsOwnersPlayerIdentity(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		com.powers.entity.EchoClone echo = helper.spawn(PowersEntities.ECHO_CLONE,
				new BlockPos(2, 1, 2));
		echo.configure(owner, 1_200);
		DarknessCreature hostile = helper.spawn(PowersEntities.DARKNESS_CREATURE,
				new BlockPos(4, 1, 2));
		helper.assertTrue(echo.ownerProfile().id().equals(owner.getUUID()),
				"Orange echo did not synchronize the owner's skin profile");
		helper.assertTrue(echo.getMainHandItem().isEmpty() && echo.getOffhandItem().isEmpty(),
				"Orange echo unexpectedly copied equipment");
		helper.assertFalse(echo.canAttack(owner), "Orange echo could attack its owner");
		helper.assertTrue(echo.canAttack(hostile), "Orange echo could not fight a hostile mob");
		helper.succeed();
	}

	@GameTest
	public void droppedAmethystRestoresBothMiniportalCharges(GameTestHelper helper) {
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
		ItemStack portalStack = com.powers.ImportedPackItems.item(
				"imported_device_miniportal").getDefaultInstance();
		portalStack.set(PowersDataComponents.MINIPORTAL_CHARGES, 0);
		var portal = new net.minecraft.world.entity.item.ItemEntity(level,
				position.x, position.y, position.z, portalStack);
		var shard = new net.minecraft.world.entity.item.ItemEntity(level,
				position.x, position.y, position.z, new ItemStack(Items.AMETHYST_SHARD));
		level.addFreshEntity(portal);
		level.addFreshEntity(shard);
		portal.tickCount = Math.floorMod(portal.getId(), 10);
		com.powers.item.MiniportalRechargeManager.tick(portal);
		helper.assertTrue(portal.getItem().get(PowersDataComponents.MINIPORTAL_CHARGES) == 2,
				"Amethyst did not restore both Miniportal charges");
		helper.assertTrue(shard.isRemoved() || shard.getItem().isEmpty(),
				"Miniportal recharge did not consume exactly one shard");
		helper.succeed();
	}

	@GameTest
	public void everyAdvertisedMagicRegistryResolvesInsideTheLiveServer(GameTestHelper helper) {
		helper.assertTrue(PowerRegistry.getAll().size() == 23
				&& PowerRegistry.getAll().stream().allMatch(power -> power.ability() != null),
				"The live innate-power registry was incomplete");
		helper.assertTrue(CrystalPowerRegistry.allAbilities().size() == 11
				&& CrystalPowerRegistry.allAbilities().values().stream().noneMatch(java.util.Objects::isNull),
				"The live crystal-action registry was incomplete");
		long spells = SpellRegistry.defaults().definitions().stream()
				.mapToLong(book -> book.spells().size()).sum();
		helper.assertTrue(spells == 12, "The live grimoire registry did not contain all 12 active spells");
		for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
			helper.assertTrue(ArtifactWeaponManager.actions(alignment).size()
					== ArtifactActionCatalogue.forAlignment(alignment).size()
					&& ArtifactWeaponManager.actions(alignment).stream()
							.allMatch(action -> action.ability() != null),
					"An artifact route failed to resolve for " + alignment);
		}
		helper.assertTrue(MagicRuntime.catalogue().definitions().size() == 65
				&& MagicRuntime.global().interactionCount() == 2_145,
				"The exhaustive live magic-collision kernel was incomplete");
		helper.succeed();
	}

	@GameTest
	public void allTwoHundredFiftyThreeInnateRankProfilesResolveLive(GameTestHelper helper) {
		var actions = com.powers.magic.MagicActionCatalogue.defaults();
		int scenarios = 0;
		for (var power : PowerRegistry.getAll()) {
			String id = power.id().getPath();
			var action = actions.definition(new com.powers.magic.MagicActionId(id));
			helper.assertTrue(action != null, "No canonical action for innate " + id);
			for (int rank = 0; rank <= 10; rank++) {
				var profile = com.powers.progression.InnatePowerLevels.forPower(id, rank);
				helper.assertTrue(profile.damageMultiplier() >= 1.0
						&& profile.rangeMultiplier() >= 1.0
						&& profile.durationMultiplier() >= 1.0
						&& profile.capacityMultiplier() >= 1.0
						&& profile.destructionTier() >= 0 && profile.destructionTier() <= 10,
						"Invalid live rank profile " + id + "@" + rank);
				scenarios++;
			}
		}
		helper.assertTrue(scenarios == 253, "Expected 253 live innate rank scenarios, got " + scenarios);
		helper.succeed();
	}

}
