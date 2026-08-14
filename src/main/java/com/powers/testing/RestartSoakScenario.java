package com.powers.testing;

import com.powers.PowersBlocks;
import com.powers.PowersMod;
import com.powers.force.LivingForceManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.power.artifact.ArtifactGuardianSummons;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.spell.CelestialRuinCancellation;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.SpellFieldKind;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Operator-only live scenario used by the repeated-restart acceptance harness. */
public final class RestartSoakScenario {
	public static final String CLIENT_NAME = "SoakClient";
	private static final int SETTLE_TICKS = 80;
	private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();

	public record Result(boolean passed, String detail) {
		public int commandResult() {
			return passed ? 1 : 0;
		}
	}

	private static final class State {
		private final int cycle;
		private final java.util.UUID owner;
		private final ResourceKey<Level> dimension;
		private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
		private final int baselineBodies;
		private final int baselineTravel;
		private final int baselineFields;
		private final int baselineGuardians;
		private final int baselineForceBlocks;
		private boolean travelResolved;
		private boolean settled;
		private boolean rollover;

		private State(int cycle, ServerPlayer player) {
			this.cycle = cycle;
			owner = player.getUUID();
			dimension = player.level().dimension();
			baselineBodies = BodyProxyManager.activeProxyCount();
			baselineTravel = TravelChunkLoader.pendingRequestCount();
			baselineFields = SpellFieldManager.activeFieldCount();
			baselineGuardians = ArtifactGuardianSummons.indexedGuardianCount();
			baselineForceBlocks = LivingForceManager.diagnostics().indexedBlocks();
		}
	}

	private RestartSoakScenario() {
	}

	/** Verifies restart cleanup and consumes the prior cycle's persisted Ruin countdown. */
	public static Result verifyStartup(ServerPlayer player, int cycle) {
		MinecraftServer server = player.level().getServer();
		if (STATES.containsKey(server)) return fail("scenario state survived process startup");
		int rituals = CelestialRuinManager.activeRitualCount(server);
		int expected = cycle == 1 ? 0 : 1;
		if (rituals != expected) {
			return fail("expected " + expected + " recovered Ruin event(s), found " + rituals);
		}
		boolean recovered = rituals == 1;
		if (recovered && CelestialRuinManager.cancelNearest((ServerLevel) player.level(),
				player.position()) != CelestialRuinCancellation.CANCELLED) {
			return fail("recovered Ruin event was not cancellable");
		}
		String leak = leakReason(server, null, true);
		return leak.isEmpty() ? pass("recoveredRuin=" + recovered) : fail(leak);
	}

	/** Seeds every required runtime owner, then schedules bounded cleanup by stable identity. */
	public static Result seed(ServerPlayer player, int cycle) {
		MinecraftServer server = player.level().getServer();
		if (cycle < 1 || STATES.containsKey(server) || GlobalTimeStopManager.isStopped(server)) {
			return fail("cycle is invalid, already active, or externally frozen");
		}
		State state = new State(cycle, player);
		STATES.put(server, state);
		com.powers.player.PlayerPowers.get(player).setSlots(player, java.util.List.of(
				PowersMod.id("thunderclap").toString(), PowersMod.id("fireball").toString(),
				PowersMod.id("flight").toString()));
		ServerLevel level = (ServerLevel) player.level();
		BlockPos origin = player.blockPosition();
		placeSummonPad(level, origin, state);
		placeForceCage(level, origin.offset(14, 0, 0), PowersBlocks.DARKNESS, state);
		placeForceCage(level, origin.offset(-14, 0, 0), PowersBlocks.PURE_LIGHT, state);
		SpellFieldManager.add(SpellFieldKind.SANCTUARY, player, SETTLE_TICKS, 3.0, 1);
		boolean body = BodyProxyManager.start(player, BodyProxyKind.ASTRAL);
		int guardians = ArtifactGuardianSummons.summon(player, ArtifactAlignment.DARKNESS,
				1, false, null, true);
		BlockPos travelTarget = origin.offset(160, 0, 160);
		boolean travel = TravelChunkLoader.request(player.getUUID(), level, travelTarget,
				"restart_soak", RestartSoakScenario::markTravelResolved,
				RestartSoakScenario::markTravelResolved);
		boolean frozen = GlobalTimeStopManager.startCrystal(player, 40);
		java.util.List<String> failures = new java.util.ArrayList<>();
		if (!body) failures.add("body");
		if (guardians != 1) failures.add("guardians=" + guardians);
		if (!travel) failures.add("travel-request");
		if (!frozen) failures.add("time-freeze");
		if (!SpellFieldManager.hasField(player.getUUID(), SpellFieldKind.SANCTUARY)) {
			failures.add("spell-field");
		}
		int forceBlocks = LivingForceManager.diagnostics().indexedBlocks();
		if (forceBlocks < state.baselineForceBlocks + 2) failures.add("living-forces=" + forceBlocks);
		int bodies = BodyProxyManager.activeProxyCount();
		if (bodies != state.baselineBodies + 1) failures.add("bodies=" + bodies);
		int indexedGuardians = ArtifactGuardianSummons.indexedGuardianCount();
		if (indexedGuardians != state.baselineGuardians + 1) {
			failures.add("guardian-index=" + indexedGuardians);
		}
		int tickets = TravelChunkLoader.pendingRequestCount();
		if (tickets < state.baselineTravel + 1) failures.add("travel-tickets=" + tickets);
		if (!GlobalTimeStopManager.isStopped(server)) failures.add("clock-not-frozen");
		if (!failures.isEmpty()) {
			settle(server, player.getUUID());
			STATES.remove(server);
			return fail("seed rejected: " + String.join(",", failures));
		}
		var token = PowersMod.scheduleDelayed(server, SETTLE_TICKS, state.owner,
				level.dimension(), state.owner, "restart_soak_settle",
				(current, task) -> settle(current, task.subjectId()));
		if (!token.accepted()) {
			settle(server, state.owner);
			STATES.remove(server);
			return fail("settlement callback was rejected");
		}
		return pass("systems=" + RestartSoakScenarioPlan.requiredSystems().size());
	}

