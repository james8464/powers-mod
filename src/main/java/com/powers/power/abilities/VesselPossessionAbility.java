package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.ParticipantPowerLock;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.PowerTargeting;
import com.powers.power.Power;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import com.powers.network.VesselControlPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vessel Possession controls a consenting player or suitable mob for a
 * bounded interval while the caster's vulnerable body remains behind.
 */
public class VesselPossessionAbility extends Ability {
	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("vessel_possession");
	// 10 seconds of possession
	private static final int POSSESS_TICKS = 200;
	private record Possession(UUID sessionId, ServerPlayer owner, LivingEntity target,
			boolean targetOriginallyNoAi, PossessionRules.SessionKind kind,
			CastSource castSource, long endsAt) {}
	// one possession per owner uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, Possession> POSSESSING = new HashMap<>();

	public VesselPossessionAbility() {
		super(POWER_ID,
				Component.translatable("ability.powers.vessel_possession"),
				600, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// already possessing someone - a second cast would strand the first target's camera
		if (POSSESSING.containsKey(player.getUUID())) return false;

		LivingEntity target = PowerTargeting.findLivingTarget(player, scaledRange(player, 32.0));
		PossessionRules.TargetKind targetKind = target instanceof ServerPlayer
				? PossessionRules.TargetKind.PLAYER
				: target instanceof Mob ? PossessionRules.TargetKind.MOB : PossessionRules.TargetKind.OTHER;
		if (target == null || !PossessionRules.isSuitable(targetKind, target == player,
				target.isAlive(), target.isRemoved(), BodyProxyManager.isProxy(target))) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "vessel_possession",
					com.powers.knowledge.MagicFailureReason.NO_TARGET);
			PowerMessages.send(player, "ability.powers.no_living_target", 4);
			return false;
		}
		// Amethyst and consent remain player-only protections; mobs are still
		// subject to safe zones through the entity overload below.
		if (AmethystDampening.isDampened(target)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "vessel_possession",
					com.powers.knowledge.MagicFailureReason.AMETHYST);
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (!rankAllowsControl(player, target, PossessionRules.SessionKind.POSSESSION)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "vessel_possession",
					com.powers.knowledge.MagicFailureReason.RANK_LOCK,
					java.util.Map.of("current_rank", (long) SkillSystem.effectiveLevel(player),
							"required_rank", target instanceof ServerPlayer targetPlayer
									? (long) SkillSystem.effectiveLevel(targetPlayer) + 1L : 0L));
			PowerMessages.overlay(player, Component.translatable("ability.powers.possession_higher_rank"));
			return false;
		}
		if (!mayControl(player, target, PossessionRules.SessionKind.POSSESSION)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "vessel_possession",
					target instanceof ServerPlayer
							? com.powers.knowledge.MagicFailureReason.CONSENT
							: com.powers.knowledge.MagicFailureReason.SAFE_ZONE);
			if (target instanceof ServerPlayer targetPlayer) {
				PowerMessages.sendImportant(player, "powers.packet.consent_denied", 1,
						targetPlayer.getName().getString());
			}
			return false;
		}
		return beginControlledSession(player, target, scaledDuration(player, POSSESS_TICKS),
				PossessionRules.SessionKind.POSSESSION, CastScalingContext.currentSource());
	}

	/** Starts the Blue Crystal's bounded full-control Dreamwalking session. */
	public static boolean beginDreamwalk(ServerPlayer player, LivingEntity target,
			int durationTicks, CastSource castSource) {
		return beginControlledSession(player, target, durationTicks,
				PossessionRules.SessionKind.DREAMWALK, castSource);
	}

	private static boolean beginControlledSession(ServerPlayer player, LivingEntity target,
			int durationTicks, PossessionRules.SessionKind kind, CastSource castSource) {
		if (player == null || target == null || kind == null || castSource == null
				|| POSSESSING.containsKey(player.getUUID())) return false;
		PossessionRules.TargetKind targetKind = target instanceof ServerPlayer
				? PossessionRules.TargetKind.PLAYER
				: target instanceof Mob ? PossessionRules.TargetKind.MOB : PossessionRules.TargetKind.OTHER;
		if (!PossessionRules.isSuitable(targetKind, target == player, target.isAlive(),
				target.isRemoved(), BodyProxyManager.isProxy(target))
				|| AmethystDampening.isDampened(target)
				|| !rankAllowsControl(player, target, kind) || !mayControl(player, target, kind)) return false;

		ServerLevel sourceLevel = (ServerLevel) player.level();
		ServerLevel targetLevel = (ServerLevel) target.level();
		boolean crossDimension = sourceLevel != targetLevel;
		if (crossDimension && (!PossessionRules.allowsCrossDimension(kind)
				|| !SafeDestinationResolver.validatePreload(player, targetLevel,
						target.position(), TravelKind.PROJECTION).allowed()
				|| !SafeDestinationResolver.validate(player, targetLevel,
						target.position(), TravelKind.PROJECTION).allowed())) return false;

		UUID sessionId = UUID.randomUUID();
		Vec3 bodyPosition = player.position();
		if (!ParticipantPowerLock.acquire(sessionId, java.util.List.of(
				player.getUUID(), target.getUUID()))) return false;
		BodyProxyKind bodyKind = kind == PossessionRules.SessionKind.DREAMWALK
				? BodyProxyKind.DREAMWALK : BodyProxyKind.POSSESSION;
		if (!BodyProxyManager.start(player, bodyKind)) {
			ParticipantPowerLock.release(sessionId);
			return false;
		}
		if (crossDimension) {
			player.teleport(new TeleportTransition(targetLevel, target.position(), Vec3.ZERO,
					target.getYRot(), target.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			if (player.level() != targetLevel) {
				ParticipantPowerLock.release(sessionId);
				BodyProxyManager.returnToBody(player);
				return false;
			}
		}

		MinecraftServer server = targetLevel.getServer();
		boolean targetOriginallyNoAi = target instanceof Mob mob && mob.isNoAi();
		if (target instanceof Mob mob) mob.setNoAi(true);
		POSSESSING.put(player.getUUID(), new Possession(sessionId, player, target,
				targetOriginallyNoAi, kind, castSource,
				server.getTickCount() + PossessionRules.durationTicks(durationTicks)));
		player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
		player.setCamera(target);
		VesselControlPackets.sendState(player, true);
		com.powers.fx.PowerFx.beam(targetLevel, player.getEyePosition(), target.getEyePosition(),
				com.powers.fx.PowerFx.dust(kind == PossessionRules.SessionKind.DREAMWALK
						? 0x7986CB : 0xBCA7FF, 0.9F), 14);
		com.powers.fx.PowerFx.burst(targetLevel, target.position().add(0, 1, 0),
				com.powers.PowersParticles.ECLIPSE, 18, 0.5, 0.01);
		com.powers.fx.PowerFx.sound(targetLevel, target.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
		com.powers.fx.PowerFx.rune(sourceLevel, bodyPosition, 1.5, 0xC27CFF, 22, 0.0);
		com.powers.fx.PowerFx.rune(targetLevel, target.position(), 1.5, 0x8FE9FF, 22, Math.PI);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = POSSESSING.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			Possession possession = entry.getValue();
			boolean ownerIsCurrent = owner != null && owner == possession.owner();
			boolean targetAvailable = !possession.target().isRemoved()
					&& (!(possession.target() instanceof ServerPlayer targetPlayer)
					|| server.getPlayerList().getPlayer(targetPlayer.getUUID()) == targetPlayer);
			boolean sameDimension = owner != null && owner.level() == possession.target().level();
			boolean protectionValid = !AmethystDampening.isDampened(possession.target())
					&& rankAllowsControl(possession.owner(), possession.target(), possession.kind())
					&& mayControl(possession.owner(), possession.target(), possession.kind());
			boolean sourceValid = ServerCastLifecycle.mayContinue(
					owner, possession.castSource(), ownsPower(owner));
			PossessionEndRules.Reason reason = PossessionEndRules.reason(
					possession.target().isAlive(), targetAvailable,
					MagicUseGate.ongoingAllowed(owner),
					PossessionRules.sessionLocationValid(ownerIsCurrent, sameDimension),
					protectionValid, sourceValid, now < possession.endsAt());
			if (reason != PossessionEndRules.Reason.NONE) {
				end(possession, owner, reason);
				it.remove();
			} else if (now % 20 == 0 && owner.level() == possession.target().level()) {
				com.powers.fx.PowerFx.clash((ServerLevel) owner.level(), owner.getEyePosition(),
						possession.target().getEyePosition(), 0xC27CFF, 0x8FE9FF);
			}
		}
	}

	/** Ends any possession by the given player and resets their camera, used on disconnect. */
	public static void clear(ServerPlayer owner) {
		Possession possession = POSSESSING.remove(owner.getUUID());
		if (possession != null) end(possession, owner, PossessionEndRules.Reason.OWNER_INVALID);
	}

	private static void end(Possession possession, ServerPlayer owner,
			PossessionEndRules.Reason reason) {
		ParticipantPowerLock.release(possession.sessionId());
		if (possession.target() instanceof Mob mob) {
			mob.setNoAi(possession.targetOriginallyNoAi());
		}
		if (owner == null) return;
		VesselControlPackets.sendState(owner, false);
		owner.setCamera(null);
		if (reason == PossessionEndRules.Reason.VESSEL_FATAL) {
			BodyProxyManager.returnToBody(owner, returned -> {
				if (returned && owner.isAlive() && !owner.isRemoved()) {
					com.powers.fx.GodlyPunishment.deadVesselWrath(owner);
				}
			});
		} else {
			BodyProxyManager.returnToBody(owner);
		}
	}

	/** True only for the remote player whose vanilla input must be suppressed. */
	public static boolean isControlledPlayer(UUID playerId) {
		if (playerId == null) return false;
		for (Possession possession : POSSESSING.values()) {
			if (possession.target() instanceof ServerPlayer target
					&& target.getUUID().equals(playerId)) return true;
		}
		return false;
	}

	/** True only while the player owns a Blue Crystal control session. */
	public static boolean isDreamwalking(UUID playerId) {
		Possession possession = playerId == null ? null : POSSESSING.get(playerId);
		return possession != null && possession.kind() == PossessionRules.SessionKind.DREAMWALK;
	}

	/** Ends a Blue Crystal control session without disturbing ordinary possession. */
	public static boolean stopDreamwalking(ServerPlayer owner) {
		Possession possession = owner == null ? null : POSSESSING.get(owner.getUUID());
		if (possession == null || possession.kind() != PossessionRules.SessionKind.DREAMWALK) return false;
		POSSESSING.remove(owner.getUUID());
		end(possession, owner, PossessionEndRules.Reason.NONE);
		return true;
	}

	/** Applies one rate-limited input frame to the exact server-owned host. */
	public static void applyControl(ServerPlayer owner, VesselControlPackets.InputPayload input) {
		Possession possession = owner == null ? null : POSSESSING.get(owner.getUUID());
		if (possession == null || input == null || possession.owner() != owner) return;
		LivingEntity host = possession.target();
		if (!host.isAlive() || host.isRemoved() || host.level() != owner.level()
				|| !Float.isFinite(input.yaw()) || !Float.isFinite(input.pitch())) return;

		float yaw = net.minecraft.util.Mth.wrapDegrees(input.yaw());
		float pitch = net.minecraft.util.Mth.clamp(input.pitch(), -90.0F, 90.0F);
		host.setYRot(yaw);
		host.setYHeadRot(yaw);
		host.setXRot(pitch);
		VesselControlRules.Movement movement = VesselControlRules.movement(
				yaw, input.forward(), input.strafe(), input.jump(), input.crouch());
		host.move(MoverType.SELF, new Vec3(movement.x(), movement.y(), movement.z()));

		if (host instanceof ServerPlayer playerHost) {
			playerHost.getInventory().setSelectedSlot(
					VesselControlRules.hotbarSlot(input.hotbarSlot()));
		}
		if (input.attackEntityId() < 0) return;
		Entity victim = host.level().getEntity(input.attackEntityId());
		if (!(victim instanceof LivingEntity living)
				|| !VesselControlRules.mayAttack(host.distanceToSqr(victim), victim.isAlive(), victim == host)
				|| BodyProxyManager.isProxy(living) || !PowerProtection.mayHarm(owner, living)) return;
		host.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		if (host instanceof ServerPlayer playerHost) playerHost.attack(victim);
		else host.doHurtTarget((ServerLevel) host.level(), victim);
	}

	private static boolean mayControl(ServerPlayer owner, LivingEntity target,
			PossessionRules.SessionKind kind) {
		if (PossessionRules.usesDreamwalkProtection(kind)) {
			return target instanceof ServerPlayer playerTarget
					? PowerProtection.mayDreamwalk(owner, playerTarget)
					: PowerProtection.mayDreamwalk(owner, target);
		}
		return target instanceof ServerPlayer playerTarget
				? PowerProtection.mayPossess(owner, playerTarget)
				: PowerProtection.mayPossess(owner, target);
	}

	private static boolean rankAllowsControl(ServerPlayer owner, LivingEntity target,
			PossessionRules.SessionKind kind) {
		return !PossessionRules.requiresRankCheck(kind) || !(target instanceof ServerPlayer playerTarget)
				|| PossessionRules.rankAllows(
						SkillSystem.effectiveLevel(owner), SkillSystem.effectiveLevel(playerTarget));
	}

	private static boolean ownsPower(ServerPlayer player) {
		if (player == null) return false;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && POWER_ID.equals(power.id())) return true;
		}
		return false;
	}

	public static void clearAll() {
		for (Possession possession : POSSESSING.values()) {
			end(possession, possession.owner(), PossessionEndRules.Reason.OWNER_INVALID);
		}
		POSSESSING.clear();
	}
}
