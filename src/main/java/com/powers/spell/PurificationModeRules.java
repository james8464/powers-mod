package com.powers.spell;

/** Pure selection and effect-removal policy for Purification Circle variants. */
public final class PurificationModeRules {
	private PurificationModeRules() {
	}

	public enum Mode { CLEANSE, LINK_SEVER, CORRUPTION_RELIEF }

	public static Mode fromCatalyst(String itemId) {
		if (itemId == null || !itemId.startsWith("powers:")) return Mode.CLEANSE;
		String path = itemId.substring(itemId.indexOf(':') + 1);
		if (path.equals("imported_magic_essence_soul_dust")
				|| path.startsWith("imported_artifact_bound_runestone_")) {
			return Mode.LINK_SEVER;
		}
		if (path.equals("imported_magic_essence_blood_dust")
				|| path.startsWith("imported_artifact_runestone_dark_")) {
			return Mode.CORRUPTION_RELIEF;
		}
		return Mode.CLEANSE;
	}

	public static boolean removesEffect(Mode mode, boolean harmful,
			boolean amethystPoisoning, boolean ownedCorruption) {
		if (amethystPoisoning || mode == null) return false;
		return switch (mode) {
			case CLEANSE -> harmful && !ownedCorruption;
			case LINK_SEVER -> false;
			case CORRUPTION_RELIEF -> ownedCorruption;
		};
	}
}
