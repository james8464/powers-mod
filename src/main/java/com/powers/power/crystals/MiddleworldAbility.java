package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.power.MagicUseGate;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
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
				2400, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel sourceLevel = (ServerLevel) player.level();
		Vec3 sourcePosition = player.position();
		ServerLevel targetLevel = player.level().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						PowersMod.id("middleworld")));
		if (targetLevel == null) return false;

		Vec3 dest = new Vec3(8.5, targetLevel.getMinY() + 1, 8.5);
		double runeRadius = scaledRange(player, 1.8);
		PowerFx.rune(sourceLevel, sourcePosition, runeRadius, 0x80CBC4, 30, 0.0);
		PowerFx.spiral(sourceLevel, sourcePosition, 1.2, 2.4, 0xB2DFDB, 24, 0.0);
		PowerFx.burst(sourceLevel, sourcePosition.add(0, 1, 0), ParticleTypes.PORTAL, 24, 0.7, 0.04);
		PowerFx.sound(sourceLevel, sourcePosition, SoundEvents.END_PORTAL_SPAWN, 0.8f, 1.35f);
		AsyncAbilityTransaction transaction = new AsyncAbilityTransaction(player, data, this);
		java.util.UUID playerId = player.getUUID();
		CastSource castSource = CastScalingContext.currentSource();
		return TravelChunkLoader.request(playerId, targetLevel, BlockPos.containing(dest), () -> {
			ServerPlayer traveler = targetLevel.getServer().getPlayerList().getPlayer(playerId);
			if (traveler == null || !traveler.isAlive()
					|| traveler.level() != sourceLevel || !MagicUseGate.ongoingAllowed(traveler)
					|| !ServerCastLifecycle.mayContinue(traveler, castSource, false)
					|| !SafeDestinationResolver.validate(
							traveler, targetLevel, dest, TravelKind.CRYSTAL).allowed()) {
				transaction.fail();
				return;
			}
			traveler.teleport(new TeleportTransition(targetLevel, dest, Vec3.ZERO,
					traveler.getYRot(), traveler.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			if (traveler.level() != targetLevel) {
				transaction.fail();
				return;
			}
			transaction.succeed();
			PowerFx.rune(targetLevel, dest, runeRadius, 0x80CBC4, 30, Math.PI);
			PowerFx.burst(targetLevel, dest.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 24, 0.7, 0.04);
		}, () -> {
			transaction.fail();
			ServerPlayer traveler = targetLevel.getServer().getPlayerList().getPlayer(playerId);
			if (traveler != null) PowerMessages.send(traveler, "ability.powers.no_room", 3);
		});
	}
}
