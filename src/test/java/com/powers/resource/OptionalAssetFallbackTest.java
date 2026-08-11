package com.powers.resource;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionalAssetFallbackTest {
	@Test
	void missingOptionalArtUsesVisibleCoreAssetWithoutThrowing() {
		Identifier requested = Identifier.parse("powers:textures/optional/missing.png");
		Identifier visible = Identifier.parse("powers:textures/gui/energy_symbols.png");
		assertEquals(visible, OptionalAssetFallback.resolve(requested, visible, ignored -> false));
	}

	@Test
	void presentOptionalArtIsRetained() {
		Identifier requested = Identifier.parse("powers:textures/imported/gui/hud_icons_wild.png");
		assertEquals(requested, OptionalAssetFallback.resolve(requested,
				Identifier.parse("powers:textures/gui/energy_symbols.png"), requested::equals));
	}
}
