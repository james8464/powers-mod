package com.powers.protection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tick-scoped, category-isolated idempotency ledger for Empyrean consent surcharges. */
public final class ConsentPaymentLedger {
	private record Key(UUID caster, UUID target, ConsentKind kind) { }

	private final Set<Key> paid = new HashSet<>();
	private long tick = Long.MIN_VALUE;

	public boolean requiresPayment(long currentTick, UUID caster, UUID target, ConsentKind kind) {
		resetAt(currentTick);
		return !paid.contains(new Key(caster, target, kind));
	}

	public void recordPayment(long currentTick, UUID caster, UUID target, ConsentKind kind) {
		resetAt(currentTick);
		paid.add(new Key(caster, target, kind));
	}

	public void clear() {
		paid.clear();
		tick = Long.MIN_VALUE;
	}

	private void resetAt(long currentTick) {
		if (tick == currentTick) return;
		paid.clear();
		tick = currentTick;
	}
}
