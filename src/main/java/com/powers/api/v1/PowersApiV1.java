package com.powers.api.v1;

/** Stable server-only POWERS integration surface, semantically versioned as 1.0. */
public interface PowersApiV1 {
	ApiVersion VERSION = new ApiVersion(1, 0);
	/** Registers one bounded extension action during the open server registration epoch. */
	RegistrationResult registerAction(ActionRegistration action);
	/** Registers one fail-closed protection service during the open server registration epoch. */
	RegistrationResult registerProtectionService(ProtectionService service);
	/** Registers an exception-isolated callback for this server epoch. */
	RegistrationResult registerLifecycleHook(LifecycleHook hook);
	/** Issues one-shot cast authority for an exact live player and registered extension action. */
	CastContext castContext(net.minecraft.server.level.ServerPlayer actor, String registeredActionId);
	/** Commits one bounded presence after revalidating authority, policy, payment, and collision. */
	PresenceHandle registerPresence(CastContext context, PhysicalPresence presence);
	/** Removes a live presence owned by the calling extension; unknown handles leave state unchanged. */
	boolean removePresence(PresenceHandle handle);
}
