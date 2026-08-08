package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Comparator;

/**
 * Portal Rift: the Indigo Crystal's power of portals and lock-on. You tear
 * a rift through space and blink from enemy to enemy in a chain of strikes,
 * each teleport delivering a crushing blow before the next rift opens
 */
public class PortalRiftAbility extends Ability {
	private static final int COOLDOWN_TICKS = 1800;
	private static final int RANGE = 32;
	private static final int MAX_STRIKES = 6;

	public PortalRiftAbility() {
		super(PowersMod.id("portal_rift"),
				Component.translatable("ability.powers.portal_rift"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		// lock onto up to six live enemies within 32 blocks, dampening-protected ones aside
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(player.position().add(0, 1, 0), RANGE * 2, RANGE * 2, RANGE * 2),
				e -> e.isAlive() && e != player && !player.isAlliedTo(e)
						&& e.distanceToSqr(player) <= RANGE * RANGE
						&& !AmethystDampening.isDampened(e)
						&& PowerProtection.mayHarm(player, e));
		targets.sort(Comparator.comparingDouble(player::distanceToSqr));
		if (targets.size() > MAX_STRIKES) {
			targets = targets.subList(0, MAX_STRIKES);
		}
		if (targets.isEmpty()) {
			return false;
		}

		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x651FFF, 24, 1.2);
		PowerFx.sound(level, player.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.7f);
		// 12 ticks of resistance per strike lined up, so the chain doesn't leave you exposed
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, targets.size() * 12, 4, true, false));

		MinecraftServer server = level.getServer();
		for (int i = 0; i < targets.size(); i++) {
			LivingEntity target = targets.get(i);
			// strikes land every 12 ticks, each one opening the next rift
			int delay = i * 12;
			PowersMod.scheduleDelayed(server, delay, () -> strike(player, target));
		}
		return true;
	}

	private static void strike(ServerPlayer player, LivingEntity target) {
		// skip the strike if either fighter died or left the dimension mid-chain
		if (!player.isAlive() || !target.isAlive() || player.level() != target.level()
				|| AmethystDampening.isDampened(target) || !PowerProtection.mayHarm(player, target)) {
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		Vec3 look = target.getViewVector(1.0F);
		// land 2.2 blocks out along the target's facing so you never clip into them
		Vec3 spot = target.position().add(look.x * 2.2, 0.2, look.z * 2.2);
		if (!SafeDestinationResolver.validate(player, level, spot, TravelKind.CRYSTAL).allowed()) {
			spot = target.position().subtract(look.x * 2.2, -0.2, look.z * 2.2);
		}
		if (!SafeDestinationResolver.validate(player, level, spot, TravelKind.CRYSTAL).allowed()) return;
		player.teleport(new TeleportTransition(level, spot, Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		// the crushing blow: 12 magic damage; if the hit is refused there's no show
		if (!target.hurtServer(level, PowerDamage.source(player), 12.0f)) {
			return;
		}
		PowerFx.coloredBurst(level, target.position().add(0, 1, 0), 0x651FFF, 16, 0.7);
		PowerFx.burst(level, spot.add(0, 1, 0), ParticleTypes.PORTAL, 20, 0.6, 0.4);
		PowerFx.sound(level, spot, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.3f);
	}
}
