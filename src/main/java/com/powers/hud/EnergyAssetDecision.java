package com.powers.hud;

/** Chooses authored atlas art or a same-layout procedural ten-symbol fallback. */
public enum EnergyAssetDecision {
	AUTHORED_ATLAS,
	PROCEDURAL_SYMBOLS;

	public static EnergyAssetDecision resolve(boolean atlasAvailable) {
		return atlasAvailable ? AUTHORED_ATLAS : PROCEDURAL_SYMBOLS;
	}
}
