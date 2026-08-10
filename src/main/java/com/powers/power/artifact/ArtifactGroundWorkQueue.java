package com.powers.power.artifact;

import com.powers.PowersBlocks;
import com.powers.force.LivingForceManager;
import com.powers.force.LivingForceRules;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.ArtifactWeaponManager;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Bounded queued block conversion used by both artifact ground rites. */
public final class ArtifactGroundWorkQueue {
	private static final int MAX_QUEUED = 4_096;
	private static final int MAX_PER_TICK = 64;
	private static final ArrayDeque<Work> QUEUE = new ArrayDeque<>();

	private record Work(UUID ownerId, ResourceKey<Level> dimension, BlockPos position,
			ArtifactAlignment alignment, boolean opposedOnly, boolean rewardEnergy) {
	}

	private ArtifactGroundWorkQueue() {
	}

	public static int enqueueDisc(ServerPlayer player, ArtifactAlignment alignment,
			int radius, boolean opposedOnly, boolean rewardEnergy) {
		int accepted = 0;
		BlockPos origin = player.blockPosition().below();
		for (int dx = -radius; dx <= radius && QUEUE.size() < MAX_QUEUED; dx++) {
			for (int dz = -radius; dz <= radius && QUEUE.size() < MAX_QUEUED; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				BlockPos ground = findGround((ServerLevel) player.level(), origin.offset(dx, 0, dz));
				if (ground == null) continue;
				QUEUE.addLast(new Work(player.getUUID(), player.level().dimension(), ground.immutable(),
						alignment, opposedOnly, rewardEnergy));
				accepted++;
			}
		}
		return accepted;
	}

	public static void tick(MinecraftServer server) {
		Set<UUID> changedEnergy = new HashSet<>();
		for (int processed = 0; processed < MAX_PER_TICK && !QUEUE.isEmpty(); processed++) {
			Work work = QUEUE.removeFirst();
			ServerPlayer owner = server.getPlayerList().getPlayer(work.ownerId());
			ServerLevel level = server.getLevel(work.dimension());
			if (owner == null || level == null || owner.level() != level
					|| !ArtifactWeaponManager.maySustain(owner, work.alignment())
					|| !PowerProtection.mayAffectBlock(owner, level, work.position())) continue;
			BlockState state = level.getBlockState(work.position());
			boolean opposed = work.alignment() == ArtifactAlignment.DARKNESS
					? state.is(PowersBlocks.PURE_LIGHT) || state.getLightEmission() > 0
					: state.is(PowersBlocks.DARKNESS);
			if (work.opposedOnly() && !opposed) continue;
			if (state.is(targetBlock(work.alignment())) || !LivingForceRules.mayReplace(
					state.isAir(), !state.getFluidState().isEmpty(), level.getBlockEntity(work.position()) != null,
					state.is(LivingForceManager.FORCE_SPREAD_IMMUNE),
					state.getDestroySpeed(level, work.position()))) continue;
			if (level.setBlock(work.position(), targetBlock(work.alignment()).defaultBlockState(), 3)
					&& work.rewardEnergy()) {
				PlayerPowers.get(owner).refundEnergy(1);
				changedEnergy.add(owner.getUUID());
			}
		}
		for (UUID playerId : changedEnergy) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player != null) PowersPackets.syncTo(player);
		}
	}

	private static net.minecraft.world.level.block.Block targetBlock(ArtifactAlignment alignment) {
		return alignment == ArtifactAlignment.DARKNESS ? PowersBlocks.DARKNESS : PowersBlocks.PURE_LIGHT;
	}

	private static BlockPos findGround(ServerLevel level, BlockPos around) {
		for (int offset = 2; offset >= -5; offset--) {
			BlockPos candidate = around.offset(0, offset, 0);
			if (!level.getBlockState(candidate).isAir()
					&& level.getBlockState(candidate.above()).isAir()) return candidate;
		}
		return null;
	}

	public static void forget(UUID ownerId) {
		QUEUE.removeIf(work -> work.ownerId().equals(ownerId));
	}

	public static void clear() {
		QUEUE.clear();
	}
}
