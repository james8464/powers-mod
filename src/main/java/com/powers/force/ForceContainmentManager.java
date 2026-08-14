package com.powers.force;

import com.powers.AmethystWardBlock;
import com.powers.PowersBlocks;
import com.powers.PowersParticles;
import com.powers.fx.PowerFx;
import com.powers.protection.PowerProtection;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.util.BoundedRoundRobinQueue;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Progressively crystallises living force around completed powered ward ceremonies. */
public final class ForceContainmentManager {
	private static final int MAX_INSPECTIONS_PER_TICK = 256;
	private static final Map<ServerLevel, LaneState> TASKS = new WeakHashMap<>();

	private ForceContainmentManager() {
	}

	public static void request(ServerLevel level, BlockPos ward) {
		LaneState state = TASKS.computeIfAbsent(level, ignored -> new LaneState());
		BlockPos key = ward.immutable();
		if (state.tasks.containsKey(key)) return;
		if (state.tasks.putIfAbsent(key, new Task()) == null) state.work.offer(key);
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
		Map<BlockWorkBudget.Lane, ServerLevel> levelsByLane = new LinkedHashMap<>();
		long providerPolicyId = PowerProtectionAdapters.blockWorkPolicyId();
		for (ServerLevel level : server.getAllLevels()) {
			LaneState state = TASKS.get(level);
			if (state == null) continue;
			if (state.tasks.isEmpty()) {
				TASKS.remove(level);
				continue;
			}
			levelsByLane.put(new BlockWorkBudget.Lane(
					level.dimension().identifier().toString(), providerPolicyId), level);
		}
		Map<BlockWorkBudget.Lane, Integer> allowances = BlockWorkBudget.allocate(
				MAX_INSPECTIONS_PER_TICK, levelsByLane.keySet(), server.getTickCount());
		for (Map.Entry<BlockWorkBudget.Lane, Integer> lane : allowances.entrySet()) {
			ServerLevel level = levelsByLane.get(lane.getKey());
			LaneState state = TASKS.get(level);
			if (state == null || lane.getValue() <= 0) continue;
			advanceBounded(level, state, lane.getValue());
			if (state.tasks.isEmpty()) TASKS.remove(level);
		}
	}

	private static void advanceBounded(ServerLevel level, LaneState state, int allowance) {
		for (int slot = 0; slot < allowance; slot++) {
			BlockPos ward = state.work.poll();
			if (ward == null) return;
			Task task = state.tasks.get(ward);
			if (task == null) continue;
			if (!isCeremony(level, ward)) {
				state.tasks.remove(ward);
				continue;
			}
			advance(level, ward, task, 1);
			if (task.complete()) state.tasks.remove(ward);
			else state.work.offer(ward);
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
			if (!PowerProtection.mayAffectBlock(level, target)) continue;
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
		return new Diagnostics(TASKS.values().stream().mapToInt(state -> state.tasks.size()).sum(),
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

	private static final class LaneState {
		private final Map<BlockPos, Task> tasks = new HashMap<>();
		private final BoundedRoundRobinQueue<BlockPos> work = new BoundedRoundRobinQueue<>();
	}
}
