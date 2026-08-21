package com.powers.client;

import com.powers.client.screen.GrimoireIndexScreen;
import com.powers.client.screen.ArtifactCatalogueScreen;
import com.powers.client.screen.RainbowConvergenceScreen;
import com.powers.client.screen.ShadowSwordScreen;
import com.powers.client.screen.TeleportInputScreen;
import net.minecraft.client.gui.screens.Screen;

/** Routes authoritative invalidations without closing an unrelated client surface. */
public final class ClientActionRefresh {
	private ClientActionRefresh() {
	}

	public static boolean shouldClose(String surface, Screen current) {
		if (current instanceof TeleportInputScreen teleport) {
			return shouldClose(surface, teleport.ownerSurface());
		}
		return current != null && shouldClose(surface, current.getClass());
	}

	static boolean shouldClose(String surface, TeleportInputScreen.OwnerSurface ownerSurface) {
		return "artifact".equals(surface)
				&& ownerSurface == TeleportInputScreen.OwnerSurface.ARTIFACT;
	}

	static boolean shouldClose(String surface, Class<? extends Screen> current) {
		if (surface == null || current == null) return false;
		return switch (surface) {
			case "artifact" -> ShadowSwordScreen.class.isAssignableFrom(current)
					|| ArtifactCatalogueScreen.class.isAssignableFrom(current);
			case "crystal" -> RainbowConvergenceScreen.class.isAssignableFrom(current);
			case "grimoire" -> GrimoireIndexScreen.class.isAssignableFrom(current);
			default -> false;
		};
	}
}
