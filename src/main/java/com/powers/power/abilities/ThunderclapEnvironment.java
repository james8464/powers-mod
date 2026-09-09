package com.powers.power.abilities;

import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-only enclosure probe for Thunderclap's directional cave reflection. */
public final class ThunderclapEnvironment {
	private static final double FORWARD_DISTANCE = 12.0;
	private static final double ENCLOSURE_DISTANCE = 6.0;
	private static final double STEP = 0.75;

	private ThunderclapEnvironment() {
	}

	public record EchoProbe(ThunderclapRules.Echo echo, Vec3 point) {
	}

	public static EchoProbe probe(ServerLevel level, Vec3 origin, Vec3 direction) {
		Vec3 forward = new Vec3(direction.x, 0.0, direction.z);
		if (forward.lengthSqr() < 1.0E-8) {
			return new EchoProbe(ThunderclapRules.Echo.NONE, origin);
		}
		forward = forward.normalize();
		Vec3 left = new Vec3(-forward.z, 0.0, forward.x);
		Probe front = firstBlock(level, origin, forward, FORWARD_DISTANCE);
		Probe leftWall = firstBlock(level, origin, left, ENCLOSURE_DISTANCE);
		Probe rightWall = firstBlock(level, origin, left.scale(-1.0), ENCLOSURE_DISTANCE);
		Probe ceiling = firstBlock(level, origin, new Vec3(0.0, 1.0, 0.0), ENCLOSURE_DISTANCE);
		ThunderclapRules.Echo echo = ThunderclapRules.echo(
				front.blocked(), leftWall.blocked(), rightWall.blocked(), ceiling.blocked());
		return new EchoProbe(echo, front.point());
	}

	private static Probe firstBlock(ServerLevel level, Vec3 origin, Vec3 direction,
			double maximumDistance) {
		Vec3 last = origin;
		for (double distance = STEP; distance <= maximumDistance; distance += STEP) {
			Vec3 sample = origin.add(direction.scale(distance));
			BlockPos pos = BlockPos.containing(sample);
			if (!LoadedChunks.contains(level, pos)) return new Probe(false, last);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				return new Probe(true, sample);
			}
			last = sample;
		}
		return new Probe(false, last);
	}

	private record Probe(boolean blocked, Vec3 point) {
	}
}
