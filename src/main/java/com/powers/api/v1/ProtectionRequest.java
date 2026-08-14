package com.powers.api.v1;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.UUID;

/** Read-only server-side protection request; null actor, target, or position means unavailable. */
public record ProtectionRequest(String action, ServerLevel level, BlockPos position, UUID actor, UUID target) { }
