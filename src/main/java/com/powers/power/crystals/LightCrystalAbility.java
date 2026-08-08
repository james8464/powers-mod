package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Light Crystal: a door into the light realm. Sneak-right-click to step
 * through yourself, right-click a player in your sights to take them
 * along, or aim at empty space and just light the way
 */
public class LightCrystalAbility extends Ability {
	private static final ResourceKey<Level> DESTINATION = ResourceKey.create(
			net.minecraft.core.registries.Registries.DIMENSION, PowersMod.id("light_realm"));
	private static final int STORM_TICKS = 80;
	private static final int TELEPORT_DELAY = 30;

	public LightCrystalAbility() {
		super(PowersMod.id("light_crystal"),
				Component.translatable("ability.powers.light_crystal"),
				2400, false);
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return BodyProxyManager.hasSession(player, BodyProxyKind.REALM);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		if (BodyProxyManager.hasSession(caster, BodyProxyKind.REALM)) {
			return BodyProxyManager.returnToBody(caster);
		}
		ServerLevel destLevel = ((ServerLevel) caster.level()).getServer().getLevel(DESTINATION);
		if (destLevel == null) return false;

		if (caster.isCrouching()) {
			// sneak-right-click: travel alone
			return teleportWithStorms(caster, caster, destLevel);
		}

		// right-click: a player in your sights travels with you
		ServerLevel level = (ServerLevel) caster.level();
		Vec3 origin = caster.getEyePosition();
		// the gaze reaches out 48 blocks to find who comes along
		LivingEntity target = PowerTargeting.findLivingTarget(caster, 48.0);

		if (target instanceof ServerPlayer targetPlayer) {
			if (!PowerProtection.mayForceMove(caster, targetPlayer)) {
				PowerMessages.send(caster, "powers.packet.consent_denied", 1, targetPlayer.getName().getString());
				return false;
			}
			if (AmethystDampening.isDampened(targetPlayer)) {
				// amethyst-dampened players are shielded and cannot be dragged
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			com.powers.fx.PowerFx.beam(level, origin, target.getEyePosition(),
					ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFFFF), 16);
			com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, 1.6f);
			return teleportWithStorms(caster, targetPlayer, destLevel);
		}

		// aiming at empty space just lights the way, no one crosses
		Vec3 end = origin.add(caster.getLookAngle().normalize().scale(48.0));
		com.powers.fx.PowerFx.beam(level, origin, end,
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFFFF), 16);
		com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRIGGER, 0.6f, 1.6f);
		return true;
	}

	private boolean teleportWithStorms(ServerPlayer caster, ServerPlayer subject, ServerLevel dest) {
		ServerLevel srcLevel = (ServerLevel) subject.level();
		Vec3 srcPos = subject.position();
		// land in the light realm's spawn clearing at 8.5
		Vec3 destPos = new Vec3(8.5, dest.getMinY() + 1, 8.5);
		// This is a trusted, fixed realm entry—not a client-selected remote
		// coordinate—so generating its one spawn chunk is intentional.
		dest.getChunk(0, 0);
		if (!SafeDestinationResolver.validate(subject, dest, destPos, TravelKind.CRYSTAL).allowed()) {
			PowerMessages.send(caster, "ability.powers.no_room", 3);
			return false;
		}

		// storms rage for 80 ticks while the traveller is carried across
		// the storm beneath the departing glitters with the light realm's
		// totem sparks; the realm itself stays clear - it has no weather
		PowersMod.startStorm(srcLevel, srcPos, STORM_TICKS, PowersMod.StormTheme.LIGHT);
		PowersMod.startStorm(dest, destPos, STORM_TICKS);
		PowersMod.scheduleDelayed(srcLevel.getServer(), TELEPORT_DELAY, () -> {
			// target died or logged off during the 30-tick delay, so leave them be
			if (subject.isRemoved() || caster.isRemoved()
					|| srcLevel.getServer().getPlayerList().getPlayer(subject.getUUID()) != subject
					|| !PowerProtection.mayForceMove(caster, subject)
					|| AmethystDampening.isDampened(subject)
					|| !SafeDestinationResolver.validate(subject, dest, destPos, TravelKind.CRYSTAL).allowed()) return;
			if (!BodyProxyManager.start(subject, BodyProxyKind.REALM)) return;
			subject.teleport(new TeleportTransition(dest, destPos, Vec3.ZERO,
					subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		});
		return true;
	}
}
