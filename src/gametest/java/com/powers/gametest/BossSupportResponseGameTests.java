package com.powers.gametest;

import com.powers.PowersBlocks;
import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.PowerRegistry;
import com.powers.power.artifact.ArtifactChainManager;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.power.artifact.ArtifactFieldManager;
import com.powers.power.artifact.ArtifactGateManager;
import com.powers.power.artifact.ArtifactGroundWorkQueue;
import com.powers.power.artifact.ArtifactGuardianSummons;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.state.MagicShieldManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Support casts must benefit the combatant without accidentally healing the opposing boss. */
public final class BossSupportResponseGameTests {
	@GameTest
	@SuppressWarnings("removal")
	public void covenantChainPullsEachBoss(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.snapTo(origin.getX() + 0.5, 100, origin.getZ() + 0.5, 0, 0);
		var bosses = spawnNearbyBosses(helper);
		for (var boss : bosses) boss.setPos(caster.getX(), 100, caster.getZ() - 5);
		try {
			for (var boss : bosses) {
				PlayerPowers.get(caster).clearCooldowns();
				boss.setPos(caster.getX(), caster.getEyeY() - boss.getBbHeight() / 2, caster.getZ() + 6);
				var oldMotion = boss.getDeltaMovement();
				activateDominion(helper, caster, ArtifactAlignment.LIGHT, 4, "dominion/covenant_chain");
				ArtifactChainManager.tick(helper.getLevel().getServer());
				helper.assertTrue(boss.getDeltaMovement().z < oldMotion.z, "Covenant Chain did not pull " + boss.getType());
				boss.setPos(caster.getX(), 100, caster.getZ() - 5);
			}
			helper.succeed();
		} finally {
			ArtifactChainManager.forget(caster.getUUID());
		}
	}

	@GameTest
	@SuppressWarnings("removal")
	public void hostOfHeavenBuildsSetupNearEveryBoss(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var bosses = spawnNearbyBosses(helper);
		var health = bosses.stream().map(Mob::getHealth).toList();
		try {
			activateDominion(helper, caster, ArtifactAlignment.LIGHT, 10, "dominion/host_heaven");
			helper.assertTrue(ArtifactGuardianSummons.ownedGuardianCount(caster.getUUID(), true) == 2,
					"Host of Heaven did not summon two elite sentinels");
			for (var boss : bosses) {
				helper.assertTrue(ArtifactFieldManager.contains(helper.getLevel(), boss.position(), ArtifactAlignment.LIGHT),
						"Host of Heaven field did not cover " + boss.getType());
			}
			helper.assertTrue(health.equals(bosses.stream().map(Mob::getHealth).toList()),
					"Host of Heaven setup caused immediate boss damage");
			helper.succeed();
		} finally {
			ArtifactFieldManager.forget(caster.getUUID());
			ArtifactGroundWorkQueue.forget(caster.getUUID());
			ArtifactGuardianSummons.revokeOwner(helper.getLevel().getServer(), caster.getUUID(), ArtifactAlignment.LIGHT);
		}
	}

