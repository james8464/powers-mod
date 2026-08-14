package com.powers.api.v1;

import net.minecraft.server.level.ServerPlayer;

/** Opaque one-shot server-authored cast authority; only the issuing runtime may consume it. */
public interface CastContext {
	ServerPlayer actor();
	String actionId();
	CastSource source();
}
