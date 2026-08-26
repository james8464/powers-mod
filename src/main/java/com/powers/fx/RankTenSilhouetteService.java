package com.powers.fx;

import com.powers.PowersMod;
import com.powers.network.PowersPlayNetworking;
import com.powers.network.RankTenSilhouettePackets;
import com.powers.player.SkillSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/** Server owner for rank-ten silhouette admission, observer selection, and guarded delivery. */
public final class RankTenSilhouetteService {
	public static final int MAX_OFFERS_PER_TICK = 32;
	public static final double OBSERVER_RADIUS = 384.0;
	private static final double OBSERVER_RADIUS_SQUARED = OBSERVER_RADIUS * OBSERVER_RADIUS;
	private static final Map<MinecraftServer, ServerState> SERVERS = new WeakHashMap<>();

	private RankTenSilhouetteService() {
	}

	/** Emits presentation only; every runtime failure is contained after gameplay has committed. */
	public static void afterSuccessfulInnateCast(ServerPlayer caster, String powerId) {
		if (caster == null) return;
		try {
			offerAndSend(caster, powerId);
		} catch (RuntimeException exception) {
			MinecraftServer server = caster.level().getServer();
			state(server).serviceFailures++;
			PowersMod.LOGGER.warn("Rank-ten silhouette presentation failed closed", exception);
		}
	}

	/** Explicitly releases one server's short-lived admission and diagnostic state. */
	public static void clear(MinecraftServer server) {
		if (server != null) SERVERS.remove(server);
	}

	/** Returns a detached immutable snapshot suitable for tests and operator diagnostics. */
	public static Diagnostics diagnostics(MinecraftServer server) {
		ServerState state = SERVERS.get(server);
		return state == null ? Diagnostics.empty()
				: state.policy.diagnostics().withDelivery(state.deliverySucceeded,
						state.deliveryFailed, state.serviceFailures);
	}

	private static void offerAndSend(ServerPlayer caster, String powerId) {
		if (!(caster.level() instanceof ServerLevel level)) return;
		MinecraftServer server = level.getServer();
		ServerState serverState = state(server);
		execute(serverState.policy, powerId,
				new MinecraftRuntime(caster, level, server, serverState));
	}

	private static int visualSeed(ServerPlayer caster, String powerId, long tick) {
		long value = caster.getUUID().getMostSignificantBits()
				^ Long.rotateLeft(caster.getUUID().getLeastSignificantBits(), 17)
				^ Long.rotateLeft(tick, 31) ^ Objects.hashCode(powerId);
		return Long.hashCode(value);
	}

	private static ServerState state(MinecraftServer server) {
		return SERVERS.computeIfAbsent(Objects.requireNonNull(server, "server"), ignored -> new ServerState());
	}

	static PolicyState initialPolicy(long nextEventId) {
		if (nextEventId < 1) throw new IllegalArgumentException("event IDs begin at one");
		return new PolicyState(nextEventId, Long.MIN_VALUE, 0, Set.of(), Diagnostics.empty());
	}

	/** Pure immutable admission path shared exactly by unit tests and production delivery. */
	static Decision offer(PolicyState input, CastOffer cast, List<ObserverOffer> observers) {
		Decision admitted = admit(input, cast);
		return admitted.accepted() ? selectObservers(admitted, observers) : admitted;
	}

	private static Decision admit(PolicyState input, CastOffer cast) {
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(cast, "cast");
		Reservation reservation = reserve(input, cast.tick());
		return reservation.accepted() ? admitReserved(reservation.state(), cast)
				: rejected(reservation.state());
	}

	private static Reservation reserve(PolicyState input, long tick) {
		PolicyState state = Objects.requireNonNull(input, "input").forTick(tick);
		if (state.offeredThisTick() >= MAX_OFFERS_PER_TICK) {
			return new Reservation(state.withDiagnostics(state.diagnostics().budgetRejection()), false);
		}
		return new Reservation(state.consumeOffer(), true);
	}

