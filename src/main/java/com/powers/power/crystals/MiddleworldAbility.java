package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Middleworld: the crystal of the realm between. You open a quiet path
 * and step into the middle world, landing safely on its surface
 */
public class MiddleworldAbility extends Ability {
	public MiddleworldAbility() {
		super(PowersMod.id("middleworld"),
				Component.translatable("ability.powers.middleworld"),
				2400, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel targetLevel = player.level().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						PowersMod.id("middleworld")));
		if (targetLevel == null) return false;

		Vec3 dest = new Vec3(8.5, targetLevel.getMinY() + 1, 8.5);
		targetLevel.getChunk(0, 0);
		if (!SafeDestinationResolver.validate(player, targetLevel, dest, TravelKind.CRYSTAL).allowed()) {
			return false;
		}
		player.teleport(new TeleportTransition(targetLevel, dest, Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		return true;
	}
}
