package com.powers.client;

import com.powers.client.screen.GrimoireIndexScreen;
import com.powers.client.screen.ArtifactCatalogueScreen;
import com.powers.client.screen.RainbowConvergenceScreen;
import com.powers.client.screen.ShadowSwordScreen;
import com.powers.client.screen.TeleportInputScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientActionRefreshTest {
	@Test
	void invalidationClosesOnlyItsOwningSurface() {
		assertTrue(ClientActionRefresh.shouldClose("crystal", RainbowConvergenceScreen.class));
		assertFalse(ClientActionRefresh.shouldClose("crystal", GrimoireIndexScreen.class));
		assertFalse(ClientActionRefresh.shouldClose("crystal", ShadowSwordScreen.class));
		assertTrue(ClientActionRefresh.shouldClose("artifact", ShadowSwordScreen.class));
		assertTrue(ClientActionRefresh.shouldClose("artifact", ArtifactCatalogueScreen.class));
		assertTrue(ClientActionRefresh.shouldClose("artifact",
				TeleportInputScreen.OwnerSurface.ARTIFACT));
		assertFalse(ClientActionRefresh.shouldClose("artifact",
				TeleportInputScreen.OwnerSurface.INNATE));
		assertFalse(ClientActionRefresh.shouldClose("artifact", RainbowConvergenceScreen.class));
		assertTrue(ClientActionRefresh.shouldClose("grimoire", GrimoireIndexScreen.class));
	}
}
