package com.powers.api.v1;

import java.util.UUID;

/** Opaque lifecycle token for removing an extension-owned presence. */
public record PresenceHandle(UUID id) { }
