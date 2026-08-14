package com.powers.protection;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Representative claim adapter contract across all protected magic families. */
class PowerProtectionAdaptersTest {
	@AfterEach
	void clear() {
		PowerProtectionAdapters.clearForTests();
	}

	@Test
	void oneClaimProviderCanDenyEveryDocumentedActionConsistently() {
		PowerProtectionAdapters.register("test_claims", 100,
				query -> query.position() == null || query.position().getX() != 7);
		for (ProtectionAction action : ProtectionAction.values()) {
			assertFalse(PowerProtectionAdapters.allows(
					new ProtectionQuery(action, null, new BlockPos(7, 64, 0), null, null)), action.name());
			assertTrue(PowerProtectionAdapters.allows(
					new ProtectionQuery(action, null, new BlockPos(8, 64, 0), null, null)), action.name());
		}
	}

	@Test
	void providerFailureFailsClosedAndDuplicateIdentityCannotReplacePolicy() {
		assertTrue(PowerProtectionAdapters.register("claims", 10, query -> false));
		assertFalse(PowerProtectionAdapters.register("claims", 20, query -> true));
		assertFalse(PowerProtectionAdapters.allows(new ProtectionQuery(
				ProtectionAction.PORTAL, null, BlockPos.ZERO, null, null)));

		PowerProtectionAdapters.clearForTests();
		PowerProtectionAdapters.register("broken", 10, query -> { throw new IllegalStateException("boom"); });
		assertFalse(PowerProtectionAdapters.allows(new ProtectionQuery(
				ProtectionAction.RITUAL, null, BlockPos.ZERO, null, null)));
	}

	@Test
	void blockWorkPolicyIdentityIsBoundedAndTracksTheProviderSet() {
		long builtInOnly = PowerProtectionAdapters.blockWorkPolicyId();
		PowerProtectionAdapters.register("later", 1, query -> true);
		PowerProtectionAdapters.register("first", 100, query -> true);
		long twoProviders = PowerProtectionAdapters.blockWorkPolicyId();

		assertFalse(builtInOnly == twoProviders);
		PowerProtectionAdapters.clearForTests();
		PowerProtectionAdapters.register("first", 100, query -> true);
		PowerProtectionAdapters.register("later", 1, query -> true);
		assertTrue(twoProviders == PowerProtectionAdapters.blockWorkPolicyId());
	}

	@Test
	void unknownActionLinkageFailuresFromOlderAdaptersFailClosed() {
		PowerProtectionAdapters.register("legacy_exhaustive_switch", 10,
				query -> { throw new IncompatibleClassChangeError("new enum constant"); });

		assertFalse(PowerProtectionAdapters.allows(new ProtectionQuery(
				ProtectionAction.OBSERVE, null, BlockPos.ZERO, null, null)));
	}
}
