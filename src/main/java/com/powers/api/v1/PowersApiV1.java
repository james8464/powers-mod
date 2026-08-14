package com.powers.api.v1;

/** Stable server-only POWERS integration surface, semantically versioned as 1.0. */
public interface PowersApiV1 {
	ApiVersion VERSION = new ApiVersion(1, 0);
	RegistrationResult registerAction(ActionRegistration action);
	RegistrationResult registerProtectionService(ProtectionService service);
	RegistrationResult registerLifecycleHook(LifecycleHook hook);
	CastContext castContext(net.minecraft.server.level.ServerPlayer actor, String registeredActionId);
	PresenceHandle registerPresence(CastContext context, PhysicalPresence presence);
	boolean removePresence(PresenceHandle handle);
}
