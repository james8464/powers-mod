package com.powers.power.damage;

import java.util.List;

/** Pure numerical contract for forcefield, Soul Link, and final-health damage. */
public final class LayeredDamageResolver {
	public enum Stage { FORCEFIELD, SOUL_LINK, HEALTH }

	private static final List<Stage> ORDER = List.of(
			Stage.FORCEFIELD, Stage.SOUL_LINK, Stage.HEALTH);

	private LayeredDamageResolver() {
	}

	/** The only legal layer order for a damage attempt. */
	public static List<Stage> order() {
		return ORDER;
	}

	/**
	 * Resolves the sacrificial ward boundary. A present ward blocks the whole
	 * impact, while spending at most the integrity that actually existed.
	 */
	public static WardDecision ward(float incomingDamage, float integrity, boolean present) {
		float incoming = positive(incomingDamage);
		float available = positive(integrity);
		boolean effective = present && available > 0.0F && incoming > 0.0F;
		if (!effective) return new WardDecision(false, incoming, 0.0F, 0.0F, available, false);
		float spent = Math.min(incoming, available);
		float remaining = Math.max(0.0F, available - spent);
		return new WardDecision(true, incoming, incoming, spent, remaining, remaining <= 0.0F);
	}

	/** Caps exactly one Soul Link forwarding pass for one target. */
	public static MirrorDecision mirror(float healthWound, double multiplier, float remainingCap) {
		float wound = positive(healthWound);
		double scale = Double.isFinite(multiplier) ? Math.max(0.0, multiplier) : 0.0;
		float requested = positive((float) (wound * scale));
		float forwarded = Math.min(requested, positive(remainingCap));
		return new MirrorDecision(wound, requested, forwarded);
	}

	/** Measures only health removed after Minecraft's mitigation and event hooks. */
	public static float health(float before, float after) {
		if (!Float.isFinite(before) || !Float.isFinite(after)) return 0.0F;
		return Math.max(0.0F, before - after);
	}

	private static float positive(float value) {
		return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
	}

	public record WardDecision(boolean present, float incomingDamage, float blockedDamage,
			float integritySpent, float remainingIntegrity, boolean shattered) { }

	public record MirrorDecision(float healthWound, float requestedDamage,
			float forwardedDamage) { }
}
