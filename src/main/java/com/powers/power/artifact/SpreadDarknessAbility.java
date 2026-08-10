package com.powers.power.artifact;

import com.powers.PowersMod;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shadow Sword command that corrupts a protected, bounded disc beneath its wielder. */
public final class SpreadDarknessAbility extends Ability {
	public SpreadDarknessAbility() {
		super(PowersMod.id("spread_darkness"),
				Component.translatable("ability.powers.spread_darkness"), 200, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return ArtifactGroundWorkQueue.enqueueDisc(
				player, ArtifactAlignment.DARKNESS, 6, false, false) > 0;
	}
}
