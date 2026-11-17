package com.powers.gametest;

import com.powers.ImportedPackItems;
import com.powers.PowersEntities;
import com.powers.player.PlayerPowers;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/** Real held-grimoire selection, channel completion, and target-specific boss effects. */
public final class BossSpellResponseGameTests {
	@GameTest(maxTicks = 80)
	public void anchorRejectsLightHeraldWithoutCharging(GameTestHelper helper) {
		anchorRefusal(helper, PowersEntities.LIGHT_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void anchorRejectsDarkHeraldWithoutCharging(GameTestHelper helper) {
		anchorRefusal(helper, PowersEntities.DARK_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void anchorRejectsFirstVesselWithoutCharging(GameTestHelper helper) {
		anchorRefusal(helper, PowersEntities.FIRST_VESSEL);
	}
	@GameTest(maxTicks = 80)
	public void anchorStillBindsCompatibleActor(GameTestHelper helper) {
		anchorRoute(helper, PowersEntities.POWER_TEST_ACTOR, true);
	}

	private static void anchorRefusal(GameTestHelper helper, EntityType<? extends Mob> type) {
		anchorRoute(helper, type, false);
	}

	@SuppressWarnings("removal")
	private static void anchorRoute(GameTestHelper helper, EntityType<? extends Mob> type, boolean supported) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0);
		caster.setXRot(0);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_deep").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_deep", 0);
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setPos(caster.getX(), caster.getY(), caster.getZ() + 3);
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		var messages = BossControlResponseGameTests.capture(caster);
		int energy = data.energy();
		long previousCooldown = data.cooldownReadyAt("spell:dimensional_anchor");
		long committedCooldown = caster.level().getGameTime() + 1200;
		SpellCastingManager.use(caster, "book_grimoire_deep");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID())
				&& data.energy() == energy - 22,
				"Anchor did not acquire and reserve the visible boss-targeted cast");
		helper.runAfterDelay(45, () -> {
			if (supported) {
				helper.assertTrue(!SpellCastingManager.isChanneling(caster.getUUID())
						&& data.cooldownReadyAt("spell:dimensional_anchor") == committedCooldown
						&& com.powers.power.abilities.DimensionalAnchorAbility.anchorDimension(boss)
						== helper.getLevel().dimension(), "Compatible actor did not retain its committed anchor");
				helper.assertTrue(messages.stream().anyMatch(packet -> packet.content().getContents()
						instanceof net.minecraft.network.chat.contents.TranslatableContents text
						&& text.getKey().equals("spell.powers.cast")), "Compatible anchor gave no success feedback");
				helper.succeed();
				return;
			}
			helper.assertTrue(!SpellCastingManager.isChanneling(caster.getUUID())
					&& data.energy() == energy
					&& data.cooldownReadyAt("spell:dimensional_anchor") == previousCooldown,
					"Unsupported boss target consumed payment, cooldown or channel state");
			helper.assertTrue(!com.powers.power.abilities.DimensionalAnchorAbility.isAnchored(boss)
					&& boss.getHealth() == 37F, "Unsupported boss target acquired an anchor or lost health");
			helper.assertTrue(messages.stream().anyMatch(packet -> packet.content().getContents()
					instanceof net.minecraft.network.chat.contents.TranslatableContents text
					&& text.getKey().equals("spell.powers.failed")), "Anchor rejection gave no caster feedback");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void verdantTendsTerrainNotLightHerald(GameTestHelper helper) {
		verdant(helper, PowersEntities.LIGHT_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void verdantTendsTerrainNotDarkHerald(GameTestHelper helper) {
		verdant(helper, PowersEntities.DARK_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void verdantTendsTerrainNotFirstVessel(GameTestHelper helper) {
		verdant(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void verdant(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_wild").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_wild", 1);
		data.forceRestoreEnergy();
		var soil = new BlockPos(3, 1, 4);
		helper.setBlock(soil, net.minecraft.world.level.block.Blocks.FARMLAND);
		helper.setBlock(soil.above(), net.minecraft.world.level.block.Blocks.WHEAT);
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
		int energy = data.energy();
		long cooldown = caster.level().getGameTime() + 600;
		SpellCastingManager.use(caster, "book_grimoire_wild");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID())
				&& data.energy() == energy - 22 && data.cooldownReadyAt("spell:verdant_tending") == cooldown,
				"Verdant Tending bypassed its channel, payment or cooldown");
		helper.runAfterDelay(45, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()), "Verdant channel did not finish");
			helper.assertTrue(helper.getBlockState(soil).getValue(
					net.minecraft.world.level.block.FarmlandBlock.MOISTURE) == 7,
					"Verdant Tending did not hydrate dry farmland near the boss");
			helper.assertTrue(helper.getBlockState(soil.above()).getValue(
					net.minecraft.world.level.block.CropBlock.AGE) > 0,
					"Verdant Tending did not grow the crop near the boss");
			helper.assertTrue(boss.getHealth() == 37F && boss.hasEffect(MobEffects.STRENGTH)
					&& data.cooldownReadyAt("spell:verdant_tending") == cooldown,
					"Terrain support changed the boss or rolled back its committed cooldown");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void sanctuaryWardsLightHeraldIndependently(GameTestHelper helper) {
		sanctuary(helper, PowersEntities.LIGHT_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void sanctuaryWardsDarkHeraldIndependently(GameTestHelper helper) {
		sanctuary(helper, PowersEntities.DARK_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void sanctuaryWardsFirstVesselIndependently(GameTestHelper helper) {
		sanctuary(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void sanctuary(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_wild").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_wild", 2);
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 6));
		boss.setPos(caster.getX(), caster.getY(), caster.getZ() + 2);
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		var outside = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(8, 2, 4));
		outside.setNoAi(true);
		outside.setNoGravity(true);
		outside.setPos(caster.getX() + 4, caster.getY(), caster.getZ());
		int energy = data.energy();
		long cooldown = caster.level().getGameTime() + 1000;
		SpellCastingManager.use(caster, "book_grimoire_wild");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID())
				&& data.energy() == energy - 28 && data.cooldownReadyAt("spell:hearth_sanctuary") == cooldown,
				"Sanctuary bypassed channel, payment or cooldown");
		helper.runAfterDelay(45, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()), "Sanctuary channel did not finish");
			var shields = com.powers.power.state.MagicShieldManager.global();
			long now = helper.getLevel().getServer().getTickCount();
			helper.assertTrue(shields.snapshot(caster.getUUID(), now).orElseThrow().integrity() == 40F
					&& shields.snapshot(boss.getUUID(), now).orElseThrow().integrity() == 40F,
					"Sanctuary did not grant independent 40-integrity wards");
			helper.assertFalse(shields.active(outside.getUUID(), now), "Sanctuary exceeded its three-block radius");
			var before = com.powers.power.damage.LayeredDamageLedger.global().snapshot();
			boss.hurtServer(helper.getLevel(), boss.damageSources().generic(), 50_000F);
			var after = com.powers.power.damage.LayeredDamageLedger.global().snapshot();
			helper.assertTrue(after.incomingDamage() - before.incomingDamage() == 50_000
					&& after.wardBlockedDamage() - before.wardBlockedDamage() == 50_000
					&& after.wardIntegritySpent() - before.wardIntegritySpent() == 40,
					"Damage accounting did not preserve the original hit and finite shield cost");
			helper.assertTrue(boss.getHealth() == 37F && !shields.active(boss.getUUID(), now)
					&& shields.snapshot(caster.getUUID(), now).orElseThrow().integrity() == 40F,
					"Boss ward failed its sacrificial hit or consumed the caster's independent ward: health="
							+ boss.getHealth() + ", boss=" + shields.snapshot(boss.getUUID(), now)
							+ ", caster=" + shields.snapshot(caster.getUUID(), now));
			com.powers.power.abilities.ForcefieldAbility.raiseSpellWard(helper.getLevel(), boss, 40F);
			boss.hurtServer(helper.getLevel(), boss.damageSources().generic(), 1F);
			helper.assertTrue(shields.snapshot(boss.getUUID(), now).orElseThrow().integrity() == 39F
					&& boss.getHealth() == 37F, "A subsequent small hit reused stale original damage");
			helper.assertTrue(data.cooldownReadyAt("spell:hearth_sanctuary") == cooldown, "Sanctuary cooldown changed");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void purificationSupportsCasterNotLightHerald(GameTestHelper helper) {
		purification(helper, PowersEntities.LIGHT_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void purificationSupportsCasterNotDarkHerald(GameTestHelper helper) {
		purification(helper, PowersEntities.DARK_HERALD);
	}
	@GameTest(maxTicks = 80)
	public void purificationSupportsCasterNotFirstVessel(GameTestHelper helper) {
		purification(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void purification(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_wild").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_wild", 0);
		data.forceRestoreEnergy();
		caster.setHealth(5F);
		caster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0));
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		boss.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0));
		helper.assertTrue(boss.hasEffect(MobEffects.SLOWNESS), "Boss fixture did not acquire its debuff");
		int energy = data.energy();
		long cooldown = caster.level().getGameTime() + 600;
		SpellCastingManager.use(caster, "book_grimoire_wild");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID())
				&& data.energy() == energy - 20 && data.cooldownReadyAt("spell:purification_circle") == cooldown,
				"Purification did not reserve its channel, payment and cooldown");
		helper.runAfterDelay(55, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()), "Purification channel did not finish");
			helper.assertTrue(caster.getHealth() == 9F && !caster.hasEffect(MobEffects.SLOWNESS),
					"Purification failed its authored four-point heal or cleanse");
			helper.assertTrue(boss.getHealth() == 37F && boss.hasEffect(MobEffects.SLOWNESS),
					"Purification healed or cleansed an opposing boss");
			helper.assertTrue(data.cooldownReadyAt("spell:purification_circle") == cooldown,
					"Purification rolled back or restarted cooldown");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 50)
	public void bloodReadingReportsLightHeraldFacts(GameTestHelper helper) {
		bloodReading(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 50)
	public void bloodReadingReportsDarkHeraldFacts(GameTestHelper helper) {
		bloodReading(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 50)
	public void bloodReadingReportsFirstVesselFacts(GameTestHelper helper) {
		bloodReading(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void bloodReading(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0);
		caster.setXRot(0);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_blight").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_blight", 0);
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(caster.getX(), caster.getY(), caster.getZ() + 3);
		boss.setHealth(37F);
		boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
		float maximum = boss.getMaxHealth();
		var messages = BossControlResponseGameTests.capture(caster);
		int energy = data.energy();
		long cooldown = caster.level().getGameTime() + 200;
		SpellCastingManager.use(caster, "book_grimoire_blight");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID()), "Blood Reading did not start channeling");
		helper.assertTrue(data.energy() == energy - 12 && data.cooldownReadyAt("spell:blood_reading") == cooldown,
				"Blood Reading bypassed authored payment or cooldown");
		helper.runAfterDelay(25, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()), "Blood Reading channel did not finish");
			var reports = messages.stream().map(packet -> packet.content().getContents())
					.filter(net.minecraft.network.chat.contents.TranslatableContents.class::isInstance)
					.map(net.minecraft.network.chat.contents.TranslatableContents.class::cast).toList();
			helper.assertTrue(reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.blood_reading.vitals")
					&& text.getArgs().length == 4 && text.getArgs()[1] instanceof Number health
					&& health.floatValue() == 37F && text.getArgs()[2] instanceof Number max
					&& max.floatValue() == maximum), "Blood Reading did not report the boss's actual vitals");
			helper.assertTrue(reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.blood_reading.effects")
					&& text.getArgs().length == 1 && text.getArgs()[0].toString().contains("minecraft:strength")),
					"Blood Reading omitted the boss's active effect");
			helper.assertTrue(boss.getHealth() == 37F && boss.hasEffect(MobEffects.STRENGTH)
					&& data.cooldownReadyAt("spell:blood_reading") == cooldown,
					"Reading changed the boss or rolled back its committed cooldown");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 50)
	public void dispelStripsLightHeraldBuffs(GameTestHelper helper) {
		dispel(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 50)
	public void dispelStripsDarkHeraldBuffs(GameTestHelper helper) {
		dispel(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 50)
	public void dispelStripsFirstVesselBuffs(GameTestHelper helper) {
		dispel(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void dispel(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0);
		caster.setXRot(0);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_abyssal").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_abyssal", 1);
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(caster.getX(), caster.getY(), caster.getZ() + 3);
		boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
		var bystander = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(7, 2, 4));
		bystander.setNoAi(true);
		bystander.setNoGravity(true);
		bystander.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
		helper.assertTrue(boss.hasEffect(MobEffects.STRENGTH), "Boss fixture rejected its buff");
		int energy = data.energy();
		long cooldown = caster.level().getGameTime() + 500;
		SpellCastingManager.use(caster, "book_grimoire_abyssal");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID()), "Dispel did not begin its real channel");
		helper.assertTrue(data.energy() == energy - 18 && data.cooldownReadyAt("spell:dispel") == cooldown,
				"Dispel did not reserve its authored payment and cooldown");
		helper.runAfterDelay(25, () -> {
			helper.assertFalse(SpellCastingManager.isChanneling(caster.getUUID()), "Dispel channel never completed");
			helper.assertFalse(boss.hasEffect(MobEffects.STRENGTH), "Dispel did not remove the targeted boss buff");
			helper.assertTrue(bystander.hasEffect(MobEffects.STRENGTH), "Dispel affected an untargeted bystander");
			helper.assertTrue(data.cooldownReadyAt("spell:dispel") == cooldown, "Dispel rolled back or extended its cooldown");
			helper.succeed();
		});
	}
}
