package com.powers.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Save-backed origin used to recover safely after a relog or server restart. */
public record MindBodyState(String dimension, double x, double y, double z,
		float yRot, float xRot, String gameMode, String kind) {
	public static final Codec<MindBodyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("dimension").forGetter(MindBodyState::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(MindBodyState::x),
			Codec.DOUBLE.fieldOf("y").forGetter(MindBodyState::y),
			Codec.DOUBLE.fieldOf("z").forGetter(MindBodyState::z),
			Codec.FLOAT.fieldOf("y_rot").forGetter(MindBodyState::yRot),
			Codec.FLOAT.fieldOf("x_rot").forGetter(MindBodyState::xRot),
			Codec.STRING.fieldOf("game_mode").forGetter(MindBodyState::gameMode),
			Codec.STRING.fieldOf("kind").forGetter(MindBodyState::kind)
	).apply(instance, MindBodyState::new));

	public BodyProxyKind proxyKind() {
		return BodyProxyKind.fromSerialized(kind);
	}
}
