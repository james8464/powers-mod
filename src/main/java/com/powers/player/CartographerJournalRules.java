package com.powers.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pure copy-on-write rules for the persistent discovered-site journal. */
public final class CartographerJournalRules {
	public static final int MAX_SITES = 32;

	private CartographerJournalRules() {
	}

	public static List<CartographerSite> discover(List<CartographerSite> current,
			CartographerSite discovered) {
		List<CartographerSite> updated = new ArrayList<>(current.size() + 1);
		for (CartographerSite site : current) {
			if (!sameIdentity(site, discovered)) updated.add(site);
		}
		updated.add(discovered);
		if (updated.size() > MAX_SITES) {
			updated = new ArrayList<>(updated.subList(updated.size() - MAX_SITES, updated.size()));
		}
		return List.copyOf(updated);
	}

	public static Optional<CartographerSite> find(List<CartographerSite> sites,
			String dimensionId, String queryKind, String queryTarget) {
		for (int index = sites.size() - 1; index >= 0; index--) {
			CartographerSite site = sites.get(index);
			if (site.dimensionId().equals(dimensionId) && site.queryKind().equals(queryKind)
					&& site.queryTarget().equals(queryTarget)) return Optional.of(site);
		}
		return Optional.empty();
	}

	private static boolean sameIdentity(CartographerSite left, CartographerSite right) {
		return left.dimensionId().equals(right.dimensionId())
				&& left.queryKind().equals(right.queryKind())
				&& left.queryTarget().equals(right.queryTarget());
	}
}
