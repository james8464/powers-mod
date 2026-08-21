package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.BodyReturnFallbackRules;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.progression.PowerScalingService;
import com.powers.power.Ability;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.travel.DestinationFailure;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelCohort;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
import com.powers.power.travel.TeleportStormTracker;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.powers.power.abilities.TeleportDelayedState.MARKING;
import static com.powers.power.abilities.TeleportDelayedState.PENDING_MARKING;
import static com.powers.power.abilities.TeleportDelayedState.PENDING_TELEPORTS;
import static com.powers.power.abilities.TeleportDelayedState.MarkingState;
import static com.powers.power.abilities.TeleportDelayedState.PendingMarking;
import static com.powers.power.abilities.TeleportDelayedState.PendingTeleport;
import static com.powers.power.abilities.TeleportDelayedState.findLiving;

/**
 * time shift - mark a target spot or player, then blink there after a short
 * storm while riding along with anything close by; marking puts you in
 * spectator mode so you can scout the landing zone
 */
public class TeleportAbility extends Ability {
	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("time_shift");
	private static final int STORM_TICKS = 100;
	private static final int TELEPORT_DELAY_TICKS = 50;
	private static final int MARK_TIMEOUT_TICKS = 200;

	private static final TeleportStormTracker ACTIVE_STORMS = new TeleportStormTracker();

	public TeleportAbility() {
		super(POWER_ID,
				Component.translatable("ability.powers.time_shift"),
				400, true);
	}

	public static boolean startMarking(ServerPlayer player, LivingEntity target, int slot) {
		ServerLevel targetLevel = (ServerLevel) target.level();
		Vec3 entry = target.position().add(0, 2, 0);
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validate(
				player, targetLevel, entry, TravelKind.PROJECTION);
		if (!destination.allowed()) {
				TravelFailurePresenter.report(player, player, entry, destination.failure());
			return false;
		}
		if (!BodyProxyManager.start(player, BodyProxyKind.MARKING)) return false;
		MARKING.put(player.getUUID(), new MarkingState(
				player.level().dimension(), player.position(), player.gameMode(),
				targetLevel.dimension(), target.position(), CastScalingContext.currentSource(),
				((ServerLevel) player.level()).getServer().getTickCount()
						+ PowerScalingService.duration(player, "time_shift", MARK_TIMEOUT_TICKS), slot));
		player.setGameMode(GameType.SPECTATOR);
		PowerFx.rune((ServerLevel) target.level(), target.position().add(0, 2, 0), 1.5, 0x88CCFF, 20, 0.6);
		PowerFx.sound((ServerLevel) target.level(), target.position(), SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.2f);
		player.teleport(new TeleportTransition(targetLevel,
				entry, Vec3.ZERO, player.getYRot(), player.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND));
		PowerMessages.send(player, "ability.powers.marking_mode", 3);
		return true;
	}

