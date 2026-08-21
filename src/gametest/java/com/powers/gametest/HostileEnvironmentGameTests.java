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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
		int guardiansBefore = ArtifactGuardianSummons.indexedGuardianCount();
		player.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		player.getInventory().add(com.powers.PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(com.powers.companion.PrivateCompanionManager.handleChat(
				player, "shadow, reveal yourself"), "Claim fixture could not reveal the real Shadow body");
		com.powers.companion.PrivateCompanionManager.tickPlayer(player, 0);
		var shadow = com.powers.companion.PrivateCompanionManager.body(player.getUUID()).orElseThrow();
		shadow.setEnergy(com.powers.companion.ShadowCompanionRules.MAX_ENERGY);
		int energyBefore = data.energy();
		int shadowEnergyBefore = shadow.energy();
		long shadowCastsBefore = com.powers.companion.combat.ShadowPowerRuntime.diagnostics().casts();
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
			var shadowSummon = com.powers.companion.combat.ShadowPowerExecutor.execute(
					helper.getLevel(), shadow, host,
					com.powers.companion.combat.ShadowPowerCatalogue.find("call_hollowed"),
					new com.powers.companion.combat.ShadowPowerExecutor.ExecutionContext(
							player, false, helper.getLevel().getServer().getTickCount()));
			helper.assertFalse(shadowSummon.success(),
					"External ritual denial allowed the owner-directed Shadow summon path");
			helper.assertTrue(shadow.energy() == shadowEnergyBefore
					&& com.powers.companion.combat.ShadowPowerRuntime.diagnostics().casts()
					== shadowCastsBefore,
					"Denied Shadow summon charged energy or recorded a cast");
			helper.assertTrue(ArtifactGuardianSummons.indexedGuardianCount() == guardiansBefore,
					"Denied Shadow summon created owned guardians");
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
			ArtifactGuardianSummons.revokeOwner(helper.getLevel().getServer(), shadow.getUUID(),
					ArtifactAlignment.DARKNESS);
			ArtifactGuardianSummons.revokeOwner(helper.getLevel().getServer(), player.getUUID(),
					ArtifactAlignment.DARKNESS);
			com.powers.companion.PrivateCompanionManager.handleChat(player, "shadow, leave me");
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
		WorldBorder border = helper.getLevel().getWorldBorder();
		double oldX = border.getCenterX();
		double oldZ = border.getCenterZ();
		double oldSize = border.getSize();
		java.util.List<net.minecraft.world.entity.Entity> ownedEntities = new java.util.ArrayList<>();
		try {
			BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
			player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			data.setSlots(player, java.util.List.of("powers:time_shift"));
			var horse = EntityTypes.HORSE.create(helper.getLevel(),
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			var zombie = EntityTypes.ZOMBIE.create(helper.getLevel(),
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			var shadow = com.powers.PowersEntities.SHADOW_COMPANION.create(helper.getLevel(),
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			helper.assertTrue(horse != null && zombie != null && shadow != null,
					"World-border fixture could not create its bounded real cohort");
			ownedEntities.add(horse);
			ownedEntities.add(zombie);
			ownedEntities.add(shadow);
			horse.snapTo(player.getX() + 1.0, player.getY(), player.getZ());
			zombie.snapTo(horse.position());
			shadow.snapTo(player.getX() - 1.0, player.getY(), player.getZ());
			horse.setNoAi(true);
			zombie.setNoAi(true);
			shadow.setNoAi(true);
			helper.getLevel().addFreshEntity(horse);
			helper.getLevel().addFreshEntity(zombie);
			helper.getLevel().addFreshEntity(shadow);
			player.startRiding(horse, true, false);
			zombie.startRiding(horse, true, false);
			helper.assertTrue(BodyProxyManager.start(player, com.powers.mind.BodyProxyKind.ASTRAL),
					"World-border fixture could not establish a real confinement-applicable body session");
			java.util.UUID bodyId = BodyProxyManager.bodyIdForOwner(player.getUUID());
			var body = bodyId == null ? null : helper.getLevel().getEntity(bodyId);
			helper.assertTrue(body != null, "World-border fixture did not create its real body proxy");
			int energy = data.energy();
			Vec3 position = player.position();
			Vec3 horsePosition = horse.position();
			Vec3 zombiePosition = zombie.position();
			Vec3 shadowPosition = shadow.position();
			var mindBody = data.mindBody();
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
			helper.assertTrue(horse.position().equals(horsePosition)
					&& zombie.position().equals(zombiePosition) && shadow.position().equals(shadowPosition),
					"World-border rejection moved a bounded cohort member");
			helper.assertTrue(player.getVehicle() == horse && zombie.getVehicle() == horse
					&& horse.getPassengers().size() == 2 && horse.getPassengers().contains(player)
					&& horse.getPassengers().contains(zombie) && shadow.getVehicle() == null
					&& shadow.getPassengers().isEmpty(),
					"World-border rejection changed cohort relationships");
			helper.assertTrue(BodyProxyManager.hasSession(player, com.powers.mind.BodyProxyKind.ASTRAL)
					&& data.mindBody() == mindBody && helper.getLevel().getEntity(bodyId) == body
					&& !body.isRemoved(),
					"World-border rejection changed the real mind/body session");
			helper.assertFalse(hasTravelTicket(player),
					"World-border rejection leaked owner-scoped travel work");
		} finally {
			border.setCenter(oldX, oldZ);
			border.setSize(oldSize);
			TravelChunkLoader.cancel(helper.getLevel().getServer(), player.getUUID());
			com.powers.PowersMod.cancelDelayedTasks(player.getUUID());
			BodyProxyManager.finish(player);
			player.stopRiding();
			for (var entity : ownedEntities) {
				entity.stopRiding();
				entity.ejectPassengers();
				entity.discard();
			}
			player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 160)
	@SuppressWarnings("removal")
	public void voidCoordinateRemainsExactAndFatalBodyDamageClearsMindAndTravelState(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		FlightAbility flight = new FlightAbility();
		AtomicBoolean cleaned = new AtomicBoolean();
		Runnable cleanup = () -> {
			if (cleaned.compareAndSet(false, true)) cleanupVoidFixture(player, flight);
		};
		helper.runBeforeTestEnd(cleanup);
		AtomicBoolean staleCallbackRan = new AtomicBoolean();
		try {
			BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
			player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			data.setSlots(player, java.util.List.of("powers:time_shift", "powers:astral_projection", "powers:flight"));
			TestingOverrides.setAll(player.getUUID(), true);
			double voidY = helper.getLevel().getMinY() + 2.0;
			Vec3 requested = new Vec3(player.getX() + 8.0, voidY, player.getZ() + 8.0);
			helper.getLevel().getChunkAt(BlockPos.containing(requested));
			player.setNoGravity(true);
			helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
					new TeleportAbility(), helper.getLevel().dimension(), requested.x, requested.y,
					requested.z, true) == AbilityActivationService.Result.ACTIVATED,
					"Exact void coordinate was safety-corrected before Minecraft travel");
			helper.runAfterDelay(52, () -> {
				try {
					helper.assertTrue(player.position().distanceToSqr(requested) < 0.01,
							"Direct travel rewrote the entered void coordinate at commit");
					helper.assertTrue(AbilityActivationService.activate(player, flight, "powers:flight", true)
							== AbilityActivationService.Result.ACTIVATED,
							"Void fixture could not start a real persistent toggle before fatal cleanup");
					helper.assertTrue(AbilityActivationService.activate(player, new AstralProjectionAbility(),
							"powers:astral_projection", true) == AbilityActivationService.Result.ACTIVATED,
							"Void fixture could not start the real vulnerable-body lifecycle");
					var token = com.powers.PowersMod.scheduleDelayed(helper.getLevel().getServer(), 120,
							player.getUUID(), helper.getLevel().dimension(), player.getUUID(), "qa010_void_stale",
							(server, task) -> staleCallbackRan.set(true));
					helper.assertTrue(token.accepted(), "Void fixture could not queue owned lifecycle work");
					java.util.UUID bodyId = BodyProxyManager.bodyIdForOwner(player.getUUID());
					helper.assertTrue(bodyId != null && helper.getLevel().getEntity(bodyId) != null,
							"Astral lifecycle did not create its real body proxy");
					player.setGameMode(GameType.SURVIVAL);
					player.setInvulnerable(false);
					player.getAbilities().invulnerable = false;
					for (int i = 0; i < 60; i++) player.connection.tickClientLoadTimeout();
					player.setNoGravity(false);
					player.setHealth(4.0F);
					player.setPos(player.getX(), helper.getLevel().getMinY() - 80.0, player.getZ());
					helper.assertTrue(player.getY() < helper.getLevel().getMinY() - 64.0,
							"Void fixture did not cross the vanilla below-world threshold; y=" + player.getY());
					helper.assertFalse(player.isInvulnerableTo(helper.getLevel(), player.damageSources().fellOutOfWorld()),
							"Void fixture player remained invulnerable to vanilla below-world damage; removed="
									+ player.isRemoved() + ", baseInvulnerable=" + player.isInvulnerable());
					player.checkBelowWorld();
					helper.assertTrue(player.getHealth() < 4.0F,
							"Vanilla below-world dispatch did not damage the projected avatar; health="
									+ player.getHealth());
				} catch (Throwable failure) {
					cleanup.run();
					throw failure;
				}
			});
			helper.runAfterDelay(140, () -> {
				try {
					helper.assertFalse(AstralProjectionAbility.isActive(player.getUUID()),
							"Fatal void-body damage left astral state active");
					helper.assertFalse(BodyProxyManager.hasSession(player, com.powers.mind.BodyProxyKind.ASTRAL),
							"Fatal void-body damage left a body proxy session");
					helper.assertFalse(data.isToggleActive("powers:flight"),
							"Fatal void lifecycle retained an active movement toggle");
					helper.assertTrue(com.powers.PowersMod.delayedTasks().stream()
							.noneMatch(task -> task.cancellationOwner().equals(player.getUUID())),
							"Fatal void lifecycle retained owned delayed work");
					helper.assertFalse(staleCallbackRan.get(),
							"Fatal void lifecycle allowed owned delayed work to execute");
					helper.assertFalse(hasTravelTicket(player),
							"Fatal void-body damage left owner-scoped travel work: "
									+ TravelChunkLoader.diagnostics().tickets());
				} finally {
					cleanup.run();
				}
				helper.succeed();
			});
		} catch (Throwable failure) {
			cleanup.run();
			throw failure;
		}
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 30)
	@SuppressWarnings("removal")
	public void iceAuthorizesAndAppliesOneImmutableMutationPlanBeforeEntityDamage(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos feet = helper.absolutePos(new BlockPos(3, 2, 3));
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.setSkillLevel(player, 10);
		data.setSlots(player, java.util.List.of("powers:ice_manipulation"));
		for (int z = 0; z <= 8; z++) {
			helper.getLevel().setBlockAndUpdate(feet.offset(0, -1, z), Blocks.STONE.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(feet.offset(0, 0, z), Blocks.AIR.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(feet.offset(0, 1, z), Blocks.AIR.defaultBlockState());
		}
		BlockPos water = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2.5)));
		helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
		var target = EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(target != null, "Immutable Ice fixture could not create its entity target");
		target.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 6.5);
		target.setNoAi(true);
		helper.getLevel().addFreshEntity(target);
		float health = target.getHealth();
		AtomicInteger deniedAboveQueries = new AtomicInteger();
		helper.assertTrue(PowerProtectionAdapters.register("qa010_ice_plan", 10_000, query -> {
			if (water.above().equals(query.position())) {
				deniedAboveQueries.incrementAndGet();
				return false;
			}
			return true;
		}), "Immutable Ice fixture did not install exactly once");
		try {
			helper.assertTrue(AbilityActivationService.activate(player, new IceManipulationAbility(),
					"powers:ice_manipulation", false) == AbilityActivationService.Result.ACTIVATED,
					"Authorized immutable Ice plan did not commit");
			helper.assertTrue(target.getHealth() < health, "Immutable Ice plan did not damage its real entity hit");
			helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.ICE),
					"Immutable Ice plan did not apply its authorized fluid mutation");
			helper.assertTrue(deniedAboveQueries.get() == 0
					&& helper.getLevel().getBlockState(water.above()).isAir(),
					"Ice re-read mutated fluid and discovered an unplanned denied snow position after damage");
		} finally {
			PowerProtectionAdapters.unregister("qa010_ice_plan");
			target.discard();
			player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 30)
	@SuppressWarnings("removal")
	public void waterAndLavaUseRealThermalBlocksAndSubmergedMovementStaysFinite(GameTestHelper helper) {
		AtomicReference<ServerPlayer> iceCasterRef = new AtomicReference<>();
		AtomicReference<ServerPlayer> infernoCasterRef = new AtomicReference<>();
		AtomicReference<net.minecraft.world.entity.LivingEntity> targetRef = new AtomicReference<>();
		AtomicBoolean cleaned = new AtomicBoolean();
		Runnable cleanup = () -> {
			if (cleaned.compareAndSet(false, true)) {
				cleanupFluidFixture(iceCasterRef.get(), infernoCasterRef.get(), targetRef.get());
			}
		};
		helper.runBeforeTestEnd(cleanup);
		try {
			ServerPlayer iceCaster = helper.makeMockServerPlayerInLevel();
			iceCasterRef.set(iceCaster);
			BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
			iceCaster.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
			iceCaster.setYRot(0.0F);
			iceCaster.setXRot(0.0F);
			PlayerPowers.PlayerPowersData iceData = PlayerPowers.get(iceCaster);
			iceData.setSlots(iceCaster, java.util.List.of("powers:ice_manipulation"));
			TestingOverrides.setAll(iceCaster.getUUID(), true);
			BlockPos water = BlockPos.containing(
					iceCaster.getEyePosition().add(iceCaster.getLookAngle().scale(2.5)));
			helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(water.above(), Blocks.STONE.defaultBlockState());
			BlockPos lava = BlockPos.containing(
					iceCaster.getEyePosition().add(iceCaster.getLookAngle().scale(4.5)));
			helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
			ServerPlayer infernoCaster = helper.makeMockServerPlayerInLevel();
			infernoCasterRef.set(infernoCaster);
			infernoCaster.snapTo(origin.getX() + 8.5, origin.getY(), origin.getZ() + 0.5);
			helper.getLevel().setBlockAndUpdate(infernoCaster.blockPosition(), Blocks.WATER.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(infernoCaster.blockPosition().above(), Blocks.WATER.defaultBlockState());
			var target = EntityTypes.ZOMBIE.create(helper.getLevel(),
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			helper.assertTrue(target != null, "Fluid fixture could not create its Inferno target");
			targetRef.set(target);
			target.snapTo(infernoCaster.getX() + 1.0, infernoCaster.getY(), infernoCaster.getZ());
			target.setNoAi(true);
			helper.getLevel().addFreshEntity(target);
			var observed = new net.minecraft.world.phys.AABB(
					Vec3.atLowerCornerOf(water), Vec3.atLowerCornerOf(lava.above())).inflate(8.0);
			int dropsBefore = helper.getLevel().getEntities(
					net.minecraft.world.level.entity.EntityTypeTest.forClass(
							net.minecraft.world.entity.item.ItemEntity.class),
					entity -> entity.getBoundingBox().intersects(observed)).size();
			helper.assertTrue(AbilityActivationService.activate(iceCaster, new IceManipulationAbility(),
					"powers:ice_manipulation", true) == AbilityActivationService.Result.ACTIVATED,
					"Ice production entrypoint rejected real water");
			helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.ICE),
					"Ice production entrypoint did not freeze real water exactly once");
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

			float health = target.getHealth();
			helper.assertTrue(com.powers.magic.runtime.CastScalingContext.withSource(
					com.powers.magic.runtime.CastSource.CRYSTAL,
					() -> new InfernoAbility().activate(infernoCaster, PlayerPowers.get(infernoCaster))),
					"Inferno rejected a submerged caster");
			InfernoAbility.tickAll(helper.getLevel().getServer());
			helper.assertTrue(target.getHealth() < health, "Inferno did not resolve bounded entity damage in water");
			float settledHealth = target.getHealth();
			InfernoAbility.clearAll();
			helper.runAfterDelay(10, () -> {
				try {
					helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.ICE)
							&& helper.getLevel().getBlockState(lava).is(Blocks.OBSIDIAN),
							"Thermal results were not stable after scheduled fluid ticks");
					helper.assertTrue(target.isAlive() && target.getHealth() == settledHealth,
							"Cleared bounded Inferno work continued damaging its target");
					int dropsAfter = helper.getLevel().getEntities(
							net.minecraft.world.level.entity.EntityTypeTest.forClass(
									net.minecraft.world.entity.item.ItemEntity.class),
							entity -> entity.getBoundingBox().intersects(observed)).size();
					helper.assertTrue(dropsAfter == dropsBefore,
							"Thermal/fluid work emitted duplicate item drops");
				} finally {
					cleanup.run();
				}
				helper.succeed();
			});
		} catch (Throwable failure) {
			cleanup.run();
			throw failure;
		}
	}

	@GameTest(environment = "qa010_hostile:travel_graph", maxTicks = 90)
	@SuppressWarnings("removal")
	public void mountedNestedPassengersTravelWithoutDuplicationOrCrossLevelReferences(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		FlightAbility flight = new FlightAbility();
		java.util.List<net.minecraft.world.entity.Entity> ownedEntities = new java.util.ArrayList<>();
		AtomicBoolean cleaned = new AtomicBoolean();
		Runnable cleanup = () -> {
			if (cleaned.compareAndSet(false, true)) cleanupTravelFixture(player, flight, ownedEntities);
		};
		helper.runBeforeTestEnd(cleanup);
		try {
			BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
			player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
			var horse = EntityTypes.HORSE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			var zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			var chicken = EntityTypes.CHICKEN.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			helper.assertTrue(horse != null && zombie != null && chicken != null,
					"Passenger fixture could not create its real entity graph");
			ownedEntities.add(chicken);
			ownedEntities.add(zombie);
			ownedEntities.add(horse);
			horse.snapTo(player.getX() + 1.0, player.getY(), player.getZ());
			zombie.snapTo(horse.position());
			chicken.snapTo(horse.position());
			horse.setNoAi(true);
			zombie.setNoAi(true);
			zombie.setBaby(true);
			chicken.setNoAi(true);
			helper.getLevel().addFreshEntity(horse);
			helper.getLevel().addFreshEntity(zombie);
			helper.getLevel().addFreshEntity(chicken);
			player.startRiding(horse, true, false);
			zombie.startRiding(horse, true, false);
			chicken.startRiding(zombie, true, false);
			helper.assertTrue(java.util.List.of(horse, zombie, chicken).stream()
					.allMatch(entity -> entity.position().distanceToSqr(player.position())
							<= com.powers.power.travel.TravelCohortRules.RADIUS
									* com.powers.power.travel.TravelCohortRules.RADIUS),
					"Passenger graph began outside the production cohort radius");
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			data.setSlots(player, java.util.List.of("powers:flight", "powers:time_shift"));
			TestingOverrides.setAll(player.getUUID(), true);
			helper.assertTrue(AbilityActivationService.activate(player, flight, "powers:flight", true)
					== AbilityActivationService.Result.ACTIVATED,
					"Mounted player could not enter its persistent movement owner");
			Vec3 destination = player.position().add(8.0, 0.0, 8.0);
			helper.getLevel().getChunkAt(BlockPos.containing(destination));
			helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
					new TeleportAbility(), helper.getLevel().dimension(), destination.x,
					destination.y, destination.z, true) == AbilityActivationService.Result.ACTIVATED,
					"Production Time Shift entrypoint rejected the mounted graph");
			helper.assertTrue(hasTravelTicket(player),
					"Production Time Shift did not own a delayed destination ticket");
			helper.runAfterDelay(65, () -> {
				try {
					helper.assertTrue(player.position().distanceToSqr(destination) < 0.01,
							"Production Time Shift did not commit the exact destination");
					for (var entity : java.util.List.of(horse, zombie, chicken)) {
						helper.assertTrue(!entity.isRemoved() && helper.getLevel().getEntity(entity.getUUID()) == entity,
								"Cohort travel lost or duplicated a passenger entity");
						helper.assertTrue(entity.getVehicle() == null || entity.getVehicle().level() == entity.level(),
								"Cohort travel retained a cross-level passenger reference");
					}
					helper.assertTrue(player.getVehicle() == null && horse.getPassengers().isEmpty()
							&& horse.getVehicle() == null && zombie.getPassengers().isEmpty()
							&& zombie.getVehicle() == null && chicken.getPassengers().isEmpty()
							&& chicken.getVehicle() == null,
							"Production cohort policy did not deterministically detach the exact graph: "
									+ "playerVehicle=" + player.getVehicle() + ", horsePassengers="
									+ horse.getPassengers() + ", zombieVehicle=" + zombie.getVehicle()
									+ ", zombiePassengers=" + zombie.getPassengers() + ", chickenVehicle="
									+ chicken.getVehicle());
					helper.assertTrue(data.isToggleActive("powers:flight"),
							"Cohort travel silently dropped persistent toggle ownership");
					helper.assertFalse(hasTravelTicket(player),
							"Completed mounted travel retained owner-scoped destination work");
				} finally {
					cleanup.run();
				}
				helper.succeed();
			});
		} catch (Throwable failure) {
			cleanup.run();
			throw failure;
		}
	}

	@GameTest(environment = "qa010_hostile:isolated", maxTicks = 40)
	@SuppressWarnings("removal")
	public void netherPortalIsLegalInOrdinaryWorldAndDeniedWithoutStateChangeInMindscape(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos portalPos = helper.absolutePos(new BlockPos(2, 2, 2));
		player.snapTo(portalPos.getX() + 0.5, portalPos.getY(), portalPos.getZ() + 0.5);
		var portal = (NetherPortalBlock) Blocks.NETHER_PORTAL;
		var overworld = helper.getLevel();
		BlockPos bodyOrigin = portalPos.offset(6, 0, 0);
		overworld.setBlockAndUpdate(bodyOrigin.below(), Blocks.STONE.defaultBlockState());
		try {
			placePortal(overworld, portalPos);
			helper.assertTrue(RealmPortalRules.mayDepart("minecraft:overworld", false, 0, 0),
					"Ordinary-world portal policy was unexpectedly confined");
			var ordinary = portal.getPortalDestination(overworld, player, portalPos);
			helper.assertTrue(ordinary != null,
					"A physically placed ordinary-world Nether portal had no legal transition");
			player.teleport(ordinary);
			helper.assertTrue(player.level() != overworld,
					"The legal ordinary portal transition was not actually applied");
			player.teleport(new TeleportTransition(overworld, Vec3.atBottomCenterOf(portalPos), Vec3.ZERO,
					0.0F, 0.0F, TeleportTransition.DO_NOTHING));
			for (String realmId : java.util.List.of("dark_realm", "light_realm")) {
				PlayerPowers.get(player).setSkillLevel(player, 0);
				PlayerPowers.get(player).setDarknessLevel(player, 0);
				player.removeTag(com.powers.player.SkillSystem.DARKNESS_TAG);
				var realm = overworld.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
						Identifier.fromNamespaceAndPath("powers", realmId)));
				helper.assertTrue(realm != null, realmId + " was unavailable to the portal fixture");
				player.snapTo(bodyOrigin.getX() + 0.5, bodyOrigin.getY(), bodyOrigin.getZ() + 0.5);
				helper.assertTrue(BodyProxyManager.start(player, com.powers.mind.BodyProxyKind.ASTRAL),
						"Portal fixture could not start a real mind/body session for " + realmId);
				java.util.UUID bodyId = BodyProxyManager.bodyIdForOwner(player.getUUID());
				net.minecraft.world.entity.Entity body = bodyId == null ? null : overworld.getEntity(bodyId);
				helper.assertTrue(body != null, "Portal fixture did not create a real body for " + realmId);
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
				helper.assertTrue(BodyProxyManager.hasSession(player, com.powers.mind.BodyProxyKind.ASTRAL)
						&& overworld.getEntity(bodyId) == body && !body.isRemoved()
						&& PlayerPowers.get(player).mindBody() != null,
						"Portal denial changed the real proxy/body session in " + realmId);
				if ("dark_realm".equals(realmId)) {
					player.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
					PlayerPowers.get(player).setDarknessLevel(player,
							com.powers.player.SkillSystem.DARKNESS_GATE_LEVEL);
				} else {
					PlayerPowers.get(player).setSkillLevel(player,
							com.powers.player.SkillSystem.DARKNESS_GATE_LEVEL);
				}
				helper.assertTrue(BodyProxyManager.returnToBody(player),
						"Qualified explicit body return failed from " + realmId);
				helper.assertTrue(player.level() == overworld
						&& !BodyProxyManager.hasSession(player, com.powers.mind.BodyProxyKind.ASTRAL)
						&& overworld.getEntity(bodyId) == null,
						"Body return did not complete before the next realm transition");
			}
		} finally {
			BodyProxyManager.finish(player);
			player.teleport(new TeleportTransition(overworld, Vec3.atBottomCenterOf(portalPos), Vec3.ZERO,
					0.0F, 0.0F, TeleportTransition.DO_NOTHING));
			player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		helper.succeed();
	}

	@GameTest(environment = "qa010_hostile:synthetic_reload", maxTicks = 600)
	@SuppressWarnings("removal")
	public void syntheticForeignDimensionRunsPolicyFxTravelAndStableDelayedCleanup(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(SYNTHETIC_DIMENSION);
		helper.assertTrue(level != null, "GameTest-only synthetic dimension was absent from the live registry");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var overworld = helper.getLevel();
		Vec3 origin = player.position();
		Vec3 destination = new Vec3(0.5, 70.0, 0.5);
		BlockPos destinationFloor = BlockPos.containing(destination).below();
		var originalFloor = level.getBlockState(destinationFloor);
		FlightAbility flight = new FlightAbility();
		AtomicBoolean cleaned = new AtomicBoolean();
		Runnable cleanup = () -> {
			if (cleaned.compareAndSet(false, true)) {
				cleanupSyntheticFixture(player, flight, level, destinationFloor, originalFloor);
			}
		};
		helper.runBeforeTestEnd(cleanup);
		AtomicBoolean reloadComplete = new AtomicBoolean();
		AtomicBoolean delayedRebound = new AtomicBoolean();
		AtomicBoolean exactArrivalObserved = new AtomicBoolean();
		AtomicBoolean exactReturnObserved = new AtomicBoolean();
		AtomicBoolean watchingReturn = new AtomicBoolean();
		AtomicBoolean initialTravelStarted = new AtomicBoolean();
		AtomicBoolean foreignPhaseStarted = new AtomicBoolean();
		AtomicBoolean returnStarted = new AtomicBoolean();
		java.util.concurrent.atomic.AtomicInteger fixtureTicks = new java.util.concurrent.atomic.AtomicInteger();
		AtomicReference<Throwable> reloadFailure = new AtomicReference<>();
		try {
			helper.onEachTick(() -> {
				try {
					if (fixtureTicks.incrementAndGet() >= 20
							&& initialTravelStarted.compareAndSet(false, true)) {
					level.setBlockAndUpdate(destinationFloor, Blocks.STONE.defaultBlockState());
					level.getChunkAt(BlockPos.containing(destination));
					TestingOverrides.setAll(player.getUUID(), true);
					PlayerPowers.get(player).setSlots(player,
							java.util.List.of("powers:time_shift", "powers:flight"));
					helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
							new TeleportAbility(), SYNTHETIC_DIMENSION, destination.x, destination.y,
							destination.z, true) == AbilityActivationService.Result.ACTIVATED,
							"POWERS travel entrypoint rejected the synthetic dimension");
					}
					if (player.level() == level && player.position().distanceToSqr(destination) < 0.01) {
						exactArrivalObserved.set(true);
					}
					if (watchingReturn.get() && player.level() == overworld
							&& player.position().distanceToSqr(origin) < 0.01) {
						exactReturnObserved.set(true);
					}
					if (exactArrivalObserved.get() && foreignPhaseStarted.compareAndSet(false, true)) {
						helper.assertTrue(com.powers.config.ResolvedPowerPolicy.resolve(level) != null,
								"Policy resolution switched on a foreign namespace");
						var beforeFx = com.powers.fx.PowerFx.lodSnapshot(level.getServer());
						helper.assertTrue(AbilityActivationService.activate(player, flight, "powers:flight", true)
								== AbilityActivationService.Result.ACTIVATED,
								"Action/FX production entrypoint rejected a foreign dimension");
						var afterFx = com.powers.fx.PowerFx.lodSnapshot(level.getServer());
						helper.assertTrue(fxDeliveries(afterFx) > fxDeliveries(beforeFx),
								"Synthetic-dimension action emitted no observable authoritative FX delivery");
						AbilityActivationService.activate(player, flight, "powers:flight", true);
						var server = level.getServer();
						var callback = com.powers.PowersMod.scheduleDelayed(server, 30, player.getUUID(),
								SYNTHETIC_DIMENSION, player.getUUID(), "qa010_synthetic_rebind",
								(current, task) -> delayedRebound.set(
										current.getLevel(SYNTHETIC_DIMENSION) == level && player.level() == level));
						helper.assertTrue(callback.accepted(),
								"Synthetic dimension rejected owned delayed work before reload");
						server.reloadResources(server.getPackRepository().getSelectedIds())
								.whenComplete((ignored, failure) -> server.execute(() -> {
									reloadFailure.set(failure);
									reloadComplete.set(true);
								}));
					}
					if (reloadComplete.get()) {
						helper.assertTrue(reloadFailure.get() == null,
								"Synthetic-dimension reload/rebind failed: " + reloadFailure.get());
					}
					if (reloadComplete.get() && delayedRebound.get() && !hasTravelTicket(player)
							&& com.powers.PowersMod.delayedTasks().stream().noneMatch(task ->
									task.cancellationOwner().equals(player.getUUID())
											&& "teleport_storm_finish".equals(task.purpose()))
							&& returnStarted.compareAndSet(false, true)) {
						helper.assertTrue(level.getServer().getLevel(SYNTHETIC_DIMENSION) == level
								&& com.powers.config.ResolvedPowerPolicy.resolve(level) != null,
								"Synthetic level/policy did not rebind after resource reload");
						watchingReturn.set(true);
						helper.assertTrue(AbilityActivationService.activateTeleport(player, player,
								new TeleportAbility(), overworld.dimension(), origin.x, origin.y, origin.z, true)
								== AbilityActivationService.Result.ACTIVATED,
								"POWERS travel entrypoint could not leave the synthetic dimension");
					}
				} catch (Throwable failure) {
					cleanup.run();
					throw failure;
				}
			});
			helper.succeedWhen(() -> {
				helper.assertTrue(foreignPhaseStarted.get() && reloadComplete.get()
						&& delayedRebound.get() && returnStarted.get(),
						"Synthetic reload/return phases did not complete within their production bounds: foreign="
								+ foreignPhaseStarted.get() + ", reload=" + reloadComplete.get()
								+ ", reloadFailure=" + reloadFailure.get() + ", rebound=" + delayedRebound.get()
								+ ", returnStarted=" + returnStarted.get() + ", arrival="
								+ exactArrivalObserved.get() + ", return=" + exactReturnObserved.get()
								+ ", level=" + player.level().dimension().identifier() + ", tickets="
								+ TravelChunkLoader.diagnostics().tickets() + ", tasks="
								+ com.powers.PowersMod.delayedTasks().stream().filter(task ->
										task.cancellationOwner().equals(player.getUUID())).toList());
				helper.assertTrue(exactReturnObserved.get() && player.level() == overworld,
						"POWERS travel did not commit the exact return from the synthetic dimension");
				helper.assertFalse(hasTravelTicket(player),
						"Synthetic round trip retained owner-scoped travel work");
				helper.assertTrue(com.powers.PowersMod.delayedTasks().stream()
						.noneMatch(task -> task.cancellationOwner().equals(player.getUUID())),
						"Synthetic round trip/reload retained owned delayed work: "
								+ com.powers.PowersMod.delayedTasks().stream()
										.filter(task -> task.cancellationOwner().equals(player.getUUID())).toList());
				cleanup.run();
			});
		} catch (Throwable failure) {
			cleanup.run();
			throw failure;
		}
	}

	private static long fxDeliveries(com.powers.fx.PowerFx.LodSnapshot snapshot) {
		return snapshot.nearDeliveries() + snapshot.midDeliveries() + snapshot.farDeliveries();
	}

	private static boolean hasTravelTicket(ServerPlayer player) {
		return TravelChunkLoader.diagnostics().tickets().stream()
				.anyMatch(ticket -> ticket.owner().equals(player.getUUID()));
	}

	private static void cleanupFluidFixture(ServerPlayer iceCaster, ServerPlayer infernoCaster,
			net.minecraft.world.entity.LivingEntity target) {
		InfernoAbility.clearAll();
		if (iceCaster != null && PlayerPowers.get(iceCaster).isToggleActive("powers:flight")) {
			AbilityActivationService.activate(iceCaster, new FlightAbility(), "powers:flight", true);
		}
		if (iceCaster != null) {
			TestingOverrides.clear(iceCaster.getUUID());
			iceCaster.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
		if (target != null) target.discard();
		if (infernoCaster != null) {
			infernoCaster.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		}
	}

	private static void cleanupVoidFixture(ServerPlayer player, FlightAbility flight) {
		if (PlayerPowers.get(player).isToggleActive("powers:flight")) {
			AbilityActivationService.activate(player, flight, "powers:flight", true);
		}
		AstralProjectionAbility.clear(player.level().getServer(), player.getUUID());
		TeleportAbility.clearStorm(player.level().getServer(), player.getUUID());
		TravelChunkLoader.cancel(player.level().getServer(), player.getUUID());
		com.powers.PowersMod.cancelDelayedTasks(player.getUUID());
		BodyProxyManager.finish(player);
		TestingOverrides.clear(player.getUUID());
		player.setNoGravity(false);
		player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
	}

	private static void cleanupSyntheticFixture(ServerPlayer player, FlightAbility flight,
			net.minecraft.server.level.ServerLevel level, BlockPos destinationFloor,
			net.minecraft.world.level.block.state.BlockState originalFloor) {
		level.setBlockAndUpdate(destinationFloor, originalFloor);
		if (PlayerPowers.get(player).isToggleActive("powers:flight")) {
			AbilityActivationService.activate(player, flight, "powers:flight", true);
		}
		TeleportAbility.clearStorm(player.level().getServer(), player.getUUID());
		TravelChunkLoader.cancel(player.level().getServer(), player.getUUID());
		com.powers.PowersMod.cancelDelayedTasks(player.getUUID());
		BodyProxyManager.finish(player);
		TestingOverrides.clear(player.getUUID());
		player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
	}

	private static void cleanupTravelFixture(ServerPlayer player, FlightAbility flight,
			java.util.List<? extends net.minecraft.world.entity.Entity> entities) {
		if (PlayerPowers.get(player).isToggleActive("powers:flight")) {
			AbilityActivationService.activate(player, flight, "powers:flight", true);
		}
		TeleportAbility.clearStorm(player.level().getServer(), player.getUUID());
		TravelChunkLoader.cancel(player.level().getServer(), player.getUUID());
		com.powers.PowersMod.cancelDelayedTasks(player.getUUID());
		BodyProxyManager.finish(player);
		TestingOverrides.clear(player.getUUID());
		player.stopRiding();
		for (var entity : entities) {
			entity.stopRiding();
			entity.discard();
		}
		player.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
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