	private static void markTravelResolved(MinecraftServer server, UUID owner) {
		State state = STATES.get(server);
		if (state != null && state.owner.equals(owner)) state.travelResolved = true;
	}

	/** Starts one persisted countdown shortly before the harness restarts the server. */
	public static Result rollover(ServerPlayer player, int cycle) {
		MinecraftServer server = player.level().getServer();
		State state = STATES.get(server);
		if (state == null || state.cycle != cycle || !state.owner.equals(player.getUUID())
				|| !state.settled || state.rollover) return fail("cycle is not settled for rollover");
		String leak = leakReason(server, state, false);
		if (!leak.isEmpty()) return fail(leak);
		BlockPos center = player.blockPosition().offset(256 + cycle * 4, 0, 256);
		if (!CelestialRuinManager.begin(player, center)) return fail("Ruin rollover was rejected");
		state.rollover = true;
		return CelestialRuinManager.activeRitualCount(server) == 1
				? pass("persistedRuin=true") : fail("Ruin rollover was not indexed");
	}

	/** Machine-readable state probe used immediately before save/termination. */
	public static Result status(ServerPlayer player, int cycle) {
		MinecraftServer server = player.level().getServer();
		State state = STATES.get(server);
		if (state == null || state.cycle != cycle || !state.settled) {
			return fail("cycle has not settled");
		}
		String leak = leakReason(server, state, false);
		if (!leak.isEmpty()) return fail(leak);
		int rituals = CelestialRuinManager.activeRitualCount(server);
		if (rituals != (state.rollover ? 1 : 0)) return fail("unexpected Ruin count " + rituals);
		return pass("travelResolved=" + state.travelResolved + "; rollover=" + state.rollover);
	}

	public static void clear(MinecraftServer server) {
		STATES.remove(server);
	}

	private static void settle(MinecraftServer server, java.util.UUID ownerId) {
		State state = STATES.get(server);
		if (state == null || !state.owner.equals(ownerId) || state.settled) return;
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		if (player != null) {
			GlobalTimeStopManager.stopCrystal(player);
			BodyProxyManager.finish(player);
			ArtifactGuardianSummons.revokeOwner(server, ownerId, ArtifactAlignment.DARKNESS);
		}
		TravelChunkLoader.clear(server);
		SpellFieldManager.clearAll();
		ServerLevel level = server.getLevel(state.dimension);
		if (level != null) state.originals.forEach((position, original) ->
				level.setBlock(position, original, Block.UPDATE_ALL));
		state.settled = true;
		String leak = leakReason(server, state, false);
		PowersMod.LOGGER.info("POWERS_SOAK_SETTLED cycle={} passed={} detail={}", state.cycle,
				leak.isEmpty(), leak.isEmpty() ? "clean" : leak);
	}

	private static void placeForceCage(ServerLevel level, BlockPos center, Block force,
			State state) {
		for (Direction direction : Direction.values()) {
			rememberAndSet(level, center.relative(direction), Blocks.BEDROCK.defaultBlockState(), state);
		}
		rememberAndSet(level, center, force.defaultBlockState(), state);
	}

	private static void placeSummonPad(ServerLevel level, BlockPos center, State state) {
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				BlockPos floor = center.offset(x, -1, z);
				if (level.getBlockState(floor).isAir()) {
					rememberAndSet(level, floor, Blocks.STONE.defaultBlockState(), state);
				}
			}
		}
	}

	private static void rememberAndSet(ServerLevel level, BlockPos position, BlockState replacement,
			State state) {
		BlockPos immutable = position.immutable();
		state.originals.putIfAbsent(immutable, level.getBlockState(immutable));
		level.setBlock(immutable, replacement, Block.UPDATE_ALL);
	}

	private static String leakReason(MinecraftServer server, State state, boolean requireNoRuin) {
		int bodies = state == null ? 0 : state.baselineBodies;
		int travel = state == null ? 0 : state.baselineTravel;
		int fields = state == null ? 0 : state.baselineFields;
		int guardians = state == null ? 0 : state.baselineGuardians;
		int forceBlocks = state == null ? 0 : state.baselineForceBlocks;
		if (BodyProxyManager.activeProxyCount() != bodies) return "body proxy leak";
		if (TravelChunkLoader.pendingRequestCount() != travel) return "travel ticket leak";
		if (SpellFieldManager.activeFieldCount() != fields) return "spell field leak";
		if (ArtifactGuardianSummons.indexedGuardianCount() != guardians) return "guardian index leak";
		if (LivingForceManager.diagnostics().indexedBlocks() != forceBlocks) return "living-force index leak";
		if (GlobalTimeStopManager.isStopped(server)) return "Time Freeze ownership leak";
		if (requireNoRuin && CelestialRuinManager.activeRitualCount(server) != 0) {
			return "Celestial Ruin leak";
		}
		return "";
	}

	private static Result pass(String detail) {
		return new Result(true, detail);
	}

	private static Result fail(String detail) {
		return new Result(false, detail);
	}
}
