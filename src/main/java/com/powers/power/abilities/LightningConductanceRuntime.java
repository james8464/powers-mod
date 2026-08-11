package com.powers.power.abilities;

import com.powers.PowersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Resolves bounded body-local conductance through data-driven block and item tags. */
public final class LightningConductanceRuntime {
	private static final TagKey<Block> GROUNDING_RODS = TagKey.create(
			Registries.BLOCK, PowersMod.id("lightning_grounding_rods"));
	private static final TagKey<Block> CONDUCTIVE_BLOCKS = TagKey.create(
			Registries.BLOCK, PowersMod.id("lightning_conductors"));
	private static final TagKey<Item> CONDUCTIVE_ARMOUR = TagKey.create(
			Registries.ITEM, PowersMod.id("conductive_armor"));
	private static final EquipmentSlot[] ARMOUR_SLOTS = {
			EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
	};

	private LightningConductanceRuntime() {
	}

	/** Classifies water, seven fixed contact blocks, and four armour slots without scans. */
	public static LightningStrikeRules.Conductance classify(
			ServerLevel level, LivingEntity target) {
		if (level == null || target == null) return LightningStrikeRules.Conductance.NONE;
		BlockPos feet = target.blockPosition();
		BlockPos[] contacts = {
				feet, feet.below(), feet.above(), feet.north(),
				feet.south(), feet.east(), feet.west()
		};
		boolean groundingRod = false;
		boolean conductiveBlock = false;
		for (BlockPos contact : contacts) {
			groundingRod |= level.getBlockState(contact).is(GROUNDING_RODS);
			conductiveBlock |= level.getBlockState(contact).is(CONDUCTIVE_BLOCKS);
		}
		boolean conductiveArmour = false;
		for (EquipmentSlot slot : ARMOUR_SLOTS) {
			if (target.getItemBySlot(slot).is(CONDUCTIVE_ARMOUR)) {
				conductiveArmour = true;
				break;
			}
		}
		return LightningStrikeRules.conductance(
				target.isInWater(), groundingRod, conductiveBlock, conductiveArmour);
	}
}
