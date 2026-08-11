package com.powers.force;

import com.powers.AmethystWardBlock;
import com.powers.PowersBlocks;
import com.powers.PowersParticles;
import com.powers.fx.PowerFx;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Progressively crystallises living force around completed powered ward ceremonies. */
public final class ForceContainmentManager {
	private static final int MAX_INSPECTIONS_PER_TICK = 256;
	private static final Map<ServerLevel, LinkedHashMap<BlockPos, Task>> TASKS = new WeakHashMap<>();

	private ForceContainmentManager() {
	}

	public static void request(ServerLevel level, BlockPos ward) {
		if (!isCeremony(level, ward)) return;
		TASKS.computeIfAbsent(level, ignored -> new LinkedHashMap<>())
				.putIfAbsent(ward.immutable(), new Task());
	}

	public static boolean isCeremony(ServerLevel level, BlockPos ward) {
		if (!LoadedChunks.contains(level, ward)) return false;
		var wardState = level.getBlockState(ward);
		if (!wardState.is(PowersBlocks.AMETHYST_WARD) || !AmethystWardBlock.isPowered(wardState)) {
			return false;
		}
		for (ForceContainmentRules.Offset offset : ForceContainmentRules.cardinalCrystals()) {
			BlockPos crystal = ward.offset(offset.x(), offset.y(), offset.z());
			if (!LoadedChunks.contains(level, crystal)
					|| !level.getBlockState(crystal).is(Blocks.AMETHYST_BLOCK)) return false;
		}
		return true;
	}

	public static void tick(MinecraftServer server) {
		int remaining = MAX_INSPECTIONS_PER_TICK;
		for (ServerLevel level : server.getAllLevels()) {
			LinkedHashMap<BlockPos, Task> tasks = TASKS.get(level);
			if (tasks == null) continue;
			Iterator<Map.Entry<BlockPos, Task>> iterator = tasks.entrySet().iterator();
			while (iterator.hasNext() && remaining > 0) {
				Map.Entry<BlockPos, Task> entry = iterator.next();
				if (!isCeremony(level, entry.getKey())) {
					iterator.remove();
					continue;
				}
				remaining -= advance(level, entry.getKey(), entry.getValue(), remaining);
				if (entry.getValue().complete()) iterator.remove();
			}
			if (tasks.isEmpty()) TASKS.remove(level);
			if (remaining <= 0) break;
		}
	}

	private static int advance(ServerLevel level, BlockPos ward, Task task, int allowance) {
		int inspected = 0;
		int visuals = 0;
		var sphere = ForceContainmentRules.sphere();
		while (inspected < allowance && task.cursor < sphere.size()) {
			ForceContainmentRules.Offset offset = sphere.get(task.cursor++);
			BlockPos target = ward.offset(offset.x(), offset.y(), offset.z());
			inspected++;
			if (!LoadedChunks.contains(level, target)) continue;
			LivingForceKind kind = LivingForceKind.from(level.getBlockState(target));
			if (kind == null) continue;
			level.setBlock(target, Blocks.AMETHYST_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
			LivingForceManager.unregister(level, target);
			if (visuals++ < 8) {
				PowerFx.rune(level, net.minecraft.world.phys.Vec3.atCenterOf(target), 0.65,
						0xB06CFF, 12, level.getGameTime() * 0.08);
				PowerFx.burst(level, net.minecraft.world.phys.Vec3.atCenterOf(target),
						PowersParticles.SHARD, 6, 0.3, 0.08);
			}
		}
		if (task.complete()) {
			var center = net.minecraft.world.phys.Vec3.atCenterOf(ward);
			PowerFx.ring(level, center, ForceContainmentRules.RADIUS, 0xC78CFF, 40, 0.0);
			PowerFx.sound(level, center, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.72F);
		}
		return inspected;
	}

	public static Diagnostics diagnostics() {
		return new Diagnostics(TASKS.values().stream().mapToInt(Map::size).sum(),
				MAX_INSPECTIONS_PER_TICK);
	}

	public static void clear() {
		TASKS.clear();
	}

	public record Diagnostics(int activeCeremonies, int inspectionBudget) {
	}

	private static final class Task {
		private int cursor;

		private boolean complete() {
			return cursor >= ForceContainmentRules.sphere().size();
		}
	}
}
