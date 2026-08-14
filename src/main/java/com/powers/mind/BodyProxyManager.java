package com.powers.mind;

import com.powers.PowersMod;
import com.powers.config.ResolvedPowerPolicy;
import com.powers.fx.PowerFx;
import com.powers.fx.GodlyPunishment;
import com.powers.magic.runtime.MagicLifecycleRules;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.state.PowerEntityState;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.MindscapeMobReturnTracker;
import com.powers.power.travel.TravelKind;
import com.powers.power.travel.WorldBoundaryRules;
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

/** Owns vulnerable, skin-matched bodies left behind by mind and spirit travel. */
public final class BodyProxyManager {
	private enum ReturnContinuation { NONE, FATAL_DEATH, VESSEL_WRATH }
	private record Active(UUID ownerId, Mannequin body, Vec3 position,
			ServerLevel level, ChunkPos chunk, BodySnapshot snapshot, FatalResolutionGate fatalResolution) {
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
				snapshot, new FatalResolutionGate());
		BY_OWNER.put(player.getUUID(), active);
		BY_BODY.put(body.getUUID(), active);
		PhysicalMagicPresences.registerEntity(new MagicActionId("astral_projection"),
				player.getUUID(), body, 0.9, Long.MAX_VALUE);
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
		if (!ResolvedPowerPolicy.resolve(active.level())
				.projectionBodiesVulnerable()) return false;
		ServerPlayer owner = active.level().getServer().getPlayerList().getPlayer(active.ownerId());
		if (PowerDamage.isPowerDamage(source)) {
			if (!com.powers.protection.PowerProtection.mayPowerDamage(source.getEntity(), entity)) return false;
			if (owner != null && AmethystDampening.isDampened(owner)) return false;
		}
		if (owner != null && ForcefieldAbility.absorbDamage(owner, entity, source, amount)) return false;
		return true;
	}

	/** Cancels remote death while it is recalled and replayed at the physical body. */
	public static boolean allowsAvatarDeath(ServerPlayer player, DamageSource source) {
		Active active = player == null ? null : BY_OWNER.get(player.getUUID());
		if (active == null) return true;
		MindBodyState state = PlayerPowers.get(player).mindBody();
		MagicLifecycleRules.Form form = state == null ? MagicLifecycleRules.Form.ASTRAL_AVATAR
				: lifecycleForm(state.proxyKind());
		if (MagicLifecycleRules.resolve(form, MagicLifecycleRules.Source.NONE,
				MagicLifecycleRules.Event.AVATAR_FATAL).outcome()
				!= MagicLifecycleRules.Outcome.RETURN_AND_DIE) return true;
		if (active.fatalResolution().claim(FatalResolutionGate.Cause.AVATAR)) {
			beginFatalReturn(player);
		}
		return false;
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
		active.body().setHealth(active.body().getMaxHealth());
		PowerFx.coloredBurst((ServerLevel) active.body().level(), active.position().add(0, 1, 0),
				0xBCA7FF, 8, 0.45);
		if (health <= 0.0F) {
			if (active.fatalResolution().claim(FatalResolutionGate.Cause.BODY)) {
				beginFatalReturn(owner);
			}
		} else {
			owner.setHealth(health);
		}
	}

	/** Returns first, then replays vanilla death outside the active damage callback. */
	private static void beginFatalReturn(ServerPlayer owner) {
		owner.setCamera(null);
		owner.setHealth(Math.max(1.0F, owner.getHealth()));
		returnToBody(owner, TravelKind.FATAL_SOUL_RETURN, ReturnContinuation.FATAL_DEATH);
	}

	public static boolean returnToBody(ServerPlayer player) {
		return returnToBody(player, TravelKind.PLAYER_RETURN);
	}

	/** Returns a slain possessed host's controller before applying divine wrath. */
	public static boolean returnAfterVesselDeath(ServerPlayer player) {
		return returnToBody(player, TravelKind.PLAYER_RETURN, ReturnContinuation.VESSEL_WRATH);
	}

	/** Operator-only recovery path; callers must enforce administrative permission. */
	public static boolean recoverToBody(ServerPlayer player) {
		return returnToBody(player, TravelKind.ADMIN_RECOVERY);
	}

	private static boolean returnToBody(ServerPlayer player, TravelKind travelKind) {
		return returnToBody(player, travelKind, ReturnContinuation.NONE);
	}

	private static boolean returnToBody(ServerPlayer player, TravelKind travelKind,
			ReturnContinuation continuation) {
		MindBodyState state = PlayerPowers.get(player).mindBody();
		MinecraftServer server = player.level().getServer();
		UUID ownerId = player.getUUID();
		if (state == null) return completed(server, ownerId, continuation, false);
		Identifier dimensionId = Identifier.tryParse(state.dimension());
		ServerLevel target = dimensionId == null ? null
				: server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
		boolean fallback = MissingDimensionRecoveryRules.useOverworldFallback(travelKind, target != null);
		if (target == null && !fallback) {
			PersistentDimensionDiagnostics.record("body", state.dimension());
			PowersMod.LOGGER.error("Body return blocked because recorded dimension is unavailable: player={}, dimension={}; use /powers recoverbody as an operator",
					player.getUUID(), state.dimension());
			return completed(server, ownerId, continuation, false);
		}
		if (fallback) {
			PersistentDimensionDiagnostics.record("body_recovery", state.dimension());
			target = server.overworld();
			PowersMod.LOGGER.warn("Administratively recovering body with deleted dimension to Overworld spawn: player={}, missing={}",
					player.getUUID(), state.dimension());
		}
		ServerLevel returnLevel = target;
		var border = returnLevel.getWorldBorder();
		Vec3 recorded = fallback ? Vec3.atBottomCenterOf(server.getRespawnData().pos())
				: new Vec3(state.x(), state.y(), state.z());
		Vec3 requested = new Vec3(
				WorldBoundaryRules.clampCoordinate(recorded.x, border.getMinX(), border.getMaxX(), 1.0),
				recorded.y,
				WorldBoundaryRules.clampCoordinate(recorded.z, border.getMinZ(), border.getMaxZ(), 1.0));
		BlockPos requestedBlock = BlockPos.containing(requested);
		if (!LoadedChunks.contains(returnLevel, requestedBlock)) {
			ResourceKey<Level> returnDimension = returnLevel.dimension();
			return TravelChunkLoader.request(ownerId, returnLevel, requestedBlock, "body_return",
					(current, owner) -> {
						ServerLevel loadedReturn = current.getLevel(returnDimension);
						boolean returned = loadedReturn != null && completeReturn(current, owner, null,
								loadedReturn, state, requested, travelKind);
						completed(current, owner, continuation, returned);
					}, (current, ownerIdOnFailure) -> {
						ServerPlayer owner = current.getPlayerList().getPlayer(ownerIdOnFailure);
						if (owner != null) PowerMessages.send(owner, "ability.powers.no_room", 3);
						completed(current, ownerIdOnFailure, continuation, false);
					});
		}
		return completed(server, ownerId, continuation,
				completeReturn(server, ownerId, player, returnLevel, state, requested, travelKind));
	}

	private static boolean completed(MinecraftServer server, UUID ownerId,
			ReturnContinuation continuation, boolean result) {
		ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
		if (continuation == ReturnContinuation.VESSEL_WRATH && result && owner != null
				&& owner.isAlive() && !owner.isRemoved()) {
			GodlyPunishment.deadVesselWrath(owner);
		}
		if (continuation == ReturnContinuation.FATAL_DEATH) {
			if (!result && owner != null) finish(owner);
			ResourceKey<Level> dimension = owner == null
					? Level.OVERWORLD : owner.level().dimension();
			PowersMod.scheduleDelayed(server, 1, ownerId, dimension, ownerId,
					"fatal_body_death", (currentServer, task) -> {
						ServerPlayer current = currentServer.getPlayerList().getPlayer(task.subjectId());
						if (current == null || current.isRemoved()) return;
						current.setHealth(0.0F);
						current.die(current.damageSources().generic());
					});
		}
		return result;
	}

	private static boolean completeReturn(MinecraftServer server, UUID ownerId, ServerPlayer directOwner,
			ServerLevel target, MindBodyState state, Vec3 requested, TravelKind travelKind) {
		ServerPlayer listedOwner = server.getPlayerList().getPlayer(ownerId);
		BodyReturnOwnerPolicy.Source source = BodyReturnOwnerPolicy.resolve(
				directOwner != null && ownerId.equals(directOwner.getUUID()), listedOwner != null);
		ServerPlayer player = switch (source) {
			case DIRECT -> directOwner;
			case LOOKUP -> listedOwner;
			case MISSING -> null;
		};
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
			MindscapeMobReturnTracker.returnOwned(player.level().getServer(), player.getUUID());
			BY_BODY.remove(active.body().getUUID());
			BodyProxyPackets.remove(active.body());
			PhysicalMagicPresences.unload(active.body());
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
		MindscapeMobReturnTracker.returnOwned(active.level().getServer(), active.ownerId());
		BY_BODY.remove(active.body().getUUID());
		BodyProxyPackets.remove(active.body());
		PhysicalMagicPresences.unload(active.body());
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

	private static MagicLifecycleRules.Form lifecycleForm(BodyProxyKind kind) {
		return switch (kind) {
			case REALM -> MagicLifecycleRules.Form.REALM_AVATAR;
			case ASTRAL -> MagicLifecycleRules.Form.ASTRAL_AVATAR;
			case MARKING -> MagicLifecycleRules.Form.TELEPORT_MARKER;
			case POSSESSION -> MagicLifecycleRules.Form.POSSESSION_CONTROLLER;
			case DREAMWALK -> MagicLifecycleRules.Form.DREAMWALK_CONTROLLER;
		};
	}
}
