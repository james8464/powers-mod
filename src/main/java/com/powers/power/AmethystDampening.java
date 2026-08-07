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

/**
 * The rules for amethyst's anti-power field: what counts as amethyst
 * (items, blocks, powered wards) and the poisoning effect and sting a
 * suppressed player gets
 */
public final class AmethystDampening {
	// how close amethyst blocks need to be to suppress powers
	private static final int RADIUS = 6;
	// how far a powered ward extends its dampening field
	private static final int WARD_RADIUS = 20;

	private AmethystDampening() {
	}

	/** rechecks the field around the player and applies or clears the amethyst poisoning effect, returning whether they're suppressed */
	public static boolean update(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		boolean dampened = hasAmethystItem(player)
				|| nearAmethyst(level, player.blockPosition())
				|| findPoweredWard(level, player.blockPosition()).isPresent();
		if (dampened) {
			// 30 ticks is plenty because this effect gets refreshed on every update
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					PowersEffects.AMETHYST_POISONING, 30, 0, true, false, true));
		} else {
			player.removeEffect(PowersEffects.AMETHYST_POISONING);
		}
		return dampened;
	}

	/** finds a redstone-powered ward within range, if any */
	public static java.util.Optional<BlockPos> findPoweredWard(ServerLevel level, BlockPos center) {
		return BlockPos.findClosestMatch(center, WARD_RADIUS, WARD_RADIUS,
				pos -> level.hasChunkAt(pos)
						&& level.getBlockState(pos).is(PowersBlocks.AMETHYST_WARD)
						&& AmethystWardBlock.isPowered(level.getBlockState(pos)));
	}

	/** whether the entity is currently under the amethyst poisoning effect */
	public static boolean isDampened(LivingEntity entity) {
		return entity instanceof ServerPlayer player && player.hasEffect(PowersEffects.AMETHYST_POISONING);
	}

	/**
	 * The sting for using powers while suppressed: 2.5 magic damage,
	 * violet spark bursts, and a message that the power was blocked
	 */
	public static void punish(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 pos = player.position().add(0, 1, 0);
		// don't deal damage to a player who's already down
		if (player.isAlive()) {
			player.hurtServer(level, player.damageSources().magic(), 2.5f);
		}
		PowerFx.burst(level, pos, ParticleTypes.ELECTRIC_SPARK, 16, 0.5, 0.1);
		PowerFx.coloredBurst(level, pos, 0xB36BFF, 22, 0.7);
		PowerFx.burst(level, pos, ParticleTypes.END_ROD, 10, 0.4, 0.2);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.1f);
		PowerMessages.send(player, "amethyst.powers.suppressed", 6);
	}

	// any amethyst item suppresses powers, from the main inventory, offhand, or armor
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

	// matches anything whose item id contains "amethyst"
	private static boolean isAmethystItem(ItemStack stack) {
		return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("amethyst");
	}

	// brute-forces the 6-block cube around the player for any amethyst block
	private static boolean nearAmethyst(ServerLevel level, BlockPos center) {
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					// skip unloaded chunks rather than forcing them to load
					if (!level.hasChunkAt(pos)) continue;
					String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
					if (path.contains("amethyst")) return true;
				}
			}
		}
		return false;
	}
}
