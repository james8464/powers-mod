package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.entity.PowerTestActor;
import com.powers.player.PlayerPowers;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.Power;
import com.powers.power.PowerAbilityRuntime;
import com.powers.power.PowerEnergy;
import com.powers.power.PowerRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;

/** Live-world probes for innates that previously had registry/rule evidence only. */
public final class LiveActionAcceptanceGameTests {
	private static final List<String> SAFE_ACTIONS = List.of(
			"size_shift", "starfall", "thunderclap", "speed_burst", "telekinesis",
			"super_speed", "breezy_bash", "invisibility", "gravity_displacement",
			"ice_manipulation", "double_health");

	@GameTest(maxTicks = 200)
	@SuppressWarnings("removal")
	public void uncoveredInnatesEnterTheirRealAbilityPipelines(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 10);

		for (String id : SAFE_ACTIONS) {
			caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
			caster.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			caster.setYRot(0.0F);
			caster.setXRot(0.0F);
			Power power = PowerRegistry.get(id);
			helper.assertTrue(power != null, "Missing innate " + id);
			data.setSlots(caster, List.of("powers:" + id, "powers:flight", "powers:forcefield"));
			data.forceRestoreEnergy();
			if ("size_shift".equals(id)) {
				helper.assertTrue(power.ability().selectOption(caster, data, 0),
						"Size Shift test form was not selectable at rank 10");
			}
			LivingEntity target = "breezy_bash".equals(id)
					? helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 4, 5))
					: helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 2, 7));
			target.setCustomName(net.minecraft.network.chat.Component.literal("Target_" + id));
			if ("breezy_bash".equals(id)) {
				target.setNoGravity(true);
			}
			int energyBefore = data.energy();
			int expectedCost = PowerEnergy.cost(caster, power.ability());
			int presencesBefore = MagicRuntime.global().activePresenceCount();
			try {
				AbilityActivationService.Result result = AbilityActivationService.activate(
						caster, power.ability(), power.id().toString());
				helper.assertTrue(result == AbilityActivationService.Result.ACTIVATED,
						"Authoritative activation pipeline rejected " + id);
				helper.assertTrue(data.energy() == energyBefore - expectedCost,
						"Authoritative energy payment differed for " + id);
				helper.assertTrue(MagicRuntime.global().activePresenceCount() > presencesBefore,
						"Successful cast did not commit magic presence for " + id);
				assertImmediateEffect(helper, id, caster, target, data);
				if (power.ability().isToggle()) {
					helper.assertTrue(data.isToggleActive(power.id().toString()),
							"Toggle ownership was not committed for " + id);
					helper.assertTrue(AbilityActivationService.activate(caster, power.ability(),
							power.id().toString()) == AbilityActivationService.Result.ACTIVATED,
							"Toggle cleanup was rejected for " + id);
					helper.assertFalse(data.isToggleActive(power.id().toString()),
							"Toggle ownership survived cleanup for " + id);
				} else if (power.ability().cooldownTicksFor(caster, data) > 0) {
					helper.assertTrue(ActivationCooldowns.remainingTicks(caster, power.ability()) > 0,
							"Successful cast did not start cooldown for " + id);
				}
			} finally {
				PowerAbilityRuntime.rollbackFailedActivation(caster, id);
				MagicRuntime.global().clearOwner(caster.getUUID());
				if (target.isAlive()) target.discard();
			}
		}
		helper.succeed();
	}

	private static void assertImmediateEffect(GameTestHelper helper, String id,
			ServerPlayer caster, LivingEntity target, PlayerPowers.PlayerPowersData data) {
		switch (id) {
			case "size_shift" -> helper.assertTrue(caster.getAttributeValue(
					net.minecraft.world.entity.ai.attributes.Attributes.SCALE) != 1.0,
					"Size Shift did not change the caster model scale");
			case "speed_burst" -> helper.assertTrue(caster.getDeltaMovement().lengthSqr() > 0.0,
					"Speed Burst did not launch the caster");
			case "telekinesis", "breezy_bash" -> helper.assertTrue(
					target.getDeltaMovement().lengthSqr() > 0.0,
					id + " did not move its live target");
			case "invisibility" -> helper.assertTrue(caster.hasEffect(
					net.minecraft.world.effect.MobEffects.INVISIBILITY),
					"Invisibility did not install its owned hidden effect");
			case "double_health" -> helper.assertTrue(caster.getMaxHealth() > 20.0F,
					"Double Health did not raise maximum health");
			default -> helper.assertTrue(data.energy() >= 0,
					"Ability left an invalid energy state: " + id);
		}
	}
}
