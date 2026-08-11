package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
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
import com.powers.entity.FirstVessel;
import com.powers.entity.PowerTestActor;
import com.powers.entity.TestActorPowerState;
import com.powers.entity.RealmHerald;
import com.powers.boss.FirstVesselPowerCatalogue;
import com.powers.boss.FirstVesselRitual;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.FantasyWeaponItem;
import com.powers.player.SkillSystem;
import com.powers.player.PlayerGuide;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.power.PowerDamage;
import com.powers.item.ArtifactWeaponManager;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.FireballAbility;
import com.powers.power.abilities.PlantHealingAbility;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.TeleportAbility;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.network.VesselControlPackets;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
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
import com.powers.spell.SpellRegistry;
import com.powers.spell.SpellCastingManager;
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
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.effect.MobEffects;

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

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void shadowChatOwnsVisibilityAndFormerBookKnowledge(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.addTag(SkillSystem.DARKNESS_TAG);
		player.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(PrivateCompanionManager.handleChat(player,
				"shadow, reveal yourself"), "Explicit Shadow chat leaked into ordinary chat");
		PrivateCompanionManager.tickPlayer(player, 0);
		helper.assertTrue(PrivateCompanionManager.activeSessionCount() == 1,
				"Shadow address did not create exactly one lightweight session");
		helper.assertTrue(PrivateCompanionManager.isRevealed(player.getUUID()),
				"Reveal command did not change global visibility");
		var answer = KnowledgeService.answer(player, "How do the crystals work?");
		helper.assertTrue(!answer.answer().isBlank() && KnowledgeService.entryCount() > 0,
				"Shadow could not answer the former curated-book question offline");
		PrivateCompanionManager.handleChat(player, "shadow, hide yourself");
		helper.assertFalse(PrivateCompanionManager.isRevealed(player.getUUID()),
				"Hide command left global visibility enabled");
		PrivateCompanionManager.handleChat(player, "shadow, leave me");
		helper.assertTrue(PrivateCompanionManager.activeSessionCount() == 0,
				"Dismiss command leaked a Shadow session");
		helper.assertFalse(PrivateCompanionManager.handleChat(player, "ordinary chat"),
				"Unrelated signed chat was intercepted");
		PrivateCompanionManager.clear();
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

	@GameTest
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void rankZeroCombatImpactStillLeavesBoundedTerrainDamage(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos center = new BlockPos(5, 2, 5);
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) helper.setBlock(center.offset(x, 0, z), Blocks.STONE);
		}
		Vec3 impact = Vec3.atCenterOf(helper.absolutePos(center));
		int removed = CombatTerrainImpact.crater(helper.getLevel(), caster, impact, 0);

		helper.assertTrue(removed > 0, "A rank-zero offensive impact left no terrain damage");
		helper.assertTrue(removed <= com.powers.power.abilities.CombatTerrainRules.craterBudget(0),
				"Combat crater exceeded its rank-zero work budget");
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
	public void shadowSwordLightningCreatesAVisibleBolt(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(2, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		caster.addTag(SkillSystem.DARKNESS_TAG);
		com.powers.player.PlayerPowers.get(caster).setDarknessLevel(caster, 10);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				PowersWeapons.weapon("lycanbane").getDefaultInstance());
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 6));
		target.setNoAi(true);
		helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS,
				"innate/lightning_strike", -1), "Shadow Sword rejected Lightning");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Shadow Sword Lightning activation pipeline failed");
		boolean[] observedBolt = {false};
		for (int delay = 7; delay <= 12; delay++) {
			helper.runAfterDelay(delay, () -> observedBolt[0] |= !helper.getLevel().getEntitiesOfClass(
					LightningBolt.class, caster.getBoundingBox().inflate(12.0), entity -> true).isEmpty());
		}
		helper.runAfterDelay(13, () -> {
			helper.assertTrue(observedBolt[0], "Shadow Sword Lightning did not create a visible bolt");
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
				1.0F, 0.0F, false, false, 0.0F, 0.0F, 0, -1));
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
				1.0F, 0.0F, false, false, 0.0F, 0.0F, 0, -1));
		helper.assertTrue(host.getZ() > before, "Dreamwalking remained camera-only");
		helper.assertTrue(VesselPossessionAbility.stopDreamwalking(caster),
				"Dreamwalking could not be toggled off");
		helper.assertFalse(host.isNoAi(), "Dreamwalking did not restore the host's AI");
		helper.succeed();
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
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
		var darkRealm = helper.getLevel().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						com.powers.PowersMod.id("dark_realm")));
		// Fabric's isolated GameTestServer intentionally creates only vanilla
		// levels. Dedicated-server boot verification covers datapack dimensions;
		// exercise the complete transfer here whenever the harness supplies it.
		if (darkRealm == null) {
			helper.assertTrue(CrystalPowerRegistry.get(PowersItems.DARK_CRYSTAL) != null,
					"Dark Crystal was not bound to its realm action");
			TestingOverrides.clear(player.getUUID());
			helper.succeed();
			return;
		}

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

	@GameTest(maxTicks = 220)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void lightCrystalUsesTheSameAuthenticatedMindscapePipeline(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		TestingOverrides.setEnergyDisabled(player.getUUID(), true);
		TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
		var lightRealm = helper.getLevel().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						com.powers.PowersMod.id("light_realm")));
		if (lightRealm == null) {
			helper.assertTrue(CrystalPowerRegistry.get(PowersItems.LIGHT_CRYSTAL) != null,
					"Light Crystal was not bound to its realm action");
			TestingOverrides.clear(player.getUUID());
			helper.succeed();
			return;
		}

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

	@GameTest(maxTicks = 80)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level ServerPlayer test factory.
	public void auguryCompletesAsAnUnrankedPracticalRitual(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, com.powers.ImportedPackItems.item(
				"imported_book_grimoire_celestial").getDefaultInstance());
		var powers = com.powers.player.PlayerPowers.get(player);
		powers.setSelectedSpell("book_grimoire_celestial", 1);
		SpellCastingManager.use(player, "book_grimoire_celestial");
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(powers.cooldownReadyAt("spell:augury") > player.level().getGameTime(),
					"Augury did not commit its authored payment and cooldown");
			helper.assertFalse(SpellCastingManager.isChanneling(player.getUUID()),
					"Augury remained stuck in its ritual channel");
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
		BlockPos fire = new BlockPos(2, 1, 3);
		helper.setBlock(crop, Blocks.WHEAT);
		helper.setBlock(farmland, Blocks.FARMLAND);
		helper.setBlock(fire, Blocks.FIRE);
		SpellCastingManager.use(player, "book_grimoire_wild");
		helper.runAfterDelay(50, () -> {
			helper.assertFalse(helper.getBlockState(crop).equals(Blocks.WHEAT.defaultBlockState()),
					"Verdant Tending did not grow a valid crop");
			helper.assertTrue(helper.getBlockState(farmland).getValue(
					net.minecraft.world.level.block.FarmlandBlock.MOISTURE)
					== net.minecraft.world.level.block.FarmlandBlock.MAX_MOISTURE,
					"Verdant Tending did not hydrate farmland");
			helper.assertBlockPresent(Blocks.AIR, fire);
			helper.succeed();
		});
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
		helper.assertTrue(testing != null && testing.getChild("coverage") != null
				&& testing.getChild("arena") != null,
				"The manual acceptance coverage/arena commands were not registered");
		helper.succeed();
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
		var data = com.powers.player.PlayerPowers.get(caster);
		data.setSlots(caster, java.util.List.of(
				"powers:time_shift", "powers:flight", "powers:forcefield"));
		BlockPos destination = helper.absolutePos(new BlockPos(7, 1, 7));
		helper.setBlock(new BlockPos(7, 0, 7), Blocks.STONE);
		TeleportAbility ability = new TeleportAbility();
		helper.assertTrue(com.powers.power.AbilityActivationService.activateTeleport(
				caster, actor, ability, helper.getLevel().dimension(),
				destination.getX(), destination.getY(), destination.getZ(), false)
				== com.powers.power.AbilityActivationService.Result.ACTIVATED,
				"Time Shift rejected a player-compatible test actor");
		helper.runAfterDelay(75, () -> {
			helper.assertTrue(actor.position().distanceToSqr(
					new Vec3(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5)) < 1.0,
					"Time Shift did not move the test actor to the selected destination");
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
		helper.assertTrue(MagicRuntime.catalogue().definitions().size() == 64
				&& MagicRuntime.global().interactionCount() == 2_080,
				"The exhaustive live magic-collision kernel was incomplete");
		helper.succeed();
	}

}
