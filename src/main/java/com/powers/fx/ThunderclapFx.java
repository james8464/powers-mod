package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.ThunderclapRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Expanding air-pressure choreography kept clear of the caster's camera. */
public final class ThunderclapFx {
	private ThunderclapFx() {
	}

	public static void release(ServerLevel level, Vec3 origin, Vec3 direction, double range,
			ThunderclapRules.Echo echo, Vec3 echoPoint) {
		PowerFx.sound(level, origin, SoundEvents.GENERIC_EXPLODE.value(), 2.2F, 0.62F);
		PowerFx.sound(level, origin, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.8F, 0.48F);
		for (int band = 1; band <= 5; band++) {
			double distance = range * band / 6.0;
			Vec3 center = origin.add(direction.scale(distance));
			double radius = 1.5 + distance * 0.38;
			PowerFx.ring(level, center, radius, 0xD7F8FF, 14 + band * 3, band * 0.35);
			PowerFx.burst(level, center, com.powers.PowersParticles.RIBBON, 5 + band * 2, radius * 0.45, 0.22);
			PowerFx.burst(level, center, PowersParticles.RIBBON, 3 + band, radius * 0.30, 0.10);
			Vec3 dustCenter = center.add(0.0, -1.1, 0.0);
			PowerFx.burst(level, dustCenter, ParticleTypes.POOF, 3 + band, radius * 0.32, 0.06);
			PowerFx.burst(level, dustCenter, PowerFx.dust(0x8B7A68, 0.8F),
					3 + band, radius * 0.26, 0.02);
		}
		if (echo == ThunderclapRules.Echo.CAVE) {
			PowerFx.sound(level, echoPoint, PowersSounds.BOSS_IMPACT_RING, 0.62F, 0.78F);
		}
	}
}
