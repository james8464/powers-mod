package com.powers.boss;

import com.powers.PowersBlocks;
import com.powers.PowersEntities;
import com.powers.PowersSounds;
import com.powers.entity.FirstVessel;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Rank-ten altar invocation for the First Vessel; the eight anchors are consumed. */
public final class FirstVesselRitual {
	private static final List<BlockPos> DARK_OFFSETS = List.of(
			new BlockPos(3, 0, 0), new BlockPos(-3, 0, 0),
			new BlockPos(0, 0, 3), new BlockPos(0, 0, -3));
	private static final List<BlockPos> LIGHT_OFFSETS = List.of(
			new BlockPos(2, 0, 2), new BlockPos(-2, 0, 2),
			new BlockPos(2, 0, -2), new BlockPos(-2, 0, -2));

	private FirstVesselRitual() {
	}

	public static boolean invoke(ServerPlayer player, BlockPos altar) {
		if (!(player.level() instanceof ServerLevel level)
				|| level.getBlockState(altar).getBlock() != PowersBlocks.ARCANE_CRUCIBLE) return false;
		if (!SkillSystem.hasDarknessTag(player) || PlayerPowers.get(player).darknessLevel() < 10) {
			PowerMessages.overlay(player, Component.translatable("boss.powers.first_vessel.ritual_locked"));
			return false;
		}
		if (!matches(level, altar)) {
			PowerMessages.overlay(player, Component.translatable("boss.powers.first_vessel.ritual_pattern"));
			return false;
		}
		Vec3 center = Vec3.atCenterOf(altar);
		if (!BoundedEntityCandidates.ofClass(level, FirstVessel.class,
				AABB.ofSize(center, 256.0, 256.0, 256.0), 1,
				FirstVessel::isAlive).isEmpty()) {
			PowerMessages.overlay(player, Component.translatable("boss.powers.first_vessel.ritual_present"));
			return false;
		}

		DARK_OFFSETS.forEach(offset -> level.setBlockAndUpdate(altar.offset(offset),
				Blocks.AIR.defaultBlockState()));
		LIGHT_OFFSETS.forEach(offset -> level.setBlockAndUpdate(altar.offset(offset),
				Blocks.AIR.defaultBlockState()));
		FirstVessel boss = PowersEntities.FIRST_VESSEL.create(level, EntitySpawnReason.EVENT);
		if (boss == null) return false;
		boss.setPos(altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5);
		level.addFreshEntity(boss);
		PowerFx.rune(level, center, 12.0, 0xE4D6FF, 64, 0.0);
		PowerFx.spiral(level, center, 6.0, 16.0, 0x54205F, 56, 0.0);
		PowerFx.burst(level, center.add(0, 1, 0),
				ParticleTypes.REVERSE_PORTAL, 48, 3.5, 0.18);
		PowerFx.sound(level, center, PowersSounds.DARK_WHISPER, 4.0F, 0.35F);
		PowerMessages.overlay(player, Component.translatable("boss.powers.first_vessel.ritual_complete"));
		return true;
	}

	public static boolean matches(ServerLevel level, BlockPos altar) {
		return DARK_OFFSETS.stream().allMatch(offset ->
				level.getBlockState(altar.offset(offset)).getBlock() == PowersBlocks.DARKNESS)
				&& LIGHT_OFFSETS.stream().allMatch(offset ->
				level.getBlockState(altar.offset(offset)).getBlock() == PowersBlocks.PURE_LIGHT);
	}
}
