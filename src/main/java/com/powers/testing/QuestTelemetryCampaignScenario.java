package com.powers.testing;

import com.powers.player.DarknessQuestTracker;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillQuestTracker;
import com.powers.player.SkillSystem;
import com.powers.power.Power;
import com.powers.power.PowerDamage;
import com.powers.power.PowerRegistry;
import com.powers.progression.QuestCompletionTelemetry;
import com.powers.progression.QuestTelemetryLedger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Operator-only live campaign that replays human-cadence deeds for connected clients. */
public final class QuestTelemetryCampaignScenario {
	public static final String LIGHT_PREFIX = "QuestLight";
	public static final String DARK_PREFIX = "QuestDark";
	private static final int REQUIRED_PLAYERS = QuestCompletionTelemetry.PUBLICATION_SAMPLE_MINIMUM;
	private static final Map<MinecraftServer, State> ACTIVE = new WeakHashMap<>();

	public record Result(boolean passed, String detail) {
		public int commandResult() {
			return passed ? 1 : 0;
		}
	}

	private record Actor(UUID player, QuestTelemetryCampaignPlan.Profile profile) { }

	private static final class State {
		private final QuestTelemetryLedger.Alignment alignment;
		private final List<Actor> actors;
		private final long startedTick;
		private final int equivalentTicksPerServerTick;
		private long previousEquivalentTick;
		private boolean finished;
		private String failure = "";

		private State(QuestTelemetryLedger.Alignment alignment, List<Actor> actors,
				long startedTick, int equivalentTicksPerServerTick) {
			this.alignment = alignment;
			this.actors = actors;
			this.startedTick = startedTick;
			this.equivalentTicksPerServerTick = equivalentTicksPerServerTick;
		}
	}

	private QuestTelemetryCampaignScenario() {
	}

	/** Starts against the exact connected roster; the multiplier is one outside GameTests. */
	public static Result start(List<ServerPlayer> players,
			QuestTelemetryLedger.Alignment alignment, int equivalentTicksPerServerTick) {
		if (players == null || alignment == null || players.size() != REQUIRED_PLAYERS
				|| equivalentTicksPerServerTick < 1 || equivalentTicksPerServerTick > 10_000) {
			return fail("requires exactly ten players and a bounded tick multiplier");
		}
		MinecraftServer server = players.getFirst().level().getServer();
		if (ACTIVE.containsKey(server)) return fail("another campaign is active");
		if (players.stream().anyMatch(player -> player == null
				|| player.level().getServer() != server)
				|| new HashSet<>(players.stream().map(ServerPlayer::getUUID).toList()).size()
				!= REQUIRED_PLAYERS) {
			return fail("players must be unique and connected to one server");
		}
		List<ServerPlayer> ordered = players.stream()
				.sorted(Comparator.comparing(ServerPlayer::getScoreboardName)).toList();
		List<QuestTelemetryCampaignPlan.Profile> profiles =
				QuestTelemetryCampaignPlan.profiles(alignment);
		List<Actor> actors = new ArrayList<>(REQUIRED_PLAYERS);
		for (int index = 0; index < ordered.size(); index++) {
			ServerPlayer player = ordered.get(index);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			if (data.skillLevel() != 0 || data.darknessLevel() != 0) {
				return fail("campaign players must use fresh progression identities");
			}
			if (alignment == QuestTelemetryLedger.Alignment.DARK) {
				player.addTag(SkillSystem.DARKNESS_TAG);
			} else {
				player.removeTag(SkillSystem.DARKNESS_TAG);
			}
			QuestCompletionTelemetry.noteActivity(player, alignment);
			actors.add(new Actor(player.getUUID(), profiles.get(index)));
		}
		ACTIVE.put(server, new State(alignment, List.copyOf(actors), server.getTickCount(),
				equivalentTicksPerServerTick));
		return pass("alignment=" + alignment + "; players=" + REQUIRED_PLAYERS);
	}

	/** Selects the ten purpose-named real clients used by the external campaign harness. */
	public static Result startConnected(MinecraftServer server,
			QuestTelemetryLedger.Alignment alignment) {
		String prefix = alignment == QuestTelemetryLedger.Alignment.LIGHT
				? LIGHT_PREFIX : DARK_PREFIX;
		List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
				.filter(player -> player.getScoreboardName().startsWith(prefix)).toList();
		return start(players, alignment, 1);
	}

