package com.powers.api.v1;

import java.util.Objects;

/** Prioritised claim adapter; every service must allow and any failure denies. */
public record ProtectionService(String id, int priority, Decision decision) {
	@FunctionalInterface public interface Decision { boolean allows(ProtectionRequest request); }
	public ProtectionService {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(decision, "decision");
	}
}
