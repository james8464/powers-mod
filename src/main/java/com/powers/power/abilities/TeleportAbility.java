package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.GodlyPunishment;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.progression.PowerScalingService;
import com.powers.power.Ability;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.protection.PowerProtection;
import com.powers.power.travel.DestinationFailure;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
import com.powers.power.travel.TeleportStormTracker;
import com.powers.util.PowerMessages;
import com.powers.util.LoadedChunks;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * time shift - mark a target spot or player, then blink there after a short
 * storm while riding along with anything close by; marking puts you in
 * spectator mode so you can scout the landing zone
 */
public class TeleportAbility extends Ability {
	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("time_shift");
	// how long the storm visuals play out at both ends
	private static final int STORM_TICKS = 100;
	// the pause between activating and the actual blink, so the storm can build
	private static final int TELEPORT_DELAY_TICKS = 50;
	// any entity within this distance of the caster gets dragged along
	private static final double COMPANION_RADIUS = 1.3;
	// 10 seconds to pick a spot before the marking expires and you're pulled back
	private static final int MARK_TIMEOUT_TICKS = 200;

	// per-player marking state keyed by uuid; cleared on disconnect and server stop so it can't leak
	private static final Map<UUID, MarkingState> MARKING = new HashMap<>();
	private static final TeleportStormTracker ACTIVE_STORMS = new TeleportStormTracker();

