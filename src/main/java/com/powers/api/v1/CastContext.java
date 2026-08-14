package com.powers.api.v1;

import net.minecraft.server.level.ServerPlayer;

/** Opaque server-authored cast facts; instances are created only by {@link PowersApiV1}. */
public interface CastContext {
	ServerPlayer actor();
	String actionId();
	CastSource source();
}
