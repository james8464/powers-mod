package com.powers.mind;

import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.PowerEntityState;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns vulnerable, skin-matched bodies left behind by mind and spirit travel. */
public final class BodyProxyManager {
	private record Active(ServerPlayer owner, Mannequin body, Vec3 position,
			ServerLevel level, ChunkPos chunk) {
	}
	private static final TicketType BODY_TICKET = new TicketType(TicketType.NO_TIMEOUT,
			TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

	private static final Map<UUID, Active> BY_OWNER = new HashMap<>();
	private static final Map<UUID, Active> BY_BODY = new HashMap<>();
	private static final EquipmentSlot[] VISIBLE_EQUIPMENT = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
	};

	private BodyProxyManager() {
	}

	public static boolean start(ServerPlayer player, BodyProxyKind kind) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (data.mindBody() != null || BY_OWNER.containsKey(player.getUUID())) return false;
		ServerLevel level = (ServerLevel) player.level();
		Mannequin body = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
		if (body == null) return false;

		body.setPos(player.getX(), player.getY(), player.getZ());
		body.setYRot(player.getYRot());
		body.setYHeadRot(player.getYHeadRot());
		body.setPose(player.getPose());
		body.setNoGravity(true);
		body.setSilent(true);
		body.setCustomName(player.getName().copy());
		body.setCustomNameVisible(false);
		body.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
		for (EquipmentSlot slot : VISIBLE_EQUIPMENT) {
			body.setItemSlot(slot, player.getItemBySlot(slot).copy());
		}
		PowerEntityState.markEphemeral(body);
		if (!level.addFreshEntity(body)) return false;

		MindBodyState state = new MindBodyState(level.dimension().identifier().toString(),
				player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
				player.gameMode().getName(), kind.serializedName());
		data.setMindBody(state);
		ChunkPos bodyChunk = ChunkPos.containing(player.blockPosition());
		level.getChunkSource().addTicketWithRadius(BODY_TICKET, bodyChunk, 1);
		Active active = new Active(player, body, player.position(), level, bodyChunk);
		BY_OWNER.put(player.getUUID(), active);
		BY_BODY.put(body.getUUID(), active);
		PowerFx.rune(level, player.position(), 1.4, 0xBCA7FF, 20, 0.0);
		return true;
	}

	public static boolean hasSession(ServerPlayer player, BodyProxyKind kind) {
		MindBodyState state = PlayerPowers.get(player).mindBody();
		return state != null && state.proxyKind() == kind;
	}

	public static boolean isProxy(LivingEntity entity) {
		return BY_BODY.containsKey(entity.getUUID());
	}

	public static boolean allowsDamage(LivingEntity entity, DamageSource source) {
		Active active = BY_BODY.get(entity.getUUID());
		if (active == null) return true;
		if (!PowersConfigLoader.get().projectionBodiesVulnerable()) return false;
		return !(PowerDamage.isPowerDamage(source) && AmethystDampening.isDampened(active.owner()));
	}

	public static boolean allowsDeath(LivingEntity entity) {
		Active active = BY_BODY.get(entity.getUUID());
		if (active == null) return true;
		active.body().setHealth(active.body().getMaxHealth());
		return false;
	}

	public static void afterDamage(LivingEntity entity, DamageSource source, float damageTaken) {
		Active active = BY_BODY.get(entity.getUUID());
		if (active == null || damageTaken <= 0.0f) return;
		ServerPlayer owner = active.owner();
		if (!owner.isAlive() || owner.isRemoved()) return;
		float health = owner.getHealth() - damageTaken;
		owner.setHealth(Math.max(0.0f, health));
		active.body().setHealth(active.body().getMaxHealth());
		PowerFx.coloredBurst((ServerLevel) active.body().level(), active.position().add(0, 1, 0),
				0xBCA7FF, 8, 0.45);
		if (health <= 0.0f) owner.die(source);
	}

	public static boolean returnToBody(ServerPlayer player) {
		MindBodyState state = PlayerPowers.get(player).mindBody();
		if (state == null) return false;
		Identifier dimensionId = Identifier.tryParse(state.dimension());
		if (dimensionId == null) return false;
		MinecraftServer server = player.level().getServer();
		ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (target == null) return false;
		int chunkX = ((int) Math.floor(state.x())) >> 4;
		int chunkZ = ((int) Math.floor(state.z())) >> 4;
		target.getChunk(chunkX, chunkZ);
		Vec3 requested = new Vec3(state.x(), state.y(), state.z());
		Vec3 destination = findReturnSpot(player, target, requested);
		if (destination == null) return false;
		player.setCamera(null);
		player.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
				state.yRot(), state.xRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		GameType mode = gameMode(state.gameMode());
		if (mode != null) player.setGameMode(mode);
		finish(player);
		PowerFx.rune(target, destination, 1.4, 0xBCA7FF, 20, Math.PI);
		return true;
	}

	public static void finish(ServerPlayer player) {
		Active active = BY_OWNER.remove(player.getUUID());
		if (active != null) {
			BY_BODY.remove(active.body().getUUID());
			active.body().discard();
			active.level().getChunkSource().removeTicketWithRadius(BODY_TICKET, active.chunk(), 1);
		}
		PlayerPowers.get(player).setMindBody(null);
	}

	public static void recoverOnJoin(ServerPlayer player) {
		if (PlayerPowers.get(player).mindBody() != null) returnToBody(player);
	}

	public static void discardOnDeath(ServerPlayer player) {
		finish(player);
	}

	public static void tickAll() {
		for (Active active : new ArrayList<>(BY_OWNER.values())) {
			if (!active.owner().isAlive() || active.owner().isRemoved()) {
				finish(active.owner());
				continue;
			}
			active.body().setDeltaMovement(Vec3.ZERO);
			active.body().setNoGravity(true);
			active.body().setPos(active.position().x, active.position().y, active.position().z);
		}
	}

	public static void returnAll(MinecraftServer server) {
		for (Active active : new ArrayList<>(BY_OWNER.values())) returnToBody(active.owner());
		for (Active active : new ArrayList<>(BY_OWNER.values())) finish(active.owner());
		BY_BODY.clear();
	}

	private static Vec3 findReturnSpot(ServerPlayer player, ServerLevel target, Vec3 requested) {
		for (int dy = 0; dy <= 3; dy++) {
			for (int radius = 0; radius <= 2; radius++) {
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						Vec3 candidate = requested.add(dx, dy, dz);
						if (SafeDestinationResolver.validate(player, target, candidate, TravelKind.RETURN).allowed()) {
							return candidate;
						}
					}
				}
			}
		}
		return null;
	}

	private static GameType gameMode(String name) {
		for (GameType mode : GameType.values()) if (mode.getName().equals(name)) return mode;
		return null;
	}
}
