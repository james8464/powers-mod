package com.powers.power;

/** Controls which allegiance may receive an innate power; artifacts may bypass it. */
public enum PowerAffinity {
	UNIVERSAL,
	RADIANT,
	DARKNESS;

	public boolean permits(PowerAffinity allegiance) {
		return this == UNIVERSAL || this == allegiance;
	}
}
