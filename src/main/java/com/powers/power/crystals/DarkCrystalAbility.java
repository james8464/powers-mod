package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
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
 * dark crystal - fused with darkness itself, so anyone can use it to drag
 * the player they're looking at (or themselves when sneaking) into the dark
 * realm after a storm and a short delay, no gate required
 */
public class DarkCrystalAbility extends Ability {
	// the dark realm dimension
	private static final ResourceKey<Level> DESTINATION = ResourceKey.create(
			net.minecraft.core.registries.Registries.DIMENSION, PowersMod.id("dark_realm"));
	// 4 seconds of storm visuals at both ends
	private static final int STORM_TICKS = 80;
	// the blink lands 1.5 seconds after casting
	private static final int TELEPORT_DELAY = 30;

	public DarkCrystalAbility() {
		super(PowersMod.id("dark_crystal"),
				Component.translatable("ability.powers.dark_crystal"),
				2400, false);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		ServerLevel destLevel = ((ServerLevel) caster.level()).getServer().getLevel(DESTINATION);
		// the dark realm isn't loaded - refuse and let the caller refund the energy
		if (destLevel == null) return false;

		// sneaking sends yourself instead of someone else
		if (caster.isCrouching()) {
			teleportWithStorms(caster, caster, destLevel);
			return true;
		}

		ServerLevel level = (ServerLevel) caster.level();
		Vec3 origin = caster.getEyePosition();
		// look up to 48 blocks for a target
		LivingEntity target = PowerTargeting.findLivingTarget(caster, 48.0);

		if (target != null) {
			if (target instanceof ServerPlayer targetPlayer && !PowerProtection.mayForceMove(caster, targetPlayer)) {
				PowerMessages.send(caster, "powers.packet.consent_denied", 1, targetPlayer.getName().getString());
				return false;
			}
			// amethyst-dampened players are protected
			if (target instanceof ServerPlayer targetPlayer && AmethystDampening.isDampened(targetPlayer)) {
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			com.powers.fx.PowerFx.beam(level, origin, target.getEyePosition(),
					ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF1A237E), 16);
			com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, 0.4f);
			teleportWithStorms(caster, target, destLevel);
			return true;
		}

		// no entity hit - just show the beam to where it would reach as a warning
		Vec3 end = origin.add(caster.getLookAngle().normalize().scale(48.0));
		com.powers.fx.PowerFx.beam(level, origin, end,
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF1A237E), 16);
		com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRIGGER, 0.6f, 0.4f);
		return true;
	}

	private void teleportWithStorms(ServerPlayer caster, net.minecraft.world.entity.Entity subject, ServerLevel dest) {
		ServerLevel srcLevel = (ServerLevel) subject.level();
		Vec3 srcPos = subject.position();
		// the fixed landing spot at the dark realm's spawn
		Vec3 destPos = new Vec3(8.5, dest.getMinY() + 1, 8.5);

		// the storm beneath the banished builds the dark realm's smoke; the
		// arrival spot stays clear - the realm itself has no weather
		PowersMod.startStorm(srcLevel, srcPos, STORM_TICKS, PowersMod.StormTheme.DARK);
		PowersMod.startStorm(dest, destPos, STORM_TICKS);
		PowersMod.scheduleDelayed(srcLevel.getServer(), TELEPORT_DELAY, () -> {
			// the subject may have died during the storm - never teleport a corpse
			if (subject.isRemoved()) return;
			subject.teleport(new TeleportTransition(dest, destPos, Vec3.ZERO,
					subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		});
	}
}
