package com.powers.api.v1;

/** Exception-isolated lifecycle callback. Hooks are discarded after SERVER_STOPPING. */
@FunctionalInterface public interface LifecycleHook { void onLifecycle(ApiLifecycleEvent event); }