	public static Result status(MinecraftServer server) {
		State state = ACTIVE.get(server);
		if (state == null) return fail("no campaign is active");
		if (!state.failure.isEmpty()) return fail(state.failure);
		long equivalent = equivalentTick(server, state);
		return pass("alignment=" + state.alignment + "; finished=" + state.finished
				+ "; equivalentTick=" + equivalent + "; maximumTick="
				+ maximumTick(state));
	}

	public static void tick(MinecraftServer server) {
		State state = ACTIVE.get(server);
		if (state == null || state.finished || !state.failure.isEmpty()) return;
		long equivalent = equivalentTick(server, state);
		for (Actor actor : state.actors) {
			ServerPlayer player = server.getPlayerList().getPlayer(actor.player);
			if (player == null) {
				state.failure = "client disconnected before completion";
				return;
			}
			replay(player, state.alignment, actor.profile,
					state.previousEquivalentTick, equivalent);
		}
		state.previousEquivalentTick = equivalent;
		state.finished = state.actors.stream().allMatch(actor -> {
			ServerPlayer player = server.getPlayerList().getPlayer(actor.player);
			if (player == null) return false;
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			return state.alignment == QuestTelemetryLedger.Alignment.LIGHT
					? data.skillLevel() == 10 : data.darknessLevel() == 10;
		});
		if (state.finished) {
			com.powers.PowersMod.LOGGER.info(
					"POWERS_QUEST_CAMPAIGN alignment={} passed=true equivalentTicks={}",
					state.alignment, equivalent);
		}
	}

	public static void clear(MinecraftServer server) {
		ACTIVE.remove(server);
	}

	private static void replay(ServerPlayer player, QuestTelemetryLedger.Alignment alignment,
			QuestTelemetryCampaignPlan.Profile profile, long previous, long current) {
		if (alignment == QuestTelemetryLedger.Alignment.LIGHT) {
			Power power = PowerRegistry.get("powers:thunderclap");
			repeat(profile, "power_use", previous, current,
					() -> SkillQuestTracker.recordPowerUse(player, power.ability()));
			repeat(profile, "power_kill", previous, current,
					() -> SkillQuestTracker.recordKill(victim(player, EntityTypes.COW),
							PowerDamage.source(player)));
			repeat(profile, "boss_kill", previous, current,
					() -> SkillQuestTracker.recordKill(victim(player, EntityTypes.WARDEN),
							PowerDamage.source(player)));
			repeat(profile, "light_memory", previous, current,
					() -> SkillQuestTracker.recordLightMemory(player));
			return;
		}
		repeat(profile, "passive", previous, current,
				() -> DarknessQuestTracker.recordKill(victim(player, EntityTypes.COW),
						PowerDamage.source(player)));
		repeat(profile, "villager", previous, current,
				() -> DarknessQuestTracker.recordKill(victim(player, EntityTypes.VILLAGER),
						PowerDamage.source(player)));
		repeat(profile, "wolf", previous, current,
				() -> DarknessQuestTracker.recordKill(victim(player, EntityTypes.WOLF),
						PowerDamage.source(player)));
		repeat(profile, "baby_villager", previous, current, () -> {
			LivingEntity victim = victim(player, EntityTypes.VILLAGER);
			if (victim instanceof Villager villager) villager.setBaby(true);
			DarknessQuestTracker.recordKill(victim, PowerDamage.source(player));
		});
		repeat(profile, "iron_golem", previous, current,
				() -> DarknessQuestTracker.recordKill(victim(player, EntityTypes.IRON_GOLEM),
						PowerDamage.source(player)));
	}

	private static void repeat(QuestTelemetryCampaignPlan.Profile profile, String deed,
			long previous, long current, Runnable action) {
		long count = current / profile.interval(deed) - previous / profile.interval(deed);
		for (long event = 0; event < count; event++) action.run();
	}

	private static LivingEntity victim(ServerPlayer player,
			EntityType<? extends LivingEntity> type) {
		ServerLevel level = (ServerLevel) player.level();
		LivingEntity victim = type.create(level, EntitySpawnReason.COMMAND);
		if (victim == null) throw new IllegalStateException("Could not create quest deed fixture");
		return victim;
	}

	private static long equivalentTick(MinecraftServer server, State state) {
		return Math.max(0L, server.getTickCount() - state.startedTick)
				* state.equivalentTicksPerServerTick;
	}

	private static long maximumTick(State state) {
		return state.actors.stream().mapToLong(actor ->
				actor.profile.maximumCompletionTick(state.alignment)).max().orElse(0L);
	}

	private static Result pass(String detail) {
		return new Result(true, detail);
	}

	private static Result fail(String detail) {
		return new Result(false, detail);
	}
}
