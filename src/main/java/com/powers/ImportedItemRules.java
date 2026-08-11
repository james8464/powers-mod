package com.powers;

/** Classification rules that keep texture-composition helpers out of gameplay. */
public final class ImportedItemRules {
	private static final String IMPORTED_PREFIX = "imported_";

	private ImportedItemRules() {
	}

	public static boolean isLegacyAssetLayer(String idOrTexture) {
		if (idOrTexture == null) return false;
		String texture = idOrTexture.startsWith(IMPORTED_PREFIX)
				? idOrTexture.substring(IMPORTED_PREFIX.length()) : idOrTexture;
		return texture.equals("artifact_runestone_back")
				|| texture.startsWith("artifact_runestone_overlay_")
				|| texture.equals("device_miniportal_active");
	}
}
