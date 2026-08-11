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
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.spell.CelestialRuinCancellation;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellEffect;
import com.powers.spell.SpellFieldKind;
import com.powers.spell.SpellFieldManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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

import java.util.UUID;

/** Live acceptance cases added while closing the P0/P1 release backlog. */
public final class P1AcceptanceGameTests {
	private static final double FAR_OFFSET = 40.0;

	public P1AcceptanceGameTests() {
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
				center, 2.0, tick + 100, MagicPresenceHandle.Kind.FIELD);
		move(projectile, level, center);
		assertOnce(helper, projectile, level, center, tick, "projectile/field");
		PhysicalMagicPresences.remove(projectile);

		helper.assertTrue(MagicRayCollisionRuntime.publish(level, "energy_beam", firstOwner.getUUID(),
				center.add(-4, 0, 0), center.add(4, 0, 0), tick + 20).isPresent(),
				"Beam/field geometry did not reach the live resolver");
		PhysicalMagicPresences.remove(projectileField);

		MagicPresenceHandle force = PhysicalMagicPresences.registerFixed(
				new MagicActionId("darkness_block"), firstOwner.getUUID(), level,
				center, 1.0, tick + 100, MagicPresenceHandle.Kind.FORCE_BLOCK);
		MagicPresenceHandle impact = PhysicalMagicPresences.registerFixed(
				new MagicActionId("fireball"), secondOwner.getUUID(), level,
				center.add(FAR_OFFSET, 0, 0), 1.0, tick + 100, MagicPresenceHandle.Kind.IMPACT);
		move(impact, level, center);
		assertOnce(helper, impact, level, center, tick + 40, "force/block");
		PhysicalMagicPresences.remove(force);
		PhysicalMagicPresences.remove(impact);

		MagicPresenceHandle body = entity(level, firstOwner, "astral_projection",
				center.add(FAR_OFFSET, 0, 0), tick, MagicPresenceHandle.Kind.ENTITY);
		MagicPresenceHandle bodyField = PhysicalMagicPresences.registerFixed(
				new MagicActionId("dimensional_anchor"), secondOwner.getUUID(), level,
				center, 2.0, tick + 100, MagicPresenceHandle.Kind.FIELD);
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
		SpellCastingManager.use(caster, "book_grimoire_deep");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID()),
				"Dimensional Anchor did not begin its interruptible channel");
		helper.assertTrue(PlayerPowers.get(caster).energy() == before - 22,
				"Ritual did not reserve its exact authored payment");
		caster.snapTo(origin.add(2.0, 0.0, 0.0), 0.0F, 0.0F);
		helper.runAfterDelay(3, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()),
					"Movement did not interrupt the ritual");
			helper.assertTrue(PlayerPowers.get(caster).energy() == before - 11,
					"Interrupted ritual did not retain exactly half its payment");
			helper.assertFalse(DimensionalAnchorAbility.isAnchored(target),
					"Interrupted ritual executed its effect");
			helper.succeed();
		});
	}

	@GameTest(padding = 128)
	public void all262CataloguedItemsExistInTheLiveServerRegistry(GameTestHelper helper) {
		var registered = net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
				.filter(entry -> entry.getKey().identifier().getNamespace().equals("powers"))
				.toList();
		helper.assertTrue(registered.size() == 262,
				"Live POWERS item registry differs from the 262-row catalogue: " + registered.size());
		for (var entry : registered) {
			helper.assertFalse(entry.getValue().getDefaultInstance().isEmpty(),
					"Registered item has no usable default stack: " + entry.getKey().identifier());
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 120, padding = 128)
	@SuppressWarnings("removal")
	public void abyssalWardBreakingAndDispelMutateOnlyTheirLockedTargets(GameTestHelper helper) {
		ServerPlayer breaker = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
		breaker.snapTo(origin, 0.0F, 0.0F);
		BlockPos ward = new BlockPos(2, 2, 5);
		helper.setBlock(ward, PowersBlocks.AMETHYST_WARD.defaultBlockState()
				.setValue(BlockStateProperties.POWER, 15));
		helper.setBlock(new BlockPos(3, 2, 5), Blocks.REDSTONE_BLOCK);
		breaker.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_abyssal").getDefaultInstance());
		PlayerPowers.get(breaker).setSelectedSpell("book_grimoire_abyssal", 0);
		SpellCastingManager.use(breaker, "book_grimoire_abyssal");

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
					helper.getLevel(), ward).isEmpty(),
					"Ward-Breaking Ritual did not suppress its locked powered ward");
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
		return PhysicalMagicPresences.bindExistingEntity(id, entity, kind, tick + 100);
	}

	private static void move(MagicPresenceHandle handle, ServerLevel level, Vec3 position) {
		MagicRuntime.global().movePresence(handle.presenceId(), level.dimension().identifier().toString(),
				PresenceAnchor.fixed(position.x, position.y, position.z));
	}
}
