package com.powers.companion;

import net.minecraft.server.level.ServerPlayer;

/** Server entry point for bounded, non-consuming natural Shadow conversation. */
public final class ShadowChatRuntime {
	private ShadowChatRuntime() {
	}

	/** Records ordinary public chat and answers only inside the speaker's active dialogue focus. */
	public static boolean observe(ServerPlayer speaker, String message) {
		long tick = speaker.level().getServer().getTickCount();
		if (!ShadowChatContext.observe(speaker, message, tick)
				|| !PrivateCompanionManager.requested(speaker.getUUID())
				|| !PrivateCompanionManager.eligible(speaker)) return false;
		ShadowCompanionMessaging.answer(speaker, message);
		return true;
	}
}
