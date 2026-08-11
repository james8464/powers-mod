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
import java.util.UUID;
import java.util.WeakHashMap;

/** Lifecycle-maintained index for uniquely named, currently loaded mobs. */
public final class NamedLivingTargetIndex {
	private record TargetRef(UUID id, ResourceKey<Level> dimension) {
	}

	private static final Map<MinecraftServer, UniqueNameIndex<TargetRef>> INDEXES = new WeakHashMap<>();

	private NamedLivingTargetIndex() {
	}

	public static void track(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		TargetRef ref = new TargetRef(entity.getUUID(), level.dimension());
		UniqueNameIndex<TargetRef> index = INDEXES.computeIfAbsent(level.getServer(),
				ignored -> new UniqueNameIndex<>());
		if (!(entity instanceof Mob mob) || !mob.hasCustomName()) {
			index.remove(ref);
			return;
		}
		Component name = mob.getCustomName();
		if (name == null) index.remove(ref);
		else index.upsert(ref, name.getString());
	}

	public static void untrack(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		UniqueNameIndex<TargetRef> index = INDEXES.get(level.getServer());
		if (index != null) index.remove(new TargetRef(entity.getUUID(), level.dimension()));
	}

	public static void appendMatches(MinecraftServer server, String requestedName,
			List<NamedTargetRules.Candidate<LivingEntity>> matches) {
		UniqueNameIndex<TargetRef> index = INDEXES.get(server);
		if (index == null || matches.size() >= 2) return;
		for (TargetRef ref : index.candidates(requestedName, 2)) {
			ServerLevel level = server.getLevel(ref.dimension());
			Entity entity = level == null ? null : level.getEntity(ref.id());
			if (!(entity instanceof Mob mob) || !mob.isAlive() || !mob.hasCustomName()) {
				index.removeStale(ref);
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
		INDEXES.values().forEach(UniqueNameIndex::clear);
		INDEXES.clear();
	}

	/** Aggregate bounded-name index counters for one server. */
	public static UniqueNameIndex.Diagnostics diagnostics(MinecraftServer server) {
		UniqueNameIndex<TargetRef> index = INDEXES.get(server);
		return index == null ? new UniqueNameIndex.Diagnostics(0, 0, 0, 0, 0, 0, 0)
				: index.diagnostics();
	}
}
