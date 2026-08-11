package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.ParticipantPowerLock;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.PowerTargeting;
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
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vessel Possession watches through a consenting player or suitable mob for
 * 10 seconds while the caster's vulnerable body remains behind.
 */
public class VesselPossessionAbility extends Ability {
	// 10 seconds of possession
	private static final int POSSESS_TICKS = 200;
	private record Possession(UUID sessionId, ServerPlayer owner, LivingEntity target,
			boolean targetOriginallyNoAi, long endsAt) {}
	// one possession per owner uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, Possession> POSSESSING = new HashMap<>();

	public VesselPossessionAbility() {
		super(PowersMod.id("vessel_possession"),
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
			PowerMessages.send(player, "ability.powers.no_living_target", 4);
			return false;
		}
		// Amethyst and consent remain player-only protections; mobs are still
		// subject to safe zones through the entity overload below.
		if (AmethystDampening.isDampened(target)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (target instanceof ServerPlayer targetPlayer && !PossessionRules.rankAllows(
				SkillSystem.effectiveLevel(player), SkillSystem.effectiveLevel(targetPlayer))) {
			PowerMessages.overlay(player, Component.translatable("ability.powers.possession_higher_rank"));
			return false;
		}
		if (!mayPossess(player, target)) {
			if (target instanceof ServerPlayer targetPlayer) {
				PowerMessages.sendImportant(player, "powers.packet.consent_denied", 1,
						targetPlayer.getName().getString());
			}
			return false;
		}
		UUID sessionId = UUID.randomUUID();
		if (!ParticipantPowerLock.acquire(sessionId, java.util.List.of(
				player.getUUID(), target.getUUID()))) return false;
		if (!BodyProxyManager.start(player, BodyProxyKind.POSSESSION)) {
			ParticipantPowerLock.release(sessionId);
			return false;
		}

		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		boolean targetOriginallyNoAi = target instanceof Mob mob && mob.isNoAi();
		if (target instanceof Mob mob) mob.setNoAi(true);
		POSSESSING.put(player.getUUID(), new Possession(sessionId, player, target,
				targetOriginallyNoAi,
				server.getTickCount() + PossessionRules.durationTicks(
						scaledDuration(player, POSSESS_TICKS))));
		// watch the world through the target's eyes
		player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
		player.setCamera(target);
		VesselControlPackets.sendState(player, true);
		ServerLevel level = (ServerLevel) player.level();
		com.powers.fx.PowerFx.beam(level, player.getEyePosition(), target.getEyePosition(),
				com.powers.fx.PowerFx.dust(0xBCA7FF, 0.9F), 14);
		com.powers.fx.PowerFx.burst(level, target.position().add(0, 1, 0),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18, 0.5, 0.01);
		com.powers.fx.PowerFx.sound(level, target.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
		com.powers.fx.PowerFx.rune(level, player.position(), 1.5, 0xC27CFF, 22, 0.0);
		com.powers.fx.PowerFx.rune(level, target.position(), 1.5, 0x8FE9FF, 22, Math.PI);
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
			// End early if either participant becomes invalid, protection changes,
			// or a mob unloads while its camera session is active.
			if (!MagicUseGate.ongoingAllowed(owner) || !possession.target().isAlive()
					|| !targetAvailable || now >= possession.endsAt()
					|| !PossessionRules.sessionLocationValid(ownerIsCurrent, sameDimension)
					|| AmethystDampening.isDampened(possession.target())
					|| !mayPossess(possession.owner(), possession.target())) {
				// reset the owner's camera before dropping the possession
				end(possession, owner);
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
		if (possession != null) end(possession, owner);
	}

	private static void end(Possession possession, ServerPlayer owner) {
		ParticipantPowerLock.release(possession.sessionId());
		if (possession.target() instanceof Mob mob) {
			mob.setNoAi(possession.targetOriginallyNoAi());
		}
		if (owner == null) return;
		VesselControlPackets.sendState(owner, false);
		owner.setCamera(null);
		BodyProxyManager.returnToBody(owner);
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

	private static boolean mayPossess(ServerPlayer owner, LivingEntity target) {
		return target instanceof ServerPlayer playerTarget
				? PowerProtection.mayPossess(owner, playerTarget)
				: PowerProtection.mayPossess(owner, target);
	}

	public static void clearAll() {
		for (Possession possession : POSSESSING.values()) end(possession, possession.owner());
		POSSESSING.clear();
	}
}
