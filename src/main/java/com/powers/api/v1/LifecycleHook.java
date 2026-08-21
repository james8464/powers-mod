package com.powers.api.v1;

/** Exception-isolated lifecycle callback. Hooks are discarded after SERVER_STOPPING. */
@FunctionalInterface public interface LifecycleHook {
	/** Receives one authoritative server-epoch lifecycle event on the server thread. */
	void onLifecycle(ApiLifecycleEvent event);
}
