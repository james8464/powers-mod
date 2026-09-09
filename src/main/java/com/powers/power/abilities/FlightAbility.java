package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Flight: fly freely through the air, exactly like Rainbow Steve's flight
 * from the lore. Landing cushions you with slow falling so the drop never
 * hurts.
 */
public class FlightAbility extends ToggleAbility {
	public FlightAbility() {
		super(PowersMod.id("flight"), Component.translatable("ability.powers.flight"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// Propulsion is server velocity, not the creative mayfly flag. Other mods
		// and game modes therefore keep complete ownership of the vanilla flags.
		data.setFlightSnapshot(0);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.rune(level, player.position(), 1.4, 0xFFFFFF, 24, 0.0);
			com.powers.fx.PowerFx.sound(level, player.position(),
					net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, 0.6f, 1.4f);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		data.setFlightSnapshot(-1);
		// 3 seconds of slow falling so the way down is soft
		if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
			player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
					scaledDuration(player, 60), 0, true, true));
		}
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		FlightRules.Step flightStep = null;
		if (!player.gameMode().isCreative() && !player.isSpectator()) {
			// Propulsion is an authorised server-side movement source. Reset only
			// this connection's vanilla floating counter while the toggle is active;
			// the server-wide allow-flight policy and vanilla ability flags remain
			// untouched for every other player.
			player.connection.resetFlyingTicks();
			var input = player.getLastClientInput();
			int snapshot = Math.max(0, data.flightSnapshot());
			boolean wasSonic = (snapshot & 1) != 0;
			boolean wasInWater = (snapshot & 2) != 0;
			boolean inWater = player.isInWater();
			var velocity = player.getDeltaMovement();
			FlightRules.Step step = FlightRules.step(
					new FlightRules.Motion(velocity.x, velocity.y, velocity.z), player.getYRot(),
					new FlightRules.Input(input.forward(), input.backward(), input.left(), input.right(),
							input.jump(), input.shift(), input.sprint()),
					new FlightRules.Environment(inWater, data.energy(), data.energyCapacity(), wasSonic),
					com.powers.player.SkillSystem.effectiveLevel(player));
			player.setDeltaMovement(step.motion().x(), step.motion().y(), step.motion().z());
			flightStep = step;
			data.setFlightSnapshot((step.sonic() ? 1 : 0) | (inWater ? 2 : 0));
			player.hurtMarked = true;
			player.resetFallDistance();
			if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
				flightFeedback(level, player, step, wasInWater != inWater);
			}
		}
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// rainbow trail while actually airborne
			int rgb = com.powers.fx.PowerFx.rainbow(level.getServer().getTickCount(), 4);
			net.minecraft.world.phys.Vec3 trail = player.position().add(0, 0.3, 0);
			if (flightStep != null && flightStep.motion().horizontalSpeed() > 1.0E-8) {
				double bank = flightStep.tiltDegrees() / FlightRules.MAX_TILT_DEGREES * 0.35;
				trail = trail.add(-flightStep.motion().z() / flightStep.motion().horizontalSpeed() * bank,
						0.0, flightStep.motion().x() / flightStep.motion().horizontalSpeed() * bank);
			}
			com.powers.fx.PowerFx.coloredBurst(level, trail, rgb, 2, 0.12);
			if (scaling(player).unlockedVariants().contains("second_step") && level.getServer().getTickCount() % 10 == 0) {
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 0.1, 0), 0.8, rgb, 12,
						level.getServer().getTickCount() * 0.1);
			}
		}
	}

	private static void flightFeedback(net.minecraft.server.level.ServerLevel level,
			ServerPlayer player, FlightRules.Step step, boolean waterTransition) {
		int tick = level.getServer().getTickCount();
		if (step.sonicCrossed()) {
			com.powers.fx.PowerFx.sound(level, player.position(),
					net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.8f);
			com.powers.fx.PowerFx.ring(level, player.position(), 2.2, 0xF8FFFF, 32, 0.0);
		} else if (waterTransition) {
			com.powers.fx.PowerFx.sound(level, player.position(),
					net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, 0.45f, 0.65f);
		} else if (tick % 20 == 0) {
			float pitch = step.soundCue() == FlightRules.SoundCue.LOW_ENERGY ? 0.65f : 1.25f;
			com.powers.fx.PowerFx.sound(level, player.position(),
					net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, 0.28f, pitch);
		}
	}

	@Override
	public int activeTickInterval() {
		return 1;
	}
}
