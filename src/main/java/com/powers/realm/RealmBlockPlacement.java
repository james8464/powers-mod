package com.powers.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** One immutable absolute block edit in a progressively built mindscape landmark. */
public record RealmBlockPlacement(BlockPos position, Block block) {
}
