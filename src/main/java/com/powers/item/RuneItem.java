package com.powers.item;

import com.powers.player.PlayerPowers;
import com.powers.network.PowersPackets;
import com.powers.fx.PowerFx;
import com.powers.util.PowerMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Stackable single-use rune whose authored tier controls its energy restoration. */
public class RuneItem extends Item {
	private final int energy;

	public RuneItem(Properties properties, int energy) {
		super(properties.stacksTo(64));
		this.energy = Math.max(1, energy);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		// try to refill the bar; the message tells you whether it worked or was already full
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			var stack = player.getItemInHand(hand);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			if (data.regenerateEnergy(energy)) {
				stack.shrink(1);
				PowersPackets.syncTo(player);
				PowerMessages.send(player, "rune.powers.channelled", 4, String.valueOf(energy));
				var serverLevel = (net.minecraft.server.level.ServerLevel) level;
				PowerFx.rune(serverLevel, player.position().add(0.0, 0.08, 0.0),
						1.0 + Math.min(1.5, energy / 400.0), 0x7455A8, 18, 0.0);
				PowerFx.sound(serverLevel, player.position(),
						net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.2F);
			} else {
				PowerMessages.send(player, "rune.powers.full", 4);
			}
		}
		return InteractionResult.SUCCESS;
	}

	public int energy() {
		return energy;
	}
}
