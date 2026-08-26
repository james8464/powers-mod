package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the immutable policy used by the real bounded ServerPlayer delivery path. */
class RankTenSilhouetteServiceTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final UUID CASTER = UUID.fromString("7332af41-05ca-423c-b1e8-11c1fece4377");

	@Test
	void filtersOnlyLiveSupportedSameDimensionObserversInsideInclusiveRange() {
		List<RankTenSilhouetteService.ObserverOffer> observers = List.of(
				observer(1, OVERWORLD, 384, 0, true, true),
				observer(2, OVERWORLD, 384.01, 0, true, true),
				observer(3, "powers:dark_realm", 1, 0, true, true),
				observer(4, OVERWORLD, 1, 0, false, true),
				observer(5, OVERWORLD, 1, 0, true, false));

		var result = RankTenSilhouetteService.offer(
				RankTenSilhouetteService.initialPolicy(1L), cast(7, CASTER, "fireball"), observers);

		assertTrue(result.accepted());
		assertEquals(List.of(uuid(1)), result.recipients());
		assertEquals(1, result.diagnostics().unsupportedObservers());
		assertEquals(1, result.diagnostics().staleObservers());
		assertEquals(1, result.diagnostics().rangeObservers());
		assertEquals(1, result.diagnostics().dimensionObservers());
		assertEquals(0, result.diagnostics().chunkTicketsRequested());
	}

	@Test
	void canonicalRankAndSameTickCoalescingAreFailClosed() {
		var state = RankTenSilhouetteService.initialPolicy(1L);
		var belowRank = RankTenSilhouetteService.offer(state,
				new RankTenSilhouetteService.CastOffer(4, CASTER, 9, "fireball", OVERWORLD,
						0, 0, 0, 0, 0, 0, 1), List.of());
		var unknown = RankTenSilhouetteService.offer(belowRank.state(), cast(4, CASTER, "not_a_power"), List.of());
		var alias = RankTenSilhouetteService.offer(unknown.state(), cast(4, CASTER, "size_morph"), List.of());
		var duplicateAlias = RankTenSilhouetteService.offer(alias.state(), cast(4, CASTER, "size_shift"), List.of());
		var differentProfile = RankTenSilhouetteService.offer(duplicateAlias.state(), cast(4, CASTER, "flight"), List.of());

		assertFalse(belowRank.accepted());
		assertFalse(unknown.accepted());
		assertTrue(alias.accepted());
		assertEquals("size_shift", alias.profile().powerId());
		assertFalse(duplicateAlias.accepted());
		assertTrue(differentProfile.accepted());
		assertEquals(1, differentProfile.diagnostics().belowRank());
		assertEquals(1, differentProfile.diagnostics().invalidProfiles());
		assertEquals(1, differentProfile.diagnostics().coalescedEvents());
	}

	@Test
	void hardGlobalCapResetsOnNextTickWithoutCreatingTickets() {
		var state = RankTenSilhouetteService.initialPolicy(1L);
		List<RankTenSilhouetteService.Decision> decisions = new ArrayList<>();
		for (int index = 0; index < 33; index++) {
			var result = RankTenSilhouetteService.offer(state, cast(12, uuid(index + 100), "flight"), List.of());
			decisions.add(result);
			state = result.state();
		}
		assertEquals(32, decisions.stream().filter(RankTenSilhouetteService.Decision::accepted).count());
		assertFalse(decisions.get(32).accepted());
		assertEquals(1, state.diagnostics().budgetRejected());
		assertEquals(0, state.diagnostics().chunkTicketsRequested());

		var nextTick = RankTenSilhouetteService.offer(state, cast(13, uuid(200), "flight"), List.of());
		assertTrue(nextTick.accepted());
		assertEquals(33, nextTick.diagnostics().acceptedEvents());
	}

	@Test
	void everyInvocationConsumesBudgetBeforeRejectedOrCoalescedObserverWork() {
		var state = RankTenSilhouetteService.initialPolicy(1L);
		var accepted = RankTenSilhouetteService.offer(state, cast(18, CASTER, "flight"), List.of());
		state = accepted.state();
		for (int index = 0; index < 31; index++) {
			var coalesced = RankTenSilhouetteService.offer(state, cast(18, CASTER, "flight"), List.of());
			assertFalse(coalesced.accepted());
			state = coalesced.state();
		}
		var cappedState = state;
		assertDoesNotThrow(() -> RankTenSilhouetteService.offer(cappedState,
				cast(18, uuid(500), "fireball"), explodingObservers()));
		var overCap = RankTenSilhouetteService.offer(cappedState,
				cast(18, uuid(500), "fireball"), List.of());
		assertFalse(overCap.accepted());
		assertEquals(1, overCap.diagnostics().acceptedEvents());
		assertEquals(31, overCap.diagnostics().coalescedEvents());
		assertEquals(1, overCap.diagnostics().budgetRejected());

		assertDoesNotThrow(() -> RankTenSilhouetteService.offer(
				RankTenSilhouetteService.initialPolicy(1), cast(19, CASTER, "not_a_power"),
				explodingObservers()));
		assertDoesNotThrow(() -> RankTenSilhouetteService.offer(
				RankTenSilhouetteService.initialPolicy(1),
				new RankTenSilhouetteService.CastOffer(19, CASTER, 9, "flight", OVERWORLD,
						0, 0, 0, 0, 0, 0, 1), explodingObservers()));
	}

	@Test
	void eventIdsRanksVerticalDistanceAndDiagnosticsCollectionsKeepExactBoundaries() {
		var first = RankTenSilhouetteService.offer(RankTenSilhouetteService.initialPolicy(1),
				new RankTenSilhouetteService.CastOffer(2, CASTER, 11, "flight", OVERWORLD,
						0, 0, 0, 0, 0, 0, 1),
				List.of(observerAt(uuid(20), 0, 384, true, true),
						observerAt(uuid(21), 0, 384.01, true, true)));
		var invalid = RankTenSilhouetteService.offer(first.state(),
				cast(2, uuid(30), "not_a_power"), List.of());
		var coalesced = RankTenSilhouetteService.offer(invalid.state(),
				cast(2, CASTER, "flight"), List.of());
		var second = RankTenSilhouetteService.offer(coalesced.state(),
				cast(2, uuid(30), "fireball"), List.of());

		assertTrue(first.accepted());
		assertEquals(List.of(uuid(20)), first.recipients());
		assertEquals(1L, first.payload().eventId());
		assertFalse(invalid.accepted());
		assertFalse(coalesced.accepted());
		assertEquals(2L, second.payload().eventId());
		assertEquals(1, second.diagnostics().rangeObservers());
		assertThrows(UnsupportedOperationException.class,
				() -> second.diagnostics().acceptedProfiles().add("mutated"));
	}

	@Test
	void executableProductionSeamReservesBeforePreparationAndOwnsAllDeliveryBoundaries() {
		var state = RankTenSilhouetteService.initialPolicy(1);
		for (int index = 0; index < 32; index++) {
			state = RankTenSilhouetteService.offer(state,
					cast(30, uuid(1000 + index), "flight"), List.of()).state();
		}
		FakeRuntime capped = new FakeRuntime(30);
		var rejected = RankTenSilhouetteService.execute(state, "fireball", capped);
		assertFalse(rejected.decision().accepted());
		assertEquals(1, capped.tickCalls);
		assertEquals(1, capped.persistCalls);
		assertEquals(0, capped.castPreparationCalls);
		assertEquals(0, capped.playerEnumerationCalls);
		assertEquals(0, capped.capabilityCalls);
		assertEquals(0, capped.guardedSendAttempts);
		assertEquals(List.of("tick", "persist"), capped.callOrder);

		FakeRuntime runtime = new FakeRuntime(31);
		runtime.add(observerAt(uuid(1), 10, 0, true, true));
		runtime.add(observerAt(uuid(2), 385, 0, true, true));
		runtime.add(observer(3, "powers:dark_realm", 1, 0, true, true));
		runtime.add(observerAt(uuid(4), 10, 0, false, true));
		runtime.add(observerAt(uuid(5), 10, 0, true, false));
		runtime.add(observerAt(uuid(6), 10, 0, true, true));
		runtime.staleDuringSend.add(uuid(6));
		var delivered = RankTenSilhouetteService.execute(
				RankTenSilhouetteService.initialPolicy(1), "fireball", runtime);

		assertTrue(delivered.decision().accepted());
		assertEquals(1, runtime.castPreparationCalls);
		assertEquals(1, runtime.playerEnumerationCalls);
		assertEquals(4, runtime.capabilityCalls);
		assertEquals(2, runtime.guardedSendAttempts);
		assertEquals(1, runtime.guardedSendSuccesses);
		assertEquals(0, runtime.chunkOrTicketRequests);
		assertEquals(java.util.Set.of("tick", "persist", "prepareCast", "players", "canSend",
				"current", "sendGuarded"), java.util.Arrays.stream(
						RankTenSilhouetteService.RuntimeAccess.class.getDeclaredMethods())
						.map(java.lang.reflect.Method::getName).collect(java.util.stream.Collectors.toSet()));
		assertEquals(List.of(uuid(1), uuid(6)), delivered.decision().recipients());
	}

	@Test
	void productionPathUsesOnlyBoundedPlayerEnumerationGuardedSendAndNoTickets() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/powers/fx/RankTenSilhouetteService.java"));
		assertTrue(source.contains("execute(serverState.policy, powerId"));
		assertTrue(source.contains("List.copyOf(level.players())"));
		assertTrue(source.contains("ServerPlayNetworking.canSend(player"));
		assertTrue(source.contains("PowersPlayNetworking.sendGuarded(player"));
		assertTrue(source.contains("server.getPlayerList().getPlayer(player.getUUID()) == player"));
		int reservation = source.indexOf("Reservation reservation = reserve(input, tick)");
		int preparation = source.indexOf("access.prepareCast(powerId, tick)");
		int enumeration = source.indexOf("List.copyOf(access.players())");
		assertTrue(reservation >= 0 && reservation < preparation && preparation < enumeration);
		for (String forbidden : List.of("getChunk(", "getChunkSource(", "addRegionTicket(",
				"setChunkForced(", "forceLoad(", "TicketType")) {
			assertFalse(source.contains(forbidden), forbidden);
		}
	}

	@Test
	void eventIdExhaustionRejectsPresentationWithoutMutatingGameplayFacingInputs() {
		var state = RankTenSilhouetteService.initialPolicy(Long.MAX_VALUE);
		var result = RankTenSilhouetteService.offer(state, cast(1, CASTER, "double_health"), List.of());

		assertFalse(result.accepted());
		assertEquals(1, result.diagnostics().exhaustedEvents());
		assertEquals(Long.MAX_VALUE, result.state().nextEventId());
		assertEquals(0, result.diagnostics().acceptedEvents());
	}

	private static RankTenSilhouetteService.CastOffer cast(long tick, UUID caster, String power) {
		return new RankTenSilhouetteService.CastOffer(tick, caster, 10, power, OVERWORLD,
				0, 0, 0, 45, -12, 1, 0xC0FFEE);
	}

	private static RankTenSilhouetteService.ObserverOffer observer(int id, String dimension,
			double x, double z, boolean supported, boolean live) {
		return new RankTenSilhouetteService.ObserverOffer(uuid(id), dimension, x, 0, z, supported, live);
	}

	private static RankTenSilhouetteService.ObserverOffer observerAt(UUID id, double x, double y,
			boolean supported, boolean live) {
		return new RankTenSilhouetteService.ObserverOffer(id, OVERWORLD, x, y, 0, supported, live);
	}

	private static List<RankTenSilhouetteService.ObserverOffer> explodingObservers() {
		return new AbstractList<>() {
			@Override public RankTenSilhouetteService.ObserverOffer get(int index) {
				throw new AssertionError("observer work must not run for a rejected offer");
			}
			@Override public int size() { return 1; }
		};
	}

	private static final class FakeRuntime implements RankTenSilhouetteService.RuntimeAccess {
		private final long tick;
		private final List<RankTenSilhouetteService.RuntimeObserver> players = new ArrayList<>();
		private final Map<UUID, RankTenSilhouetteService.RuntimeObserver> current = new HashMap<>();
		private final java.util.Set<UUID> staleDuringSend = new java.util.HashSet<>();
		private int tickCalls;
		private int persistCalls;
		private int castPreparationCalls;
		private int playerEnumerationCalls;
		private int capabilityCalls;
		private int guardedSendAttempts;
		private int guardedSendSuccesses;
		private int chunkOrTicketRequests;
		private final List<String> callOrder = new ArrayList<>();

		private FakeRuntime(long tick) { this.tick = tick; }

		private void add(RankTenSilhouetteService.ObserverOffer observer) {
			Object handle = new Object();
			Object connection = new Object();
			var runtime = new RankTenSilhouetteService.RuntimeObserver(observer.player(),
					observer.dimension(), observer.x(), observer.y(), observer.z(), handle, connection,
					observer.liveSession());
			players.add(runtime);
			current.put(observer.player(), runtime);
		}

		@Override public long tick() { tickCalls++; callOrder.add("tick"); return tick; }
		@Override public void persist(RankTenSilhouetteService.PolicyState state) {
			persistCalls++;
			callOrder.add("persist");
		}
		@Override public RankTenSilhouetteService.CastOffer prepareCast(String powerId, long reservedTick) {
			castPreparationCalls++;
			callOrder.add("prepare");
			return cast(reservedTick, CASTER, powerId);
		}
		@Override public List<RankTenSilhouetteService.RuntimeObserver> players() {
			playerEnumerationCalls++;
			return players;
		}
		@Override public boolean canSend(RankTenSilhouetteService.RuntimeObserver observer) {
			capabilityCalls++;
			return observer.player().getLeastSignificantBits() != 4;
		}
		@Override public RankTenSilhouetteService.RuntimeObserver current(
				RankTenSilhouetteService.RuntimeObserver observer) {
			return current.get(observer.player());
		}
		@Override public void sendGuarded(RankTenSilhouetteService.RuntimeObserver observer,
				com.powers.network.RankTenSilhouettePackets.Payload payload,
				java.util.function.Predicate<RankTenSilhouetteService.RuntimeObserver> guard) {
			guardedSendAttempts++;
			var candidate = current(observer);
			if (staleDuringSend.contains(observer.player())) {
				candidate = new RankTenSilhouetteService.RuntimeObserver(candidate.player(),
						"powers:dark_realm", candidate.x(), candidate.y(), candidate.z(),
						candidate.handle(), candidate.connection(), true);
			}
			if (guard.test(candidate)) guardedSendSuccesses++;
		}
	}

	private static UUID uuid(int value) {
		return new UUID(0xABCDL, value);
	}
}