	/** Completes the marking teleport to the coordinates picked in spectator mode, restoring your game mode. */
	public static void completeMarking(ServerPlayer player, int slot, Vec3 pos) {
		MarkingState state = MARKING.get(player.getUUID());
		if (state == null || state.slot() != slot) return;
		MARKING.remove(player.getUUID());
		if (!MagicUseGate.ongoingAllowed(player) || !ServerCastLifecycle.mayContinue(
				player, state.castSource(), ownsPower(player))) {
			restore(player, state);
			return;
		}
		if (!Double.isFinite(pos.x()) || !Double.isFinite(pos.y()) || !Double.isFinite(pos.z())) {
			restore(player, state);
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		double maximumDistance = PowersConfigLoader.get().teleportMaxChunkDistance() * 16.0;
		if (!level.dimension().equals(state.markingDimension())
				|| pos.distanceToSqr(state.markingCenter()) > maximumDistance * maximumDistance) {
			PowerMessages.send(player, "ability.powers.out_of_bounds", 3);
			restore(player, state);
			return;
		}
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validateExact(
				player, level, pos, TravelKind.POWER);
		if (!destination.allowed()) {
			TravelFailurePresenter.report(player, player, pos, destination.failure());
			restore(player, state);
			return;
		}
		if (!ACTIVE_STORMS.begin(player.getUUID())) {
			PowerMessages.overlay(player, Component.translatable("ability.powers.teleport_storm_active"));
			restore(player, state);
			return;
		}
		MinecraftServer server = level.getServer();
		ServerLevel originalLevel = server.getLevel(state.originalDimension());
		if (originalLevel == null) {
			ACTIVE_STORMS.finish(player.getUUID());
			restore(player, state);
			return;
		}
		PowersMod.startStorm(originalLevel, state.originalPos(), STORM_TICKS);
		PowersMod.startStorm(level, pos, STORM_TICKS);
		PowerFx.rune(originalLevel, state.originalPos(), 2.0, 0x8AE8FF, 24, 0.0);
		PowerFx.rune(level, pos, 2.0, 0x8AE8FF, 24, Math.PI * 0.5);
		UUID ownerId = player.getUUID();
		PENDING_MARKING.put(ownerId, new PendingMarking(state, level.dimension(), pos));
		PowersMod.scheduleDelayed(server, TELEPORT_DELAY_TICKS, ownerId, level.dimension(), ownerId,
				"marking_teleport", TeleportAbility::completeDelayedMarking);
	}

	private static void completeDelayedMarking(MinecraftServer server,
			com.powers.util.ScheduledTaskQueue.TaskDescriptor task) {
		UUID ownerId = task.subjectId();
		PendingMarking pending = PENDING_MARKING.remove(ownerId);
		ServerPlayer current = server.getPlayerList().getPlayer(ownerId);
		ServerLevel level = pending == null ? null : server.getLevel(pending.destination());
		ServerLevel originalLevel = pending == null ? null
				: server.getLevel(pending.state().originalDimension());
		if (pending == null || current == null || level == null || originalLevel == null
				|| !BodyProxyManager.hasSession(current, BodyProxyKind.MARKING)
				|| current.level() != level || !MagicUseGate.ongoingAllowed(current)
				|| !ServerCastLifecycle.mayContinue(current, pending.state().castSource(), ownsPower(current))
				|| !SafeDestinationResolver.validateExact(current, level, pending.position(),
						TravelKind.POWER).allowed()) {
			ACTIVE_STORMS.finish(ownerId);
			if (current != null && pending != null) restore(current, pending.state());
			return;
		}
		TravelCohort.Snapshot cohort = TravelCohort.captureAt(originalLevel, current, current,
				pending.state().originalPos());
		current.teleport(new TeleportTransition(level, pending.position(), Vec3.ZERO,
				current.getYRot(), current.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		current.setGameMode(pending.state().originalMode());
		BodyProxyManager.finish(current);
		TravelCohort.move(cohort, level, pending.position());
		scheduleStormFinish(server, ownerId, level.dimension(),
				STORM_TICKS - TELEPORT_DELAY_TICKS);
	}

	/**
	 * Called on disconnect. Dropping the state on its own would strand the
	 * player: marking puts them in spectator at the target's feet, so logging
	 * out mid-mark used to mean coming back a permanent spectator. Undo the
	 * marking first, then forget it.
	 */
	public static void clearMarking(ServerPlayer player) {
		MarkingState state = MARKING.remove(player.getUUID());
		PendingMarking pending = PENDING_MARKING.remove(player.getUUID());
		if (state == null && pending != null) state = pending.state();
		if (state != null) {
			restore(player, state);
		}
	}

	public static void clearAllMarking(MinecraftServer server) {
		for (var entry : new ArrayList<>(MARKING.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) restore(player, entry.getValue());
		}
		for (var entry : new ArrayList<>(PENDING_MARKING.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) restore(player, entry.getValue().state());
		}
		MARKING.clear();
		PENDING_MARKING.clear();
	}

	public static boolean isMarking(UUID owner) {
		return MARKING.containsKey(owner);
	}

	private static void restore(ServerPlayer player, MarkingState state) {
		boolean hadAnchor = BodyProxyManager.hasSession(player, BodyProxyKind.MARKING);
		boolean returned = hadAnchor && BodyProxyManager.returnToBody(player);
		if (returned) return;
		if (!BodyReturnFallbackRules.mayUseLegacyFallback(hadAnchor, returned)) {
			player.setGameMode(GameType.SPECTATOR);
			PowerMessages.send(player, "realm.powers.return_restricted", 4);
			return;
		}
		MinecraftServer server = player.level().getServer();
		ServerLevel originalLevel = server == null ? null : server.getLevel(state.originalDimension());
		if (originalLevel != null) {
			player.teleportTo(originalLevel, state.originalPos().x, state.originalPos().y, state.originalPos().z,
					Set.of(), player.getYRot(), player.getXRot(), false);
		}
		player.setGameMode(state.originalMode());
		BodyProxyManager.finish(player);
	}

	public static void tickMarking(MinecraftServer server) {
		var it = MARKING.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			MarkingState state = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			boolean expired = server.getTickCount() >= state.deadline();
			boolean interrupted = !MagicUseGate.ongoingAllowed(player)
					|| !ServerCastLifecycle.mayContinue(
							player, state.castSource(), ownsPower(player));
			if (expired || interrupted) {
				restore(player, state);
				PowerMessages.overlay(player, expired
						? PowerMessages.random("ability.powers.marking_expired", 3)
						: Component.translatable("spell.powers.interrupted"));
				it.remove();
			}
		}
	}

	@Override
	public boolean activateTeleport(ServerPlayer caster, LivingEntity player, PlayerPowers.PlayerPowersData data,
			ResourceKey<Level> dimension, double x, double y, double z) {
		MinecraftServer server = player.level().getServer();
		ServerLevel targetLevel = server == null ? null : server.getLevel(dimension);
		if (targetLevel == null) {
			PowerMessages.send(caster, "ability.powers.bad_dimension", 3);
			return false;
		}
		ServerLevel originLevel = (ServerLevel) player.level();
		Vec3 target = new Vec3(x, y, z);
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validatePreload(
				player, targetLevel, target, TravelKind.POWER);
		if (!destination.allowed()) {
				TravelFailurePresenter.report(caster, player, target, destination.failure());
			return false;
		}
		AsyncAbilityTransaction transaction = new AsyncAbilityTransaction(caster, data, this);
		CastSource castSource = CastScalingContext.currentSource();
		if (!ACTIVE_STORMS.begin(caster.getUUID())) {
			PowerMessages.overlay(caster, Component.translatable("ability.powers.teleport_storm_active"));
			return false;
		}
		UUID ownerId = caster.getUUID();
		PENDING_TELEPORTS.put(ownerId, new PendingTeleport(ownerId, player.getUUID(),
				originLevel.dimension(), dimension, target, castSource, STORM_TICKS,
				TELEPORT_DELAY_TICKS, transaction, false));
		boolean accepted = TravelChunkLoader.request(ownerId, targetLevel, BlockPos.containing(target),
				"teleport_power", TeleportAbility::beginLoadedTeleport,
				TeleportAbility::failLoadedTeleport);
		if (!accepted) {
			ACTIVE_STORMS.finish(ownerId);
			PENDING_TELEPORTS.remove(ownerId);
		}
		return accepted;
	}

	private static void beginLoadedTeleport(MinecraftServer server, UUID ownerId) {
		PendingTeleport pending = PENDING_TELEPORTS.get(ownerId);
		ServerPlayer caster = server.getPlayerList().getPlayer(ownerId);
		LivingEntity player = pending == null ? null : findLiving(server, pending.travellerId());
		ServerLevel originLevel = pending == null ? null : server.getLevel(pending.origin());
		ServerLevel targetLevel = pending == null ? null : server.getLevel(pending.destination());
		if (pending == null || caster == null || player == null || originLevel == null
				|| targetLevel == null) {
			abortStorm(server, pending, caster, player);
			return;
		}
		if (!MagicUseGate.ongoingAllowed(caster) || !subjectMayContinue(player)
				|| !ServerCastLifecycle.mayContinue(caster, pending.castSource(), ownsPower(caster))
				|| !player.isAlive() || player.level() != originLevel) {
			abortStorm(server, pending, caster, player);
			return;
		}
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validateExact(
				player, targetLevel, pending.target(), TravelKind.POWER);
		if (!destination.allowed()) {
			abortStorm(server, pending, caster, player);
			TravelFailurePresenter.report(caster, player, pending.target(), destination.failure());
			return;
		}
		boolean bodyStarted = player instanceof ServerPlayer subject
				&& BodyProxyManager.start(subject, BodyProxyKind.MARKING);
		if (player instanceof ServerPlayer && !bodyStarted) {
			abortStorm(server, pending, caster, player);
			return;
		}
		pending = pending.withBodyStarted(bodyStarted);
		PENDING_TELEPORTS.put(ownerId, pending);
		Vec3 origin = player.position();
		PowerFx.rune(originLevel, origin, 2.0, 0x8AE8FF, 24, 0.0);
		PowerFx.rune(targetLevel, pending.target(), 2.0, 0x8AE8FF, 24, Math.PI * 0.5);
		PowerFx.sound(originLevel, origin, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.0f);
		PowerFx.sound(targetLevel, pending.target(), SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.15f);
		// the blink itself is delayed so the storm can build up at both ends;
		// the lightning beneath the traveler echoes the realm they're bound for
		PowersMod.startStorm(originLevel, origin,
				player instanceof ServerPlayer serverPlayer ? serverPlayer : null,
				pending.stormTicks(), pending.teleportDelay(), themeFor(pending.destination()));
		PowersMod.startStorm(targetLevel, pending.target(), null, pending.stormTicks(), 0);
		PowersMod.scheduleDelayed(server, pending.teleportDelay(), ownerId, pending.origin(), ownerId,
				"teleport_commit", TeleportAbility::completeDelayedTeleport);
	}

	private static void completeDelayedTeleport(MinecraftServer server,
			com.powers.util.ScheduledTaskQueue.TaskDescriptor task) {
		UUID ownerId = task.subjectId();
		PendingTeleport pending = PENDING_TELEPORTS.get(ownerId);
		ServerPlayer caster = server.getPlayerList().getPlayer(ownerId);
		LivingEntity traveller = pending == null ? null : findLiving(server, pending.travellerId());
		ServerLevel origin = pending == null ? null : server.getLevel(pending.origin());
		ServerLevel target = pending == null ? null : server.getLevel(pending.destination());
		if (pending == null || caster == null || traveller == null || origin == null || target == null) {
			abortStorm(server, pending, caster, traveller);
			return;
		}
		if (traveller instanceof ServerPlayer subject) AmethystDampening.update(subject);
		if (caster != traveller) AmethystDampening.update(caster);
		if (!DelayedTravelRules.travellerMayContinue(true,
				caster.isAlive() && !caster.isRemoved(), traveller.isAlive() && !traveller.isRemoved(),
				caster.level() == origin, traveller.level() == origin,
				AmethystDampening.isDampened(caster), AmethystDampening.isDampened(traveller))
				|| !MagicUseGate.ongoingAllowed(caster) || !subjectMayContinue(traveller)
				|| !ServerCastLifecycle.mayContinue(caster, pending.castSource(), ownsPower(caster))) {
			abortStorm(server, pending, caster, traveller);
			return;
		}
		SafeDestinationResolver.Result revalidated = SafeDestinationResolver.validateExact(
				traveller, target, pending.target(), TravelKind.POWER);
		if (!revalidated.allowed()) {
			abortStorm(server, pending, caster, traveller);
			TravelFailurePresenter.report(caster, traveller, pending.target(), revalidated.failure());
			return;
		}
		TravelCohort.Snapshot cohort = TravelCohort.capture(origin, caster, traveller);
		var moved = traveller.teleport(new TeleportTransition(target, pending.target(), Vec3.ZERO,
				traveller.getYRot(), traveller.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		if (!(moved instanceof LivingEntity arrived) || arrived.level() != target) {
			abortStorm(server, pending, caster, traveller);
			return;
		}
		if (pending.bodyStarted() && traveller instanceof ServerPlayer subject) {
			BodyProxyManager.finish(subject);
		}
		pending.transaction().succeed();
		PENDING_TELEPORTS.remove(ownerId, pending);
		TravelCohort.move(cohort, target, pending.target());
		scheduleStormFinish(server, ownerId, target.dimension(),
				Math.max(1, pending.stormTicks() - pending.teleportDelay()));
	}

	private static void failLoadedTeleport(MinecraftServer server, UUID ownerId) {
		PendingTeleport pending = PENDING_TELEPORTS.remove(ownerId);
		ACTIVE_STORMS.finish(ownerId);
		if (pending == null) return;
		pending.transaction().fail(server);
		ServerPlayer caster = server.getPlayerList().getPlayer(ownerId);
		LivingEntity traveller = findLiving(server, pending.travellerId());
		if (caster != null && traveller != null) {
			TravelFailurePresenter.report(caster, traveller, pending.target(),
					DestinationFailure.UNLOADED_CHUNK);
		}
	}

	private static void abortStorm(MinecraftServer server, PendingTeleport pending,
			ServerPlayer caster, LivingEntity traveller) {
		UUID ownerId = pending == null ? (caster == null ? null : caster.getUUID()) : pending.casterId();
		if (ownerId != null) {
			ACTIVE_STORMS.finish(ownerId);
			PENDING_TELEPORTS.remove(ownerId);
		}
		if (pending != null && pending.bodyStarted() && traveller instanceof ServerPlayer player) {
			BodyProxyManager.finish(player);
		}
		if (pending != null) pending.transaction().fail(server);
	}

	private static void scheduleStormFinish(MinecraftServer server, UUID ownerId,
			ResourceKey<Level> dimension, int delay) {
		PowersMod.scheduleDelayed(server, Math.max(1, delay), ownerId, dimension, ownerId,
				"teleport_storm_finish", (current, task) -> ACTIVE_STORMS.finish(task.subjectId()));
	}

	/** Lifecycle cleanup for disconnects and server shutdown. */
	public static void clearStorm(MinecraftServer server, UUID owner) {
		ACTIVE_STORMS.finish(owner);
		PendingTeleport pending = PENDING_TELEPORTS.remove(owner);
		if (pending != null) pending.transaction().fail(server);
	}

	public static void clearAllStorms(MinecraftServer server) {
		for (PendingTeleport pending : new ArrayList<>(PENDING_TELEPORTS.values())) {
			pending.transaction().fail(server);
		}
		PENDING_TELEPORTS.clear();
		ACTIVE_STORMS.clear();
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && POWER_ID.equals(power.id())) return true;
		}
		return false;
	}

	private static boolean subjectMayContinue(LivingEntity subject) {
		return subject.isAlive() && !subject.isRemoved()
				&& (!(subject instanceof ServerPlayer player) || MagicUseGate.ongoingAllowed(player));
	}

	// Preserve the destination realm signature so departure telegraphs the impending arrival.
	private static PowersMod.StormTheme themeFor(ResourceKey<Level> dimension) {
		if (SkillSystem.isDarkRealm(dimension)) return PowersMod.StormTheme.DARK;
		if (dimension.identifier().equals(PowersMod.id("light_realm"))) return PowersMod.StormTheme.LIGHT;
		return PowersMod.StormTheme.NONE;
	}
}
