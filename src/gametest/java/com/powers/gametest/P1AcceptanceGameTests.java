package com.powers.gametest;

import com.powers.entity.DarknessFireballProjectile;
import com.powers.AmethystWardBlock;
import com.powers.PowerStatusEffects;
import com.powers.PowersBlocks;
import com.powers.PowersEntities;
import com.powers.ImportedPackItems;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicPresence;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.MagicRayCollisionRuntime;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.player.PlayerPowers;
import com.powers.player.EnergyHistorySource;
import com.powers.player.PlayerEnergyHistory;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.spell.CelestialRuinCancellation;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellEffect;
import com.powers.spell.SpellFieldKind;
import com.powers.spell.SpellFieldManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

/** Live acceptance cases added while closing the P0/P1 release backlog. */
public final class P1AcceptanceGameTests {
	private static final double FAR_OFFSET = 40.0;

	public P1AcceptanceGameTests() {
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void beamCounteredAtCasterOriginSkipsEmptyCollisionGeometry(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		Vec3 origin = caster.getEyePosition();

		helper.assertFalse(MagicRayCollisionRuntime.publish(helper.getLevel(), "energy_beam",
				caster.getUUID(), origin, origin, helper.getLevel().getServer().getTickCount()).isPresent(),
				"An origin-local beam counter should not publish empty collision geometry");
		helper.succeed();
	}

	@GameTest(padding = 128)
	@SuppressWarnings("removal")
	public void everyPhysicalCollisionFamilyReachesTheLiveResolverExactlyOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer firstOwner = helper.makeMockServerPlayerInLevel();
		ServerPlayer secondOwner = helper.makeMockServerPlayerInLevel();
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 4, 5)));
		long tick = level.getServer().getTickCount();

		// Beam/beam uses exact line intersection and is covered without a spherical proxy.
		MagicRayCollisionRuntime.clearAll();
		helper.assertTrue(MagicRayCollisionRuntime.publish(level, "energy_beam", firstOwner.getUUID(),
				center.add(-4, 0, 0), center.add(4, 0, 0), tick).isEmpty(),
				"The first beam collided without a counterpart");
		helper.assertTrue(MagicRayCollisionRuntime.publish(level, "void_beam", secondOwner.getUUID(),
				center.add(0, 0, -4), center.add(0, 0, 4), tick).isPresent(),
				"Beam/beam geometry did not reach the live resolver");

		MagicPresenceHandle projectileOne = projectile(level, firstOwner, "fireball",
				center.add(FAR_OFFSET, 0, 0), tick);
		MagicPresenceHandle projectileTwo = projectile(level, secondOwner, "fireball",
				center.add(-FAR_OFFSET, 0, 0), tick);
		move(projectileOne, level, center);
		move(projectileTwo, level, center);
		assertOnce(helper, projectileTwo, level, center, tick, "projectile/projectile");
		PhysicalMagicPresences.remove(projectileOne);
		PhysicalMagicPresences.remove(projectileTwo);

		MagicPresenceHandle projectile = projectile(level, firstOwner, "fireball",
				center.add(FAR_OFFSET, 0, 0), tick);
		MagicPresenceHandle projectileField = PhysicalMagicPresences.registerFixed(
				new MagicActionId("forcefield"), secondOwner.getUUID(), level,
				center, 2.0, com.powers.time.WorldTick.at(tick + 100),
				MagicPresenceHandle.Kind.FIELD);
		move(projectile, level, center);
		assertOnce(helper, projectile, level, center, tick, "projectile/field");
		PhysicalMagicPresences.remove(projectile);

		helper.assertTrue(MagicRayCollisionRuntime.publish(level, "energy_beam", firstOwner.getUUID(),
				center.add(-4, 0, 0), center.add(4, 0, 0), tick + 20).isPresent(),
				"Beam/field geometry did not reach the live resolver");
		PhysicalMagicPresences.remove(projectileField);

		MagicPresenceHandle force = PhysicalMagicPresences.registerFixed(
				new MagicActionId("darkness_block"), firstOwner.getUUID(), level,
				center, 1.0, com.powers.time.WorldTick.at(tick + 100),
				MagicPresenceHandle.Kind.FORCE_BLOCK);
		MagicPresenceHandle impact = PhysicalMagicPresences.registerFixed(
				new MagicActionId("fireball"), secondOwner.getUUID(), level,
				center.add(FAR_OFFSET, 0, 0), 1.0,
				com.powers.time.WorldTick.at(tick + 100), MagicPresenceHandle.Kind.IMPACT);
		move(impact, level, center);
		assertOnce(helper, impact, level, center, tick + 40, "force/block");
		PhysicalMagicPresences.remove(force);
		PhysicalMagicPresences.remove(impact);

		MagicPresenceHandle body = entity(level, firstOwner, "astral_projection",
				center.add(FAR_OFFSET, 0, 0), tick, MagicPresenceHandle.Kind.ENTITY);
		MagicPresenceHandle bodyField = PhysicalMagicPresences.registerFixed(
				new MagicActionId("dimensional_anchor"), secondOwner.getUUID(), level,
				center, 2.0, com.powers.time.WorldTick.at(tick + 100),
				MagicPresenceHandle.Kind.FIELD);
		move(body, level, center);
		assertOnce(helper, body, level, center, tick + 60, "body/field");
		PhysicalMagicPresences.remove(body);
		PhysicalMagicPresences.remove(bodyField);
		MagicRayCollisionRuntime.clearAll();
		helper.succeed();
	}

	@GameTest(padding = 128)
	@SuppressWarnings("removal")
	public void bothInteractiveLocatorSpellsCommitAuthoredUnrankedPayments(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		powers.setSkillLevel(player, 10);
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		int before = powers.energy();
		powers.setSelectedSpell("book_grimoire_celestial", 0);
		helper.assertTrue(SpellCastingManager.commitSoulCompass(player),
				"Soul Compass did not commit through its authenticated locator route");
		helper.assertTrue(powers.energy() == before - 14,
				"Rank changed Soul Compass's fixed spell payment");
		powers.setSelectedSpell("book_grimoire_celestial", 2);
		helper.assertTrue(SpellCastingManager.commitLocator(player, SpellEffect.CARTOGRAPHERS_STAR),
				"Cartographer's Star did not commit through its authenticated locator route");
		helper.assertTrue(powers.energy() == before - 14 - 24,
				"Rank changed Cartographer's Star's fixed spell payment");
		helper.succeed();
	}

	@GameTest(maxTicks = 90, padding = 128)
	@SuppressWarnings("removal")
	public void deepAnchorAndWildPurificationCompleteAsPracticalRituals(GameTestHelper helper) {
		ServerPlayer anchorCaster = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		anchorCaster.snapTo(origin, 0.0F, 0.0F);
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		target.setNoAi(true);
		anchorCaster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_deep").getDefaultInstance());
		SpellCastingManager.use(anchorCaster, "book_grimoire_deep");

		ServerPlayer purifier = helper.makeMockServerPlayerInLevel();
		purifier.snapTo(origin.add(6.0, 0.0, 0.0), 0.0F, 0.0F);
		purifier.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 200, 1, false, true));
		purifier.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_wild").getDefaultInstance());
		PlayerPowers.get(purifier).setSelectedSpell("book_grimoire_wild", 0);
		SpellCastingManager.use(purifier, "book_grimoire_wild");

		helper.runAfterDelay(60, () -> {
			helper.assertTrue(DimensionalAnchorAbility.isAnchored(target),
					"Dimensional Anchor did not bind its player-compatible target");
			helper.assertFalse(purifier.hasEffect(MobEffects.WITHER),
					"Purification Circle left an ordinary harmful effect in its circle");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 30, padding = 128)
	@SuppressWarnings("removal")
	public void interruptedRitualKeepsExactlyHalfItsAuthoredPayment(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		caster.snapTo(origin, 0.0F, 0.0F);
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		target.setNoAi(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_deep").getDefaultInstance());
		int before = PlayerPowers.get(caster).energy();
		int historyBefore = PlayerEnergyHistory.snapshot(caster).history().size();
		SpellCastingManager.use(caster, "book_grimoire_deep");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID()),
				"Dimensional Anchor did not begin its interruptible channel");
		helper.assertTrue(PlayerPowers.get(caster).energy() == before - 22,
				"Ritual did not reserve its exact authored payment");
		caster.snapTo(origin.add(2.0, 0.0, 0.0), 0.0F, 0.0F);
		helper.runAfterDelay(3, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()),
					"Movement did not interrupt the ritual");
			var history = PlayerEnergyHistory.snapshot(caster).history();
			var ritual = history.subList(historyBefore, history.size());
			var costs = ritual.stream()
					.filter(entry -> entry.source() == EnergyHistorySource.PLAYER_POOL_COST)
					.map(com.powers.player.EnergyHistorySnapshot.Entry::delta).toList();
			boolean restoredBaseline = ritual.stream()
					.filter(entry -> entry.source() == EnergyHistorySource.TRANSACTION_ROLLBACK)
					.anyMatch(entry -> entry.after() == before);
			helper.assertTrue(costs.equals(java.util.List.of(-22L, -11L)) && restoredBaseline,
					"Interrupted ritual did not reserve, restore, then retain exactly half its payment: "
							+ ritual);
			helper.assertFalse(DimensionalAnchorAbility.isAnchored(target),
					"Interrupted ritual executed its effect");
			helper.succeed();
		});
	}

	@GameTest(padding = 128)
	public void all260CataloguedItemsExistInTheLiveServerRegistry(GameTestHelper helper) {
		var registered = net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
				.filter(entry -> entry.getKey().identifier().getNamespace().equals("powers"))
				.toList();
		helper.assertTrue(registered.size() == 260,
				"Live POWERS item registry differs from the 260-row catalogue: " + registered.size());
		for (var entry : registered) {
			helper.assertFalse(entry.getValue().getDefaultInstance().isEmpty(),
					"Registered item has no usable default stack: " + entry.getKey().identifier());
		}
		helper.succeed();
	}

	@GameTest(padding = 128)
	public void removedMemoryObelisksAreAbsentFromLiveRegistries(GameTestHelper helper) {
		for (String path : java.util.List.of("light_memory_obelisk", "dark_memory_obelisk")) {
			var id = com.powers.PowersMod.id(path);
			helper.assertFalse(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id),
					"Removed Memory Obelisk block remains registered: " + id);
			helper.assertFalse(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id),
					"Removed Memory Obelisk item remains registered: " + id);
		}
		helper.succeed();
	}

	@GameTest(padding = 128)
	@SuppressWarnings("removal")
	public void ordinaryCrystalUseExecutesEveryLocalEffectFamily(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer red = crystalPlayer(helper, new BlockPos(8, 1, 8));
		var infernoTarget = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
				new BlockPos(8, 1, 10));
		float infernoHealth = infernoTarget.getHealth();
		helper.assertTrue(com.powers.power.crystals.CrystalPowerRegistry.tryActivate(
				red, com.powers.PowersItems.RED_CRYSTAL), "Red Crystal rejected ordinary item use");
		com.powers.power.crystals.CrystalPowerRegistry.tick(level.getServer());
		helper.assertTrue(infernoTarget.getHealth() < infernoHealth && infernoTarget.isOnFire(),
				"Red Inferno did not damage and ignite a living mob");
		com.powers.power.crystals.InfernoAbility.clearAll();
		infernoTarget.discard();

		ServerPlayer orange = crystalPlayer(helper, new BlockPos(8, 1, 8));
		helper.assertTrue(com.powers.power.crystals.CrystalPowerRegistry.tryActivate(
				orange, com.powers.PowersItems.ORANGE_CRYSTAL), "Orange Crystal rejected ordinary item use");
		helper.runAfterDelay(1, () -> {
			var echoes = com.powers.util.BoundedEntityCandidates.ofClass(
					level, com.powers.entity.EchoClone.class,
					net.minecraft.world.phys.AABB.ofSize(orange.position(), 16.0, 8.0, 16.0),
					16, entity -> true);
			helper.assertTrue(echoes.size() == 3,
					"Orange Clone Swarm did not create exactly three echoes: " + echoes.size());
			echoes.forEach(Entity::discard);

			ServerPlayer yellow = crystalPlayer(helper, new BlockPos(8, 1, 8));
			double scaleBefore = yellow.getAttributeValue(
					net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
			helper.assertTrue(com.powers.power.crystals.CrystalPowerRegistry.tryActivate(
					yellow, com.powers.PowersItems.YELLOW_CRYSTAL),
					"Yellow Crystal rejected ordinary item use");
			helper.assertTrue(yellow.getAttributeValue(
					net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != scaleBefore,
					"Yellow Size Shift did not change the player scale");

			ServerPlayer green = crystalPlayer(helper, new BlockPos(8, 1, 8));
			green.setHealth(2.0F);
			helper.assertTrue(com.powers.power.crystals.CrystalPowerRegistry.tryActivate(
					green, com.powers.PowersItems.GREEN_CRYSTAL),
					"Green Crystal rejected ordinary item use");
			helper.assertTrue(green.getHealth() == green.getMaxHealth(),
					"Green Life Bloom did not heal its caster");

			ServerPlayer violet = crystalPlayer(helper, new BlockPos(8, 1, 8));
			var firstSoul = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
					new BlockPos(8, 1, 10));
			var secondSoul = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,
					new BlockPos(10, 1, 10));
			// Fabric's mock players deliberately share a profile UUID across GameTests.
			com.powers.power.crystals.SoulLinkAbility.clear(violet.getUUID());
			helper.runAfterDelay(2, () -> {
				boolean violetActivated = com.powers.power.crystals.CrystalPowerRegistry.tryActivate(
						violet, com.powers.PowersItems.VIOLET_CRYSTAL);
				helper.assertTrue(violetActivated, "Violet Crystal rejected ordinary item use: "
						+ com.powers.knowledge.KnowledgeService.answer(violet,
						"why did soul link fail?").answer());
				float secondBefore = secondSoul.getHealth();
				firstSoul.hurtServer(level, level.damageSources().generic(), 3.0F);
				com.powers.power.crystals.CrystalPowerRegistry.tick(level.getServer());
				helper.assertTrue(secondSoul.getHealth() < secondBefore,
						"Violet Soul Link did not mirror a wound between living mobs");

				for (ServerPlayer player : java.util.List.of(red, orange, yellow, green, violet)) {
					com.powers.testing.TestingOverrides.clear(player.getUUID());
				}
				com.powers.power.crystals.SoulLinkAbility.clearAll();
				com.powers.power.crystals.SizeShiftAbility.clear(yellow);
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 120, padding = 128)
	@SuppressWarnings("removal")
	public void abyssalWardBreakingAndDispelMutateOnlyTheirLockedTargets(GameTestHelper helper) {
		ServerPlayer breaker = helper.makeMockServerPlayerInLevel();
		// Both positions stay on the padded side of the template so its enclosure
		// cannot intercept the real 32-block spell ray.
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 55)));
		breaker.snapTo(origin, 180.0F, 0.0F);
		BlockPos ward = new BlockPos(2, 2, 30);
		BlockPos absoluteWard = helper.absolutePos(ward);
		helper.setBlock(ward, PowersBlocks.AMETHYST_WARD.defaultBlockState()
				.setValue(BlockStateProperties.POWER, 15));
		helper.setBlock(new BlockPos(3, 2, 30), Blocks.REDSTONE_BLOCK);
		breaker.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(absoluteWard));
		breaker.setOldPosAndRot();
		var wardHit = breaker.pick(32.0, 0.0F, false);
		helper.assertTrue(wardHit instanceof BlockHitResult blockHit
				&& blockHit.getBlockPos().equals(absoluteWard),
				"Ward fixture aim missed: type=" + wardHit.getType()
						+ ", block=" + (wardHit instanceof BlockHitResult blockHit
						? blockHit.getBlockPos() : "none")
						+ ", location=" + wardHit.getLocation() + ", expected=" + absoluteWard
						+ ", player=" + breaker.position() + ", eye=" + breaker.getEyePosition(0.0F)
						+ ", view=" + breaker.getViewVector(0.0F)
						+ ", yaw=" + breaker.getYRot() + ", pitch=" + breaker.getXRot());
		breaker.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_abyssal").getDefaultInstance());
		PlayerPowers.get(breaker).setSelectedSpell("book_grimoire_abyssal", 0);
		SpellCastingManager.use(breaker, "book_grimoire_abyssal");
		helper.assertTrue(SpellCastingManager.isChanneling(breaker.getUUID()),
				"Ward-Breaking Ritual did not begin its authored spell channel: "
						+ com.powers.knowledge.KnowledgeService.answer(breaker,
						"why did ward breaking fail?").answer());

		ServerPlayer fieldOwner = helper.makeMockServerPlayerInLevel();
		// Keep Dispel outside the powered ward's suppression radius. The spell is
		// supposed to remove the field, not bypass Amethyst's universal cast gate.
		fieldOwner.snapTo(origin.add(50.0, 0.0, 0.0), 0.0F, 0.0F);
		SpellFieldManager.add(SpellFieldKind.KINETIC_WARD, fieldOwner, 200, 3.0, 1);
		ServerPlayer dispeller = helper.makeMockServerPlayerInLevel();
		dispeller.snapTo(origin.add(50.0, 0.0, 1.0), 0.0F, 0.0F);
		dispeller.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_abyssal").getDefaultInstance());
		PlayerPowers.get(dispeller).setSelectedSpell("book_grimoire_abyssal", 1);
		SpellCastingManager.use(dispeller, "book_grimoire_abyssal");
		helper.assertTrue(SpellCastingManager.isChanneling(dispeller.getUUID()),
				"Dispel did not begin its authored spell channel: "
						+ com.powers.knowledge.KnowledgeService.answer(dispeller,
						"why did dispel fail?").answer());

		helper.runAfterDelay(90, () -> {
			helper.assertTrue(AmethystWardBlock.isPowered(helper.getBlockState(ward)),
					"Ward fixture lost power during its ritual");
			helper.assertTrue(com.powers.power.AmethystDampening.findPoweredWard(
					helper.getLevel(), absoluteWard).isEmpty(),
					"Ward-Breaking Ritual did not suppress its locked powered ward");
			breaker.snapTo(Vec3.atBottomCenterOf(absoluteWard).add(0.0, 0.0, 1.0), 0.0F, 0.0F);
			helper.assertFalse(com.powers.power.AmethystDampening.update(breaker),
					"suppressed ward still poisoned its caster through the natural-amethyst index");
			helper.assertFalse(SpellFieldManager.hasField(fieldOwner.getUUID(), SpellFieldKind.KINETIC_WARD),
					"Dispel did not remove the nearest legal field: "
							+ com.powers.knowledge.KnowledgeService.answer(dispeller,
							"why did dispel fail?").answer());
			SpellFieldManager.clearAll();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 260, setupTicks = 300, padding = 128)
	@SuppressWarnings("removal")
	public void celestialRuinCompletesItsSpellChannelBeforeTheIrreversibleWindow(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		caster.snapTo(origin, 0.0F, 0.0F);
		BlockPos focus = new BlockPos(2, 2, 5);
		helper.setBlock(focus, Blocks.STONE);
		helper.assertTrue(CelestialRuinManager.canBegin(helper.getLevel(), helper.absolutePos(focus)),
				"Celestial Ruin fixture was not a legal staging location");
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		PlayerPowers.get(caster).setSelectedSpell("book_grimoire_celestial", 3);
		SpellCastingManager.use(caster, "book_grimoire_celestial");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID()),
				"Celestial Ruin did not begin its authored spell channel");
		helper.runAfterDelay(215, () -> {
			BlockPos absoluteFocus = helper.absolutePos(focus);
			helper.assertTrue(CelestialRuinManager.activeRitualCount(helper.getLevel().getServer()) > 0,
					"Celestial Ruin channel never created its persistent event: "
							+ com.powers.knowledge.KnowledgeService.answer(caster,
							"why did celestial ruin fail?").answer()
							+ " canBegin=" + CelestialRuinManager.canBegin(helper.getLevel(), absoluteFocus)
							+ " safe=" + com.powers.protection.PowerProtection.isSafeZone(
							helper.getLevel(), Vec3.atCenterOf(absoluteFocus))
							+ " energy=" + PlayerPowers.get(caster).energy());
			helper.assertTrue(CelestialRuinManager.cancelNearest(helper.getLevel(),
					Vec3.atCenterOf(absoluteFocus))
					== CelestialRuinCancellation.CANCELLED,
					"Fresh Celestial Ruin was not cancellable before its commit window");
			helper.succeed();
		});
	}

	private static void assertOnce(GameTestHelper helper, MagicPresenceHandle handle,
			ServerLevel level, Vec3 point, long tick, String family) {
		helper.assertTrue(PhysicalMagicPresences.collideNearby(handle, level, point, tick) == 1,
				family + " did not resolve exactly once");
		helper.assertTrue(PhysicalMagicPresences.collideNearby(handle, level, point, tick) == 0,
				family + " repeated inside its collision window");
	}

	@SuppressWarnings("removal")
	private static ServerPlayer crystalPlayer(GameTestHelper helper, BlockPos relativePosition) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(relativePosition));
		player.snapTo(position, 0.0F, 0.0F);
		com.powers.testing.TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		com.powers.testing.TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
		return player;
	}

	private static MagicPresenceHandle projectile(ServerLevel level, ServerPlayer owner,
			String action, Vec3 position, long tick) {
		DarknessFireballProjectile entity = new DarknessFireballProjectile(level, owner, Vec3.ZERO);
		entity.setPos(position);
		level.addFreshEntity(entity);
		return bind(level, entity, owner.getUUID(), action, position, tick,
				MagicPresenceHandle.Kind.PROJECTILE);
	}

	private static MagicPresenceHandle entity(ServerLevel level, ServerPlayer owner,
			String action, Vec3 position, long tick, MagicPresenceHandle.Kind kind) {
		var body = com.powers.PowersEntities.POWER_TEST_ACTOR.create(level,
				net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (body == null) throw new IllegalStateException("Could not create test body");
		body.setPos(position);
		level.addFreshEntity(body);
		return bind(level, body, owner.getUUID(), action, position, tick, kind);
	}

	private static MagicPresenceHandle bind(ServerLevel level, Entity entity, UUID owner,
			String action, Vec3 position, long tick, MagicPresenceHandle.Kind kind) {
		MagicPresenceId id = MagicPresenceId.random();
		MagicRuntime.global().registerPresence(new MagicPresence(id, new MagicActionId(action), owner,
				level.dimension().identifier().toString(), PresenceAnchor.fixed(
				position.x, position.y, position.z), 1.0, tick + 100));
		return PhysicalMagicPresences.bindExistingEntity(id, entity, kind,
				com.powers.time.WorldTick.at(tick + 100));
	}

	private static void move(MagicPresenceHandle handle, ServerLevel level, Vec3 position) {
		MagicRuntime.global().movePresence(handle.presenceId(), level.dimension().identifier().toString(),
				PresenceAnchor.fixed(position.x, position.y, position.z));
	}
}
