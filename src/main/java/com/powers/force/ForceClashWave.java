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
		BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
		for (Offset offset : cursor.take(maxChecks)) {
			target.set(center.getX() + offset.x(), center.getY() + offset.y(), center.getZ() + offset.z());
			if (!LoadedChunks.contains(level, target)) continue;
			BlockState state = level.getBlockState(target);
			if (LivingForceKind.from(state) == null) continue;
			level.setBlock(target, Blocks.AIR.defaultBlockState(), REMOVAL_FLAGS);
			LivingForceManager.unregister(level, target);
		}
		double visualRadius = Math.min(radius, Math.max(1.5, cursor.frontierRadius() + 0.5));
		PowerFx.forceClashWave(level, Vec3.atCenterOf(center), visualRadius, age);
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

	/** Stateful radial-shell sphere traversal split into caller-sized batches. */
	static final class Cursor {
		private final int radius;
		private int shell;
		private int x;
		private int z;
		private int y;
		private int outerY = -1;
		private int innerY = -1;
		private int frontierRadius;
		private boolean finished;

		Cursor(int radius) {
			this.radius = Math.max(0, radius);
		}

		List<Offset> take(int maxPositions) {
			if (maxPositions <= 0 || finished) return List.of();
			List<Offset> result = new ArrayList<>(maxPositions);
			while (!finished && result.size() < maxPositions) {
				Offset offset = next();
				if (offset != null) result.add(offset);
			}
			return result;
		}

		boolean finished() {
			return finished;
		}

		int frontierRadius() {
			return frontierRadius;
		}

		private Offset next() {
			while (!finished) {
				if (outerY >= 0) {
					while (y <= outerY) {
						int candidateY = y++;
						if (Math.abs(candidateY) <= innerY) continue;
						frontierRadius = shell;
						return new Offset(x, candidateY, z);
					}
					outerY = -1;
					advanceColumn();
					continue;
				}
				prepareColumn();
			}
			return null;
		}

		private void prepareColumn() {
			// At each horizontal coordinate, subtract the previous sphere's vertical
			// interval from the current sphere's interval to emit this shell only.
			long horizontalSquared = (long) x * x + (long) z * z;
			long outerSquared = (long) shell * shell;
			if (horizontalSquared > outerSquared) {
				advanceColumn();
				return;
			}
			outerY = floorSqrt(outerSquared - horizontalSquared);
			long previousRadius = shell - 1L;
			long innerSquared = previousRadius * previousRadius;
			innerY = shell > 0 && horizontalSquared <= innerSquared
					? floorSqrt(innerSquared - horizontalSquared) : -1;
			y = -outerY;
		}

		private void advanceColumn() {
			z++;
			if (z <= shell) return;
			x++;
			if (x <= shell) {
				z = -shell;
				return;
			}
			shell++;
			if (shell > radius) {
				finished = true;
				return;
			}
			x = -shell;
			z = -shell;
		}

		private static int floorSqrt(long value) {
			return (int) Math.floor(Math.sqrt(value));
		}
	}
}
