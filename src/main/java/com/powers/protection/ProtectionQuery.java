package com.powers.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/** Immutable claim query; actor/target are UUIDs so adapters need not retain entity references. */
public record ProtectionQuery(ProtectionAction action, ServerLevel level, BlockPos position,
		UUID actor, UUID target) {
	public ProtectionQuery {
		if (action == null) throw new IllegalArgumentException("Protection action is required");
		position = position == null ? null : position.immutable();
	}
}
