package com.powers.magic.runtime;

import com.powers.magic.InteractionOutcome;
import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionId;
import com.powers.magic.MagicInteractionResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicInteractionArbitratorTest {
	@Test
	void threeWayOrderingIsStableAndPrioritizesCancellation() {
		var catalogue = MagicActionCatalogue.defaults();
		var resolver = MagicInteractionResolver.defaults(catalogue);
		var fire = catalogue.definition(new MagicActionId("fireball"));
		var ice = catalogue.definition(new MagicActionId("ice_manipulation"));
		var anchor = catalogue.definition(new MagicActionId("dimensional_anchor"));
		var travel = catalogue.definition(new MagicActionId("time_shift"));
		var transform = resolver.resolve(fire, ice, com.powers.magic.InteractionContext.DEFAULT);
		var cancel = resolver.resolve(travel, anchor, com.powers.magic.InteractionContext.DEFAULT);

		assertEquals(List.of(InteractionOutcome.CANCEL, InteractionOutcome.TRANSFORM),
				MagicInteractionArbitrator.order(List.of(transform, cancel)).stream()
						.map(com.powers.magic.InteractionResolution::outcome).toList());
		assertEquals(MagicInteractionArbitrator.order(List.of(transform, cancel)),
				MagicInteractionArbitrator.order(List.of(cancel, transform)));
	}
}
