package com.powers.power.abilities;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Pure, coordinate-free facts for the Astral Projection return cue. */
public final class AstralReturnIndicator {
	private static final String[] COMPASS = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
	private static final int MAX_DISPLAY_DISTANCE = 9_999;

	private AstralReturnIndicator() {
	}

	public static Cue resolve(String bodyDimension, Vec3 bodyPosition,
			String spiritDimension, Vec3 spiritPosition) {
		Objects.requireNonNull(bodyDimension, "bodyDimension");
		Objects.requireNonNull(bodyPosition, "bodyPosition");
		Objects.requireNonNull(spiritDimension, "spiritDimension");
		Objects.requireNonNull(spiritPosition, "spiritPosition");
		if (!bodyDimension.equals(spiritDimension)) {
			return new Cue(false, bodyDimension, "", -1);
		}
		double dx = bodyPosition.x - spiritPosition.x;
		double dz = bodyPosition.z - spiritPosition.z;
		int octant = Math.floorMod((int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4.0)), 8);
		int distance = Math.min(MAX_DISPLAY_DISTANCE, (int) Math.round(Math.hypot(dx, dz)));
		return new Cue(true, bodyDimension, COMPASS[octant], distance);
	}

	public static Component message(Cue cue) {
		Component dimension = dimensionName(cue.dimension());
		return cue.sameDimension()
				? Component.translatable("ability.powers.astral_return.same",
						cue.direction(), cue.distanceBlocks(), dimension)
				: Component.translatable("ability.powers.astral_return.other", dimension);
	}

	private static Component dimensionName(String identifier) {
		int separator = identifier.indexOf(':');
		if (separator <= 0 || separator == identifier.length() - 1) return Component.literal(identifier);
		return Component.translatable("dimension." + identifier.substring(0, separator)
				+ "." + identifier.substring(separator + 1));
	}

	/** The record intentionally has no body coordinates or entity identity. */
	public record Cue(boolean sameDimension, String dimension, String direction, int distanceBlocks) {
		public Cue {
			dimension = Objects.requireNonNull(dimension, "dimension");
			direction = Objects.requireNonNull(direction, "direction");
			if (sameDimension && (direction.isBlank() || distanceBlocks < 0)) {
				throw new IllegalArgumentException("same-dimension cues need direction and distance");
			}
			if (!sameDimension && (!direction.isEmpty() || distanceBlocks != -1)) {
				throw new IllegalArgumentException("cross-dimension cues must redact direction and distance");
			}
		}
	}
}
