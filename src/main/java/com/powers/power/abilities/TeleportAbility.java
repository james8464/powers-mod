package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TeleportAbility extends Ability {
	private static final int STORM_TICKS = 100;
	private static final int TELEPORT_DELAY_TICKS = 50;
	private static final double COMPANION_RADIUS = 1.3;
	private static final int MARK_TIMEOUT_TICKS = 200;

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
		MARKING.put(player.getUUID(), new MarkingState(
				player, player.level().dimension(), player.position(), player.gameMode(),
				((ServerLevel) player.level()).getServer().getTickCount() + MARK_TIMEOUT_TICKS, slot));
		player.setGameMode(GameType.SPECTATOR);
		player.teleport(new TeleportTransition((ServerLevel) target.level(),
				target.position().add(0, 2, 0), Vec3.ZERO, player.getYRot(), player.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND));
		player.sendSystemMessage(Component.translatable("ability.powers.marking_mode"));
	}

	public static void completeMarking(ServerPlayer player, int slot, Vec3 pos) {
		MarkingState state = MARKING.remove(player.getUUID());
		if (state == null || state.slot() != slot || !Double.isFinite(pos.x())
				|| !Double.isFinite(pos.y()) || !Double.isFinite(pos.z())) return;
		player.teleport(new TeleportTransition((ServerLevel) player.level(),
				pos, Vec3.ZERO, player.getYRot(), player.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND));
		player.setGameMode(state.originalMode());
	}

	public static void clearMarking(ServerPlayer player) {
		MARKING.remove(player.getUUID());
	}

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
				ServerLevel originalLevel = ((ServerLevel) state.player().level()).getServer().getLevel(state.originalDimension());
				if (originalLevel != null) {
					state.player().teleport(new TeleportTransition(originalLevel,
						state.originalPos(), Vec3.ZERO, state.player().getYRot(), state.player().getXRot(),
						TeleportTransition.PLAY_PORTAL_SOUND));
				}
				state.player().setGameMode(state.originalMode());
				state.player().sendSystemMessage(Component.translatable("ability.powers.marking_expired"));
				it.remove();
			}
		}
	}

	@Override
	public boolean activateTeleport(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			ResourceKey<Level> dimension, double x, double y, double z) {
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
			player.sendSystemMessage(Component.translatable("ability.powers.out_of_bounds"));
			return false;
		}
		if (DimensionalAnchorAbility.isAnchored(player)) {
			ResourceKey<Level> anchor = DimensionalAnchorAbility.anchorDimension(player);
			if (!dimension.equals(anchor)) {
				player.sendSystemMessage(Component.translatable("ability.powers.anchored_teleport_blocked"));
				return false;
			}
		}

		if (dimension.identifier().getPath().equals("middleworld")) {
			player.sendSystemMessage(Component.translatable("ability.powers.no_entry"));
			return false;
		}

		ServerLevel targetLevel = player.level().getServer().getLevel(dimension);
		if (targetLevel == null) {
			player.sendSystemMessage(Component.translatable("ability.powers.bad_dimension"));
			return false;
		}
		if (AmethystDampening.findPoweredWard(targetLevel, BlockPos.containing(x, y, z)).isPresent()) {
			com.powers.fx.PowerFx.clash((ServerLevel) player.level(), player.position().add(0, 1, 0),
					new Vec3(x + 0.5, y + 1, z + 0.5), 0xFFD4FF, 0xB36BFF);
			player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), 20.0f);
			player.sendSystemMessage(Component.translatable("amethyst.powers.teleport_repelled"));
			return false;
		}

		int minY = targetLevel.getMinY();
		int maxY = targetLevel.getMaxY();
		if (y < minY || y > maxY || x < -30_000_000 || x > 30_000_000 || z < -30_000_000 || z > 30_000_000) {
			player.sendSystemMessage(Component.translatable("ability.powers.out_of_bounds"));
			return false;
		}

		BlockState feetBlock = targetLevel.getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
		BlockState headBlock = targetLevel.getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y) + 1, (int) Math.floor(z)));
		if (feetBlock.isSolid() || headBlock.isSolid()) {
			player.sendSystemMessage(Component.translatable("ability.powers.solid_block"));
			return false;
		}

		ServerLevel originLevel = (ServerLevel) player.level();
		Vec3 target = new Vec3(x + 0.5, y, z + 0.5);
		if (!targetLevel.noCollision(player, player.getBoundingBox().move(target.subtract(player.position())))) {
			player.sendSystemMessage(Component.translatable("ability.powers.solid_block"));
			return false;
		}
		Vec3 origin = player.position();

		List<Companion> companions = new ArrayList<>();
		for (Entity entity : originLevel.getEntities(
				EntityTypeTest.forClass(Entity.class), player.getBoundingBox().inflate(COMPANION_RADIUS),
				e -> e.isAlive() && e != player)) {
			companions.add(new Companion(entity, entity.position().subtract(origin)));
		}

		PowersMod.startStorm(originLevel, origin, player, STORM_TICKS, TELEPORT_DELAY_TICKS);
		PowersMod.startStorm(targetLevel, target, null, STORM_TICKS, 0);
		PowersMod.scheduleDelayed(player.level().getServer(), TELEPORT_DELAY_TICKS, () -> {
			if (!player.isAlive()) return;
			player.teleport(new TeleportTransition(targetLevel, target, Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			for (Companion companion : companions) {
				Entity entity = companion.entity();
				if (entity.isRemoved()) continue;
				Vec3 dest = target.add(companion.offset());
				BlockState feet = targetLevel.getBlockState(new BlockPos(
						(int) Math.floor(dest.x), (int) Math.floor(dest.y), (int) Math.floor(dest.z)));
				BlockState head = targetLevel.getBlockState(new BlockPos(
						(int) Math.floor(dest.x), (int) Math.floor(dest.y) + 1, (int) Math.floor(dest.z)));
				if (feet.isSolid() || head.isSolid()) continue;
				entity.teleport(new TeleportTransition(targetLevel, dest, Vec3.ZERO,
						entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			}
		});
		return true;
	}
}
