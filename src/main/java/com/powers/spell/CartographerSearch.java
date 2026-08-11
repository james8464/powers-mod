package com.powers.spell;

import com.mojang.datafixers.util.Pair;
import com.powers.PowersMod;
import com.powers.realm.MemorySite;
import com.powers.realm.RealmKind;
import com.powers.realm.RealmLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Locale;
import java.util.Optional;

/** Bounded, non-teleporting world lookup used by Cartographer's Star. */
public final class CartographerSearch {
	private static final int STRUCTURE_RADIUS_CHUNKS = 64;
	private static final int BIOME_RADIUS_BLOCKS = 4_096;
	private static final int BIOME_HORIZONTAL_STEP = 64;
	private static final int BIOME_VERTICAL_STEP = 64;

	public record Result(BlockPos position, String registryId) { }

	private CartographerSearch() {
	}

	public static boolean isKnownTarget(ServerLevel level, CartographerQuery query) {
		Identifier id = Identifier.tryParse(query.target());
		return switch (query.kind()) {
			case STRUCTURE -> id != null && level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
					.containsKey(id);
			case BIOME -> id != null && level.registryAccess().lookupOrThrow(Registries.BIOME)
					.containsKey(id);
			case LANDMARK -> landmark(level, query.target()).isPresent();
		};
	}

	public static Optional<Result> find(ServerLevel level, BlockPos origin, CartographerQuery query) {
		return switch (query.kind()) {
			case STRUCTURE -> structure(level, origin, query.target());
			case BIOME -> biome(level, origin, query.target());
			case LANDMARK -> landmark(level, query.target());
		};
	}

	private static Optional<Result> structure(ServerLevel level, BlockPos origin, String rawId) {
		Identifier id = Identifier.tryParse(rawId);
		if (id == null) return Optional.empty();
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Optional<Holder.Reference<Structure>> holder = registry.get(ResourceKey.create(Registries.STRUCTURE, id));
		if (holder.isEmpty()) return Optional.empty();
		Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, HolderSet.direct(holder.get()), origin,
						STRUCTURE_RADIUS_CHUNKS, false);
		return result == null ? Optional.empty() : Optional.of(new Result(result.getFirst(), id.toString()));
	}

	private static Optional<Result> biome(ServerLevel level, BlockPos origin, String rawId) {
		Identifier id = Identifier.tryParse(rawId);
		if (id == null) return Optional.empty();
		ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
		var registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
		if (!registry.containsKey(key)) return Optional.empty();
		Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(holder -> holder.is(key), origin,
				BIOME_RADIUS_BLOCKS, BIOME_HORIZONTAL_STEP, BIOME_VERTICAL_STEP);
		return result == null ? Optional.empty() : Optional.of(new Result(result.getFirst(), id.toString()));
	}

	private static Optional<Result> landmark(ServerLevel level, String name) {
		RealmKind kind = realmKind(level);
		if (kind == null) return Optional.empty();
		String normalized = name.toLowerCase(Locale.ROOT);
		return RealmLayout.sites(kind).stream()
				.filter(site -> matches(site, normalized))
				.findFirst()
				.map(site -> new Result(new BlockPos(site.x(), level.getMinY() + 1, site.z()),
						"powers:" + site.id()));
	}

	private static boolean matches(MemorySite site, String name) {
		return site.id().equals(name)
				|| site.landmarkType().name().toLowerCase(Locale.ROOT).equals(name);
	}

	private static RealmKind realmKind(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		if (dimension.equals(PowersMod.id("light_realm"))) return RealmKind.LIGHT;
		if (dimension.equals(PowersMod.id("dark_realm"))) return RealmKind.DARK;
		return null;
	}
}
