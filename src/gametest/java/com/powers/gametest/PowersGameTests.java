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
import com.powers.companion.ShadowCompanionEntity;
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
import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.LightningConductanceRuntime;
import com.powers.power.abilities.LightningStrikeRules;
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

/** Small runtime suite for mechanics that pure unit tests cannot exercise. */
public final class PowersGameTests {
	public PowersGameTests() {
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
		helper.assertTrue(BodyProxyManager.activeProxyCount() == 0,
				"Fatal astral damage leaked a body proxy or forced-chunk ticket");
		helper.runAfterDelay(2, () -> {
			helper.assertFalse(owner.isAlive(),
					"Recalled fatal damage was not replayed through vanilla death");
			helper.succeed();
		});
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
		helper.assertTrue(BodyProxyManager.activeProxyCount() == 0,
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
		var mob = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(2, 1, 3));
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
							+ ", travel=" + com.powers.power.travel.TravelChunkLoader.diagnostics());
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
				var returnedZombies = helper.getLevel().getEntitiesOfClass(
						net.minecraft.world.entity.monster.zombie.Zombie.class,
						net.minecraft.world.phys.AABB.ofSize(
								net.minecraft.world.phys.Vec3.atCenterOf(origin), 16.0, 16.0, 16.0));
				helper.assertTrue(helper.getLevel().getEntity(mobId) != null,
						"Nearby tracked mob did not return to its recorded origin; nether="
								+ (stranded == null ? "missing" : stranded.position())
								+ ", tracked=" + (stranded instanceof net.minecraft.world.entity.LivingEntity living
								&& com.powers.power.travel.MindscapeMobReturnTracker.tracked(living))
								+ ", returned=" + returnedZombies.stream().map(entity ->
								entity.getUUID() + "@" + entity.position()).toList());
				PrivateCompanionManager.handleChat(caster, "shadow, leave me");
			}));
		});
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