	public record MarkingState(ServerPlayer player, ResourceKey<Level> originalDimension,
			Vec3 originalPos, GameType originalMode, ResourceKey<Level> markingDimension,
			Vec3 markingCenter, CastSource castSource, long deadline, int slot) {}

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
			reportTravelFailure(player, player, entry, destination.failure());
			return false;
		}
		if (!BodyProxyManager.start(player, BodyProxyKind.MARKING)) return false;
		// remember the original dimension, spot and game mode so the marking can always be undone
		MARKING.put(player.getUUID(), new MarkingState(
				player, player.level().dimension(), player.position(), player.gameMode(),
				targetLevel.dimension(), target.position(), CastScalingContext.currentSource(),
				((ServerLevel) player.level()).getServer().getTickCount()
						+ PowerScalingService.duration(player, "time_shift", MARK_TIMEOUT_TICKS), slot));
		// spectator so you can fly to the landing spot without fighting
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
		// no active marking, or the packet came from another slot - ignore it
		if (state == null || state.slot() != slot) return;
		MARKING.remove(player.getUUID());
		if (!MagicUseGate.ongoingAllowed(player) || !ServerCastLifecycle.mayContinue(
				player, state.castSource(), ownsPower(player))) {
			restore(player, state);
			return;
		}
		// a corrupted packet could carry NaN and break the teleport, bail out and stay in place
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
		Vec3 safe = findSafeMarkSpot(level, pos);
		if (safe == null) {
			// the marked spot is solid - restore the game mode and tell the player
			PowerMessages.send(player, "ability.powers.solid_block", 3);
			restore(player, state);
			return;
		}
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validate(
				player, level, safe, TravelKind.POWER);
		if (!destination.allowed()) {
			reportTravelFailure(player, player, safe, destination.failure());
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
		PowersMod.startStorm(level, safe, STORM_TICKS);
		PowerFx.rune(originalLevel, state.originalPos(), 2.0, 0x8AE8FF, 24, 0.0);
		PowerFx.rune(level, safe, 2.0, 0x8AE8FF, 24, Math.PI * 0.5);
		UUID ownerId = player.getUUID();
		PowersMod.scheduleDelayed(server, TELEPORT_DELAY_TICKS, () -> {
			ServerPlayer current = server.getPlayerList().getPlayer(ownerId);
			if (current == null || !BodyProxyManager.hasSession(current, BodyProxyKind.MARKING)
					|| current.level() != level || !MagicUseGate.ongoingAllowed(current)
					|| !ServerCastLifecycle.mayContinue(current, state.castSource(), ownsPower(current))
					|| !SafeDestinationResolver.validate(current, level, safe, TravelKind.POWER).allowed()) {
				ACTIVE_STORMS.finish(ownerId);
				if (current != null) restore(current, state);
				return;
			}
			double companionRadius = PowerScalingService.range(current, "time_shift", COMPANION_RADIUS);
			List<TeleportCompanionMover.Candidate> companions = TeleportCompanionMover.collect(
					originalLevel, current, current,
					state.originalPos(), companionRadius);
			current.teleport(new TeleportTransition(level, safe, Vec3.ZERO,
					current.getYRot(), current.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			current.setGameMode(state.originalMode());
			BodyProxyManager.finish(current);
			TeleportCompanionMover.move(originalLevel, level, state.originalPos(), safe,
					companionRadius, companions);
			PowersMod.scheduleDelayed(server, STORM_TICKS - TELEPORT_DELAY_TICKS,
					() -> ACTIVE_STORMS.finish(ownerId));
		});
	}

	/** Finds the first open spot at or above the marked position, since spectators can fly into walls. */
	private static Vec3 findSafeMarkSpot(ServerLevel level, Vec3 pos) {
		// only check up to 3 blocks up - anything higher than that wasn't really the spot you picked
		for (int dy = 0; dy <= 3; dy++) {
			Vec3 candidate = new Vec3(pos.x, pos.y + dy, pos.z);
			BlockPos feetPos = BlockPos.containing(candidate);
			if (!LoadedChunks.contains(level, feetPos)) continue;
			// both the feet and head blocks must be clear so you don't materialize inside a wall
			if (level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty()
					&& level.getBlockState(feetPos.above()).getCollisionShape(level, feetPos.above()).isEmpty()) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Called on disconnect. Dropping the state on its own would strand the
	 * player: marking puts them in spectator at the target's feet, so logging
	 * out mid-mark used to mean coming back a permanent spectator. Undo the
	 * marking first, then forget it.
	 */
	public static void clearMarking(ServerPlayer player) {
		MarkingState state = MARKING.remove(player.getUUID());
		if (state != null) {
			restore(player, state);
		}
	}

	// called on server stop - unwind every open marking so nobody is saved out
	// as a spectator, then clear the map so it can't leak across restarts
	public static void clearAllMarking() {
		for (MarkingState state : new ArrayList<>(MARKING.values())) {
			restore(state.player(), state);
		}
		MARKING.clear();
	}

	public static boolean isMarking(UUID owner) {
		return MARKING.containsKey(owner);
	}

	// puts a marking player back where they started, in the mode they started in
	private static void restore(ServerPlayer player, MarkingState state) {
		if (BodyProxyManager.hasSession(player, BodyProxyKind.MARKING)
				&& BodyProxyManager.returnToBody(player)) return;
		MinecraftServer server = player.level().getServer();
		ServerLevel originalLevel = server == null ? null : server.getLevel(state.originalDimension());
		if (originalLevel != null) {
			player.teleportTo(originalLevel, state.originalPos().x, state.originalPos().y, state.originalPos().z,
					Set.of(), player.getYRot(), player.getXRot(), false);
		}
		player.setGameMode(state.originalMode());
		BodyProxyManager.finish(player);
	}

	public static void tickMarking() {
		var it = MARKING.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			MarkingState state = entry.getValue();
			MinecraftServer server = state.player().level().getServer();
			if (server == null) {
				it.remove();
				continue;
			}
			boolean expired = server.getTickCount() >= state.deadline();
			boolean interrupted = !MagicUseGate.ongoingAllowed(state.player())
					|| !ServerCastLifecycle.mayContinue(
							state.player(), state.castSource(), ownsPower(state.player()));
			if (expired || interrupted) {
				// timeout hit - pull the player back to the dimension and spot where they started
				restore(state.player(), state);
				PowerMessages.overlay(state.player(), expired
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
			// the dimension isn't loaded on this server
			PowerMessages.send(caster, "ability.powers.bad_dimension", 3);
			return false;
		}
		ServerLevel originLevel = (ServerLevel) player.level();
		Vec3 target = new Vec3(x + 0.5, y, z + 0.5);
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validatePreload(
				player, targetLevel, target, TravelKind.POWER);
		if (!destination.allowed()) {
			reportTravelFailure(caster, player, target, destination.failure());
			return false;
		}
		AsyncAbilityTransaction transaction = new AsyncAbilityTransaction(caster, data, this);
		CastSource castSource = CastScalingContext.currentSource();
		double companionRadius = scaledRange(caster, COMPANION_RADIUS);
		if (!ACTIVE_STORMS.begin(caster.getUUID())) {
			PowerMessages.overlay(caster, Component.translatable("ability.powers.teleport_storm_active"));
			return false;
		}
		boolean accepted = TravelChunkLoader.request(caster.getUUID(), targetLevel, BlockPos.containing(target),
				() -> beginTeleport(caster, player, dimension, originLevel, targetLevel, target,
						castSource, companionRadius, STORM_TICKS, TELEPORT_DELAY_TICKS, transaction),
				() -> {
					ACTIVE_STORMS.finish(caster.getUUID());
					transaction.fail();
					reportTravelFailure(caster, player, target, DestinationFailure.UNLOADED_CHUNK);
				});
		if (!accepted) ACTIVE_STORMS.finish(caster.getUUID());
		return accepted;
	}

	private void beginTeleport(ServerPlayer caster, LivingEntity player, ResourceKey<Level> dimension,
			ServerLevel originLevel, ServerLevel targetLevel, Vec3 target, CastSource castSource,
			double companionRadius, int stormTicks, int teleportDelay,
			AsyncAbilityTransaction transaction) {
		if (!MagicUseGate.ongoingAllowed(caster) || !subjectMayContinue(player)
				|| !ServerCastLifecycle.mayContinue(caster, castSource, ownsPower(caster))
				|| !player.isAlive() || player.level() != originLevel) {
			abortStorm(caster, player, transaction, false);
			return;
		}
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validate(
				player, targetLevel, target, TravelKind.POWER);
		if (!destination.allowed()) {
			abortStorm(caster, player, transaction, false);
			reportTravelFailure(caster, player, target, destination.failure());
			return;
		}
		boolean bodyStarted = player instanceof ServerPlayer subject
				&& BodyProxyManager.start(subject, BodyProxyKind.MARKING);
		if (player instanceof ServerPlayer && !bodyStarted) {
			abortStorm(caster, player, transaction, false);
			return;
		}
		MinecraftServer server = originLevel.getServer();
		Vec3 origin = player.position();
		PowerFx.rune(originLevel, origin, 2.0, 0x8AE8FF, 24, 0.0);
		PowerFx.rune(targetLevel, target, 2.0, 0x8AE8FF, 24, Math.PI * 0.5);
		PowerFx.sound(originLevel, origin, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.0f);
		PowerFx.sound(targetLevel, target, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.15f);
		// the blink itself is delayed so the storm can build up at both ends;
		// the lightning beneath the traveler echoes the realm they're bound for
		PowersMod.startStorm(originLevel, origin,
				player instanceof ServerPlayer serverPlayer ? serverPlayer : null,
				stormTicks, teleportDelay, themeFor(dimension));
		PowersMod.startStorm(targetLevel, target, null, stormTicks, 0);
		PowersMod.scheduleDelayed(server, teleportDelay, () -> {
			ServerPlayer currentCaster = server.getPlayerList().getPlayer(caster.getUUID());
			if (player instanceof ServerPlayer subjectPlayer) AmethystDampening.update(subjectPlayer);
			if (currentCaster != null && currentCaster != player) AmethystDampening.update(currentCaster);
			if (!DelayedTravelRules.travellerMayContinue(currentCaster == caster,
					caster.isAlive() && !caster.isRemoved(), player.isAlive() && !player.isRemoved(),
					caster.level() == originLevel, player.level() == originLevel,
					AmethystDampening.isDampened(caster), AmethystDampening.isDampened(player))) {
				abortStorm(caster, player, transaction, bodyStarted);
				return;
			}
			if (!MagicUseGate.ongoingAllowed(caster) || !subjectMayContinue(player)
					|| !ServerCastLifecycle.mayContinue(caster, castSource, ownsPower(caster))) {
				abortStorm(caster, player, transaction, bodyStarted);
				return;
			}
			SafeDestinationResolver.Result revalidated = SafeDestinationResolver.validate(
					player, targetLevel, target, TravelKind.POWER);
			if (!revalidated.allowed()) {
				abortStorm(caster, player, transaction, bodyStarted);
				reportTravelFailure(caster, player, target, revalidated.failure());
				return;
			}
			Vec3 departure = player.position();
			List<TeleportCompanionMover.Candidate> companions = TeleportCompanionMover.collect(
					originLevel, caster, player,
					departure, companionRadius);
			player.teleport(new TeleportTransition(targetLevel, target, Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			if (player.level() != targetLevel) {
				abortStorm(caster, player, transaction, bodyStarted);
				return;
			}
			if (bodyStarted && player instanceof ServerPlayer subject) BodyProxyManager.finish(subject);
			transaction.succeed();
			TeleportCompanionMover.move(originLevel, targetLevel, departure, target,
					companionRadius, companions);
			PowersMod.scheduleDelayed(server, Math.max(1, stormTicks - teleportDelay),
					() -> ACTIVE_STORMS.finish(caster.getUUID()));
		});
	}

	private static void abortStorm(ServerPlayer caster, LivingEntity traveller,
			AsyncAbilityTransaction transaction, boolean bodyStarted) {
		ACTIVE_STORMS.finish(caster.getUUID());
		if (bodyStarted && traveller instanceof ServerPlayer player) BodyProxyManager.finish(player);
		transaction.fail();
	}

	/** Lifecycle cleanup for disconnects and server shutdown. */
	public static void clearStorm(UUID owner) {
		ACTIVE_STORMS.finish(owner);
	}

	public static void clearAllStorms() {
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

	private static void reportTravelFailure(ServerPlayer caster, LivingEntity subject,
			Vec3 target, DestinationFailure failure) {
		ServerLevel origin = (ServerLevel) subject.level();
		switch (failure) {
			case ANCHOR -> {
				if (subject instanceof ServerPlayer player) GodlyPunishment.chainBlock(origin, player);
				else PowerFx.rune(origin, subject.position().add(0, 1, 0), 1.8, 0xB36BFF, 24, 0.0);
				PowerMessages.send(caster, "ability.powers.anchored_teleport_blocked", 4);
			}
			case WARD -> {
				PowerFx.clash(origin, subject.position().add(0, 1, 0), target.add(0, 1, 0),
						0xFFD4FF, 0xB36BFF);
				subject.hurtServer(origin, subject.damageSources().magic(), 20.0f);
				if (subject instanceof ServerPlayer player) GodlyPunishment.strike(origin, player, 0xB36BFF, false);
				PowerMessages.send(caster, "amethyst.powers.teleport_repelled", 5);
			}
			case REALM_RESTRICTED -> {
				if (subject instanceof ServerPlayer player) GodlyPunishment.barrier(origin, player, 0x82CAFF);
				else PowerFx.rune(origin, subject.position().add(0, 1, 0), 2.0, 0x82CAFF, 24, 0.0);
				PowerMessages.send(caster, "ability.powers.no_entry", 4);
			}
			case OUT_OF_BOUNDS, UNLOADED_CHUNK -> PowerMessages.send(caster, "ability.powers.out_of_bounds", 3);
			case SAFE_ZONE -> PowerMessages.send(caster, "ability.powers.no_entry", 4);
			case COLLISION, HAZARD -> PowerMessages.send(caster, "ability.powers.solid_block", 3);
			case NONE -> { }
		}
	}

	private static boolean subjectMayContinue(LivingEntity subject) {
		return subject.isAlive() && !subject.isRemoved()
				&& (!(subject instanceof ServerPlayer player) || MagicUseGate.ongoingAllowed(player));
	}

	// which realm's signature the departing lightning should build up
	private static PowersMod.StormTheme themeFor(ResourceKey<Level> dimension) {
		if (SkillSystem.isDarkRealm(dimension)) return PowersMod.StormTheme.DARK;
		if (dimension.identifier().equals(PowersMod.id("light_realm"))) return PowersMod.StormTheme.LIGHT;
		return PowersMod.StormTheme.NONE;
	}
}
