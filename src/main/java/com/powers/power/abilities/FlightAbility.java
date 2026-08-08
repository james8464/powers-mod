package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
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
		// only the first toggle-on records the prior flags, later ones don't
		// overwrite the snapshot needed for a clean restore
		if (data.flightSnapshot() < 0) {
			int snapshot = (player.getAbilities().mayfly ? 1 : 0)
					| (player.getAbilities().flying ? 2 : 0)
					| ((player.gameMode().isCreative() || player.isSpectator()) ? 4 : 0);
			data.setFlightSnapshot(snapshot);
		}
		player.getAbilities().mayfly = true;
		player.getAbilities().flying = true;
		player.onUpdateAbilities();
		PowerMessages.send(player, "ability.powers.flight_on", 3);
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		int prior = data.flightSnapshot();
		data.setFlightSnapshot(-1);
		boolean modeOwnsFlight = player.gameMode().isCreative() || player.isSpectator();
		boolean oldModeOwnedFlight = (prior & 4) != 0;
		player.getAbilities().mayfly = modeOwnsFlight || (!oldModeOwnedFlight && (prior & 1) != 0);
		player.getAbilities().flying = modeOwnsFlight || (!oldModeOwnedFlight && (prior & 2) != 0);
		player.onUpdateAbilities();
		// 3 seconds of slow falling so the way down is soft
		if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
		}
		PowerMessages.send(player, "ability.powers.flight_off", 3);
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// re-assert flight in case some other mechanic stripped the flag
		if (!player.getAbilities().mayfly) {
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
		}
		if (player.getAbilities().flying && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// rainbow trail while actually airborne
			int rgb = com.powers.fx.PowerFx.rainbow(level.getServer().getTickCount(), 4);
			com.powers.fx.PowerFx.coloredBurst(level, player.position().add(0, 0.3, 0), rgb, 2, 0.12);
		}
	}
}
