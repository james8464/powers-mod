package com.powers.mind;

/** Selects the valid player handle used to complete a mind-body return. */
final class BodyReturnOwnerPolicy {
	enum Source {
		DIRECT,
		LOOKUP,
		MISSING
	}

	private BodyReturnOwnerPolicy() {
	}

	static Source resolve(boolean directMatchesOwner, boolean lookupAvailable) {
		if (directMatchesOwner) return Source.DIRECT;
		return lookupAvailable ? Source.LOOKUP : Source.MISSING;
	}
}
