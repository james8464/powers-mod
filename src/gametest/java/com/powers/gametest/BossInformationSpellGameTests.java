package com.powers.gametest;

import com.powers.ImportedPackItems;
import com.powers.PowersEntities;
import com.powers.player.PlayerPowers;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Utility spells report real server facts without mutating nearby bosses. */
public final class BossInformationSpellGameTests {
	@GameTest(maxTicks = 60)
	public void auguryReportsFactsNearLightHerald(GameTestHelper helper) {
		information(helper, PowersEntities.LIGHT_HERALD, true);
	}
	@GameTest(maxTicks = 60)
	public void auguryReportsFactsNearDarkHerald(GameTestHelper helper) {
		information(helper, PowersEntities.DARK_HERALD, true);
	}
	@GameTest(maxTicks = 60)
	public void auguryReportsFactsNearFirstVessel(GameTestHelper helper) {
		information(helper, PowersEntities.FIRST_VESSEL, true);
	}
	@GameTest(maxTicks = 60)
	public void graveRecallReportsDeathNearLightHerald(GameTestHelper helper) {
		information(helper, PowersEntities.LIGHT_HERALD, false);
	}
	@GameTest(maxTicks = 60)
	public void graveRecallReportsDeathNearDarkHerald(GameTestHelper helper) {
		information(helper, PowersEntities.DARK_HERALD, false);
	}
	@GameTest(maxTicks = 60)
	public void graveRecallReportsDeathNearFirstVessel(GameTestHelper helper) {
		information(helper, PowersEntities.FIRST_VESSEL, false);
	}

	@SuppressWarnings("removal")
	private static void information(GameTestHelper helper, EntityType<? extends Mob> type, boolean augury) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		var death = origin.offset(-2, 1, -2);
		caster.setPos(death.getX() + 0.75, death.getY(), death.getZ() + 0.25);
		var data = PlayerPowers.get(caster);
		data.recordDeath(caster);
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		String book = augury ? "book_grimoire_celestial" : "book_grimoire_blight";
		String spell = augury ? "augury" : "grave_recall";
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_" + book).getDefaultInstance());
		data.setSelectedSpell(book, 1);
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
		var messages = BossControlResponseGameTests.capture(caster);
		int energy = data.energy();
		long cooldown = caster.level().getGameTime() + (augury ? 600 : 200);
		SpellCastingManager.use(caster, book);
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID()) == augury,
				"Information spell used the wrong channel behavior");
		helper.assertTrue(data.energy() == energy - (augury ? 16 : 10)
				&& data.cooldownReadyAt("spell:" + spell) == cooldown,
				"Information spell did not commit its authored cost and cooldown");
		helper.runAfterDelay(25, () -> {
			var reports = messages.stream().map(packet -> packet.content().getContents())
					.filter(TranslatableContents.class::isInstance).map(TranslatableContents.class::cast).toList();
			if (augury) {
				String weather = helper.getLevel().isThundering() ? "thunder"
						: helper.getLevel().isRaining() ? "rain" : "clear";
				helper.assertTrue(reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.augury.sky")
						&& text.getArgs().length == 2 && weather.equals(text.getArgs()[0])),
						"Augury did not report the actual server weather");
				helper.assertTrue(reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.augury.event.unknown"))
						&& reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.augury.uncertainty")),
						"Augury invented a realm event or omitted loaded-only uncertainty");
			} else {
				helper.assertTrue(reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.grave_recall.coordinates")
						&& java.util.Arrays.equals(text.getArgs(), new Object[]{death.getX(), death.getY(), death.getZ()})),
						"Grave Recall reported current position or boss position instead of recorded death");
				helper.assertTrue(reports.stream().anyMatch(text -> text.getKey().equals("spell.powers.grave_recall.dimension")
						&& java.util.Arrays.equals(text.getArgs(), new Object[]{helper.getLevel().dimension().identifier().toString()})),
						"Grave Recall did not report the death dimension");
			}
			helper.assertTrue(boss.getHealth() == 37F && boss.hasEffect(MobEffects.STRENGTH)
					&& data.cooldownReadyAt("spell:" + spell) == cooldown
					&& !SpellCastingManager.isChanneling(caster.getUUID()),
					"Information spell changed the boss, leaked a channel or rolled back its cooldown");
			helper.succeed();
		});
	}
}
