package com.powers.mind;

import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.PowerEntityState;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.util.LoadedChunks;
import com.powers.util.PowerMessages;
import com.powers.network.BodyProxyPackets;
import net.minecraft.core.BlockPos;
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
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns vulnerable, skin-matched bodies left behind by mind and spirit travel. */
public final class BodyProxyManager {
	private record Active(UUID ownerId, Mannequin body, Vec3 position,
			ServerLevel level, ChunkPos chunk, BodySnapshot snapshot, AtomicBoolean fatalResolved) {
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
		body.setXRot(player.getXRot());
		body.setYHeadRot(player.getYHeadRot());
		body.setYBodyRot(player.yBodyRot);
		body.setPose(player.getPose());
		body.setMainArm(player.getMainArm());
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
		level.getChunkSource().addTicketWithRadius(BODY_TICKET, bodyChunk,
				BodyProxyTicketRules.radius());
		BodySnapshot snapshot = BodySnapshot.capture(player);
		Active active = new Active(player.getUUID(), body, player.position(), level, bodyChunk,
				snapshot, new AtomicBoolean());
		BY_OWNER.put(player.getUUID(), active);
		BY_BODY.put(body.getUUID(), active);
		BodyProxyPackets.sendToTracking(body, snapshot);
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

	/** Returns the immutable render frame for a tracked proxy body UUID. */
	public static BodySnapshot snapshotFor(UUID bodyId) {
		Active active = BY_BODY.get(bodyId);
		return active == null ? null : active.snapshot();
	}

	public static boolean allowsDamage(LivingEntity entity, DamageSource source, float amount) {
		Active active = BY_BODY.get(entity.getUUID());
		if (active == null) return true;
		if (!PowersConfigLoader.get().projectionBodiesVulnerable()) return false;
		ServerPlayer owner = active.level().getServer().getPlayerList().getPlayer(active.ownerId());
		if (owner != null && PowerDamage.isPowerDamage(source) && AmethystDampening.isDampened(owner)) {
			return false;
		}
		if (owner != null && MindBodyDamageRules.proxyDamageIsFatal(amount, owner.getHealth())
				&& active.fatalResolved().compareAndSet(false, true)) {
			resolveFatalDamage(active, owner, source, amount);
			return false;
		}
		return true;
	}

	/** Detached avatars are only cameras/minds; all physical damage belongs to the frozen body. */
	public static boolean avatarMayTakeDamage(ServerPlayer player) {
		return MindBodyDamageRules.avatarMayTakeDamage(
				player != null && PlayerPowers.get(player).mindBody() != null);
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
		ServerPlayer owner = active.level().getServer().getPlayerList().getPlayer(active.ownerId());
		if (owner == null) return;
		if (!owner.isAlive() || owner.isRemoved()) return;
		float health = owner.getHealth() - damageTaken;
		owner.setHealth(Math.max(0.0f, health));
		active.body().setHealth(active.body().getMaxHealth());
		PowerFx.coloredBurst((ServerLevel) active.body().level(), active.position().add(0, 1, 0),
				0xBCA7FF, 8, 0.45);
		if (health <= 0.0f) owner.die(source);
	}

	/** Returns the owner before applying a fatal physical-body hit, exactly once. */
	private static void resolveFatalDamage(Active active, ServerPlayer owner,
			DamageSource source, float amount) {
		owner.setCamera(null);
		boolean returned = returnToBody(owner);
		if (!returned) {
			// Realm gates may legitimately refuse a player-controlled return. End
			// the detached session before death so respawn confinement owns recovery.
			finish(owner);
			owner.die(source);
			return;
		}
		owner.hurtServer((ServerLevel) owner.level(), source,
				Math.max(amount, owner.getHealth()));
	}

	public static boolean returnToBody(ServerPlayer player) {
		return returnToBody(player, TravelKind.PLAYER_RETURN);
	}

	/** Operator-only recovery path; callers must enforce administrative permission. */
	public static boolean recoverToBody(ServerPlayer player) {
		return returnToBody(player, TravelKind.ADMIN_RECOVERY);
	}

	private static boolean returnToBody(ServerPlayer player, TravelKind travelKind) {
		MindBodyState state = PlayerPowers.get(player).mindBody();
		if (state == null) return false;
		Identifier dimensionId = Identifier.tryParse(state.dimension());
		if (dimensionId == null) return false;
		MinecraftServer server = player.level().getServer();
		ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		if (target == null) return false;
		Vec3 requested = new Vec3(state.x(), state.y(), state.z());
		BlockPos requestedBlock = BlockPos.containing(requested);
		if (!LoadedChunks.contains(target, requestedBlock)) {
			UUID ownerId = player.getUUID();
			return TravelChunkLoader.request(ownerId, target, requestedBlock,
					() -> completeReturn(server, ownerId, target, state, requested, travelKind),
					() -> {
						ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
						if (owner != null) PowerMessages.send(owner, "ability.powers.no_room", 3);
					});
		}
		return completeReturn(server, player.getUUID(), target, state, requested, travelKind);
	}

	private static boolean completeReturn(MinecraftServer server, UUID ownerId,
			ServerLevel target, MindBodyState state, Vec3 requested, TravelKind travelKind) {
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		if (player == null || !state.equals(PlayerPowers.get(player).mindBody())) return false;
		Vec3 destination = findReturnSpot(player, target, requested, travelKind);
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
			BodyProxyPackets.remove(active.body());
			active.body().discard();
			active.level().getChunkSource().removeTicketWithRadius(BODY_TICKET, active.chunk(),
					BodyProxyTicketRules.radius());
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
		if (BY_OWNER.isEmpty()) return;
		java.util.List<Active> stale = null;
		for (Active active : BY_OWNER.values()) {
			ServerPlayer owner = active.level().getServer().getPlayerList().getPlayer(active.ownerId());
			if (owner == null || !owner.isAlive() || owner.isRemoved()) {
				if (stale == null) stale = new ArrayList<>();
				stale.add(active);
				continue;
			}
			active.body().setDeltaMovement(Vec3.ZERO);
			active.body().setNoGravity(true);
			active.body().setPos(active.position().x, active.position().y, active.position().z);
		}
		if (stale != null) for (Active active : stale) finishStale(active);
	}

	public static void returnAll(MinecraftServer server) {
		for (Active active : new ArrayList<>(BY_OWNER.values())) {
			ServerPlayer owner = server.getPlayerList().getPlayer(active.ownerId());
			if (owner != null) returnToBody(owner);
		}
		for (Active active : new ArrayList<>(BY_OWNER.values())) finishStale(active);
		BY_BODY.clear();
	}

	/** Number of vulnerable mind-body anchors and their one-chunk tickets. */
	public static int activeProxyCount() {
		return BY_OWNER.size();
	}

	private static void finishStale(Active active) {
		if (!BY_OWNER.remove(active.ownerId(), active)) return;
		BY_BODY.remove(active.body().getUUID());
		BodyProxyPackets.remove(active.body());
		active.body().discard();
		active.level().getChunkSource().removeTicketWithRadius(BODY_TICKET, active.chunk(),
				BodyProxyTicketRules.radius());
	}

	private static Vec3 findReturnSpot(ServerPlayer player, ServerLevel target, Vec3 requested,
			TravelKind travelKind) {
		for (int dy = 0; dy <= 3; dy++) {
			for (int radius = 0; radius <= 2; radius++) {
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						Vec3 candidate = requested.add(dx, dy, dz);
						if (SafeDestinationResolver.validate(player, target, candidate,
								travelKind).allowed()) {
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
