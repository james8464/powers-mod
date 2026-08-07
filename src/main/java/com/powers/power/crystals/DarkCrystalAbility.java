package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.GodlyPunishment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
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

public class DarkCrystalAbility extends Ability {
	private static final ResourceKey<Level> DESTINATION = ResourceKey.create(
			net.minecraft.core.registries.Registries.DIMENSION, PowersMod.id("dark_realm"));
	private static final int STORM_TICKS = 80;
	private static final int TELEPORT_DELAY = 30;

	public DarkCrystalAbility() {
		super(PowersMod.id("dark_crystal"),
				Component.translatable("ability.powers.dark_crystal"),
				2400, false);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		ServerLevel destLevel = ((ServerLevel) caster.level()).getServer().getLevel(DESTINATION);
		if (destLevel == null) return false;

		if (caster.isCrouching()) {
			if (!canTeleportDarkRealm(caster, caster, destLevel)) return false;
			teleportWithStorms(caster, caster, destLevel);
			return true;
		}

		ServerLevel level = (ServerLevel) caster.level();
		Vec3 origin = caster.getEyePosition();
		HitResult hit = caster.pick(48.0, 0.0f, false);

		if (hit instanceof EntityHitResult entHit) {
			net.minecraft.world.entity.Entity target = entHit.getEntity();
			if (target instanceof ServerPlayer targetPlayer && AmethystDampening.isDampened(targetPlayer)) {
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			com.powers.fx.PowerFx.beam(level, origin, target.getEyePosition(),
					ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF1A237E), 16);
			com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, 0.4f);
			if (!canTeleportDarkRealm(caster, target, destLevel)) return false;
			teleportWithStorms(caster, target, destLevel);
			return true;
		}

		Vec3 end = origin.add(caster.getLookAngle().normalize().scale(48.0));
		com.powers.fx.PowerFx.beam(level, origin, end,
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF1A237E), 16);
		com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PORTAL_TRIGGER, 0.6f, 0.4f);
		return true;
	}

	private boolean canTeleportDarkRealm(ServerPlayer caster, net.minecraft.world.entity.Entity subject, ServerLevel dest) {
		if (!(subject instanceof ServerPlayer player)) {
			return true;
		}
		boolean enteringDarkRealm = SkillSystem.isDarkRealm(dest.dimension());
		boolean alreadyInDarkRealm = SkillSystem.isDarkRealm(player.level().dimension());
		if (enteringDarkRealm && !alreadyInDarkRealm) {
			if (!SkillSystem.canTraverseDarknessDimension(player, caster.entityTags().contains("darkness"))) {
				GodlyPunishment.voidReject((ServerLevel) caster.level(), caster);
				PowerMessages.send(caster, "ability.powers.darkness_realm_restricted", 5);
				return false;
			}
		}
		return true;
	}

	private void teleportWithStorms(ServerPlayer caster, net.minecraft.world.entity.Entity subject, ServerLevel dest) {
		ServerLevel srcLevel = (ServerLevel) subject.level();
		Vec3 srcPos = subject.position();
		Vec3 destPos = new Vec3(8.5, dest.getMinY() + 1, 8.5);

		PowersMod.startStorm(srcLevel, srcPos, STORM_TICKS);
		PowersMod.startStorm(dest, destPos, STORM_TICKS);
		PowersMod.scheduleDelayed(srcLevel.getServer(), TELEPORT_DELAY, () -> {
			if (subject.isRemoved()) return;
			subject.teleport(new TeleportTransition(dest, destPos, Vec3.ZERO,
					subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		});
	}
}