	@GameTest(maxTicks = 45)
	@SuppressWarnings("removal")
	public void nightfallDominionPressuresNonDarknessBosses(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, 100, origin.getZ() + 0.5);
		caster.setNoGravity(true);
		var bosses = spawnNearbyBosses(helper);
		for (var boss : bosses) {
			boss.setNoGravity(true);
			boss.setPos(boss.getX(), 100, boss.getZ());
		}
		float darkHealth = bosses.get(0).getHealth();
		float lightHealth = bosses.get(1).getHealth();
		var vessel = (com.powers.entity.FirstVessel) bosses.get(2);
		float vesselHealth = vessel.effectiveHealth();
		activateDominion(helper, caster, ArtifactAlignment.DARKNESS, 10, "unique/nightfall_dominion");
		helper.runAfterDelay(22, () -> {
			try {
				helper.assertTrue(caster.hasEffect(MobEffects.STRENGTH), "Nightfall did not buff the caster");
				helper.assertTrue(bosses.get(0).getHealth() == darkHealth, "Nightfall damaged Dark Herald");
				helper.assertTrue(bosses.get(1).getHealth() < lightHealth && vessel.effectiveHealth() < vesselHealth,
						"Nightfall did not damage both non-Darkness bosses");
				helper.succeed();
			} finally {
				caster.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			}
		});
	}

	@GameTest(padding = 64)
	@SuppressWarnings("removal")
	public void heavenGateCarriesCasterAndNearbyBossCohort(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
		var bosses = spawnNearbyBosses(helper);
		for (int i = 0; i < bosses.size(); i++) {
			bosses.get(i).setPos(caster.getX() + (i - 1) * 1.5, caster.getY(), caster.getZ() + 1);
		}
		var bossPositions = bosses.stream().map(Mob::position).toList();
		var entrance = caster.position();
		try {
			activateDominion(helper, caster, ArtifactAlignment.LIGHT, 6, "dominion/heaven_gate");
			ArtifactGateManager.tick(helper.getLevel().getServer());
			helper.assertTrue(caster.position().distanceToSqr(entrance) >= 16, "Heaven Gate did not move its caster");
			var displacement = caster.position().subtract(entrance);
			for (int i = 0; i < bosses.size(); i++) {
				var boss = bosses.get(i);
				helper.assertTrue(boss.position().distanceToSqr(bossPositions.get(i).add(displacement)) < 1.0E-6,
						"Heaven Gate did not preserve the cohort offset for " + boss.getType()
								+ ": before=" + bossPositions.get(i) + ", after=" + boss.position()
								+ ", displacement=" + displacement);
			}
			helper.succeed();
		} finally {
			ArtifactGateManager.forget(caster.getUUID());
		}
	}

	@GameTest(maxTicks = 45)
	@SuppressWarnings("removal")
	public void solarFirmamentDamagesDarkHeraldWithoutDamagingOtherBosses(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.snapTo(origin.getX() + 0.5, 100, origin.getZ() + 0.5, 0, 0);
		caster.setNoGravity(true);
		var bosses = spawnNearbyBosses(helper);
		for (int i = 0; i < bosses.size(); i++) {
			var boss = bosses.get(i);
			boss.setNoGravity(true);
			boss.setPos(caster.getX() + i - 1, 100, caster.getZ() + 12);
		}
		float darkHealth = bosses.get(0).getHealth();
		var vessel = (com.powers.entity.FirstVessel) bosses.get(2);
		bosses.get(1).setHealth(bosses.get(1).getHealth() - 12);
		vessel.hurtServer(helper.getLevel(), vessel.damageSources().generic(), 32);
		float vesselHealth = vessel.effectiveHealth();
		float lightHealth = bosses.get(1).getHealth();
		helper.assertTrue(vesselHealth < vessel.effectiveMaximumHealth(), "First Vessel fixture was not injured");
		activateDominion(helper, caster, ArtifactAlignment.LIGHT, 8, "dominion/solar_firmament");
		helper.assertTrue(ArtifactFieldManager.contains(helper.getLevel(), bosses.get(0).position(), ArtifactAlignment.LIGHT),
				"Solar Firmament did not cover the boss arena");
		helper.runAfterDelay(22, () -> {
			try {
				helper.assertTrue(bosses.get(0).getHealth() < darkHealth, "Solar Firmament did not damage Dark Herald");
				helper.assertTrue(bosses.get(1).getHealth() > lightHealth && vessel.effectiveHealth() > vesselHealth,
						"Solar Firmament did not heal both injured non-Darkness bosses");
				helper.succeed();
			} finally {
				ArtifactFieldManager.forget(caster.getUUID());
			}
		});
	}

	private static java.util.List<Mob> spawnNearbyBosses(GameTestHelper helper) {
		var bosses = java.util.List.<Mob>of(
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(2, 2, 3)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(3, 2, 3)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(4, 2, 3)));
		bosses.forEach(boss -> boss.setNoAi(true));
		return bosses;
	}

	private static void activateDominion(GameTestHelper helper, net.minecraft.server.level.ServerPlayer caster,
			ArtifactAlignment alignment, int rank, String action) {
		var data = PlayerPowers.get(caster);
		if (alignment == ArtifactAlignment.DARKNESS) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, rank);
		} else {
			data.setSkillLevel(caster, rank);
		}
		data.forceRestoreEnergy();
		int energy = data.energy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(
				alignment == ArtifactAlignment.DARKNESS ? "lycanbane" : "heavenly_partisan").getDefaultInstance());
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, action, -1), "Selection failed: " + action);
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
				== AbilityActivationService.Result.ACTIVATED && data.energy() < energy, "Activation did not commit: " + action);
	}

	@GameTest
	public void consecrateGroundConvertsArenaNearEveryBoss(GameTestHelper helper) {
		verifyGroundRite(helper, ArtifactAlignment.LIGHT, "heavenly_partisan", "dominion/consecrate_ground");
	}

	@GameTest
	public void blightGroundConvertsArenaNearEveryBoss(GameTestHelper helper) {
		verifyGroundRite(helper, ArtifactAlignment.DARKNESS, "lycanbane", "unique/blight_ground");
	}

	@SuppressWarnings("removal")
	private static void verifyGroundRite(GameTestHelper helper, ArtifactAlignment alignment, String weapon, String action) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		var floor = new BlockPos(origin.getX(), 99, origin.getZ());
		var level = helper.getLevel();
		var original = level.getBlockState(floor);
		level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
		caster.setPos(floor.getX() + 0.5, 100, floor.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		if (alignment == ArtifactAlignment.DARKNESS) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 2);
		} else {
			data.setSkillLevel(caster, 2);
		}
		data.forceRestoreEnergy();
		int energy = data.energy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(weapon).getDefaultInstance());
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(2, 2, 3)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(3, 2, 3)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(4, 2, 3)));
		for (var boss : bosses) boss.setPos(boss.getX(), 100, boss.getZ());
		var health = bosses.stream().map(Mob::getHealth).toList();
		try {
			helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, action, -1), "Ground rite selection failed");
			helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
					== AbilityActivationService.Result.ACTIVATED && data.energy() < energy, "Ground rite did not commit");
			ArtifactGroundWorkQueue.tick(level.getServer());
			var expected = alignment == ArtifactAlignment.LIGHT ? PowersBlocks.PURE_LIGHT : PowersBlocks.DARKNESS;
			helper.assertTrue(level.getBlockState(floor).is(expected), "Ground rite did not convert the arena floor");
			helper.assertTrue(bosses.stream().map(Mob::getHealth).toList().equals(health),
					"Ground conversion caused immediate damage to a nearby boss");
			helper.succeed();
		} finally {
			ArtifactGroundWorkQueue.forget(caster.getUUID());
			level.setBlockAndUpdate(floor, original);
		}
	}

	@GameTest
	public void callRadiantSummonsOwnedGuardiansNearEveryBoss(GameTestHelper helper) {
		verifyGuardianCall(helper, ArtifactAlignment.LIGHT, "heavenly_partisan", "dominion/call_radiant");
	}

	@GameTest
	public void callHollowedSummonsOwnedGuardiansNearEveryBoss(GameTestHelper helper) {
		verifyGuardianCall(helper, ArtifactAlignment.DARKNESS, "lycanbane", "unique/call_hollowed");
	}

	@SuppressWarnings("removal")
	private static void verifyGuardianCall(GameTestHelper helper, ArtifactAlignment alignment, String weapon, String action) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		if (alignment == ArtifactAlignment.DARKNESS) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 1);
		} else {
			data.setSkillLevel(caster, 1);
		}
		data.forceRestoreEnergy();
		int energy = data.energy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(weapon).getDefaultInstance());
		var dark = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(2, 2, 3));
		var light = helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(3, 2, 3));
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(4, 2, 3));
		float darkHealth = dark.getHealth();
		float lightHealth = light.getHealth();
		float vesselHealth = vessel.effectiveHealth();
		try {
			helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, action, -1), "Guardian call selection failed");
			helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
					== AbilityActivationService.Result.ACTIVATED && data.energy() < energy, "Guardian call did not commit");
			var summonedType = alignment == ArtifactAlignment.LIGHT
					? PowersEntities.RADIANT_SENTINEL : PowersEntities.DARKNESS_CREATURE;
			int owned = ArtifactGuardianSummons.ownedGuardianCount(caster.getUUID(), false);
			helper.assertTrue(owned == 4 && helper.getLevel().getEntities(summonedType,
					caster.getBoundingBox().inflate(8), guardian -> caster.getUUID().equals(guardian.guardianOwner())).size() == owned,
					"Guardian call did not create four owned, correctly aligned entities: " + owned);
			helper.assertTrue(dark.getHealth() == darkHealth && light.getHealth() == lightHealth
					&& vessel.effectiveHealth() == vesselHealth, "Guardian arrival damaged a nearby boss");
			helper.succeed();
		} finally {
			ArtifactGuardianSummons.revokeOwner(helper.getLevel().getServer(), caster.getUUID(), alignment);
		}
	}

	@GameTest
	public void secondDawnSurvivesOneLethalHitFromEachBoss(GameTestHelper helper) {
		var level = helper.getLevel();
		var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "second-dawn");
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
		var caster = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, caster, cookie);
		caster.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		caster.setGameMode(GameType.SURVIVAL);
		caster.setInvulnerable(false);
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 9);
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("heavenly_partisan").getDefaultInstance());
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(3, 2, 3)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(4, 2, 3)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(5, 2, 3)));
		try {
			for (var boss : bosses) {
				data.forceRestoreEnergy();
				int energy = data.energy();
				caster.setHealth(1);
				caster.invulnerableTime = 0;
				caster.removeEffect(MobEffects.RESISTANCE);
				caster.removeEffect(MobEffects.ABSORPTION);
				caster.setAbsorptionAmount(0);
				data.clearCooldowns();
				helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.LIGHT, "dominion/second_dawn", -1),
						"Second Dawn selection failed");
				helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.LIGHT)
						== AbilityActivationService.Result.ACTIVATED && data.energy() < energy, "Second Dawn did not commit");
				var hit = caster.damageSources().mobAttack(boss);
				boolean accepted = caster.hurtServer(helper.getLevel(), hit, 200);
				helper.assertTrue(caster.isAlive() && caster.getHealth() > 1 && caster.getAbsorptionAmount() >= 20,
						"Second Dawn did not restore the caster after a lethal hit from " + boss.getType()
								+ ": accepted=" + accepted + ", health=" + caster.getHealth()
								+ ", absorption=" + caster.getAbsorptionAmount() + ", alive=" + caster.isAlive());
				helper.assertFalse(ArtifactDeathWardManager.preventDeath(caster, hit),
						"Second Dawn ward survived consumption");
			}
			helper.succeed();
		} finally {
			ArtifactDeathWardManager.forget(caster.getUUID());
			level.getServer().getPlayerList().remove(caster);
			channel.finishAndReleaseAll();
		}
	}

	@GameTest
	@SuppressWarnings("removal")
	public void daybreakWaveDamagesDarkHeraldAndCleansesOtherBosses(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 3, 2));
		caster.snapTo(origin.getX() + 0.5, 100, origin.getZ() + 0.5, 0, 0);
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 5);
		data.forceRestoreEnergy();
		int energy = data.energy();
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("heavenly_partisan").getDefaultInstance());
		var dark = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(4, 3, 7));
		var light = helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(3, 3, 7));
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(5, 3, 7));
		dark.setPos(caster.getX(), 100, caster.getZ() + 5);
		light.setPos(caster.getX() - 2, 100, caster.getZ() + 5);
		vessel.setPos(caster.getX(), 100, caster.getZ() + 5);
		vessel.setPos(vessel.getX(), vessel.getY() + caster.getEyeY() - vessel.getEyeY(), vessel.getZ());
		for (var target : java.util.List.of(light, vessel)) {
			// The artifact reaction independently applies Slowness after the wave.
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
			helper.assertTrue(target.hasEffect(MobEffects.POISON), "Boss fixture could not apply Poison");
		}
		float darkHealth = dark.getHealth();
		float lightHealth = light.getHealth();
		double vesselHealth = vessel.effectiveHealth();
		helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.LIGHT, "dominion/daybreak_wave", -1),
				"Daybreak Wave selection failed");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.LIGHT)
				== AbilityActivationService.Result.ACTIVATED && data.energy() < energy, "Daybreak Wave did not commit");
		helper.assertTrue(dark.getHealth() < darkHealth, "Daybreak Wave did not damage the Dark Herald");
		helper.assertTrue(light.getHealth() >= lightHealth && vessel.effectiveHealth() >= vesselHealth
				&& !light.hasEffect(MobEffects.POISON) && !vessel.hasEffect(MobEffects.POISON),
				"Daybreak Wave response: light=" + light.getHealth() + "/" + lightHealth
						+ ", vessel=" + vessel.effectiveHealth() + "/" + vesselHealth
						+ ", poison=" + light.hasEffect(MobEffects.POISON) + "/" + vessel.hasEffect(MobEffects.POISON));
		helper.assertTrue(vessel.hasEffect(MobEffects.SLOWNESS), "First Vessel did not react to the Light artifact");
		helper.succeed();
	}

	@GameTest
	public void darknessArtifactPlantHealingSupportsPlayersNearBosses(GameTestHelper helper) {
		verifyArtifactPlantHealing(helper, ArtifactAlignment.DARKNESS, "lycanbane");
	}

	@GameTest
	public void lightArtifactPlantHealingSupportsPlayersNearBosses(GameTestHelper helper) {
		verifyArtifactPlantHealing(helper, ArtifactAlignment.LIGHT, "heavenly_partisan");
	}

	@SuppressWarnings("removal")
	private static void verifyArtifactPlantHealing(GameTestHelper helper, ArtifactAlignment alignment, String weapon) {
		var caster = helper.makeMockServerPlayerInLevel();
		var ally = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		ally.setPos(caster.getX() + 1, caster.getY(), caster.getZ());
		caster.setShiftKeyDown(true);
		caster.setPose(net.minecraft.world.entity.Pose.CROUCHING);
		caster.setHealth(4);
		ally.setHealth(5);
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 1);
		data.setDarknessLevel(caster, 1);
		if (alignment == ArtifactAlignment.DARKNESS) caster.addTag(SkillSystem.DARKNESS_TAG);
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(weapon).getDefaultInstance());
		data.forceRestoreEnergy();
		int energy = data.energy();
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(3, 2, 4)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(4, 2, 3)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(4, 2, 5)));
		for (var boss : bosses) {
			boss.setNoAi(true);
			boss.setHealth(boss.getMaxHealth() * 0.5F);
		}
		var initialHealth = bosses.stream().map(Mob::getHealth).toList();
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "innate/plant_healing_acceleration", -1),
				"Artifact refused Plant Healing selection");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
				== AbilityActivationService.Result.ACTIVATED && data.energy() < energy,
				"Plant Healing did not activate and spend energy");
		helper.assertTrue(caster.getHealth() > 4 && ally.getHealth() > 5,
				"Plant Healing did not heal both nearby players");
		helper.assertTrue(bosses.stream().map(Mob::getHealth).toList().equals(initialHealth),
				"Plant Healing changed the bosses' health");
		helper.succeed();
	}

	@GameTest
	public void forcefieldRoutesBlockLightHeraldDamage(GameTestHelper helper) {
		forcefieldRoutes(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest
	public void forcefieldRoutesBlockDarkHeraldDamage(GameTestHelper helper) {
		forcefieldRoutes(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest
	public void forcefieldRoutesBlockFirstVesselDamage(GameTestHelper helper) {
		forcefieldRoutes(helper, PowersEntities.FIRST_VESSEL);
	}

	private static void forcefieldRoutes(GameTestHelper helper, EntityType<? extends Mob> type) {
		var level = helper.getLevel();
		var difficulty = level.getDifficulty();
		var server = level.getServer();
		server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
		try {
			verifyForcefieldRoutes(helper, type);
		} finally {
			server.setDifficulty(difficulty, true);
		}
	}

	private static void verifyForcefieldRoutes(GameTestHelper helper, EntityType<? extends Mob> type) {
		var level = helper.getLevel();
		var boss = helper.spawn(type, new BlockPos(4, 2, 4));
		boss.setNoAi(true);
		float bossHealth = boss.getHealth();
		for (String route : java.util.List.of("innate", "darkness", "light")) {
			var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ward-" + route);
			var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
			var caster = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
			var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
			var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
			level.getServer().getPlayerList().placeNewPlayer(connection, caster, cookie);
			caster.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
			caster.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			caster.setPos(boss.getX() + 1.5, boss.getY(), boss.getZ());
			var data = PlayerPowers.get(caster);
			var ability = PowerRegistry.get("forcefield").ability();
			ArtifactAlignment alignment = route.equals("innate") ? null
					: route.equals("darkness") ? ArtifactAlignment.DARKNESS : ArtifactAlignment.LIGHT;
			if (alignment == null) {
				data.setSlots(caster, java.util.List.of("powers:forcefield", "powers:flight", "powers:double_health"));
			} else {
				data.setSkillLevel(caster, 1);
				data.setDarknessLevel(caster, 1);
				if (alignment == ArtifactAlignment.DARKNESS) caster.addTag(SkillSystem.DARKNESS_TAG);
				caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(
						alignment == ArtifactAlignment.DARKNESS ? "lycanbane" : "heavenly_partisan").getDefaultInstance());
				helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "innate/forcefield", -1),
						"Forcefield selection failed: " + route);
			}
			data.forceRestoreEnergy();
			int energy = data.energy();
			var result = alignment == null ? AbilityActivationService.activate(caster, ability, "powers:forcefield")
					: ArtifactWeaponManager.activateSelected(caster, alignment);
			long tick = level.getServer().getTickCount();
			var shields = MagicShieldManager.global();
			helper.assertTrue(result == AbilityActivationService.Result.ACTIVATED && data.energy() < energy
					&& shields.active(caster.getUUID(), tick), "Forcefield was not raised and paid for: " + route);
			helper.assertFalse(shields.active(boss.getUUID(), tick), "Forcefield shielded the opposing boss: " + route);
			var attack = caster.damageSources().mobAttack(boss);
			helper.assertFalse(caster.isInvulnerableTo(level, attack), "Survival fixture is invulnerable: " + route);
			float health = caster.getHealth();
			helper.assertFalse(caster.hurtServer(level, attack, 1_000.0F),
					"Forcefield did not block the breaking boss hit: " + route);
			helper.assertTrue(caster.getHealth() == health && !shields.active(caster.getUUID(), tick)
					&& boss.getHealth() == bossHealth,
					"Breaking hit did not consume the ward cleanly: " + route);
			helper.assertTrue(caster.hurtServer(level, attack, 4.0F) && caster.getHealth() < health,
					"Breaking the ward left the caster protected: " + route);
			level.getServer().getPlayerList().remove(caster);
			channel.finishAndReleaseAll();
		}
		helper.succeed();
	}

	@GameTest
	public void doubleHealthRoutesSupportCasterAgainstLightHerald(GameTestHelper helper) {
		doubleHealthRoutes(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest
	public void doubleHealthRoutesSupportCasterAgainstDarkHerald(GameTestHelper helper) {
		doubleHealthRoutes(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest
	public void doubleHealthRoutesSupportCasterAgainstFirstVessel(GameTestHelper helper) {
		doubleHealthRoutes(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void doubleHealthRoutes(GameTestHelper helper, EntityType<? extends Mob> type) {
		for (String route : java.util.List.of("innate", "darkness", "light")) {
			var caster = helper.makeMockServerPlayerInLevel();
			var position = helper.absolutePos(new BlockPos(2, 2, 2));
			caster.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
			var data = PlayerPowers.get(caster);
			var ability = PowerRegistry.get("double_health").ability();
			ArtifactAlignment alignment = route.equals("innate") ? null
					: route.equals("darkness") ? ArtifactAlignment.DARKNESS : ArtifactAlignment.LIGHT;
			if (alignment == null) {
				data.setSlots(caster, java.util.List.of("powers:double_health", "powers:flight", "powers:forcefield"));
			} else {
				data.setSkillLevel(caster, 1);
				data.setDarknessLevel(caster, 1);
				if (alignment == ArtifactAlignment.DARKNESS) caster.addTag(SkillSystem.DARKNESS_TAG);
				caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(
						alignment == ArtifactAlignment.DARKNESS ? "lycanbane" : "heavenly_partisan").getDefaultInstance());
				helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "innate/double_health", -1),
						"Artifact could not select Double Health: " + route);
			}
			data.forceRestoreEnergy();
			int energy = data.energy();
			float originalMaximum = caster.getMaxHealth();
			caster.setHealth(5.0F);
			var boss = helper.spawn(type, new BlockPos(2, 2, 5));
			boss.setNoAi(true);
			float bossMaximum = boss.getMaxHealth();
			float bossHealth = boss.getHealth();
			var result = alignment == null ? AbilityActivationService.activate(caster, ability, "powers:double_health")
					: ArtifactWeaponManager.activateSelected(caster, alignment);
			helper.assertTrue(result == AbilityActivationService.Result.ACTIVATED, "Double Health activation failed: " + route);
			helper.assertTrue(caster.getMaxHealth() >= originalMaximum * 2
					&& caster.getHealth() == 5.0F + caster.getMaxHealth() - originalMaximum,
					"Double Health did not grant the authored capacity and bounded heal: " + route);
			helper.assertTrue(boss.getMaxHealth() == bossMaximum && boss.getHealth() == bossHealth,
					"Double Health changed the opposing boss: " + route);
			helper.assertTrue(data.energy() < energy, "Double Health bypassed payment: " + route);
			float ratio = caster.getHealth() / caster.getMaxHealth();
			int paidEnergy = data.energy();
			var stopped = alignment == null ? AbilityActivationService.activate(caster, ability, "powers:double_health")
					: ArtifactWeaponManager.activateSelected(caster, alignment);
			helper.assertTrue(stopped == AbilityActivationService.Result.ACTIVATED
					&& !data.isToggleActive(alignment == null ? "powers:double_health"
							: "artifact/" + alignment.serializedName() + "/innate/double_health")
					&& caster.getMaxHealth() == originalMaximum
					&& Math.abs(caster.getHealth() - originalMaximum * ratio) < 0.0001F
					&& data.energy() == paidEnergy,
					"Double Health teardown leaked capacity, healed, or changed payment: " + route);
			boss.discard();
		}
		helper.succeed();
	}

	@GameTest
	public void darkArtifactLifeBloomAgainstLightHerald(GameTestHelper helper) {
		artifactLifeBloom(helper, PowersEntities.LIGHT_HERALD, ArtifactAlignment.DARKNESS, 100);
	}

	@GameTest
	public void darkArtifactLifeBloomAgainstDarkHerald(GameTestHelper helper) {
		artifactLifeBloom(helper, PowersEntities.DARK_HERALD, ArtifactAlignment.DARKNESS, 0);
	}

	@GameTest
	public void darkArtifactLifeBloomAgainstFirstVessel(GameTestHelper helper) {
		artifactLifeBloom(helper, PowersEntities.FIRST_VESSEL, ArtifactAlignment.DARKNESS, 160);
	}

	@GameTest
	public void lightArtifactLifeBloomAgainstLightHerald(GameTestHelper helper) {
		artifactLifeBloom(helper, PowersEntities.LIGHT_HERALD, ArtifactAlignment.LIGHT, 0);
	}

	@GameTest
	public void lightArtifactLifeBloomAgainstDarkHerald(GameTestHelper helper) {
		artifactLifeBloom(helper, PowersEntities.DARK_HERALD, ArtifactAlignment.LIGHT, 100);
	}

	@GameTest
	public void lightArtifactLifeBloomAgainstFirstVessel(GameTestHelper helper) {
		artifactLifeBloom(helper, PowersEntities.FIRST_VESSEL, ArtifactAlignment.LIGHT, 160);
	}

	@SuppressWarnings("removal")
	private static void artifactLifeBloom(GameTestHelper helper, EntityType<? extends Mob> type,
			ArtifactAlignment alignment, int reactionTicks) {
		var caster = helper.makeMockServerPlayerInLevel();
		var position = helper.absolutePos(new BlockPos(2, 2, 2));
		caster.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
		if (alignment == ArtifactAlignment.DARKNESS) caster.addTag(SkillSystem.DARKNESS_TAG);
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(
				alignment == ArtifactAlignment.DARKNESS ? "lycanbane" : "heavenly_partisan").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 9);
		data.setDarknessLevel(caster, 9);
		data.forceRestoreEnergy();
		caster.setHealth(5.0F);
		caster.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		var boss = helper.spawn(type, new BlockPos(2, 2, 5));
		boss.setNoAi(true);
		boss.setHealth(boss.getMaxHealth() / 2);
		float bossHealth = boss.getHealth();
		int energy = data.energy();
		helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "crystal/life_bloom", -1),
				"Authorized held artifact could not select Life Bloom");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment)
				== AbilityActivationService.Result.ACTIVATED, "Selected artifact support failed");
		helper.assertTrue(caster.getHealth() == caster.getMaxHealth()
				&& !caster.hasEffect(MobEffects.POISON) && caster.hasEffect(MobEffects.REGENERATION),
				"Artifact Life Bloom did not heal, cleanse, and sustain its caster");
		helper.assertTrue(boss.getHealth() == bossHealth && !boss.hasEffect(MobEffects.REGENERATION),
				"Artifact Life Bloom healed the opposing boss");
		var action = ArtifactWeaponManager.action(alignment, "crystal/life_bloom");
		helper.assertTrue(data.energy() < energy
				&& ActivationCooldowns.remainingTicks(caster, action.ability())
				== 2400,
				"Artifact support bypassed payment or its rank-specific cooldown");
		for (var effect : java.util.List.of(MobEffects.GLOWING, MobEffects.WEAKNESS, MobEffects.SLOWNESS)) {
			var instance = boss.getEffect(effect);
			helper.assertTrue(reactionTicks == 0 ? instance == null
					: instance != null && instance.getDuration() == reactionTicks,
					"Artifact alignment produced the wrong boss reaction: " + effect);
		}
		helper.succeed();
	}

	@GameTest
	public void lifeBloomSupportsCasterAgainstLightHerald(GameTestHelper helper) {
		lifeBloom(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest
	public void lifeBloomSupportsCasterAgainstDarkHerald(GameTestHelper helper) {
		lifeBloom(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest
	public void lifeBloomSupportsCasterAgainstFirstVessel(GameTestHelper helper) {
		lifeBloom(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void lifeBloom(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var position = helper.absolutePos(new BlockPos(2, 2, 2));
		caster.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
		caster.setHealth(5.0F);
		caster.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		var boss = helper.spawn(type, new BlockPos(2, 2, 5));
		boss.setNoAi(true);
		boss.setHealth(boss.getMaxHealth() / 2);
		float bossHealth = boss.getHealth();
		var data = PlayerPowers.get(caster);
		data.forceRestoreEnergy();
		int energy = data.energy();
		helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.GREEN_CRYSTAL),
				"Actual Green Crystal activation failed");
		helper.assertTrue(caster.getHealth() == caster.getMaxHealth()
				&& !caster.hasEffect(MobEffects.POISON) && caster.hasEffect(MobEffects.REGENERATION),
				"Life Bloom did not heal, cleanse, and sustain the caster");
		helper.assertTrue(boss.getHealth() == bossHealth && !boss.hasEffect(MobEffects.REGENERATION),
				"Life Bloom accidentally healed the opposing boss");
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster,
				CrystalPowerRegistry.get(PowersItems.GREEN_CRYSTAL)) > 0,
				"Successful support bypassed the crystal's payment or cooldown");
		helper.succeed();
	}
}
