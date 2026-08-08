package com.powers.mind;

import java.util.Locale;

public enum BodyProxyKind {
	REALM,
	ASTRAL,
	MARKING,
	POSSESSION,
	DREAMWALK;

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static BodyProxyKind fromSerialized(String name) {
		if (name != null) {
			for (BodyProxyKind kind : values()) {
				if (kind.serializedName().equals(name)) return kind;
			}
		}
		return REALM;
	}
}
