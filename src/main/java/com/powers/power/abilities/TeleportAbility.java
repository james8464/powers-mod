package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.GodlyPunishment;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.*;

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
			Vec3 originalPos, GameType originalMode, long deadline, int slot) {}

	private record Companion(Entity entity, Vec3 offset) {}

	public TeleportAbility() {
		super(PowersMod.id("time_shift"),
				Component.translatable("ability.powers.time_shift"),
				400, true);
	}

	public static void startMarking(ServerPlayer player, ServerPlayer target, int slot) {
		// remember the original dimension, spot and game mode so the marking can always be undone
		MARKING.put(player.getUUID(), new MarkingState(
				player, player.level().dimension(), player.position(), player.gameMode(),
				((ServerLevel) player.level()).getServer().getTickCount() + MARK_TIMEOUT_TICKS, slot));
		// spectator so you can fly to the landing spot without fighting
		player.setGameMode(GameType.SPECTATOR);
		PowerFx.rune((ServerLevel) target.level(), target.position().add(0, 2, 0), 1.5, 0x88CCFF, 20, 0.6);
		PowerFx.sound((ServerLevel) target.level(), target.position(), SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.2f);
		player.teleport(new TeleportTransition((ServerLevel) target.level(),
				target.position().add(0, 2, 0), Vec3.ZERO, player.getYRot(), player.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND));
		PowerMessages.send(player, "ability.powers.marking_mode", 3);
	}

	/** Completes the marking teleport to the coordinates picked in spectator mode, restoring your game mode. */
	public static void completeMarking(ServerPlayer player, int slot, Vec3 pos) {
		MarkingState state = MARKING.remove(player.getUUID());
		// no active marking, or the packet came from another slot - ignore it
		if (state == null || state.slot() != slot) return;
		// a corrupted packet could carry NaN and break the teleport, bail out and stay in place
		if (!Double.isFinite(pos.x()) || !Double.isFinite(pos.y()) || !Double.isFinite(pos.z())) {
			player.setGameMode(state.originalMode());
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		Vec3 safe = findSafeMarkSpot(level, pos);
		if (safe == null) {
			// the marked spot is solid - restore the game mode and tell the player
			PowerMessages.send(player, "ability.powers.solid_block", 3);
			player.setGameMode(state.originalMode());
			return;
		}
		player.teleport(new TeleportTransition(level, safe, Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		player.setGameMode(state.originalMode());
	}

	/** Finds the first open spot at or above the marked position, since spectators can fly into walls. */
	private static Vec3 findSafeMarkSpot(ServerLevel level, Vec3 pos) {
		// only check up to 3 blocks up - anything higher than that wasn't really the spot you picked
		for (int dy = 0; dy <= 3; dy++) {
			Vec3 candidate = new Vec3(pos.x, pos.y + dy, pos.z);
			BlockState feet = level.getBlockState(new BlockPos(
					(int) Math.floor(candidate.x), (int) Math.floor(candidate.y), (int) Math.floor(candidate.z)));
			BlockState head = level.getBlockState(new BlockPos(
					(int) Math.floor(candidate.x), (int) Math.floor(candidate.y) + 1, (int) Math.floor(candidate.z)));
			// both the feet and head blocks must be clear so you don't materialize inside a wall
			if (!feet.isSolid() && !head.isSolid()) {
				return candidate;
			}
		}
		return null;
	}

	// called on disconnect - never leave a stale marking behind for a logged-off player
	public static void clearMarking(ServerPlayer player) {
		MARKING.remove(player.getUUID());
	}

	// called on server stop so the map can't leak across restarts
	public static void clearAllMarking() {
		MARKING.clear();
	}

	public static void tickMarking() {
		var it = MARKING.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			MarkingState state = entry.getValue();
			long now = ((ServerLevel) state.player().level()).getServer().getTickCount();
			if (now >= state.deadline()) {
				// timeout hit - pull the player back to the dimension and spot where they started
				ServerLevel originalLevel = ((ServerLevel) state.player().level()).getServer().getLevel(state.originalDimension());
				if (originalLevel != null) {
					state.player().teleport(new TeleportTransition(originalLevel,
						state.originalPos(), Vec3.ZERO, state.player().getYRot(), state.player().getXRot(),
						TeleportTransition.PLAY_PORTAL_SOUND));
				}
				state.player().setGameMode(state.originalMode());
				state.player().sendSystemMessage(PowerMessages.random("ability.powers.marking_expired", 3));
				it.remove();
			}
		}
	}

	@Override
	public boolean activateTeleport(ServerPlayer caster, ServerPlayer player, PlayerPowers.PlayerPowersData data,
			ResourceKey<Level> dimension, double x, double y, double z) {
		// refuse out-of-range or NaN coordinates before anything else
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
			PowerMessages.send(player, "ability.powers.out_of_bounds", 3);
			return false;
		}
		// a dimensional anchor pins you to its dimension - teleports elsewhere are punished
		if (DimensionalAnchorAbility.isAnchored(player)) {
			ResourceKey<Level> anchor = DimensionalAnchorAbility.anchorDimension(player);
			if (!dimension.equals(anchor)) {
				GodlyPunishment.chainBlock((ServerLevel) player.level(), player);
				PowerMessages.send(player, "ability.powers.anchored_teleport_blocked", 4);
				return false;
			}
		}

		// the middleworld is off-limits to teleporters
		if (dimension.identifier().getPath().equals("middleworld")) {
			GodlyPunishment.barrier((ServerLevel) player.level(), player, 0x82CAFF);
			PowerMessages.send(player, "ability.powers.no_entry", 4);
			return false;
		}

		ServerLevel targetLevel = player.level().getServer().getLevel(dimension);
		if (targetLevel == null) {
			// the dimension isn't loaded on this server
			PowerMessages.send(player, "ability.powers.bad_dimension", 3);
			return false;
		}
		boolean enteringDarkRealm = SkillSystem.isDarkRealm(dimension);
		boolean leavingDarkRealm = SkillSystem.isDarkRealm(player.level().dimension());
		// entering the dark realm needs the darkness mark at rank 5+; the
		// dark crystal and riding along as a companion are the only bypasses.
		// leaving is always free - nobody who gets in is ever trapped
		if (enteringDarkRealm && !leavingDarkRealm) {
			if (!SkillSystem.canEnterDarkRealm(player)) {
				GodlyPunishment.voidReject((ServerLevel) caster.level(), caster);
				PowerMessages.send(caster, "ability.powers.darkness_realm_restricted", 5);
				return false;
			}
		}
		// an amethyst ward at the landing spot repels the teleport and blasts the caller
		if (AmethystDampening.findPoweredWard(targetLevel, BlockPos.containing(x, y, z)).isPresent()) {
			ServerLevel originLevel = (ServerLevel) player.level();
			PowerFx.clash(originLevel, player.position().add(0, 1, 0),
					new Vec3(x + 0.5, y + 1, z + 0.5), 0xFFD4FF, 0xB36BFF);
			// 20 points of magic damage plus a divine strike for touching a ward
			player.hurtServer(originLevel, player.damageSources().magic(), 20.0f);
			GodlyPunishment.strike(originLevel, player, 0xB36BFF, false);
			PowerMessages.send(player, "amethyst.powers.teleport_repelled", 5);
			return false;
		}

		// keep the destination inside the dimension's build height and the world border
		int minY = targetLevel.getMinY();
		int maxY = targetLevel.getMaxY();
		if (y < minY || y > maxY || x < -30_000_000 || x > 30_000_000 || z < -30_000_000 || z > 30_000_000) {
			PowerMessages.send(player, "ability.powers.out_of_bounds", 3);
			return false;
		}

		// both the feet and head blocks must be clear or you'd materialize inside a wall
		BlockState feetBlock = targetLevel.getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
		BlockState headBlock = targetLevel.getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y) + 1, (int) Math.floor(z)));
		if (feetBlock.isSolid() || headBlock.isSolid()) {
			PowerMessages.send(player, "ability.powers.solid_block", 3);
			return false;
		}

		ServerLevel originLevel = (ServerLevel) player.level();
		Vec3 target = new Vec3(x + 0.5, y, z + 0.5);
		// also sweep the full hitbox at the landing spot for collisions, not just the feet and head blocks
		if (!targetLevel.noCollision(player, player.getBoundingBox().move(target.subtract(player.position())))) {
			PowerMessages.send(player, "ability.powers.solid_block", 3);
			return false;
		}
		Vec3 origin = player.position();

		List<Companion> companions = new ArrayList<>();
		// bring along everything alive within 1.3 blocks, remembering each one's offset from you
		for (Entity entity : originLevel.getEntities(
				EntityTypeTest.forClass(Entity.class), player.getBoundingBox().inflate(COMPANION_RADIUS),
				e -> e.isAlive() && e != player)) {
			companions.add(new Companion(entity, entity.position().subtract(origin)));
		}

		PowerFx.rune(originLevel, origin, 2.0, 0x8AE8FF, 24, 0.0);
		PowerFx.rune(targetLevel, target, 2.0, 0x8AE8FF, 24, Math.PI * 0.5);
		PowerFx.sound(originLevel, origin, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.0f);
		PowerFx.sound(targetLevel, target, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.15f);
		// the blink itself is delayed so the storm can build up at both ends;
		// the lightning beneath the traveler echoes the realm they're bound for
		PowersMod.startStorm(originLevel, origin, player, STORM_TICKS, TELEPORT_DELAY_TICKS, themeFor(dimension));
		PowersMod.startStorm(targetLevel, target, null, STORM_TICKS, 0);
		PowersMod.scheduleDelayed(player.level().getServer(), TELEPORT_DELAY_TICKS, () -> {
			// the player may have died during the storm - never teleport a corpse
			if (!player.isAlive()) return;
			player.teleport(new TeleportTransition(targetLevel, target, Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			for (Companion companion : companions) {
				Entity entity = companion.entity();
				// companions that died or despawned during the storm stay behind
				if (entity.isRemoved()) continue;
				Vec3 dest = target.add(companion.offset());
				BlockState feet = targetLevel.getBlockState(new BlockPos(
						(int) Math.floor(dest.x), (int) Math.floor(dest.y), (int) Math.floor(dest.z)));
				BlockState head = targetLevel.getBlockState(new BlockPos(
						(int) Math.floor(dest.x), (int) Math.floor(dest.y) + 1, (int) Math.floor(dest.z)));
				// skip companions whose landing spot got blocked - they stay where they are
				if (feet.isSolid() || head.isSolid()) continue;
			entity.teleport(new TeleportTransition(targetLevel, dest, Vec3.ZERO,
					entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			}
		});
		return true;
	}

	// which realm's signature the departing lightning should build up
	private static PowersMod.StormTheme themeFor(ResourceKey<Level> dimension) {
		if (SkillSystem.isDarkRealm(dimension)) return PowersMod.StormTheme.DARK;
		if (dimension.identifier().equals(PowersMod.id("light_realm"))) return PowersMod.StormTheme.LIGHT;
		return PowersMod.StormTheme.NONE;
	}
}
