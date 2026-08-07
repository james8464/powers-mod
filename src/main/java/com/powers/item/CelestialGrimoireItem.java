package com.powers.item;

import com.powers.network.PowersPackets;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** the celestial grimoire's locator spell: right click to scry an online player's position for 30 levels of experience */
public class CelestialGrimoireItem extends Item {
	public static final int XP_COST = 30;

	public CelestialGrimoireItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			// frozen time holds even the stars still
			if (SpaceTimeAbility.isFrozen(player)) {
				SpaceTimeAbility.reject(player);
				return InteractionResult.SUCCESS;
			}
			if (player.experienceLevel < XP_COST) {
				// the grimoire won't even open for a pauper
				PowerMessages.send(player, "grimoire.celestial.low_xp", 3);
				return InteractionResult.SUCCESS;
			}
			// the server vouches for the cast; the client then asks for a target
			ServerPlayNetworking.send(player, new PowersPackets.OpenLocatorScreenPayload());
		}
		return InteractionResult.SUCCESS;
	}
}
