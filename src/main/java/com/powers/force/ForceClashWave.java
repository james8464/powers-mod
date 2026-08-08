package com.powers.force;

import com.powers.fx.PowerFx;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** One staged light-versus-darkness wave that removes only loaded living-force blocks. */
final class ForceClashWave {
	private static final int REMOVAL_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
			| Block.UPDATE_SKIP_ON_PLACE;

	private final ServerLevel level;
	private final BlockPos center;
	private final int radius;
	private final Cursor cursor;
	private int age;

	ForceClashWave(ServerLevel level, BlockPos center, int radius) {
		this.level = level;
		this.center = center.immutable();
		this.radius = radius;
		this.cursor = new Cursor(radius);
	}

	boolean tick(int maxChecks) {
		age++;
		double visualRadius = Math.min(radius, 1.5 + age * 1.35);
		PowerFx.forceClashWave(level, Vec3.atCenterOf(center), visualRadius, age);
		BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
		for (Offset offset : cursor.take(maxChecks)) {
			target.set(center.getX() + offset.x(), center.getY() + offset.y(), center.getZ() + offset.z());
			if (!LoadedChunks.contains(level, target)) continue;
			BlockState state = level.getBlockState(target);
			if (LivingForceKind.from(state) == null) continue;
			level.setBlock(target, Blocks.AIR.defaultBlockState(), REMOVAL_FLAGS);
			LivingForceManager.unregister(level, target);
		}
		if (!cursor.finished()) return false;
		PowerFx.forceClashFinished(level, Vec3.atCenterOf(center), radius);
		return true;
	}

	boolean overlaps(BlockPos other) {
		return center.distSqr(other) <= (double) radius * radius;
	}

	/** Immutable block offset from a clash centre. */
	record Offset(int x, int y, int z) {
		long distanceSquared() {
			return (long) x * x + (long) y * y + (long) z * z;
		}
	}

	/** Stateful allocation-light sphere traversal split into caller-sized batches. */
	static final class Cursor {
		private final int radius;
		private int x;
		private int y;
		private int z;
		private boolean finished;

		Cursor(int radius) {
			this.radius = Math.max(0, radius);
			x = -this.radius;
			y = -this.radius;
			z = -this.radius;
		}

		List<Offset> take(int maxPositions) {
			if (maxPositions <= 0 || finished) return List.of();
			List<Offset> result = new ArrayList<>(maxPositions);
			while (!finished && result.size() < maxPositions) {
				Offset offset = new Offset(x, y, z);
				advance();
				if (LivingForceRules.insideSphere(offset.x(), offset.y(), offset.z(), radius)) {
					result.add(offset);
				}
			}
			return result;
		}

		boolean finished() {
			return finished;
		}

		private void advance() {
			x++;
			if (x <= radius) return;
			x = -radius;
			z++;
			if (z <= radius) return;
			z = -radius;
			y++;
			if (y > radius) finished = true;
		}
	}
}
