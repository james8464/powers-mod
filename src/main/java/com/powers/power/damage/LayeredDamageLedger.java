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
	private double mirrorActualHealthRemoved;
	private double actualHealthRemoved;

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
			float healthRemoved) {
		mirrorRequestedDamage = add(mirrorRequestedDamage, decision.requestedDamage());
		mirrorForwardedDamage = add(mirrorForwardedDamage, decision.forwardedDamage());
		mirrorActualHealthRemoved = add(mirrorActualHealthRemoved, healthRemoved);
	}

	/** Records only health removed between entry and return of one damage call. */
	public synchronized void recordHealthTransition(float before, float after) {
		actualHealthRemoved = add(actualHealthRemoved, LayeredDamageResolver.health(before, after));
	}

	public synchronized Snapshot snapshot() {
		return new Snapshot(incomingDamage, wardBlockedDamage, wardIntegritySpent,
				mirrorRequestedDamage, mirrorForwardedDamage, mirrorActualHealthRemoved,
				actualHealthRemoved);
	}

	public synchronized void clear() {
		incomingDamage = 0.0;
		wardBlockedDamage = 0.0;
		wardIntegritySpent = 0.0;
		mirrorRequestedDamage = 0.0;
		mirrorForwardedDamage = 0.0;
		mirrorActualHealthRemoved = 0.0;
		actualHealthRemoved = 0.0;
	}

	private static double add(double total, double value) {
		if (!Double.isFinite(value) || value <= 0.0) return total;
		return total >= Double.MAX_VALUE - value ? Double.MAX_VALUE : total + value;
	}

	public record Snapshot(double incomingDamage, double wardBlockedDamage,
			double wardIntegritySpent, double mirrorRequestedDamage,
			double mirrorForwardedDamage, double mirrorActualHealthRemoved,
			double actualHealthRemoved) {
		public String line() {
			return String.format(Locale.ROOT,
					"damageLayers: order=forcefield>soul_link>health; incoming=%.1f; "
							+ "wardBlocked=%.1f; wardIntegritySpent=%.1f; "
							+ "mirrorRequested=%.1f; mirrorForwarded=%.1f; "
							+ "mirrorActualHealthRemoved=%.1f; actualHealthRemoved=%.1f",
					incomingDamage, wardBlockedDamage, wardIntegritySpent,
					mirrorRequestedDamage, mirrorForwardedDamage,
					mirrorActualHealthRemoved, actualHealthRemoved);
		}
	}
}
