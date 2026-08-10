package com.powers;

import com.powers.forge.ArcaneCrucibleBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** Custom persistent block-entity registrations. */
public final class PowersBlockEntities {
	public static final BlockEntityType<ArcaneCrucibleBlockEntity> ARCANE_CRUCIBLE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, PowersMod.id("arcane_crucible"),
			new BlockEntityType<>(ArcaneCrucibleBlockEntity::new, Set.of(PowersBlocks.ARCANE_CRUCIBLE)));

	private PowersBlockEntities() {
	}

	public static void initialize() {
	}
}
