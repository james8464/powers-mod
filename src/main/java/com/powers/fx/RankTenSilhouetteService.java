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
		String dimension = level.dimension().identifier().toString();
		CastOffer cast = new CastOffer(server.getTickCount(), caster.getUUID(),
				SkillSystem.effectiveLevel(caster), powerId, dimension, caster.getX(), caster.getY(),
				caster.getZ(), caster.getYRot(), caster.getXRot(), SkillSystem.hasDarknessTag(caster) ? 1 : 0,
				visualSeed(caster, powerId, server.getTickCount()));
		Decision decision = admit(serverState.policy, cast);
		serverState.policy = decision.state();
		if (!decision.accepted()) return;
		List<ServerPlayer> players = List.copyOf(level.players());
		Map<UUID, ServerPlayer> byId = new LinkedHashMap<>();
		List<ObserverOffer> observers = new ArrayList<>(players.size());
		for (ServerPlayer observer : players) {
			byId.put(observer.getUUID(), observer);
			boolean supported;
			try {
				supported = ServerPlayNetworking.canSend(observer, RankTenSilhouettePackets.Payload.TYPE);
			} catch (RuntimeException exception) {
				supported = false;
			}
			observers.add(new ObserverOffer(observer.getUUID(),
					observer.level().dimension().identifier().toString(), observer.getX(), observer.getY(),
					observer.getZ(), supported, live(server, level, observer, observer.connection)));
		}
		decision = selectObservers(decision, observers);
		serverState.policy = decision.state();
		for (UUID recipientId : decision.recipients()) {
			ServerPlayer observer = byId.get(recipientId);
			if (observer == null) {
				serverState.deliveryFailed++;
				continue;
			}
			Object connection = observer.connection;
			try {
				PowersPlayNetworking.sendGuarded(observer, decision.payload(),
						current -> live(server, level, current, connection)
								&& current.getUUID().equals(recipientId),
						() -> serverState.deliverySucceeded++, failure -> serverState.deliveryFailed++);
			} catch (RuntimeException exception) {
				serverState.deliveryFailed++;
			}
		}
	}

	private static boolean live(MinecraftServer server, ServerLevel level,
			ServerPlayer player, Object connection) {
		return player != null && player.connection != null && player.connection == connection
				&& server.getPlayerList().getPlayer(player.getUUID()) == player
				&& player.level() == level && !player.hasDisconnected()
				&& player.isAlive() && !player.isRemoved();
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
		PolicyState state = input.forTick(cast.tick());
		if (state.offeredThisTick() >= MAX_OFFERS_PER_TICK) {
			return rejected(state.withDiagnostics(state.diagnostics().budgetRejection()));
		}
		state = state.consumeOffer();
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
		double x = observer.x() - payload.x();
		double y = observer.y() - payload.y();
		double z = observer.z() - payload.z();
		return x * x + y * y + z * z;
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
