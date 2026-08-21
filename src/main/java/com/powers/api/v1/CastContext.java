package com.powers.api.v1;

import net.minecraft.server.level.ServerPlayer;

/** Opaque one-shot server-authored cast authority; only the issuing runtime may consume it. */
public interface CastContext {
	/** Returns the exact live server player authorised when this one-shot context was issued. */
	ServerPlayer actor();
	/** Returns the canonical registered action identity bound to this authority. */
	String actionId();
	/** Identifies the registration boundary that supplied the action. */
	CastSource source();
}
