package com.powers.power.damage;

import java.util.Locale;

/** Bounded aggregate accounting for the authoritative layered-damage pipeline. */
public final class LayeredDamageLedger {
	private static final LayeredDamageLedger GLOBAL = new LayeredDamageLedger();

	private double incomingDamage;
	private double wardBlockedDamage;
	private double wardIntegritySpent;
	private double mirrorRequestedDamage;
	private double mirrorForwardedDamage;
	private double mirrorHealthDamage;
	private double healthDamage;
	private long recursiveMirrorsSuppressed;

	public static LayeredDamageLedger global() {
		return GLOBAL;
	}

	public synchronized void recordIncoming(float damage) {
		incomingDamage = add(incomingDamage, damage);
	}

	public synchronized void recordWard(LayeredDamageResolver.WardDecision decision) {
		if (!decision.present()) return;
		wardBlockedDamage = add(wardBlockedDamage, decision.blockedDamage());
		wardIntegritySpent = add(wardIntegritySpent, decision.integritySpent());
	}

	public synchronized void recordMirror(LayeredDamageResolver.MirrorDecision decision,
			float observedHealthDamage) {
		mirrorRequestedDamage = add(mirrorRequestedDamage, decision.requestedDamage());
		mirrorForwardedDamage = add(mirrorForwardedDamage, decision.forwardedDamage());
		mirrorHealthDamage = add(mirrorHealthDamage, observedHealthDamage);
	}

	public synchronized void recordHealth(float observedHealthDamage) {
		healthDamage = add(healthDamage, observedHealthDamage);
	}

	public synchronized void recordRecursiveMirrorSuppressed() {
		if (recursiveMirrorsSuppressed < Long.MAX_VALUE) recursiveMirrorsSuppressed++;
	}

	public synchronized Snapshot snapshot() {
		return new Snapshot(incomingDamage, wardBlockedDamage, wardIntegritySpent,
				mirrorRequestedDamage, mirrorForwardedDamage, mirrorHealthDamage,
				healthDamage, recursiveMirrorsSuppressed);
	}

	public synchronized void clear() {
		incomingDamage = 0.0;
		wardBlockedDamage = 0.0;
		wardIntegritySpent = 0.0;
		mirrorRequestedDamage = 0.0;
		mirrorForwardedDamage = 0.0;
		mirrorHealthDamage = 0.0;
		healthDamage = 0.0;
		recursiveMirrorsSuppressed = 0L;
	}

	private static double add(double total, double value) {
		if (!Double.isFinite(value) || value <= 0.0) return total;
		return total >= Double.MAX_VALUE - value ? Double.MAX_VALUE : total + value;
	}

	public record Snapshot(double incomingDamage, double wardBlockedDamage,
			double wardIntegritySpent, double mirrorRequestedDamage,
			double mirrorForwardedDamage, double mirrorHealthDamage,
			double healthDamage, long recursiveMirrorsSuppressed) {
		public String line() {
			return String.format(Locale.ROOT,
					"damageLayers: order=forcefield>soul_link>health; incoming=%.1f; "
							+ "wardBlocked=%.1f; wardIntegritySpent=%.1f; "
							+ "mirrorRequested=%.1f; mirrorForwarded=%.1f; mirrorHealth=%.1f; "
							+ "health=%.1f; recursiveMirrorsSuppressed=%d",
					incomingDamage, wardBlockedDamage, wardIntegritySpent,
					mirrorRequestedDamage, mirrorForwardedDamage, mirrorHealthDamage,
					healthDamage, recursiveMirrorsSuppressed);
		}
	}
}
