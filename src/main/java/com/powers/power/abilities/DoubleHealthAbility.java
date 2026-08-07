package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

// Double Health: while toggled on your max health is doubled, and you get a
// free 20-heart top-up so turning it on doesn't leave you at half a bar.
public class DoubleHealthAbility extends ToggleAbility {
	public DoubleHealthAbility() {
		super(PowersMod.id("double_health"), Component.translatable("ability.powers.double_health"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// amplifier 4 health boost with no expiry adds 20 max health
		player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 4, false, false, true));
		// heal up to 20 so the jump in max health actually fills the bar
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 20.0f));
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.spiral(level, player.position(), 0.8, 2.0, 0xFF1744, 20, 0);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.removeEffect(MobEffects.HEALTH_BOOST);
		// clamp current health down to the new lower max, no free healing
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth()));
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// re-assert the boost so a milk bucket or effect clear can't silently
		// strip the doubled health while the toggle is still on
		if (!player.hasEffect(MobEffects.HEALTH_BOOST)) {
			player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 4, false, false, true));
		}
	}
}
