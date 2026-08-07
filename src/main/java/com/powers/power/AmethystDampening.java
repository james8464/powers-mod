package com.powers.power;

import com.powers.PowersEffects;
import com.powers.AmethystWardBlock;
import com.powers.PowersBlocks;
import com.powers.fx.PowerFx;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Central rule set for amethyst's anti-power field. */
public final class AmethystDampening {
	private static final int RADIUS = 6;
	private static final int WARD_RADIUS = 20;

	private AmethystDampening() {
	}

	public static boolean update(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		boolean dampened = hasAmethystItem(player)
				|| nearAmethyst(level, player.blockPosition())
				|| findPoweredWard(level, player.blockPosition()).isPresent();
		if (dampened) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					PowersEffects.AMETHYST_POISONING, 30, 0, true, false, true));
		} else {
			player.removeEffect(PowersEffects.AMETHYST_POISONING);
		}
		return dampened;
	}

	public static java.util.Optional<BlockPos> findPoweredWard(ServerLevel level, BlockPos center) {
		return BlockPos.findClosestMatch(center, WARD_RADIUS, WARD_RADIUS,
				pos -> level.hasChunkAt(pos)
						&& level.getBlockState(pos).is(PowersBlocks.AMETHYST_WARD)
						&& AmethystWardBlock.isPowered(level.getBlockState(pos)));
	}

	public static boolean isDampened(LivingEntity entity) {
		return entity instanceof ServerPlayer player && player.hasEffect(PowersEffects.AMETHYST_POISONING);
	}

	/**
	 * Punishes a dampened player who still tries to draw on their powers:
	 * the amethyst bites back with a splintering sting of magic damage and
	 * violet sparks, as if the crystal itself is enraged by the defiance.
	 */
	public static void punish(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 pos = player.position().add(0, 1, 0);
		if (player.isAlive()) {
			player.hurtServer(level, player.damageSources().magic(), 2.5f);
		}
		PowerFx.burst(level, pos, ParticleTypes.ELECTRIC_SPARK, 16, 0.5, 0.1);
		PowerFx.coloredBurst(level, pos, 0xB36BFF, 22, 0.7);
		PowerFx.burst(level, pos, ParticleTypes.END_ROD, 10, 0.4, 0.2);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.1f);
		PowerMessages.send(player, "amethyst.powers.suppressed", 6);
	}

	private static boolean hasAmethystItem(ServerPlayer player) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (isAmethystItem(stack)) return true;
		}
		if (isAmethystItem(player.getOffhandItem())) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.HEAD))) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.CHEST))) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.LEGS))) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.FEET))) return true;
		return false;
	}

	private static boolean isAmethystItem(ItemStack stack) {
		return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("amethyst");
	}

	private static boolean nearAmethyst(ServerLevel level, BlockPos center) {
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (!level.hasChunkAt(pos)) continue;
					String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
					if (path.contains("amethyst")) return true;
				}
			}
		}
		return false;
	}
}
