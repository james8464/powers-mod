package com.powers.api.v1;

/** Stable non-throwing outcomes; LIMIT also rejects and rolls back the current extension. */
public enum RegistrationResult { ACCEPTED, DUPLICATE, LATE, INVALID, LIMIT }
