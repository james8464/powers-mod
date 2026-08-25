package com.powers.fx;

import com.powers.force.BlockWorkBudget;
import com.powers.network.MagicFxPackets;
import com.powers.network.PowersPlayNetworking;
import com.powers.protection.PowerProtection;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.protection.ProtectionDecision;
import com.powers.util.ChunkSpatialIndex;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-thread owner for bounded, transient visual scars. It reads already-loaded blocks only and
 * never mutates terrain, persists state, registers world objects, or creates chunk tickets.
 */
public final class VisualScarService {
	private static final VisualScarRules.Limits LIMITS = VisualScarRules.Limits.hardCeilings();
	private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();
	private VisualScarService() {
	}
	/** Queues one presentation-only scar identity without inspecting world or protection state. */
	public static boolean request(ServerLevel level, LivingEntity owner, BlockPos support,
			Direction face, VisualScarRules.Impact impact, int visualSeed) {
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(support, "support");
		Objects.requireNonNull(face, "face");
		Objects.requireNonNull(impact, "impact");
		MinecraftServer server = level.getServer();
		if (server == null) return false;
		State state = STATES.computeIfAbsent(server, ignored -> new State());
		long providerPolicyId = PowerProtectionAdapters.blockWorkPolicyId() & Long.MAX_VALUE;
		return state.offer(new PendingRequest(level.dimension().identifier().toString(), providerPolicyId,
				owner.getUUID(),
				support.immutable(), face, impact, visualSeed));
	}
	/** Advances bounded admission, expiry, revalidation, observer resync, and delivery work. */
	public static void tick(MinecraftServer server) {
		State state = STATES.get(server);
		if (state != null) state.tick(server);
	}
	/** Drops one observer session without retaining retries across disconnect. */
	public static void disconnect(ServerPlayer player) {
		State state = STATES.get(player.level().getServer());
		if (state != null) state.disconnect(player.getUUID());
	}
	public static void requestResync(ServerPlayer player) {
		State state = STATES.get(player.level().getServer());
		if (state != null) state.requestResync(player);
	}
	public static void clear(MinecraftServer server) {
		State removed = STATES.remove(server);
		if (removed != null) removed.clear();
	}
	static void observeSessionForTest(MinecraftServer server, UUID observer) {
		State state = STATES.computeIfAbsent(server, ignored -> new State()); state.observeSessions(server);
		state.sessions.keySet().stream().filter(id -> !id.equals(observer)).toList().forEach(state::disconnect); }
	static void broadcastForTest(MinecraftServer server, String dimension, ScarFxProtocolRules.Wire wire) {
		STATES.computeIfAbsent(server, ignored -> new State()).broadcast(dimension, wire); }
	static void setGenerationForTest(MinecraftServer server, long generation) {
		if (generation < 0) throw new IllegalArgumentException("generation");
		STATES.computeIfAbsent(server, ignored -> new State()).generation = generation;
	} static TestDiagnostics diagnosticsForTest(MinecraftServer server, UUID observer) {
		State state = STATES.get(server);
		if (state == null) return new TestDiagnostics(0, false, 0, 0, false);
		VisualScarLedgerRules.ObserverSession session = state.sessions.get(observer);
		return new TestDiagnostics(state.generation, state.admissionsDisabled, state.pending.globalSize(),
				session == null ? 0 : state.pending.observerSize(session),
				session != null && state.pending.needsResync(session));
	}
	record TestDiagnostics(long generation, boolean admissionsDisabled, int pendingGlobal, int pendingObserver, boolean needsResync) { }
	private static VisualScarRules.Material classify(BlockState state) {
		if (state.is(BlockTags.ICE) || state.is(BlockTags.SNOW)) return VisualScarRules.Material.COLD;
		if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) return VisualScarRules.Material.WOOD;
		if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)) {
			return VisualScarRules.Material.SAND;
		}
		if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MUD)
				|| state.is(Blocks.CLAY)) return VisualScarRules.Material.EARTH;
		if (state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK)
				|| state.is(Blocks.ANVIL)) {
			return VisualScarRules.Material.METAL;
		}
		return state.isAir() ? null : VisualScarRules.Material.STONE;
	}
	private record Key(String dimension, long position, int face) {
		private Key {
			Objects.requireNonNull(dimension, "dimension");
		}
	}
	private record Lane(String dimension, long policy, UUID owner, VisualScarRules.Impact impact) {
	}
	private record ObserverPosition(double x, double y, double z) {
	}
	private record PendingRequest(String dimension, long providerPolicyId, UUID owner, BlockPos support,
			Direction face, VisualScarRules.Impact impact, int visualSeed) {
		private Lane lane() { return new Lane(dimension, providerPolicyId, owner, impact); }
		private Key key() { return new Key(dimension, support.asLong(), face.ordinal()); }
	}
	private static final class State {
		private final VisualScarRequestQueue requests = new VisualScarRequestQueue(
				LIMITS.queuedGlobal(), LIMITS.queuedPerOwner());
		private final Map<Lane, ArrayDeque<Key>> requestDetails = new LinkedHashMap<>();
		private final Map<Key, PendingRequest> pendingByKey = new HashMap<>();
		private final Map<Key, VisualScarLedgerRules.Record> active = new LinkedHashMap<>();
		private final Map<UUID, Integer> activeByOwner = new HashMap<>();
		private final VisualScarExpiryIndex<Key> expiry = new VisualScarExpiryIndex<>(LIMITS.activeGlobal());
		private final VisualScarRevalidationRing<Key> revalidation =
				VisualScarRevalidationRing.of(List.of(), LIMITS.activeGlobal());
		private final ChunkSpatialIndex<Key, Key> spatial = new ChunkSpatialIndex<>(16);
		private final NavigableMap<Long, Integer> boundedExpiryDiagnostics = new TreeMap<>();
		private final Map<UUID, VisualScarLedgerRules.ObserverSession> sessions = new HashMap<>();
		private final Map<UUID, ServerPlayer> sessionPlayers = new HashMap<>();
		private final Map<UUID, ObserverPosition> observerPositions = new HashMap<>();
		private final VisualScarDeliveryRules.Pending pending = VisualScarDeliveryRules.empty(
				LIMITS.pendingPerObserver(), LIMITS.pendingGlobal());
		private long generation;
		private long revision;
		private long sessionSequence;
		private boolean admissionsDisabled;

		private boolean offer(PendingRequest request) {
			if (admissionsDisabled) return false;
			Key key = request.key();
			PendingRequest current = pendingByKey.get(key);
			if (current != null) {
				if (!current.owner.equals(request.owner)) return false;
				pendingByKey.put(key, request);
				return true;
			}
			Lane lane = request.lane();
			VisualScarLedgerRules.Request fair = new VisualScarLedgerRules.Request(
					lane.dimension, lane.policy, lane.owner, lane.impact);
			if (!requests.offer(fair)) return false;
			pendingByKey.put(key, request);
			requestDetails.computeIfAbsent(lane, ignored -> new ArrayDeque<>()).addLast(key);
			return true;
		}

		private void tick(MinecraftServer server) {
			long now = server.getTickCount();
			observeSessions(server);
			List<VisualScarLedgerRules.Request> selected = requests.poll(LIMITS.requestsPerTick());
			List<BlockWorkBudget.Lane> lanes = selected.stream().map(request ->
					new BlockWorkBudget.Lane(request.dimension(), request.providerPolicyId())).toList();
			Map<BlockWorkBudget.Lane, Integer> allowances = BlockWorkBudget.allocate(
					LIMITS.requestsPerTick(), lanes, now);
			if (allowances.values().stream().mapToInt(Integer::intValue).sum()
					> LIMITS.requestsPerTick()) throw new IllegalStateException("scar work budget overflow");
			for (VisualScarLedgerRules.Request selectedRequest : selected) {
				BlockWorkBudget.Lane budgetLane = new BlockWorkBudget.Lane(
						selectedRequest.dimension(), selectedRequest.providerPolicyId());
				if (!allowances.containsKey(budgetLane)) continue;
				Lane lane = new Lane(selectedRequest.dimension(), selectedRequest.providerPolicyId(),
						selectedRequest.owner(), selectedRequest.impact());
				ArrayDeque<Key> details = requestDetails.get(lane);
				Key key = details == null ? null : details.pollFirst();
				if (details != null && details.isEmpty()) requestDetails.remove(lane);
				PendingRequest pending = key == null ? null : pendingByKey.remove(key);
				if (pending != null) inspectAndActivate(server, pending, now);
			}
			for (Key key : expiry.pollDue(now, LIMITS.revalidationsPerTick()).keys()) remove(server, key);
			for (Key key : revalidation.inspectNext(LIMITS.revalidationsPerTick())) revalidate(server, key);
			drainDelivery(server, now);
		}
		private void inspectAndActivate(MinecraftServer server, PendingRequest pending, long now) {
			ServerLevel level = level(server, pending.dimension);
			if (level == null) return;
			BlockPos support = pending.support;
			BlockPos origin = support.relative(pending.face);
			if (!LoadedChunks.contains(level, support)) return;
			if (!LoadedChunks.contains(level, origin)) return;
			BlockState supportState = level.getBlockState(support);
			Object supportEntity = level.getBlockEntity(support);
			BlockState originState = level.getBlockState(origin);
			Object originEntity = level.getBlockEntity(origin);
			if (!(level.getEntity(pending.owner) instanceof LivingEntity owner)) return;
			ProtectionDecision protection = PowerProtection.blockDecision(owner, level, support);
			VisualScarRules.Material material = classify(supportState);
			VisualScarRules.SupportFacts facts = new VisualScarRules.SupportFacts(true, true,
					protection == ProtectionDecision.ALLOW,
					supportEntity != null, !supportState.getFluidState().isEmpty(),
					supportState.isFaceSturdy(level, support, pending.face), material != null,
					originState.isAir(), originEntity != null, !originState.getFluidState().isEmpty());
			if (VisualScarRules.admit(facts) != VisualScarRules.Admission.ALLOW) return;
			activate(server, pending, material, supportState.hashCode(), now);
		}
		private void activate(MinecraftServer server, PendingRequest pending,
				VisualScarRules.Material material, long fingerprint, long now) {
			Key key = pending.key();
			VisualScarLedgerRules.Record current = active.get(key);
			if (current != null && !current.owner().equals(pending.owner)) return;
			int ownerActive = activeByOwner.getOrDefault(pending.owner, 0);
			if (current == null && VisualScarLedgerRules.reserve(ownerActive, active.size(), 0, 0, LIMITS)
					!= VisualScarLedgerRules.Admission.ALLOW) return;
			if (generation == Long.MAX_VALUE) {
				admissionsDisabled = true;
				return;
			}
			long nextGeneration = ++generation;
			long expiresAt = now + LIMITS.maximumLease();
			VisualScarLedgerRules.Record record = new VisualScarLedgerRules.Record(pending.dimension,
					pending.support.asLong(), toFace(pending.face), pending.owner, pending.impact,
					material, pending.visualSeed, nextGeneration, fingerprint,
					now, expiresAt);
			active.put(key, record);
			if (current == null) {
				activeByOwner.merge(pending.owner, 1, Integer::sum);
				revalidation.insert(key);
				spatial.put(key, pending.dimension, pending.support.getX() + 0.5,
						pending.support.getZ() + 0.5, 0.0, key);
			}
			expiry.put(key, expiresAt);
			if (current != null) decrementExpiryDiagnostic(current.expiresAt());
			boundedExpiryDiagnostics.merge(expiresAt, 1, Integer::sum);
			revision++;
			broadcast(pending.dimension, createWire(record, LIMITS.maximumLease()));
		}
		private void revalidate(MinecraftServer server, Key key) {
			VisualScarLedgerRules.Record record = active.get(key);
			if (record == null) return;
			ServerLevel level = level(server, record.dimension());
			if (level == null) return;
			BlockPos support = BlockPos.of(record.position());
			Direction face = Direction.values()[record.face().ordinal()];
			BlockPos origin = support.relative(face);
			boolean supportLoaded = LoadedChunks.contains(level, support);
			boolean originLoaded = LoadedChunks.contains(level, origin);
			if (!supportLoaded || !originLoaded) return;
			BlockState supportState = level.getBlockState(support);
			BlockState originState = level.getBlockState(origin);
			boolean valid = level.getBlockEntity(support) == null
					&& level.getBlockEntity(origin) == null && originState.isAir()
					&& supportState.isFaceSturdy(level, support, face)
					&& classify(supportState) == record.material();
			if (VisualScarLedgerRules.revalidate(record, true, true, valid, supportState.hashCode())
					== VisualScarLedgerRules.Revalidation.REMOVE_STALE) remove(server, key);
		}
		private void remove(MinecraftServer server, Key key) {
			VisualScarLedgerRules.Record removed = active.remove(key);
			if (removed == null) return;
			expiry.remove(key);
			decrementExpiryDiagnostic(removed.expiresAt());
			revalidation.remove(key);
			spatial.removeStale(key);
			activeByOwner.compute(removed.owner(), (owner, count) -> count == null || count <= 1 ? null : count - 1);
			revision++;
			ScarFxProtocolRules.Wire tombstone = new ScarFxProtocolRules.Wire(
					ScarFxProtocolRules.REMOVE, removed.position(), removed.face().ordinal(),
					removed.impact().ordinal(), removed.material().ordinal(), removed.visualSeed(),
					removed.generation(), 1);
			broadcast(removed.dimension(), tombstone);
		}
		private void observeSessions(MinecraftServer server) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				String dimension = player.level().dimension().identifier().toString();
				VisualScarLedgerRules.ObserverSession previous = sessions.get(player.getUUID());
				if (previous != null && sessionPlayers.get(player.getUUID()) == player
						&& previous.dimension().equals(dimension)) {
					observeMovement(player, previous);
					continue;
				}
				if (sessionSequence == Long.MAX_VALUE) {
					disconnect(player.getUUID());
					continue;
				}
				long sessionGeneration = ++sessionSequence;
				VisualScarLedgerRules.ObserverSession current = new VisualScarLedgerRules.ObserverSession(
						player.getUUID(), sessionGeneration, dimension, sessionGeneration);
				if (previous != null) pending.cancel(previous);
				sessions.put(player.getUUID(), current);
				sessionPlayers.put(player.getUUID(), player);
				observerPositions.put(player.getUUID(), new ObserverPosition(
						player.getX(), player.getY(), player.getZ()));
				pending.markNeedsResync(current, new VisualScarDeliveryModel.ResyncCursor(revision, null));
			}
			for (UUID uuid : new ArrayList<>(sessions.keySet())) {
				if (server.getPlayerList().getPlayer(uuid) == null) disconnect(uuid);
			}
		}
		private void observeMovement(ServerPlayer player,
				VisualScarLedgerRules.ObserverSession session) {
			ObserverPosition current = new ObserverPosition(player.getX(), player.getY(), player.getZ());
			ObserverPosition previous = observerPositions.get(player.getUUID());
			if (previous == null) {
				observerPositions.put(player.getUUID(), current);
				return;
			}
			VisualScarLedgerRules.MovementObservation movement =
					VisualScarLedgerRules.observeMovement(session, previous.x, previous.y, previous.z,
							current.x, current.y, current.z, false);
			if (!movement.needsResync()) return;
			observerPositions.put(player.getUUID(), current);
			Set<Key> previouslyVisible = Set.copyOf(spatial.nearby(session.dimension(),
					previous.x, previous.z, 256.0));
			boolean enteredRange = spatial.nearby(session.dimension(), current.x, current.z, 256.0)
					.stream().anyMatch(key -> !previouslyVisible.contains(key));
			if (enteredRange) {
				pending.markNeedsResync(session,
						new VisualScarDeliveryModel.ResyncCursor(revision, null));
			}
		}

		private void drainDelivery(MinecraftServer server, long now) {
			List<VisualScarDeliveryModel.SnapshotRow> rows = active.values().stream()
					.map(record -> new VisualScarDeliveryModel.SnapshotRow(record.dimension(),
							createWire(record, ScarFxProtocolRules.remainingLease(record.expiresAt(), now))))
					.toList();
			VisualScarDeliveryModel.AuthoritativeSnapshot snapshot =
					VisualScarDeliveryRules.authoritativeSnapshot(revision, rows);
			List<VisualScarLedgerRules.ObserverSession> currentSessions = List.copyOf(sessions.values());
			VisualScarDeliveryModel.Drain drained = pending.drainFair(LIMITS.sendsPerTick(),
					192, 64, currentSessions, snapshot);
			for (VisualScarDeliveryModel.Send send : drained.sent()) {
				ServerPlayer player = server.getPlayerList().getPlayer(send.session().player());
				if (player == null) {
					discardStaleFailure(send);
					continue;
				}
				PowersPlayNetworking.sendGuarded(player,
						new MagicFxPackets.ScarFxPayload(send.payload()),
						current -> sessionCurrent(send, current),
						() -> {
							if (send.payload().operation() == ScarFxProtocolRules.RESET_DIMENSION) {
								beginSnapshotCreatesAfterResetSuccess(send);
							} else {
								onGuardedSendSuccess(send);
							}
						}, failure -> handleFailure(send, failure));
			}
		}
		private boolean sessionCurrent(VisualScarDeliveryModel.Send send, ServerPlayer player) {
			long deliveryGeneration = send.deliveryGeneration();
			return deliveryGeneration == pending.deliveryGeneration(send.session())
					&& pending.guardCurrent(send, sessions.get(player.getUUID()));
		}
		private void broadcast(String dimension, ScarFxProtocolRules.Wire wire) {
			BlockPos support = BlockPos.of(wire.position());
			for (VisualScarLedgerRules.ObserverSession session : sessions.values()) {
				ObserverPosition observer = observerPositions.get(session.player());
				if (session.dimension().equals(dimension) && observer != null
						&& VisualScarLedgerRules.withinObservationRange(observer.x, observer.z,
							support.getX() + 0.5, support.getZ() + 0.5)) pending.offerObserved(session, wire);
			}
		}
		private void beginSnapshotCreatesAfterResetSuccess(VisualScarDeliveryModel.Send send) {
			pending.recordSendSuccess(send, sessions.get(send.session().player()));
		}
		private void handleFailure(VisualScarDeliveryModel.Send send,
				PowersPlayNetworking.GuardedSendFailure failure) {
			VisualScarLedgerRules.ObserverSession current = sessions.get(send.session().player());
			if (!pending.guardCurrent(send, current)) {
				discardStaleFailure(send);
			} else if (failure == PowersPlayNetworking.GuardedSendFailure.UNSUPPORTED_CAPABILITY) {
				cancelWithoutRetryOrResync(send);
			} else {
				markNeedsResync(send, failure);
			}
		}
		private void cancelWithoutRetryOrResync(VisualScarDeliveryModel.Send send) {
			pending.recordSendFailure(send,
					VisualScarDeliveryRules.FailureReason.UNSUPPORTED_CAPABILITY,
					sessions.get(send.session().player()));
		}
		private void markNeedsResync(VisualScarDeliveryModel.Send send,
				PowersPlayNetworking.GuardedSendFailure failure) {
			VisualScarDeliveryRules.FailureReason reason =
					failure == PowersPlayNetworking.GuardedSendFailure.SESSION_PREDICATE_FALSE
							? VisualScarDeliveryRules.FailureReason.SESSION_PREDICATE_FALSE
							: VisualScarDeliveryRules.FailureReason.INJECTED_LOSS;
			pending.recordSendFailure(send, reason, sessions.get(send.session().player()));
		}
		private void discardStaleFailure(VisualScarDeliveryModel.Send send) {
			// Stale delayed callbacks own no current-session state.
		}
		private void onGuardedSendSuccess(VisualScarDeliveryModel.Send send) {
			pending.recordSendSuccess(send, sessions.get(send.session().player()));
		}
		private void disconnect(UUID player) {
			VisualScarLedgerRules.ObserverSession removed = sessions.remove(player);
			sessionPlayers.remove(player);
			observerPositions.remove(player);
			if (removed != null) pending.cancel(removed);
		}
		private void requestResync(ServerPlayer player) {
			VisualScarLedgerRules.ObserverSession session = sessions.get(player.getUUID());
			if (session == null || sessionPlayers.get(player.getUUID()) != player
					|| pending.needsResync(session)) return;
			pending.markNeedsResync(session,
					new VisualScarDeliveryModel.ResyncCursor(revision, null));
		}

		private void clear() {
			active.clear();
			activeByOwner.clear();
			requestDetails.clear();
			pendingByKey.clear();
			spatial.clear();
			sessions.clear();
			sessionPlayers.clear();
			observerPositions.clear();
			boundedExpiryDiagnostics.clear();
		}

		private void decrementExpiryDiagnostic(long expiresAt) {
			boundedExpiryDiagnostics.computeIfPresent(expiresAt,
					(at, count) -> count <= 1 ? null : count - 1);
		}

		private static ServerLevel level(MinecraftServer server, String dimension) {
			for (ServerLevel level : server.getAllLevels()) {
				if (level.dimension().identifier().toString().equals(dimension)) return level;
			}
			return null;
		}

		private static ScarFxProtocolRules.Wire createWire(VisualScarLedgerRules.Record record, int lease) {
			return new ScarFxProtocolRules.Wire(ScarFxProtocolRules.CREATE_OR_UPDATE,
					record.position(), record.face().ordinal(), record.impact().ordinal(),
					record.material().ordinal(), record.visualSeed(), record.generation(), lease);
		}

	}

	private static VisualScarRules.Face toFace(Direction direction) {
		return VisualScarRules.Face.valueOf(direction.name());
	}
}
