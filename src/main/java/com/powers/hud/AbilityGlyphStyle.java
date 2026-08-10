package com.powers.hud;

/** Semantic procedural glyph families shared by HUD and artifact interfaces. */
public enum AbilityGlyphStyle {
	EMPTY,
	LIGHTNING,
	FLAME,
	AIR,
	TIME,
	FROST,
	SHADOW,
	HEALING,
	SPEED,
	GENERIC;

	public static AbilityGlyphStyle forAbility(String abilityId) {
		if (abilityId == null || abilityId.isBlank()) return EMPTY;
		if (abilityId.contains("lightning") || abilityId.contains("thunder")) return LIGHTNING;
		if (abilityId.contains("fire") || abilityId.contains("campfire")) return FLAME;
		if (abilityId.contains("flight") || abilityId.contains("breezy")) return AIR;
		if (abilityId.contains("time") || abilityId.contains("chrono")) return TIME;
		if (abilityId.contains("ice") || abilityId.contains("frost")) return FROST;
		if (abilityId.contains("shadow") || abilityId.contains("void")
				|| abilityId.contains("invisibility") || abilityId.contains("darkness")) return SHADOW;
		if (abilityId.contains("plant") || abilityId.contains("health")
				|| abilityId.contains("healing") || abilityId.contains("bloom")) return HEALING;
		if (abilityId.contains("speed") || abilityId.contains("stride")) return SPEED;
		return GENERIC;
	}
}
