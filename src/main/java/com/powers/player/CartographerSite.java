package com.powers.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Stable, bounded facts for one player-discovered Cartographer destination. */
public record CartographerSite(String queryKind, String queryTarget, String registryId,
		String dimensionId, int x, int y, int z, long discoveredAt) {
	public static final Codec<CartographerSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("query_kind").forGetter(CartographerSite::queryKind),
			Codec.STRING.fieldOf("query_target").forGetter(CartographerSite::queryTarget),
			Codec.STRING.fieldOf("registry_id").forGetter(CartographerSite::registryId),
			Codec.STRING.fieldOf("dimension").forGetter(CartographerSite::dimensionId),
			Codec.INT.fieldOf("x").forGetter(CartographerSite::x),
			Codec.INT.fieldOf("y").forGetter(CartographerSite::y),
			Codec.INT.fieldOf("z").forGetter(CartographerSite::z),
			Codec.LONG.fieldOf("discovered_at").forGetter(CartographerSite::discoveredAt)
	).apply(instance, CartographerSite::new));

	public CartographerSite {
		queryKind = bounded(queryKind, "query kind").toLowerCase(Locale.ROOT);
		queryTarget = bounded(queryTarget, "query target").toLowerCase(Locale.ROOT);
		registryId = bounded(registryId, "registry id").toLowerCase(Locale.ROOT);
		dimensionId = bounded(dimensionId, "dimension").toLowerCase(Locale.ROOT);
		if (!queryKind.equals("structure") && !queryKind.equals("biome")
				&& !queryKind.equals("landmark")) {
			throw new IllegalArgumentException("Unknown Cartographer query kind: " + queryKind);
		}
		if (Identifier.tryParse(registryId) == null || Identifier.tryParse(dimensionId) == null
				|| discoveredAt < 0L) {
			throw new IllegalArgumentException("Cartographer site identity is malformed");
		}
	}

	private static String bounded(String value, String label) {
		String normalized = Objects.requireNonNull(value, label).trim();
		if (normalized.isEmpty() || normalized.length() > 128) {
			throw new IllegalArgumentException("Cartographer " + label + " must be 1..128 characters");
		}
		return normalized;
	}
}
