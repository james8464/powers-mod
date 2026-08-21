package com.powers.testing.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

/** Server-thread owner for explicit, operator-enabled POWERS packet fault sessions. */
public final class PacketFaultController {
	private record PlayerIdentity(UUID owner, long generation, int entityId, ResourceKey<Level> dimension) { }
	private record LiveMarker(int entityId, ResourceKey<Level> dimension, boolean alive) { }
	private static final class Session {
		private final PacketFaultProfile profile;
		private final PacketFaultEngine engine;
		private final Set<Integer> scopedEntityIds;
		private final Map<UUID, Long> generations = new HashMap<>();
		private final Set<UUID> active = new HashSet<>();
		private final Map<UUID, LiveMarker> liveMarkers = new HashMap<>();

		private Session(PacketFaultProfile profile, Set<Integer> scopedEntityIds) {
			this.profile = profile;
			this.engine = new PacketFaultEngine(profile);
			this.scopedEntityIds = Set.copyOf(scopedEntityIds);
		}

		private boolean includes(ServerPlayer player) {
			return scopedEntityIds.isEmpty() || scopedEntityIds.contains(player.getId());
		}
	}
	private static final class ServerSessions {
		private Session global;
		private final Map<Integer, Session> scoped = new HashMap<>();

		private Set<Session> all() {
			Set<Session> sessions = new HashSet<>(scoped.values());
			if (global != null) sessions.add(global);
			return sessions;
		}
	}

	public record Diagnostics(String profile, long seed, int queueDepth, PacketFaultMetrics metrics) {
		public String line() {
			return "packetFaults: profile=" + profile + "; seed=" + seed + "; queued=" + queueDepth
					+ "; offered=" + metrics.offered() + "; dropped=" + metrics.dropped()
					+ "; duplicated=" + metrics.duplicated() + "; delayed=" + metrics.delayed()
					+ "; reordered=" + metrics.reordered() + "; delivered=" + metrics.delivered()
					+ "; expired=" + metrics.expired() + "; overflowed=" + metrics.overflowed()
					+ "; staleSuppressed=" + metrics.suppressedStale()
					+ "; cancelled=" + metrics.cancelled()
					+ "; duplicateSideEffects=" + metrics.duplicateSideEffects()
					+ "; maxQueue=" + metrics.maximumQueueDepth()
					+ "; maxAgeTicks=" + metrics.maximumAgeTicks();
		}
	}

	private static final Map<MinecraftServer, ServerSessions> SESSIONS = new WeakHashMap<>();

	private PacketFaultController() {
	}

	public static boolean enabled(MinecraftServer server) {
		ServerSessions sessions = SESSIONS.get(server);
		return sessions != null && !sessions.all().isEmpty();
	}

	public static void configure(MinecraftServer server, PacketFaultProfile profile) {
		ServerSessions sessions = SESSIONS.computeIfAbsent(server, ignored -> new ServerSessions());
		if (profile == null || !profile.enabled()) {
			if (sessions.global != null) sessions.global.engine.cancelAll();
			sessions.global = null;
			removeEmpty(server, sessions);
			return;
		}
		if (sessions.global != null) sessions.global.engine.cancelAll();
		sessions.global = createSession(server, profile, Set.of());
	}

	/** Scopes live GameTest faulting so unrelated concurrent test players keep ordinary networking. */
	public static void configureScoped(MinecraftServer server, PacketFaultProfile profile, Set<Integer> entityIds) {
		if (entityIds.isEmpty()) {
			configure(server, profile);
			return;
		}
		ServerSessions sessions = SESSIONS.computeIfAbsent(server, ignored -> new ServerSessions());
		if (profile == null || !profile.enabled()) {
			for (int entityId : entityIds) {
				Session previous = sessions.scoped.remove(entityId);
				if (previous != null) previous.engine.cancelAll();
			}
			removeEmpty(server, sessions);
			return;
		}
		for (int entityId : entityIds) {
			Session previous = sessions.scoped.get(entityId);
			if (previous != null) previous.engine.cancelAll();
		}
		Session session = createSession(server, profile, entityIds);
		for (int entityId : entityIds) sessions.scoped.put(entityId, session);
	}

	/** Exact mock-player scope; entity IDs avoid Fabric GameTest's intentionally reused UUID. */
	public static void configureScoped(MinecraftServer server, PacketFaultProfile profile, ServerPlayer player) {
		configureScoped(server, profile, Set.of(player.getId()));
	}

	public static void joined(ServerPlayer player) {
		ServerSessions sessions = SESSIONS.get(player.level().getServer());
		if (sessions == null) return;
		for (Session session : sessions.all()) if (session.includes(player)) {
			session.generations.merge(player.getUUID(), 1L, Long::sum);
			session.active.add(player.getUUID());
			session.liveMarkers.put(player.getUUID(), marker(player));
		}
	}

	public static void disconnected(MinecraftServer server, UUID owner) {
		ServerSessions sessions = SESSIONS.get(server);
		if (sessions == null) return;
		for (Session session : sessions.all()) {
			Long generation = session.generations.get(owner);
			if (generation != null) session.engine.clear(new PacketFaultConnection(owner, generation));
			session.active.remove(owner);
			session.liveMarkers.remove(owner);
		}
	}

	public static <T> void receive(ServerPlayer received, PacketFaultFamily family, String streamKey, T payload,
			BiConsumer<ServerPlayer, T> handler, Runnable failure) {
		MinecraftServer server = received.level().getServer();
		Session session = sessionFor(server, received);
		if (session == null || !session.profile.enabled() || !session.includes(received)) {
			handler.accept(received, payload);
			return;
		}
		PlayerIdentity identity = identity(session, received);
		session.engine.offer(new PacketFaultConnection(identity.owner(), identity.generation()),
				PacketFaultDirection.SERVERBOUND, family, streamKey, server.getTickCount(),
				value -> {
					ServerPlayer current = resolve(server, identity);
					if (current == null) return false;
					handler.accept(current, value);
					return true;
				}, failure, payload);
	}

