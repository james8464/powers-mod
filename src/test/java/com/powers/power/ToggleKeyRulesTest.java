package com.powers.power;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleKeyRulesTest {
	@Test
	void canonicalAndSwordOwnedKeysShareOneLogicalToggle() {
		Identifier invisibility = Identifier.fromNamespaceAndPath("powers", "invisibility");
		assertTrue(ToggleKeyRules.ownsAbility("powers:invisibility", invisibility));
		assertTrue(ToggleKeyRules.ownsAbility(
				"shadow_sword/innate/invisibility", invisibility));
		assertFalse(ToggleKeyRules.ownsAbility("shadow_sword/innate/flight", invisibility));
	}

	@Test
	void detectsAnAbilityAcrossCanonicalAndArtifactToggleSets() {
		Identifier time = Identifier.fromNamespaceAndPath("powers", "time_freeze");
		assertTrue(ToggleKeyRules.anyOwnsAbility(
				List.of("shadow_sword/innate/time_freeze"), time));
		assertTrue(ToggleKeyRules.anyOwnsAbility(List.of("powers:time_freeze"), time));
		assertFalse(ToggleKeyRules.anyOwnsAbility(List.of("powers:flight"), time));
	}

	@Test
	void removesEveryInvocationOfAResetOnlyToggleWithoutTouchingOthers() {
		Identifier time = Identifier.fromNamespaceAndPath("powers", "time_freeze");
		assertEquals(List.of("powers:flight"), ToggleKeyRules.withoutAbility(List.of(
				"powers:time_freeze", "artifact/darkness/innate/time_freeze", "powers:flight"), time));
	}

	@Test
	void onlyCurrentArtifactNamespacesSurviveInnateLoadoutReplacement() {
		assertTrue(ToggleKeyRules.isArtifactOwned("artifact/darkness/innate/flight"));
		assertTrue(ToggleKeyRules.isArtifactOwned("artifact/light/innate/invisibility"));
		assertFalse(ToggleKeyRules.isArtifactOwned("shadow_sword/innate/flight"));
		assertFalse(ToggleKeyRules.isArtifactOwned("corrupt_saved_key"));
	}
}
