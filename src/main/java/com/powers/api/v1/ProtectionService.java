package com.powers.api.v1;

import java.util.Objects;

/** Prioritised claim adapter; every service must allow and any failure denies. */
public record ProtectionService(String id, int priority, Decision decision) {
	/** Fail-closed decision boundary invoked for each protected world mutation. */
	@FunctionalInterface public interface Decision {
		/** Returns whether this adapter authorises the exact immutable protection request. */
		boolean allows(ProtectionRequest request);
	}
	/** Validates the stable adapter identity and required decision callback. */
	public ProtectionService {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(decision, "decision");
	}
}
