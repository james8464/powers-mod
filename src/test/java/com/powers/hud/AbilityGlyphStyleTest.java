package com.powers.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityGlyphStyleTest {
	@Test
	void semanticIdsResolveToReusableGlyphFamilies() {
		assertEquals(AbilityGlyphStyle.LIGHTNING, AbilityGlyphStyle.forAbility("lightning_strike"));
		assertEquals(AbilityGlyphStyle.FLAME, AbilityGlyphStyle.forAbility("fireball"));
		assertEquals(AbilityGlyphStyle.FROST, AbilityGlyphStyle.forAbility("ice_manipulation"));
		assertEquals(AbilityGlyphStyle.SHADOW, AbilityGlyphStyle.forAbility("void_beam"));
		assertEquals(AbilityGlyphStyle.GENERIC, AbilityGlyphStyle.forAbility("unknown_rite"));
		assertEquals(AbilityGlyphStyle.EMPTY, AbilityGlyphStyle.forAbility(null));
	}
}
