package com.powers.companion;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Persistent high-level instruction for one Shadow manifestation. */
public enum ShadowStance {
	FOLLOW,
	STAY,
	GUARD,
	TASK,
	DOWNED;

	public static final Codec<ShadowStance> CODEC = Codec.STRING.xmap(
			ShadowStance::fromSerializedName, ShadowStance::serializedName);

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static ShadowStance fromSerializedName(String name) {
		if (name != null) {
			for (ShadowStance stance : values()) {
				if (stance.serializedName().equals(name)) return stance;
			}
		}
		return FOLLOW;
	}
}