	private static Decision admitReserved(PolicyState state, CastOffer cast) {
		if (state.tick() != cast.tick()) throw new IllegalArgumentException("cast tick was not reserved");
		var resolved = RankTenSilhouetteProfile.forPower(cast.powerId());
		if (resolved.isEmpty()) return rejected(state.withDiagnostics(state.diagnostics().invalidProfile()));
		if (cast.rank() < 10) return rejected(state.withDiagnostics(state.diagnostics().belowRankEvent()));
		RankTenSilhouetteProfile profile = resolved.orElseThrow();
		CasterProfile key = new CasterProfile(cast.caster(), profile.networkId());
		if (state.used().contains(key)) {
			return rejected(state.withDiagnostics(state.diagnostics().coalescedEvent()));
		}
		if (state.nextEventId() == Long.MAX_VALUE) {
			return rejected(state.withDiagnostics(state.diagnostics().exhaustedEvent()));
		}
		Diagnostics diagnostics = state.diagnostics().accepted(profile.powerId());
		Set<CasterProfile> used = new LinkedHashSet<>(state.used());
		used.add(key);
		long eventId = state.nextEventId();
		PolicyState accepted = new PolicyState(eventId + 1, cast.tick(), state.offeredThisTick(),
				used, diagnostics);
		var payload = new RankTenSilhouettePackets.Payload(eventId, profile.networkId(), cast.caster(),
				cast.dimension(), cast.x(), cast.y(), cast.z(), cast.yaw(), cast.pitch(), cast.alignmentId(),
				cast.visualSeed(), ClientRankTenSilhouetteState.AUTHORED_LIFETIME_TICKS);
		return new Decision(accepted, profile, payload, List.of(), true);
	}

	/** Executable production seam: reservation, preparation, bounded enumeration, and guarded sends. */
	static RuntimeResult execute(PolicyState input, String powerId, RuntimeAccess access) {
		Objects.requireNonNull(access, "access");
		long tick = access.tick();
		Reservation reservation = reserve(input, tick);
		access.persist(reservation.state());
		if (!reservation.accepted()) return new RuntimeResult(rejected(reservation.state()));
		Decision admitted = admitReserved(reservation.state(), access.prepareCast(powerId, tick));
		access.persist(admitted.state());
		if (!admitted.accepted()) return new RuntimeResult(admitted);

		List<RuntimeObserver> players = List.copyOf(access.players());
		Map<UUID, RuntimeObserver> recipientsById = new LinkedHashMap<>();
		List<UUID> recipients = new ArrayList<>();
		Diagnostics diagnostics = admitted.diagnostics();
		for (RuntimeObserver observer : players) {
			if (!admitted.payload().dimension().equals(observer.dimension())) {
				diagnostics = diagnostics.dimensionObserver();
			} else if (distanceSquared(admitted.payload(), observer.x(), observer.y(), observer.z())
					> OBSERVER_RADIUS_SQUARED) {
				diagnostics = diagnostics.rangeObserver();
			} else if (!access.canSend(observer)) {
				diagnostics = diagnostics.unsupportedObserver();
			} else if (!sessionCurrent(observer, access.current(observer))) {
				diagnostics = diagnostics.staleObserver();
			} else {
				recipients.add(observer.player());
				recipientsById.put(observer.player(), observer);
			}
		}
		diagnostics = diagnostics.eligible(recipients.size());
		Decision selected = new Decision(admitted.state().withDiagnostics(diagnostics),
				admitted.profile(), admitted.payload(), recipients, true);
		access.persist(selected.state());
		for (UUID recipient : recipients) {
			RuntimeObserver snapshot = recipientsById.get(recipient);
			access.sendGuarded(snapshot, selected.payload(),
					current -> sessionCurrent(snapshot, current));
		}
		return new RuntimeResult(selected);
	}

	private static boolean sessionCurrent(RuntimeObserver snapshot, RuntimeObserver current) {
		return snapshot != null && current != null && snapshot.handle() == current.handle()
				&& snapshot.connection() != null && snapshot.connection() == current.connection()
				&& snapshot.player().equals(current.player())
				&& snapshot.dimension().equals(current.dimension()) && current.liveSession();
	}

	private static Decision selectObservers(Decision admitted, List<ObserverOffer> input) {
		if (!admitted.accepted()) return admitted;
		List<ObserverOffer> observers = List.copyOf(input);
		List<UUID> recipients = new ArrayList<>();
		Diagnostics diagnostics = admitted.diagnostics();
		for (ObserverOffer observer : observers) {
			if (!admitted.payload().dimension().equals(observer.dimension())) diagnostics = diagnostics.dimensionObserver();
			else if (distanceSquared(admitted.payload(), observer) > OBSERVER_RADIUS_SQUARED) diagnostics = diagnostics.rangeObserver();
			else if (!observer.supported()) diagnostics = diagnostics.unsupportedObserver();
			else if (!observer.liveSession()) diagnostics = diagnostics.staleObserver();
			else recipients.add(observer.player());
		}
		diagnostics = diagnostics.eligible(recipients.size());
		PolicyState completed = admitted.state().withDiagnostics(diagnostics);
		return new Decision(completed, admitted.profile(), admitted.payload(), recipients, true);
	}

