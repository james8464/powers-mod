package com.powers.network;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

/** Lifecycle-maintained index for uniquely named, currently loaded mobs. */
public final class NamedLivingTargetIndex {
	private static final Map<MinecraftServer, Map<ResourceKey<Level>, UniqueNameIndex<UUID>>> INDEXES =
			new WeakHashMap<>();

	private NamedLivingTargetIndex() {
	}

	public static void track(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		Map<ResourceKey<Level>, UniqueNameIndex<UUID>> indexes = INDEXES.computeIfAbsent(
				level.getServer(), ignored -> new java.util.HashMap<>());
		UUID id = entity.getUUID();
		indexes.values().forEach(index -> index.remove(id));
		if (!(entity instanceof Mob mob) || !mob.hasCustomName()) {
			pruneEmpty(indexes);
			return;
		}
		Component name = mob.getCustomName();
		if (name == null) pruneEmpty(indexes);
		else indexes.computeIfAbsent(level.dimension(), ignored -> new UniqueNameIndex<>())
				.upsert(id, name.getString());
	}

	public static void untrack(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		Map<ResourceKey<Level>, UniqueNameIndex<UUID>> indexes = INDEXES.get(level.getServer());
		if (indexes == null) return;
		indexes.values().forEach(index -> index.remove(entity.getUUID()));
		pruneEmpty(indexes);
	}

	public static void appendMatches(MinecraftServer server, String requestedName,
			List<NamedTargetRules.Candidate<LivingEntity>> matches) {
		Map<ResourceKey<Level>, UniqueNameIndex<UUID>> indexes = INDEXES.get(server);
		if (indexes == null || matches.size() >= 2) return;
		var dimensions = new java.util.ArrayList<>(indexes.entrySet());
		dimensions.sort(java.util.Comparator.comparing(entry ->
				entry.getKey().identifier().toString()));
		for (var dimension : dimensions) {
			ServerLevel level = server.getLevel(dimension.getKey());
			UniqueNameIndex<UUID> index = dimension.getValue();
			for (UUID id : index.candidates(requestedName, 2 - matches.size())) {
				Entity entity = level == null ? null : level.getEntity(id);
				if (!(entity instanceof Mob mob) || !mob.isAlive() || !mob.hasCustomName()) {
					index.removeStale(id);
					continue;
				}
				Component name = mob.getCustomName();
				if (name == null || !NamedTargetRules.matches(requestedName, name.getString())) {
					track(mob);
					continue;
				}
				matches.add(new NamedTargetRules.Candidate<>(mob, name.getString()));
				if (matches.size() >= 2) return;
			}
		}
	}

	/** Resolves a unique online player username or loaded custom mob/test-actor name. */
	public static NamedTargetRules.Resolution<LivingEntity> resolve(
			MinecraftServer server, String requestedName) {
		List<NamedTargetRules.Candidate<LivingEntity>> matches = new java.util.ArrayList<>(2);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (NamedTargetRules.matches(requestedName, player.getName().getString())) {
				matches.add(new NamedTargetRules.Candidate<>(player, player.getName().getString()));
				if (matches.size() >= 2) return NamedTargetRules.resolve(requestedName, matches);
			}
		}
		appendMatches(server, requestedName, matches);
		return NamedTargetRules.resolve(requestedName, matches);
	}

	public static void clearAll() {
		INDEXES.values().forEach(indexes -> indexes.values().forEach(UniqueNameIndex::clear));
		INDEXES.clear();
	}

	/** Aggregate bounded-name index counters for one server. */
	public static UniqueNameIndex.Diagnostics diagnostics(MinecraftServer server) {
		long queries = 0L, candidates = 0L, misses = 0L, stale = 0L, memory = 0L;
		int entries = 0, names = 0;
		for (UniqueNameIndex.Diagnostics value : diagnosticsByDimension(server).values()) {
			queries += value.queries();
			candidates += value.candidates();
			misses += value.misses();
			stale += value.staleRemovals();
			entries += value.entries();
			names += value.names();
			memory += value.estimatedBytes();
		}
		return new UniqueNameIndex.Diagnostics(queries, candidates, misses, stale, entries, names, memory);
	}

	/** Per-dimension name-index work and footprint for operator diagnostics. */
	public static Map<String, UniqueNameIndex.Diagnostics> diagnosticsByDimension(
			MinecraftServer server) {
		Map<ResourceKey<Level>, UniqueNameIndex<UUID>> indexes = INDEXES.get(server);
		if (indexes == null) return Map.of();
		Map<String, UniqueNameIndex.Diagnostics> result = new TreeMap<>();
		indexes.forEach((dimension, index) ->
				result.put(dimension.identifier().toString(), index.diagnostics()));
		return java.util.Collections.unmodifiableMap(result);
	}

	private static void pruneEmpty(Map<ResourceKey<Level>, UniqueNameIndex<UUID>> indexes) {
		indexes.entrySet().removeIf(entry -> entry.getValue().diagnostics().entries() == 0);
	}
}
