package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.entity.FirstVessel;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.mind.MindSessionManager;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.crystals.SoulLinkAbility;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

/** Selected Soul Link routes share real wounds, retain bounded leases, and release authority. */
public final class BossSoulLinkGameTests {
	@GameTest(maxTicks = 320) public void violetLinksLightHerald(GameTestHelper h) { link(h, PowersEntities.LIGHT_HERALD, false); }
	@GameTest(maxTicks = 320) public void violetLinksDarkHerald(GameTestHelper h) { link(h, PowersEntities.DARK_HERALD, false); }
	@GameTest(maxTicks = 320) public void violetLinksFirstVessel(GameTestHelper h) { link(h, PowersEntities.FIRST_VESSEL, false); }
	@GameTest(maxTicks = 320) public void artifactLinksLightHerald(GameTestHelper h) { link(h, PowersEntities.LIGHT_HERALD, true); }
	@GameTest(maxTicks = 320) public void artifactLinksDarkHerald(GameTestHelper h) { link(h, PowersEntities.DARK_HERALD, true); }
	@GameTest(maxTicks = 320) public void artifactLinksFirstVessel(GameTestHelper h) { link(h, PowersEntities.FIRST_VESSEL, true); }

	@SuppressWarnings("removal")
	private static void link(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		caster.setNoGravity(true);
		var origin = helper.absolutePos(new BlockPos(4, 3, 4));
		caster.snapTo(origin.getX() + 0.5, 100, origin.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		if (artifact) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		} else caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.VIOLET_CRYSTAL.getDefaultInstance());
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(6, 3, 4));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(caster.getX() + 2, 100, caster.getZ());
		var donor = helper.spawn(EntityTypes.COW, new BlockPos(4, 3, 6));
		donor.setNoAi(true);
		donor.setNoGravity(true);
		donor.setPos(caster.getX(), 100, caster.getZ() + 2);
		donor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		donor.setHealth(100);
		int energy = data.energy();
		var ability = artifact ? ArtifactWeaponManager.action(ArtifactAlignment.DARKNESS, "crystal/soul_link").ability()
				: CrystalPowerRegistry.get(PowersItems.VIOLET_CRYSTAL);
		if (artifact) {
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS, "crystal/soul_link", -1), "Selection failed");
			helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
					== AbilityActivationService.Result.ACTIVATED, "Artifact Soul Link refused");
		} else helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.VIOLET_CRYSTAL), "Violet Soul Link refused");
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) == 2400,
				"Soul Link bypassed payment/cooldown");
		var snapshot = SoulLinkAbility.snapshots().stream().filter(s -> s.owner().equals(caster.getUUID())).findFirst().orElseThrow();
		helper.assertTrue(snapshot.topology().size() == 2 && snapshot.topology().stream().anyMatch(t -> t.target().equals(boss.getUUID())),
				"Boss and wound donor were not both linked");
		int duration = artifact ? 280 : 200;
		helper.assertTrue(snapshot.ticksLeft() == duration, "Wrong source-specific link duration");
		float initial = vitality(boss);
		float[] afterFirst = {initial};
		float[] afterSecond = {initial};
		helper.runAfterDelay(5, () -> helper.assertTrue(donor.hurtServer(helper.getLevel(), donor.damageSources().generic(), 4), "First wound refused"));
		helper.runAfterDelay(20, () -> {
			afterFirst[0] = vitality(boss);
			helper.assertTrue(afterFirst[0] < initial, "Boss received no mirrored wound");
		});
		helper.runAfterDelay(duration - 30, () -> {
			helper.assertTrue(vitality(boss) == afterFirst[0], "Mirrored wound bounced without a fresh source wound");
			helper.assertTrue(donor.hurtServer(helper.getLevel(), donor.damageSources().generic(), 4), "Late wound refused");
		});
		helper.runAfterDelay(duration - 15, () -> {
			afterSecond[0] = vitality(boss);
			helper.assertTrue(afterSecond[0] < afterFirst[0], "Link stopped mirroring before its authored lease");
		});
		helper.runAfterDelay(duration + 5, () -> {
			helper.assertTrue(SoulLinkAbility.snapshots().stream().noneMatch(s -> s.owner().equals(caster.getUUID())), "Expired topology retained");
			helper.assertTrue(MindSessionManager.activeFor(caster.getUUID()).isEmpty()
					&& MindSessionManager.activeFor(boss.getUUID()).isEmpty()
					&& MindSessionManager.activeFor(donor.getUUID()).isEmpty(), "Expired link retained participant authority");
			helper.assertTrue(donor.hurtServer(helper.getLevel(), donor.damageSources().generic(), 4), "Post-expiry wound refused");
		});
		helper.runAfterDelay(duration + 15, () -> {
			helper.assertTrue(vitality(boss) == afterSecond[0], "Expired link mirrored fresh damage");
			helper.succeed();
		});
	}

	private static float vitality(Mob boss) {
		return boss instanceof FirstVessel vessel ? vessel.effectiveHealth() : boss.getHealth();
	}
}
