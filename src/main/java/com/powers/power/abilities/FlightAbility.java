package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flight: grants the ability to fly freely, exactly like Rainbow Steve's
 * flight from the lore. Landing cushions you with slow falling so the drop
 * never hurts.
 */
public class FlightAbility extends ToggleAbility {
	private static final Map<UUID, boolean[]> PRIOR_ABILITIES = new HashMap<>();
	public FlightAbility() {
		super(PowersMod.id("flight"), Component.translatable("ability.powers.flight"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		PRIOR_ABILITIES.putIfAbsent(player.getUUID(),
				new boolean[] {player.getAbilities().mayfly, player.getAbilities().flying});
		player.getAbilities().mayfly = true;
		player.getAbilities().flying = true;
		player.onUpdateAbilities();
		PowerMessages.send(player, "ability.powers.flight_on", 3);
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		boolean[] prior = PRIOR_ABILITIES.remove(player.getUUID());
		player.getAbilities().mayfly = prior != null ? prior[0] : false;
		player.getAbilities().flying = prior != null && prior[1];
		player.onUpdateAbilities();
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
		PowerMessages.send(player, "ability.powers.flight_off", 3);
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!player.getAbilities().mayfly) {
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
		}
		if (player.getAbilities().flying && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			int rgb = com.powers.fx.PowerFx.rainbow(level.getServer().getTickCount(), 4);
			com.powers.fx.PowerFx.coloredBurst(level, player.position().add(0, 0.3, 0), rgb, 2, 0.12);
		}
	}

	public static void clear(UUID player) {
		PRIOR_ABILITIES.remove(player);
	}
}
