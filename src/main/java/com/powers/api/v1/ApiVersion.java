package com.powers.api.v1;

/** Semantic public-API version; major changes are binary incompatible, minor changes are additive. */
public record ApiVersion(int major, int minor) {
	public ApiVersion {
		if (major < 0 || minor < 0) throw new IllegalArgumentException("API version cannot be negative");
	}

	@Override public String toString() { return major + "." + minor; }
}
