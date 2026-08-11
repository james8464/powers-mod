package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.ParticipantPowerLock;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerTargeting;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.protection.PowerProtection;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dreamwalking uses the shared bounded vessel-control channel while keeping
 * its own consent policy and a vulnerable physical body. Named security-camera
 * viewing remains a separate view-only entry point below.
 */
public class DreamwalkingAbility extends Ability {
	private static final double BASE_RANGE = 32.0;
	private static final int DURATION = 600;
	private static final Map<UUID, Dream> ACTIVE = new HashMap<>();

	private record Dream(UUID sessionId, UUID hostId, ResourceKey<Level> dimension,
			CastSource castSource, long endsAt) {}

	public DreamwalkingAbility() {
		super(PowersMod.id("dreamwalking"), Component.translatable("ability.powers.dreamwalking"),
				1200, false, false);
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return VesselPossessionAbility.isDreamwalking(player.getUUID());
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// Activating again while dreaming ends the current controlled session early.
		if (VesselPossessionAbility.stopDreamwalking(player)) return true;
		LivingEntity target = PowerTargeting.findLivingTarget(player, scaledRange(player, BASE_RANGE));
		boolean suitable = target instanceof ServerPlayer || target instanceof Mob;
		if (!suitable || target == player || target == null || !target.isAlive()
				|| BodyProxyManager.isProxy(target)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "dreamwalking",
					com.powers.knowledge.MagicFailureReason.NO_TARGET);
			PowerMessages.send(player, "ability.powers.no_living_target", 4);
			return false;
		}
		if (AmethystDampening.isDampened(target)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "dreamwalking",
					com.powers.knowledge.MagicFailureReason.AMETHYST);
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (!PowerProtection.mayDreamwalk(player, target)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "dreamwalking",
					target instanceof ServerPlayer
							? com.powers.knowledge.MagicFailureReason.CONSENT
							: com.powers.knowledge.MagicFailureReason.SAFE_ZONE);
			if (target instanceof ServerPlayer host) PowerMessages.sendImportant(player,
					"powers.packet.consent_denied", 1, host.getName().getString());
			return false;
		}
		return VesselPossessionAbility.beginDreamwalk(
				player, target, DURATION, CastScalingContext.currentSource());
	}

	/** Starts a named security camera after all caller-specific payment and consent checks. */
	public static boolean beginRemoteView(ServerPlayer player, LivingEntity host, int durationTicks) {
		return beginRemoteView(player, host, durationTicks, CastSource.CRYSTAL);
	}

	/** Starts a camera while retaining the route that authorized the delayed view. */
	public static boolean beginRemoteView(ServerPlayer player, LivingEntity host,
			int durationTicks, CastSource castSource) {
		if (player == null || host == null || player == host || !host.isAlive()
				|| castSource == null
				|| !MagicUseGate.ongoingAllowed(player)
				|| player.level().getServer() != host.level().getServer()
				|| ACTIVE.containsKey(player.getUUID()) || AmethystDampening.isDampened(host)
				|| !PowerProtection.mayDreamwalk(player, host)) return false;
		ServerLevel sourceLevel = (ServerLevel) player.level();
		ServerLevel hostLevel = (ServerLevel) host.level();
		boolean crossDimension = DreamwalkingRules.mustTravel(
				sourceLevel.dimension(), hostLevel.dimension());
		if (crossDimension && !SafeDestinationResolver.validatePreload(player, hostLevel,
				host.position(), TravelKind.PROJECTION).allowed()) return false;
		UUID sessionId = UUID.randomUUID();
		if (!ParticipantPowerLock.acquire(sessionId, java.util.List.of(
				player.getUUID(), host.getUUID()))) return false;
		if (!BodyProxyManager.start(player, BodyProxyKind.DREAMWALK)) {
			ParticipantPowerLock.release(sessionId);
			return false;
		}
		MinecraftServer server = player.level().getServer();
		Vec3 bodyPosition = player.position();
		Dream dream = new Dream(sessionId, host.getUUID(), host.level().dimension(), castSource,
				server.getTickCount() + Math.clamp(durationTicks, 20, DURATION));
		player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
		if (crossDimension) {
			player.teleport(new TeleportTransition(hostLevel, host.position(), Vec3.ZERO,
					host.getYRot(), host.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			if (player.level() != hostLevel) {
				ParticipantPowerLock.release(sessionId);
				BodyProxyManager.returnToBody(player);
				return false;
			}
		}
		ACTIVE.put(player.getUUID(), dream);
		player.setCamera(host);
		if (crossDimension) {
			PowerFx.rune(sourceLevel, bodyPosition, 1.5, 0x3F51B5, 28, 0.0);
			PowerFx.spiral(hostLevel, host.position(), 1.2, 2.4, 0x7986CB, 24, Math.PI);
		} else {
			PowerFx.beam(hostLevel, bodyPosition.add(0.0, player.getEyeHeight(), 0.0),
					host.getEyePosition(), com.powers.PowersParticles.ECLIPSE, 18);
		}
		PowerFx.rune(hostLevel, host.position(), 1.1, 0x81D4FA, 20, Math.PI);
		PowerFx.sound(hostLevel, host.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.45f);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer dreamer = server.getPlayerList().getPlayer(entry.getKey());
			Dream dream = entry.getValue();
			ServerLevel hostLevel = server.getLevel(dream.dimension());
			LivingEntity host = hostLevel == null ? null
					: hostLevel.getEntity(dream.hostId()) instanceof LivingEntity living ? living : null;
			boolean invalid = !MagicUseGate.ongoingAllowed(dreamer)
					|| !ServerCastLifecycle.mayContinue(dreamer, dream.castSource(), false)
					|| host == null || !host.isAlive()
					|| dreamer.level() != hostLevel
					|| now >= dream.endsAt();
			// Consent and amethyst are live counterplay, not one-time entry checks.
			if (!invalid) {
				invalid = AmethystDampening.isDampened(dreamer) || AmethystDampening.isDampened(host)
						|| !PowerProtection.mayDreamwalk(dreamer, host);
			}
			if (invalid) {
				end(dream, dreamer);
				it.remove();
			} else if (now % 20 == 0) {
				PowerFx.coloredBurst(hostLevel, host.getEyePosition(), 0x7986CB, 3, 0.22);
			}
		}
	}

	private static void end(Dream dream, ServerPlayer dreamer) {
		ParticipantPowerLock.release(dream.sessionId());
		if (dreamer == null) return;
		ServerLevel level = (ServerLevel) dreamer.level();
		PowerFx.rune(level, dreamer.position(), 1.1, 0x7986CB, 18, Math.PI);
		PowerFx.sound(level, dreamer.position(), SoundEvents.BEACON_DEACTIVATE, 0.65f, 1.35f);
		dreamer.setCamera(null);
		BodyProxyManager.returnToBody(dreamer);
	}

	/** Resets every surviving dreamer's camera during server shutdown. */
	public static void clearAll(MinecraftServer server) {
		for (var entry : ACTIVE.entrySet()) {
			ServerPlayer dreamer = server.getPlayerList().getPlayer(entry.getKey());
			ParticipantPowerLock.release(entry.getValue().sessionId());
			if (dreamer != null) dreamer.setCamera(null);
		}
		ACTIVE.clear();
	}

	/** Ends one dream and restores its camera during disconnect cleanup. */
	public static void clear(ServerPlayer dreamer) {
		Dream dream = ACTIVE.remove(dreamer.getUUID());
		if (dream != null) end(dream, dreamer);
	}
}