	public static <T> boolean send(ServerPlayer recipient, PacketFaultFamily family, String streamKey, T payload,
			BiPredicate<ServerPlayer, T> sender, Runnable failure) {
		MinecraftServer server = recipient.level().getServer();
		Session session = sessionFor(server, recipient);
		if (session == null || !session.profile.enabled() || !session.includes(recipient)) {
			return sender.test(recipient, payload);
		}
		PlayerIdentity identity = identity(session, recipient);
		PacketFaultEngine.OfferResult result = session.engine.offer(
				new PacketFaultConnection(identity.owner(), identity.generation()),
				PacketFaultDirection.CLIENTBOUND, family, streamKey, server.getTickCount(), value -> {
					ServerPlayer current = resolve(server, identity);
					if (current == null) return false;
				return sender.test(current, value);
				}, failure, payload);
		return result != PacketFaultEngine.OfferResult.DROPPED;
	}

	public static void tick(MinecraftServer server) {
		ServerSessions sessions = SESSIONS.get(server);
		if (sessions == null) return;
		for (Session session : sessions.all()) tick(server, session);
	}

	private static void tick(MinecraftServer server, Session session) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!session.includes(player)) continue;
			LiveMarker current = marker(player);
			LiveMarker previous = session.liveMarkers.put(player.getUUID(), current);
			if (previous == null || previous.equals(current)) continue;
			long generation = session.generations.getOrDefault(player.getUUID(), 1L);
			session.engine.clear(new PacketFaultConnection(player.getUUID(), generation));
			session.generations.put(player.getUUID(), generation + 1L);
		}
		session.engine.tick(server.getTickCount());
	}

	public static Diagnostics diagnostics(MinecraftServer server) {
		ServerSessions sessions = SESSIONS.get(server);
		Session session = sessions == null ? null : sessions.global;
		if (session == null && sessions != null && sessions.all().size() == 1) {
			session = sessions.all().iterator().next();
		}
		if (session == null) return new Diagnostics("disabled", 0L, 0, PacketFaultMetrics.empty());
		return new Diagnostics(session.profile.id(), session.profile.seed(),
				session.engine.queueDepth(), session.engine.snapshot());
	}

	public static Diagnostics diagnostics(MinecraftServer server, ServerPlayer player) {
		Session session = sessionFor(server, player);
		if (session == null) return new Diagnostics("disabled", 0L, 0, PacketFaultMetrics.empty());
		return new Diagnostics(session.profile.id(), session.profile.seed(),
				session.engine.queueDepth(), session.engine.snapshot());
	}

	public static void reset(MinecraftServer server) {
		ServerSessions sessions = SESSIONS.get(server);
		if (sessions != null) sessions.all().forEach(session -> session.engine.reset());
	}

	public static void clear(MinecraftServer server) {
		ServerSessions sessions = SESSIONS.remove(server);
		if (sessions != null) sessions.all().forEach(session -> session.engine.cancelAll());
	}

	public static void clearScoped(MinecraftServer server, ServerPlayer player) {
		ServerSessions sessions = SESSIONS.get(server);
		if (sessions == null) return;
		Session removed = sessions.scoped.remove(player.getId());
		if (removed != null && !sessions.scoped.containsValue(removed)) removed.engine.cancelAll();
		removeEmpty(server, sessions);
	}

	private static PlayerIdentity identity(Session session, ServerPlayer player) {
		long generation = session.generations.computeIfAbsent(player.getUUID(), ignored -> 1L);
		return new PlayerIdentity(player.getUUID(), generation, player.getId(), player.level().dimension());
	}

	private static LiveMarker marker(ServerPlayer player) {
		return new LiveMarker(player.getId(), player.level().dimension(), player.isAlive() && !player.isRemoved());
	}

	private static ServerPlayer resolve(MinecraftServer server, PlayerIdentity identity) {
		ServerSessions sessions = SESSIONS.get(server);
		Session session = sessions == null ? null : sessions.scoped.get(identity.entityId());
		if (session == null && sessions != null) session = sessions.global;
		if (session == null || !session.active.contains(identity.owner()) || !java.util.Objects.equals(
				session.generations.get(identity.owner()), identity.generation())) return null;
		var level = server.getLevel(identity.dimension());
		ServerPlayer player = level != null && level.getEntity(identity.entityId()) instanceof ServerPlayer found
				? found : null;
		return player != null && player.getId() == identity.entityId()
				&& player.getUUID().equals(identity.owner())
				&& player.connection != null && player.isAlive() && !player.isRemoved()
				&& player.level().dimension().equals(identity.dimension()) ? player : null;
	}

	private static Session createSession(MinecraftServer server, PacketFaultProfile profile,
			Set<Integer> entityIds) {
		Session session = new Session(profile, entityIds);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!session.includes(player)) continue;
			session.generations.put(player.getUUID(), 1L);
			session.active.add(player.getUUID());
			session.liveMarkers.put(player.getUUID(), marker(player));
		}
		return session;
	}

	private static Session sessionFor(MinecraftServer server, ServerPlayer player) {
		ServerSessions sessions = SESSIONS.get(server);
		if (sessions == null) return null;
		Session scoped = sessions.scoped.get(player.getId());
		return scoped != null ? scoped : sessions.global;
	}

	private static void removeEmpty(MinecraftServer server, ServerSessions sessions) {
		if (sessions.global == null && sessions.scoped.isEmpty()) SESSIONS.remove(server);
	}
}
