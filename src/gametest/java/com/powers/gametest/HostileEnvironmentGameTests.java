package com.powers.gametest;

import com.powers.api.v1.PhysicalPresence;
import com.powers.api.v1.PowersApiRuntime;
import com.powers.api.v1.PresenceKind;
import com.powers.example.ExamplePowersExtension;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.abilities.IceManipulationAbility;
import com.powers.power.abilities.FlightAbility;
import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.SizeMorphAbility;
import com.powers.power.abilities.TeleportAbility;
import com.powers.power.artifact.AlignedArtifactAbility;
import com.powers.power.artifact.ArtifactGuardianSummons;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.power.crystals.InfernoAbility;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelCohort;
import com.powers.mind.BodyProxyManager;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.realm.RealmPortalRules;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;

/** Real, isolated hostile-world fixtures for the shared production boundaries. */
public final class HostileEnvironmentGameTests {
	private static final ResourceKey<Level> SYNTHETIC_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			Identifier.fromNamespaceAndPath("qa010_hostile", "synthetic"));

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 30)
	@SuppressWarnings("removal")
	public void claimDenialPrecedesDestructionTravelObservationSummonsAndExternalApi(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(8, 2, 8));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.setSkillLevel(player, 10);
		data.setSlots(player, java.util.List.of("powers:ice_manipulation", "powers:time_shift",
				"powers:dreamwalking"));
		for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(x, -1, z), Blocks.STONE.defaultBlockState());
		}
		for (int x = -1; x <= 1; x++) for (int y = 0; y <= 3; y++) for (int z = -1; z <= 7; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		BlockPos water = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2.5)));
		helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(water.above(), Blocks.WATER.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(water.below(), Blocks.WATER.defaultBlockState());
		var host = EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(host != null, "Claim fixture could not create its observation host");
		host.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 6.5);
		host.setNoAi(true);
		helper.getLevel().addFreshEntity(host);
		int energyBefore = data.energy();
		int guardiansBefore = ArtifactGuardianSummons.indexedGuardianCount();
		helper.assertTrue(PowerProtectionAdapters.register("qa010_claim_deny", 10_000, query -> false),
				"Claim fixture did not install exactly once");
		try {
			helper.assertFalse(com.powers.protection.PowerProtection.mayAffectBlock(
					player, helper.getLevel(), water),
					"External claim adapter did not deny its real target block");
			helper.assertTrue(AbilityActivationService.activate(player, new IceManipulationAbility(),
					"powers:ice_manipulation", false) == AbilityActivationService.Result.FAILED,
					"Claim denial charged a destructive Ice cast");
			helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.WATER),
					"Claim denial allowed Ice to mutate water");
			helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
					new TeleportAbility(), helper.getLevel().dimension(),
					feet.getX() + 5.5, feet.getY(), feet.getZ() + 5.5, false)
					== AbilityActivationService.Result.FAILED,
					"Claim denial accepted travel work");
			helper.assertTrue(AbilityActivationService.activate(player, new DreamwalkingAbility(),
					"powers:dreamwalking", false) == AbilityActivationService.Result.FAILED,
					"Claim denial started observation state");
			var summon = ArtifactActionCatalogue.find(ArtifactAlignment.DARKNESS, "unique/call_hollowed");
			helper.assertTrue(AbilityActivationService.activateWithCooldown(player,
					new AlignedArtifactAbility(summon), "powers:call_hollowed", null,
					com.powers.magic.runtime.CastSource.ARTIFACT) == AbilityActivationService.Result.FAILED,
					"Claim denial spawned an artifact guardian");
			helper.assertTrue(ArtifactGuardianSummons.indexedGuardianCount() == guardiansBefore,
					"Claim denial changed summon ownership state");
			var context = PowersApiRuntime.global().api().castContext(player, ExamplePowersExtension.ACTION_ID);
			boolean apiDenied = false;
			try {
				PowersApiRuntime.global().api().registerPresence(context, new PhysicalPresence(
						helper.getLevel(), player.getX(), player.getY() + 1.0, player.getZ(), 1.0,
						helper.getLevel().getServer().getTickCount() + 5, PresenceKind.FIELD));
			} catch (IllegalStateException expected) {
				apiDenied = true;
			}
			helper.assertTrue(apiDenied, "Claim denial accepted an external API presence");
			helper.assertTrue(data.energy() == energyBefore,
					"Claim denial changed authoritative energy");
			helper.assertTrue(ActivationCooldowns.remainingTicks(player, new IceManipulationAbility()) == 0,
					"Claim denial started a destructive cooldown");
		} finally {
			PowerProtectionAdapters.unregister("qa010_claim_deny");
			ArtifactGuardianSummons.revokeOwner(helper.getLevel().getServer(), player.getUUID(),
					ArtifactAlignment.DARKNESS);
			if (!host.isRemoved()) host.discard();
			player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 20)
	@SuppressWarnings("removal")
	public void lowCeilingRejectsEnlargementWithoutClippingOrStaleScale(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(2, 2, 2));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		helper.getLevel().setBlockAndUpdate(feet.above(2), Blocks.BEDROCK.defaultBlockState());
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.setSkillLevel(player, 10);
		data.setSlots(player, java.util.List.of("powers:size_shift"));
		SizeMorphAbility ability = new SizeMorphAbility();
		data.setSizeMorphOption(7); // literal 2x body, taller than the two-block cavity
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		try {
			helper.assertTrue(AbilityActivationService.activate(player, ability,
					"powers:size_shift") == AbilityActivationService.Result.FAILED,
					"Twofold enlargement clipped into the real solid ceiling");
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F,
					"Rejected enlargement retained a stale scale modifier");
			helper.assertFalse(data.isToggleActive("powers:size_shift"),
					"Rejected enlargement retained toggle ownership");
			helper.assertTrue(player.getBoundingBox().maxY <= feet.getY() + 2.0,
					"Rejected enlargement left the authoritative hitbox inside the ceiling");

			data.setSizeMorphOption(com.powers.power.abilities.SizeMorphRules.normalOption());
			helper.assertTrue(AbilityActivationService.activate(player, ability, "powers:size_shift")
					== AbilityActivationService.Result.ACTIVATED,
					"Low-ceiling fixture could not enter the safe 1x toggle state");
			helper.assertFalse(ability.selectOption(player, data, 7),
					"Active size selection accepted an unsafe 1x-to-2x enlargement");
			helper.assertTrue(data.getSizeMorphOption()
					== com.powers.power.abilities.SizeMorphRules.normalOption(),
					"Rejected active selection persisted the unsafe option");
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F
					&& data.isToggleActive("powers:size_shift"),
					"Rejected active selection contradicted toggle and modifier ownership");
			ability.tickActive(player, data);
			helper.assertTrue(Math.abs(player.getScale() - 1.0F) < 0.001F
					&& data.getSizeMorphOption()
					== com.powers.power.abilities.SizeMorphRules.normalOption(),
					"Active tick silently re-applied the rejected unsafe scale");
		} finally {
			if (data.isToggleActive("powers:size_shift")) {
				AbilityActivationService.activate(player, ability, "powers:size_shift", true);
			}
			TestingOverrides.clear(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 20)
	@SuppressWarnings("removal")
	public void worldBorderRejectsExactCoordinatesAtomicallyWithoutRewriting(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
		player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.setSlots(player, java.util.List.of("powers:time_shift"));
		WorldBorder border = helper.getLevel().getWorldBorder();
		double oldX = border.getCenterX();
		double oldZ = border.getCenterZ();
		double oldSize = border.getSize();
		int energy = data.energy();
		Vec3 position = player.position();
		int tickets = TravelChunkLoader.pendingRequestCount();
		try {
			border.setCenter(position.x, position.z);
			border.setSize(12.0);
			double requestedX = position.x + 12.0;
			helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
					new TeleportAbility(), helper.getLevel().dimension(), requestedX,
					position.y, position.z, true) == AbilityActivationService.Result.FAILED,
					"World-border rejection accepted an impossible exact coordinate");
			helper.assertTrue(player.position().equals(position),
					"World-border rejection silently rewrote the entered coordinate");
			helper.assertTrue(data.energy() == energy, "World-border rejection charged energy");
			helper.assertTrue(TravelChunkLoader.pendingRequestCount() == tickets,
					"World-border rejection leaked a travel ticket");
		} finally {
			border.setCenter(oldX, oldZ);
			border.setSize(oldSize);
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 130)
	@SuppressWarnings("removal")
	public void voidCoordinateRemainsExactAndFatalBodyDamageClearsMindAndTravelState(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
		player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.setSlots(player, java.util.List.of("powers:time_shift", "powers:astral_projection"));
		TestingOverrides.setAll(player.getUUID(), true);
		int tickets = TravelChunkLoader.pendingRequestCount();
		double voidY = helper.getLevel().getMinY() + 2.0;
		Vec3 requested = new Vec3(player.getX() + 8.0, voidY, player.getZ() + 8.0);
		helper.getLevel().getChunkAt(BlockPos.containing(requested));
		player.setNoGravity(true);
		helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
				new TeleportAbility(), helper.getLevel().dimension(), requested.x, requested.y,
				requested.z, true) == AbilityActivationService.Result.ACTIVATED,
				"Exact void coordinate was safety-corrected before Minecraft travel");
		helper.runAfterDelay(52, () -> {
			player.setNoGravity(false);
			helper.assertTrue(player.position().distanceToSqr(requested) < 0.01,
					"Direct travel rewrote the entered void coordinate at commit");
		});
		helper.runAfterDelay(75, () -> {
			helper.assertTrue(AbilityActivationService.activate(player, new AstralProjectionAbility(),
					"powers:astral_projection", true) == AbilityActivationService.Result.ACTIVATED,
					"Void fixture could not start the real vulnerable-body lifecycle");
			var body = helper.getLevel().getEntities(
					net.minecraft.world.level.entity.EntityTypeTest.forClass(
							net.minecraft.world.entity.LivingEntity.class),
					BodyProxyManager::isProxy).stream().findFirst().orElse(null);
			helper.assertTrue(body != null, "Astral lifecycle did not create its real body proxy");
			body.hurtServer(helper.getLevel(), body.damageSources().fellOutOfWorld(), 10_000.0F);
		});
		helper.runAfterDelay(100, () -> {
			try {
				helper.assertFalse(AstralProjectionAbility.isActive(player.getUUID()),
						"Fatal void-body damage left astral state active");
				helper.assertFalse(BodyProxyManager.hasSession(player, com.powers.mind.BodyProxyKind.ASTRAL),
						"Fatal void-body damage left a body proxy session");
				helper.assertTrue(TravelChunkLoader.pendingRequestCount() == tickets,
						"Fatal void-body damage left a travel ticket");
			} finally {
				AstralProjectionAbility.clear(helper.getLevel().getServer(), player.getUUID());
				TestingOverrides.clear(player.getUUID());
				player.setNoGravity(false);
				player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
			}
			helper.succeed();
		});
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 30)
	@SuppressWarnings("removal")
	public void waterAndLavaUseRealThermalBlocksAndSubmergedMovementStaysFinite(GameTestHelper helper) {
		ServerPlayer iceCaster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
		iceCaster.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		iceCaster.setYRot(0.0F);
		iceCaster.setXRot(0.0F);
		PlayerPowers.PlayerPowersData iceData = PlayerPowers.get(iceCaster);
		iceData.setSlots(iceCaster, java.util.List.of("powers:ice_manipulation"));
		TestingOverrides.setAll(iceCaster.getUUID(), true);
		BlockPos water = BlockPos.containing(iceCaster.getEyePosition().add(iceCaster.getLookAngle().scale(2.5)));
		helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(water.above(), Blocks.STONE.defaultBlockState());
		try {
			helper.assertTrue(AbilityActivationService.activate(iceCaster, new IceManipulationAbility(),
					"powers:ice_manipulation", true) == AbilityActivationService.Result.ACTIVATED,
					"Ice production entrypoint rejected real water");
			helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.ICE),
					"Ice production entrypoint did not freeze real water exactly once");
			BlockPos lava = BlockPos.containing(iceCaster.getEyePosition().add(iceCaster.getLookAngle().scale(2.5)));
			helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
			helper.assertTrue(AbilityActivationService.activate(iceCaster, new IceManipulationAbility(),
					"powers:ice_manipulation", true) == AbilityActivationService.Result.ACTIVATED,
					"Ice production entrypoint rejected real lava");
			helper.assertTrue(helper.getLevel().getBlockState(lava).is(Blocks.OBSIDIAN),
					"Ice production entrypoint did not cool real lava exactly once");

			helper.getLevel().setBlockAndUpdate(iceCaster.blockPosition(), Blocks.WATER.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(iceCaster.blockPosition().above(), Blocks.WATER.defaultBlockState());
			FlightAbility flight = new FlightAbility();
			helper.assertTrue(AbilityActivationService.activate(iceCaster, flight, "powers:flight", true)
					== AbilityActivationService.Result.ACTIVATED,
					"Submerged movement toggle failed to enter its production owner");
			flight.tickActive(iceCaster, iceData);
			helper.assertTrue(Double.isFinite(iceCaster.getDeltaMovement().x)
					&& Double.isFinite(iceCaster.getDeltaMovement().y)
					&& Double.isFinite(iceCaster.getDeltaMovement().z),
					"Submerged movement produced non-finite velocity");
			AbilityActivationService.activate(iceCaster, flight, "powers:flight", true);

			ServerPlayer infernoCaster = helper.makeMockServerPlayerInLevel();
			infernoCaster.snapTo(origin.getX() + 8.5, origin.getY(), origin.getZ() + 0.5);
			helper.getLevel().setBlockAndUpdate(infernoCaster.blockPosition(), Blocks.WATER.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(infernoCaster.blockPosition().above(), Blocks.WATER.defaultBlockState());
			var target = EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			helper.assertTrue(target != null, "Fluid fixture could not create its Inferno target");
			target.snapTo(infernoCaster.getX() + 1.0, infernoCaster.getY(), infernoCaster.getZ());
			target.setNoAi(true);
			helper.getLevel().addFreshEntity(target);
			float health = target.getHealth();
			helper.assertTrue(com.powers.magic.runtime.CastScalingContext.withSource(
					com.powers.magic.runtime.CastSource.CRYSTAL,
					() -> new InfernoAbility().activate(infernoCaster, PlayerPowers.get(infernoCaster))),
					"Inferno rejected a submerged caster");
			InfernoAbility.tickAll(helper.getLevel().getServer());
			helper.assertTrue(target.getHealth() < health, "Inferno did not resolve bounded entity damage in water");
		} finally {
			InfernoAbility.clearAll();
			TestingOverrides.clear(iceCaster.getUUID());
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 20)
	@SuppressWarnings("removal")
	public void mountedNestedPassengersTravelWithoutDuplicationOrCrossLevelReferences(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
		player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var horse = EntityTypes.HORSE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		var zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		var chicken = EntityTypes.CHICKEN.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(horse != null && zombie != null && chicken != null,
				"Passenger fixture could not create its real entity graph");
		horse.snapTo(player.getX() + 1.0, player.getY(), player.getZ());
		zombie.snapTo(horse.position());
		chicken.snapTo(horse.position());
		helper.getLevel().addFreshEntity(horse);
		helper.getLevel().addFreshEntity(zombie);
		helper.getLevel().addFreshEntity(chicken);
		player.startRiding(horse, true, false);
		zombie.startRiding(horse, true, false);
		chicken.startRiding(zombie, true, false);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.setSlots(player, java.util.List.of("powers:flight"));
		TestingOverrides.setAll(player.getUUID(), true);
		FlightAbility flight = new FlightAbility();
		helper.assertTrue(AbilityActivationService.activate(player, flight, "powers:flight", true)
				== AbilityActivationService.Result.ACTIVATED,
				"Mounted player could not enter its persistent movement owner");
		Vec3 destination = player.position().add(8.0, 0.0, 8.0);
		helper.getLevel().getChunkAt(BlockPos.containing(destination));
		TravelCohort.Snapshot cohort = TravelCohort.capture(helper.getLevel(), player, player);
		helper.assertTrue(cohort.companions().size() == 3,
				"Cohort capture did not contain exactly the three fixture passengers");
		player.teleport(new TeleportTransition(helper.getLevel(), destination, Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
		TravelCohort.move(cohort, helper.getLevel(), destination);
		for (var entity : java.util.List.of(horse, zombie, chicken)) {
			helper.assertTrue(!entity.isRemoved() && helper.getLevel().getEntity(entity.getUUID()) == entity,
					"Cohort travel lost or duplicated a passenger entity");
			helper.assertTrue(entity.getVehicle() == null || entity.getVehicle().level() == entity.level(),
					"Cohort travel retained a cross-level passenger reference");
		}
		helper.assertTrue(player.getVehicle() == null && horse.getPassengers().size() == 1
				&& horse.getPassengers().getFirst() == zombie && zombie.getVehicle() == horse
				&& zombie.getPassengers().size() == 1 && zombie.getPassengers().getFirst() == chicken
				&& chicken.getVehicle() == zombie,
				"Same-dimension cohort did not dismount only the principal and preserve the nested mob graph");
		helper.assertTrue(data.isToggleActive("powers:flight"),
				"Cohort travel silently dropped persistent toggle ownership");
		AbilityActivationService.activate(player, flight, "powers:flight", true);
		helper.assertFalse(data.isToggleActive("powers:flight"),
				"Mounted travel retained a stale toggle after explicit cleanup");
		TestingOverrides.clear(player.getUUID());
		player.stopRiding();
		chicken.stopRiding();
		zombie.stopRiding();
		for (var entity : java.util.List.of(chicken, zombie, horse)) entity.discard();
		player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 20)
	@SuppressWarnings("removal")
	public void netherPortalIsLegalInOrdinaryWorldAndDeniedWithoutStateChangeInMindscape(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos portalPos = helper.absolutePos(new BlockPos(2, 2, 2));
		player.snapTo(portalPos.getX() + 0.5, portalPos.getY(), portalPos.getZ() + 0.5);
		var portal = (NetherPortalBlock) Blocks.NETHER_PORTAL;
		var overworld = helper.getLevel();
		try {
			placePortal(overworld, portalPos);
			helper.assertTrue(RealmPortalRules.mayDepart("minecraft:overworld", false, 0, 0),
					"Ordinary-world portal policy was unexpectedly confined");
			helper.assertTrue(portal.getPortalDestination(overworld, player, portalPos) != null,
					"A physically placed ordinary-world Nether portal had no legal transition");
			for (String realmId : java.util.List.of("dark_realm", "light_realm")) {
				var realm = overworld.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
						Identifier.fromNamespaceAndPath("powers", realmId)));
				helper.assertTrue(realm != null, realmId + " was unavailable to the portal fixture");
				placePortal(realm, portalPos);
				player.teleport(new TeleportTransition(realm, Vec3.atBottomCenterOf(portalPos), Vec3.ZERO,
						0.0F, 0.0F, TeleportTransition.DO_NOTHING));
				Vec3 before = player.position();
				GameType mode = player.gameMode();
				int energy = PlayerPowers.get(player).energy();
				int tickets = TravelChunkLoader.pendingRequestCount();
				var transition = portal.getPortalDestination(realm, player, portalPos);
				helper.assertTrue(transition == null,
						"A physically placed Nether portal escaped " + realmId + " confinement");
				helper.assertTrue(player.level() == realm && player.position().equals(before)
						&& player.gameMode() == mode && PlayerPowers.get(player).energy() == energy,
						"Portal denial changed player authority state in " + realmId);
				helper.assertTrue(TravelChunkLoader.pendingRequestCount() == tickets,
						"Portal denial leaked a travel ticket in " + realmId);
			}
		} finally {
			player.teleport(new TeleportTransition(overworld, Vec3.atBottomCenterOf(portalPos), Vec3.ZERO,
					0.0F, 0.0F, TeleportTransition.DO_NOTHING));
			player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 20)
	@SuppressWarnings("removal")
	public void syntheticForeignDimensionRunsPolicyFxTravelAndStableDelayedCleanup(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(SYNTHETIC_DIMENSION);
		helper.assertTrue(level != null, "GameTest-only synthetic dimension was absent from the live registry");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 destination = new Vec3(0.5, 70.0, 0.5);
		player.teleport(new TeleportTransition(level, destination, Vec3.ZERO, 0.0F, 0.0F,
				TeleportTransition.DO_NOTHING));
		helper.assertTrue(player.level() == level && player.position().equals(destination),
				"Vanilla foreign-dimension travel changed the requested coordinate");
		helper.assertTrue(com.powers.config.ResolvedPowerPolicy.resolve(level) != null,
				"Policy resolution switched on a foreign namespace");
		FlightAbility flight = new FlightAbility();
		TestingOverrides.setAll(player.getUUID(), true);
		helper.assertTrue(AbilityActivationService.activate(player, flight, "powers:flight", true)
				== AbilityActivationService.Result.ACTIVATED,
				"Action/FX production entrypoint rejected a foreign dimension");
		AtomicBoolean callback = new AtomicBoolean();
		var token = com.powers.PowersMod.scheduleDelayed(level.getServer(), 3, player.getUUID(),
				SYNTHETIC_DIMENSION, player.getUUID(), "qa010_synthetic",
				(server, task) -> callback.set(server.getLevel(SYNTHETIC_DIMENSION) == level));
		helper.assertTrue(token.accepted(), "Synthetic-dimension delayed work was rejected");
		helper.runAfterDelay(5, () -> {
			try {
				helper.assertTrue(callback.get(), "Synthetic-dimension callback lost stable registry identity");
				AbilityActivationService.activate(player, flight, "powers:flight", true);
				helper.assertFalse(PlayerPowers.get(player).isToggleActive("powers:flight"),
						"Foreign-dimension action cleanup left a stale toggle");
				helper.assertTrue(com.powers.PowersMod.delayedTasks().stream()
						.noneMatch(task -> task.cancellationOwner().equals(player.getUUID())),
						"Foreign-dimension delayed work left stale state");
			} finally {
				TestingOverrides.clear(player.getUUID());
				player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
			}
			helper.succeed();
		});
	}

	private static void placePortal(net.minecraft.server.level.ServerLevel level, BlockPos interior) {
		for (int x = -1; x <= 2; x++) {
			level.setBlockAndUpdate(interior.offset(x, -1, 0), Blocks.OBSIDIAN.defaultBlockState());
			level.setBlockAndUpdate(interior.offset(x, 3, 0), Blocks.OBSIDIAN.defaultBlockState());
		}
		for (int y = 0; y <= 2; y++) {
			level.setBlockAndUpdate(interior.offset(-1, y, 0), Blocks.OBSIDIAN.defaultBlockState());
			level.setBlockAndUpdate(interior.offset(2, y, 0), Blocks.OBSIDIAN.defaultBlockState());
			for (int x = 0; x <= 1; x++) {
				level.setBlockAndUpdate(interior.offset(x, y, 0), Blocks.NETHER_PORTAL.defaultBlockState());
			}
		}
	}
}
