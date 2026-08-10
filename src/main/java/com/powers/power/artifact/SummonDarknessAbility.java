package com.powers.power.artifact;

import com.powers.PowersMod;
import com.powers.item.ShadowSwordActions;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shadow Sword command that calls four bounded, naturally despawning darkness warriors. */
public final class SummonDarknessAbility extends Ability {
	public SummonDarknessAbility() {
		super(PowersMod.id("summon_darkness"),
				Component.translatable("ability.powers.summon_darkness"), 400, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return ShadowSwordActions.summon(player, 4) > 0;
	}
}
