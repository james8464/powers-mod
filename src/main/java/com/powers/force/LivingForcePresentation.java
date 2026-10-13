package com.powers.force;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Bounded force-spread presentation, separate from frontier and collision ownership. */
final class LivingForcePresentation {
	private LivingForcePresentation() { }

	static void emitSpreadCue(ServerLevel level, BlockPos source, BlockPos target,
			LivingForceKind kind, RandomSource random) {
		Vec3 origin = Vec3.atCenterOf(source);
		Vec3 center = Vec3.atCenterOf(target);
		int color = kind == LivingForceKind.DARKNESS ? 0x2A0C3D : 0xFFF4C7;
		PowerFx.beam(level, origin, center, PowerFx.dust(color, 1.0F), 6);
		PowerFx.coloredBurst(level, center, color, 5, 0.32);
		PowerFx.burst(level, center, kind == LivingForceKind.DARKNESS
				? PowersParticles.ECLIPSE : PowersParticles.MOTE, 3, 0.24, 0.015);
		PowerFx.rune(level, center.add(0.0, 0.03, 0.0), 0.42, color, 8,
				random.nextDouble() * Math.PI);
		if (random.nextInt(4) == 0) {
			PowerFx.sound(level, center, kind == LivingForceKind.DARKNESS
					? PowersSounds.DARK_WHISPER : PowersSounds.LIGHT_CHORUS, 0.35F, 0.72F);
		}
	}
}
