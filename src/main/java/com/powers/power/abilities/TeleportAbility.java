package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.GodlyPunishment;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.progression.PowerScalingService;
import com.powers.power.Ability;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.protection.PowerProtection;
import com.powers.power.travel.DestinationFailure;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

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
	// how long the storm visuals play out at both ends
	private static final int STORM_TICKS = 100;
	// the pause between activating and the actual blink, so the storm can build
	private static final int TELEPORT_DELAY_TICKS = 50;
	// any entity within this distance of the caster gets dragged along
	private static final double COMPANION_RADIUS = 1.3;
	// 10 seconds to pick a spot before the marking expires and you're pulled back
	private static final int MARK_TIMEOUT_TICKS = 200;

	// per-player marking state keyed by uuid; cleared on disconnect and server stop so it can't leak
	public static final Map<UUID, MarkingState> MARKING = new HashMap<>();

	public record MarkingState(ServerPlayer player, ResourceKey<Level> originalDimension,
			Vec3 originalPos, GameType originalMode, ResourceKey<Level> markingDimension,
			Vec3 markingCenter, long deadline, int slot) {}

	private record Companion(Entity entity, Vec3 offset) {}

	public TeleportAbility() {
		super(PowersMod.id("time_shift"),
				Component.translatable("ability.powers.time_shift"),
				400, true);
	}

	public static boolean startMarking(ServerPlayer player, ServerPlayer target, int slot) {
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
				targetLevel.dimension(), target.position(),
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
		player.teleport(new TeleportTransition(level, safe, Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		player.setGameMode(state.originalMode());
		BodyProxyManager.finish(player);
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
			if (server.getTickCount() >= state.deadline()) {
				// timeout hit - pull the player back to the dimension and spot where they started
				restore(state.player(), state);
				PowerMessages.overlay(state.player(),
						PowerMessages.random("ability.powers.marking_expired", 3));
				it.remove();
			}
		}
	}

	@Override
	public boolean activateTeleport(ServerPlayer caster, ServerPlayer player, PlayerPowers.PlayerPowersData data,
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
		return TravelChunkLoader.request(caster.getUUID(), targetLevel, BlockPos.containing(target),
				() -> beginTeleport(caster, player, dimension, originLevel, targetLevel, target, transaction),
				() -> {
					transaction.fail();
					reportTravelFailure(caster, player, target, DestinationFailure.UNLOADED_CHUNK);
				});
	}

	private void beginTeleport(ServerPlayer caster, ServerPlayer player, ResourceKey<Level> dimension,
			ServerLevel originLevel, ServerLevel targetLevel, Vec3 target,
			AsyncAbilityTransaction transaction) {
		if (!player.isAlive() || player.level() != originLevel) {
			transaction.fail();
			return;
		}
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validate(
				player, targetLevel, target, TravelKind.POWER);
		if (!destination.allowed()) {
			transaction.fail();
			reportTravelFailure(caster, player, target, destination.failure());
			return;
		}
		MinecraftServer server = originLevel.getServer();
		Vec3 origin = player.position();
		double companionRadius = scaledRange(caster, COMPANION_RADIUS);
		int stormTicks = scaledDuration(caster, STORM_TICKS);
		int teleportDelay = Math.max(20,
				(int) Math.round(TELEPORT_DELAY_TICKS / Math.min(1.5, scaling(caster).rangeMultiplier())));

		List<Companion> companions = new ArrayList<>();
		// bring along everything alive within 1.3 blocks, remembering each one's offset from you
		for (Entity entity : com.powers.util.BoundedEntityCandidates.collect(originLevel,
				EntityTypeTest.forClass(Entity.class), player.getBoundingBox().inflate(companionRadius), 64,
				e -> e.isAlive() && e != player)) {
			if (entity instanceof ServerPlayer companionPlayer
					&& !PowerProtection.mayBringCompanion(player, companionPlayer)) continue;
			companions.add(new Companion(entity, entity.position().subtract(origin)));
		}

		PowerFx.rune(originLevel, origin, 2.0, 0x8AE8FF, 24, 0.0);
		PowerFx.rune(targetLevel, target, 2.0, 0x8AE8FF, 24, Math.PI * 0.5);
		PowerFx.sound(originLevel, origin, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.0f);
		PowerFx.sound(targetLevel, target, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.15f);
		// the blink itself is delayed so the storm can build up at both ends;
		// the lightning beneath the traveler echoes the realm they're bound for
		PowersMod.startStorm(originLevel, origin, player, stormTicks, teleportDelay, themeFor(dimension));
		PowersMod.startStorm(targetLevel, target, null, stormTicks, 0);
		PowersMod.scheduleDelayed(server, teleportDelay, () -> {
			// the player may have died during the storm - never teleport a corpse
			if (!player.isAlive()) {
				transaction.fail();
				return;
			}
			SafeDestinationResolver.Result revalidated = SafeDestinationResolver.validate(
					player, targetLevel, target, TravelKind.POWER);
			if (!revalidated.allowed()) {
				transaction.fail();
				reportTravelFailure(caster, player, target, revalidated.failure());
				return;
			}
			player.teleport(new TeleportTransition(targetLevel, target, Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			transaction.succeed();
			for (Companion companion : companions) {
				Entity entity = companion.entity();
				// companions that died or despawned during the storm stay behind
				if (entity.isRemoved()) continue;
				Vec3 dest = target.add(companion.offset());
				BlockPos destPos = BlockPos.containing(dest);
				if (!LoadedChunks.contains(targetLevel, destPos)) continue;
				if (entity instanceof ServerPlayer companionPlayer) {
					SafeDestinationResolver.Result companionDestination = SafeDestinationResolver.validate(
							companionPlayer, targetLevel, dest, TravelKind.COMPANION);
					if (!companionDestination.allowed()) continue;
				}
				AABB landingBox = entity.getBoundingBox().move(dest.subtract(entity.position()));
				// skip companions whose whole collision box got blocked - they stay where they are
				if (!targetLevel.getWorldBorder().isWithinBounds(landingBox)
						|| !targetLevel.noBlockCollision(entity, landingBox)) continue;
				entity.teleport(new TeleportTransition(targetLevel, dest, Vec3.ZERO,
						entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			}
		});
	}

	private static void reportTravelFailure(ServerPlayer caster, ServerPlayer subject,
			Vec3 target, DestinationFailure failure) {
		ServerLevel origin = (ServerLevel) subject.level();
		switch (failure) {
			case ANCHOR -> {
				GodlyPunishment.chainBlock(origin, subject);
				PowerMessages.send(caster, "ability.powers.anchored_teleport_blocked", 4);
			}
			case WARD -> {
				PowerFx.clash(origin, subject.position().add(0, 1, 0), target.add(0, 1, 0),
						0xFFD4FF, 0xB36BFF);
				subject.hurtServer(origin, subject.damageSources().magic(), 20.0f);
				GodlyPunishment.strike(origin, subject, 0xB36BFF, false);
				PowerMessages.send(caster, "amethyst.powers.teleport_repelled", 5);
			}
			case REALM_RESTRICTED -> {
				GodlyPunishment.barrier(origin, subject, 0x82CAFF);
				PowerMessages.send(caster, "ability.powers.no_entry", 4);
			}
			case OUT_OF_BOUNDS, UNLOADED_CHUNK -> PowerMessages.send(caster, "ability.powers.out_of_bounds", 3);
			case SAFE_ZONE -> PowerMessages.send(caster, "ability.powers.no_entry", 4);
			case COLLISION, HAZARD -> PowerMessages.send(caster, "ability.powers.solid_block", 3);
			case NONE -> { }
		}
	}

	// which realm's signature the departing lightning should build up
	private static PowersMod.StormTheme themeFor(ResourceKey<Level> dimension) {
		if (SkillSystem.isDarkRealm(dimension)) return PowersMod.StormTheme.DARK;
		if (dimension.identifier().equals(PowersMod.id("light_realm"))) return PowersMod.StormTheme.LIGHT;
		return PowersMod.StormTheme.NONE;
	}
}
