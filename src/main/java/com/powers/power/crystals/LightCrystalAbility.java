package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		ServerLevel destLevel = ((ServerLevel) caster.level()).getServer().getLevel(DESTINATION);
		if (destLevel == null) return false;

		if (caster.isCrouching()) {
			// sneak-right-click: travel alone
			teleportWithStorms(caster, caster, destLevel);
			return true;
		}

		// right-click: a player in your sights travels with you
		ServerLevel level = (ServerLevel) caster.level();
		Vec3 origin = caster.getEyePosition();
		// the gaze reaches out 48 blocks to find who comes along
		HitResult hit = caster.pick(48.0, 0.0f, false);

		if (hit instanceof EntityHitResult entHit) {
			net.minecraft.world.entity.Entity target = entHit.getEntity();
			if (target instanceof ServerPlayer targetPlayer && AmethystDampening.isDampened(targetPlayer)) {
				// amethyst-dampened players are shielded and cannot be dragged
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			com.powers.fx.PowerFx.beam(level, origin, target.getEyePosition(),
					ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFFFF), 16);
			com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, 1.6f);
			teleportWithStorms(caster, target, destLevel);
			return true;
		}

		// aiming at empty space just lights the way, no one crosses
		Vec3 end = origin.add(caster.getLookAngle().normalize().scale(48.0));
		com.powers.fx.PowerFx.beam(level, origin, end,
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFFFF), 16);
		com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRIGGER, 0.6f, 1.6f);
		return true;
	}

	private void teleportWithStorms(ServerPlayer caster, net.minecraft.world.entity.Entity subject, ServerLevel dest) {
		ServerLevel srcLevel = (ServerLevel) subject.level();
		Vec3 srcPos = subject.position();
		// land in the light realm's spawn clearing at 8.5
		Vec3 destPos = new Vec3(8.5, dest.getMinY() + 1, 8.5);

		// storms rage for 80 ticks while the traveller is carried across
		// the storm beneath the departing glitters with the light realm's
		// totem sparks; the realm itself stays clear - it has no weather
		PowersMod.startStorm(srcLevel, srcPos, STORM_TICKS, PowersMod.StormTheme.LIGHT);
		PowersMod.startStorm(dest, destPos, STORM_TICKS);
		PowersMod.scheduleDelayed(srcLevel.getServer(), TELEPORT_DELAY, () -> {
			// target died or logged off during the 30-tick delay, so leave them be
			if (subject.isRemoved()) return;
			subject.teleport(new TeleportTransition(dest, destPos, Vec3.ZERO,
					subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		});
	}
}
