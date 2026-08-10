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
		if (!player.gameMode().isCreative() && !player.isSpectator()) {
			var input = player.getLastClientInput();
			FlightRules.Motion motion = FlightRules.motion(player.getYRot(), input.forward(), input.backward(),
					input.left(), input.right(), input.jump(), input.shift(), input.sprint(),
					com.powers.player.SkillSystem.effectiveLevel(player));
			player.setDeltaMovement(motion.x(), motion.y(), motion.z());
			player.hurtMarked = true;
			player.resetFallDistance();
		}
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// rainbow trail while actually airborne
			int rgb = com.powers.fx.PowerFx.rainbow(level.getServer().getTickCount(), 4);
			com.powers.fx.PowerFx.coloredBurst(level, player.position().add(0, 0.3, 0), rgb, 2, 0.12);
			if (scaling(player).unlockedVariants().contains("second_step") && level.getServer().getTickCount() % 10 == 0) {
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 0.1, 0), 0.8, rgb, 12,
						level.getServer().getTickCount() * 0.1);
			}
		}
	}

	@Override
	public int activeTickInterval() {
		return 1;
	}
}