	private static Decision rejected(PolicyState state) {
		return new Decision(state, null, null, List.of(), false);
	}

	private static double distanceSquared(RankTenSilhouettePackets.Payload payload, ObserverOffer observer) {
		return distanceSquared(payload, observer.x(), observer.y(), observer.z());
	}

	private static double distanceSquared(RankTenSilhouettePackets.Payload payload,
			double observerX, double observerY, double observerZ) {
		double x = observerX - payload.x();
		double y = observerY - payload.y();
		double z = observerZ - payload.z();
		return x * x + y * y + z * z;
	}

	interface RuntimeAccess {
		long tick();
		void persist(PolicyState state);
		CastOffer prepareCast(String powerId, long reservedTick);
		List<RuntimeObserver> players();
		boolean canSend(RuntimeObserver observer);
		RuntimeObserver current(RuntimeObserver observer);
		void sendGuarded(RuntimeObserver observer, RankTenSilhouettePackets.Payload payload,
				Predicate<RuntimeObserver> guard);
	}

	static record RuntimeObserver(UUID player, String dimension, double x, double y, double z,
			Object handle, Object connection, boolean liveSession) {
		RuntimeObserver {
			Objects.requireNonNull(player, "player");
			Objects.requireNonNull(dimension, "dimension");
			Objects.requireNonNull(handle, "handle");
			if (!finite(x, y, z)) throw new IllegalArgumentException("invalid runtime observer");
		}
	}

	static record RuntimeResult(Decision decision) {
	}

	private record Reservation(PolicyState state, boolean accepted) {
	}

	static record CastOffer(long tick, UUID caster, int rank, String powerId, String dimension,
			double x, double y, double z, float yaw, float pitch, int alignmentId, int visualSeed) {
		CastOffer {
			Objects.requireNonNull(caster, "caster");
			Objects.requireNonNull(powerId, "powerId");
			Objects.requireNonNull(dimension, "dimension");
			if (!finite(x, y, z, yaw, pitch) || (alignmentId != 0 && alignmentId != 1)) {
				throw new IllegalArgumentException("invalid cast offer");
			}
		}
	}

	static record ObserverOffer(UUID player, String dimension, double x, double y, double z,
			boolean supported, boolean liveSession) {
		ObserverOffer {
			Objects.requireNonNull(player, "player");
			Objects.requireNonNull(dimension, "dimension");
			if (!finite(x, y, z)) throw new IllegalArgumentException("invalid observer offer");
		}
	}

	static record PolicyState(long nextEventId, long tick, int offeredThisTick,
			Set<CasterProfile> used, Diagnostics diagnostics) {
		PolicyState {
			used = Set.copyOf(used);
			Objects.requireNonNull(diagnostics, "diagnostics");
		}
		PolicyState forTick(long current) {
			return tick == current ? this : new PolicyState(nextEventId, current, 0, Set.of(), diagnostics);
		}
		PolicyState withDiagnostics(Diagnostics changed) {
			return new PolicyState(nextEventId, tick, offeredThisTick, used, changed);
		}
		PolicyState consumeOffer() {
			return new PolicyState(nextEventId, tick, offeredThisTick + 1, used, diagnostics);
		}
	}

	static record Decision(PolicyState state, RankTenSilhouetteProfile profile,
			RankTenSilhouettePackets.Payload payload, List<UUID> recipients, boolean accepted) {
		Decision {
			recipients = List.copyOf(recipients);
		}
		Diagnostics diagnostics() { return state.diagnostics(); }
	}

	private record CasterProfile(UUID caster, int profile) {
	}

