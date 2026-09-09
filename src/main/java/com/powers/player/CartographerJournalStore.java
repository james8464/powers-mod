package com.powers.player;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import static com.powers.player.PlayerPowerAttachments.CARTOGRAPHER_SITES;

/** Persistent copy-on-write adapter for the Cartographer discovered-site journal. */
public final class CartographerJournalStore {
	private CartographerJournalStore() {
	}

	public static boolean discover(AttachmentTarget target, CartographerSite discovered) {
		List<CartographerSite> current = read(target);
		List<CartographerSite> updated = CartographerJournalRules.discover(current, discovered);
		if (updated.equals(current)) return false;
		target.setAttached(CARTOGRAPHER_SITES, updated);
		return true;
	}

	public static Optional<CartographerSite> find(AttachmentTarget target, String dimensionId,
			String queryKind, String queryTarget) {
		return CartographerJournalRules.find(read(target), dimensionId, queryKind, queryTarget);
	}

	public static List<CartographerSite> read(AttachmentTarget target) {
		return List.copyOf(target.getAttachedOrElse(CARTOGRAPHER_SITES, List.of()));
	}
}