	/** Immutable cumulative counters; zero ticket requests is an explicit invariant. */
	public record Diagnostics(long acceptedEvents, long invalidProfiles, long belowRank,
			long coalescedEvents, long budgetRejected, long exhaustedEvents,
			long dimensionObservers, long rangeObservers, long unsupportedObservers,
			long staleObservers, long eligibleObservers, long deliverySucceeded,
			long deliveryFailed, long serviceFailures, long chunkTicketsRequested,
			Set<String> acceptedProfiles) {
		public Diagnostics {
			acceptedProfiles = Set.copyOf(acceptedProfiles);
		}
		static Diagnostics empty() {
			return new Diagnostics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Set.of());
		}
		Diagnostics invalidProfile() { return copy(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, null); }
		Diagnostics belowRankEvent() { return copy(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, null); }
		Diagnostics coalescedEvent() { return copy(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, null); }
		Diagnostics budgetRejection() { return copy(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, null); }
		Diagnostics exhaustedEvent() { return copy(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, null); }
		Diagnostics dimensionObserver() { return copy(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, null); }
		Diagnostics rangeObserver() { return copy(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, null); }
		Diagnostics unsupportedObserver() { return copy(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, null); }
		Diagnostics staleObserver() { return copy(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, null); }
		Diagnostics accepted(String power) {
			Set<String> powers = new LinkedHashSet<>(acceptedProfiles);
			powers.add(power);
			return copy(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, powers);
		}
		Diagnostics eligible(int recipients) {
			return copy(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, recipients, null);
		}
		Diagnostics withDelivery(long succeeded, long failed, long services) {
			return new Diagnostics(acceptedEvents, invalidProfiles, belowRank, coalescedEvents,
					budgetRejected, exhaustedEvents, dimensionObservers, rangeObservers,
					unsupportedObservers, staleObservers, eligibleObservers, succeeded, failed,
					services, chunkTicketsRequested, acceptedProfiles);
		}
		private Diagnostics copy(long accepted, long invalid, long below, long coalesced,
				long budget, long exhausted, long dimension, long range, long unsupported,
				long stale, long eligible, Set<String> profiles) {
			return new Diagnostics(acceptedEvents + accepted, invalidProfiles + invalid, belowRank + below,
					coalescedEvents + coalesced, budgetRejected + budget, exhaustedEvents + exhausted,
					dimensionObservers + dimension, rangeObservers + range,
					unsupportedObservers + unsupported, staleObservers + stale,
					eligibleObservers + eligible, deliverySucceeded, deliveryFailed, serviceFailures,
					chunkTicketsRequested, profiles == null ? acceptedProfiles : profiles);
		}
	}

	private static final class MinecraftRuntime implements RuntimeAccess {
		private final ServerPlayer caster;
		private final ServerLevel level;
		private final MinecraftServer server;
		private final ServerState state;

		private MinecraftRuntime(ServerPlayer caster, ServerLevel level,
				MinecraftServer server, ServerState state) {
			this.caster = caster;
			this.level = level;
			this.server = server;
			this.state = state;
		}

		@Override
		public long tick() {
			return server.getTickCount();
		}

		@Override
		public void persist(PolicyState policy) {
			state.policy = policy;
		}

		@Override
		public CastOffer prepareCast(String powerId, long reservedTick) {
			return new CastOffer(reservedTick, caster.getUUID(), SkillSystem.effectiveLevel(caster),
					powerId, level.dimension().identifier().toString(), caster.getX(), caster.getY(),
					caster.getZ(), caster.getYRot(), caster.getXRot(),
					SkillSystem.hasDarknessTag(caster) ? 1 : 0,
					visualSeed(caster, powerId, reservedTick));
		}

		@Override
		public List<RuntimeObserver> players() {
			return List.copyOf(level.players()).stream().map(this::snapshot).toList();
		}

		@Override
		public boolean canSend(RuntimeObserver observer) {
			try {
				return observer.handle() instanceof ServerPlayer player
						&& ServerPlayNetworking.canSend(player, RankTenSilhouettePackets.Payload.TYPE);
			} catch (RuntimeException exception) {
				return false;
			}
		}

		@Override
		public RuntimeObserver current(RuntimeObserver observer) {
			ServerPlayer current = server.getPlayerList().getPlayer(observer.player());
			return current == null ? null : snapshot(current);
		}

		@Override
		public void sendGuarded(RuntimeObserver observer, RankTenSilhouettePackets.Payload payload,
				Predicate<RuntimeObserver> guard) {
			if (!(observer.handle() instanceof ServerPlayer player)) {
				state.deliveryFailed++;
				return;
			}
			try {
				PowersPlayNetworking.sendGuarded(player, payload,
						current -> guard.test(snapshot(current)),
						() -> state.deliverySucceeded++, failure -> state.deliveryFailed++);
			} catch (RuntimeException exception) {
				state.deliveryFailed++;
			}
		}

		private RuntimeObserver snapshot(ServerPlayer player) {
			boolean live = player.connection != null
					&& server.getPlayerList().getPlayer(player.getUUID()) == player
					&& player.level() == level && !player.hasDisconnected()
					&& player.isAlive() && !player.isRemoved();
			return new RuntimeObserver(player.getUUID(),
					player.level().dimension().identifier().toString(), player.getX(), player.getY(),
					player.getZ(), player, player.connection, live);
		}
	}

	private static final class ServerState {
		private PolicyState policy = initialPolicy(1L);
		private long deliverySucceeded;
		private long deliveryFailed;
		private long serviceFailures;
	}

	private static boolean finite(double... values) {
		for (double value : values) if (!Double.isFinite(value)) return false;
		return true;
	}
}
