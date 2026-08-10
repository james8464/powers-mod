package com.powers.power;

import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillQuestTracker;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import com.powers.protection.PowerProtection;

import java.util.function.Supplier;

/**
 * Single server-authoritative pipeline shared by innate powers and artifacts.
 * Every caller therefore receives identical suppression, payment, collision,
 * refund, cooldown, toggle, feedback, and state-synchronization semantics.
 */
public final class AbilityActivationService {
	public enum Result { ACTIVATED, FAILED, REQUIRES_INPUT }

	private AbilityActivationService() {
	}

	/** Activates one validated ability, using {@code toggleKey} for persistent toggles. */
	public static Result activate(ServerPlayer player, Ability ability, String toggleKey) {
		return activate(player, ability, toggleKey, false);
	}

	/** Activates a directly invoked innate cast with optional cooldown bypass. */
	public static Result activate(ServerPlayer player, Ability ability, String toggleKey,
			boolean bypassCooldown) {
		return activateWithCooldown(player, ability, toggleKey,
				bypassCooldown ? 0 : null, CastSource.INNATE);
	}

	/** Compatibility overload for an innate cast with a server-derived recovery time. */
	public static Result activateWithCooldown(ServerPlayer player, Ability ability, String toggleKey,
			Integer cooldownOverride) {
		return activateWithCooldown(player, ability, toggleKey, cooldownOverride, CastSource.INNATE);
	}

	/** Activates through an explicit server-derived route so scaling cannot follow a reused ability. */
	public static Result activateWithCooldown(ServerPlayer player, Ability ability, String toggleKey,
			Integer cooldownOverride, CastSource source) {
		return CastScalingContext.withSource(source,
				() -> activateScoped(player, ability, toggleKey, cooldownOverride, source));
	}

	private static Result activateScoped(ServerPlayer player, Ability ability, String toggleKey,
			Integer cooldownOverride, CastSource source) {
		if (ability == null) return Result.FAILED;
		if (!passesCasterChecks(player)) return Result.FAILED;
		if (ability.requiresInput()) return Result.REQUIRES_INPUT;

		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (ability.isSelectionAction(player)) {
			boolean selected = ability.activate(player, data);
			if (selected) PowersPackets.syncTo(player);
			return selected ? Result.ACTIVATED : Result.FAILED;
		}
		if (ability.isToggle()) {
			return toggle(player, data, ability, toggleKey, source);
		}

		return cast(player, data, ability, cooldownOverride, source,
				() -> ability.activate(player, data));
	}

	/** Completes a server-owned input flow such as picking a Time Shift landing point. */
	public static Result activateInput(ServerPlayer player, Ability ability, boolean bypassCooldown,
			Supplier<Boolean> operation) {
		if (ability == null || !ability.requiresInput() || operation == null
				|| !passesCasterChecks(player)) return Result.FAILED;
		return CastScalingContext.withSource(CastSource.INNATE, () -> cast(player,
				PlayerPowers.get(player), ability, bypassCooldown ? 0 : null,
				CastSource.INNATE, operation));
	}

	/** Shared coordinate-teleport pipeline used by an assigned power and the Shadow Sword. */
	public static Result activateTeleport(ServerPlayer caster, LivingEntity subject, Ability ability,
			ResourceKey<Level> dimension, double x, double y, double z, boolean bypassCooldown) {
		if (ability == null || !ability.requiresInput() || !Double.isFinite(x)
				|| !Double.isFinite(y) || !Double.isFinite(z)) return Result.FAILED;
		if (!passesCasterChecks(caster)) return Result.FAILED;
		if (!PowerProtection.mayForceMove(caster, subject)) {
			PowerMessages.sendImportant(caster, "powers.packet.consent_denied", 1,
					subject.getName().getString());
			return Result.FAILED;
		}
		if (subject instanceof ServerPlayer player) AmethystDampening.update(player);
		if (AmethystDampening.isDampened(subject)) {
			PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
			return Result.FAILED;
		}

		PlayerPowers.PlayerPowersData data = PlayerPowers.get(caster);
		return CastScalingContext.withSource(CastSource.INNATE, () -> cast(caster, data, ability,
				bypassCooldown ? 0 : null, CastSource.INNATE, () -> ability.activateTeleport(
						caster, subject, data, dimension, x, y, z)));
	}

	/** Completes artifact coordinate input with its alignment cooldown policy. */
	public static Result activateArtifactTeleport(ServerPlayer caster, LivingEntity subject, Ability ability,
			ResourceKey<Level> dimension, double x, double y, double z, int cooldownTicks) {
		if (ability == null || !ability.requiresInput() || !Double.isFinite(x)
				|| !Double.isFinite(y) || !Double.isFinite(z) || !passesCasterChecks(caster)) {
			return Result.FAILED;
		}
		if (!PowerProtection.mayForceMove(caster, subject)) return Result.FAILED;
		if (subject instanceof ServerPlayer player) AmethystDampening.update(player);
		if (AmethystDampening.isDampened(subject)) return Result.FAILED;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(caster);
		return CastScalingContext.withSource(CastSource.ARTIFACT, () -> cast(caster, data, ability,
				Math.max(0, cooldownTicks), CastSource.ARTIFACT, () -> ability.activateTeleport(
						caster, subject, data, dimension, x, y, z)));
	}

	private static Result cast(ServerPlayer player, PlayerPowers.PlayerPowersData data, Ability ability,
			Integer cooldownOverride, CastSource source, Supplier<Boolean> operation) {
		int remaining = cooldownOverride != null && cooldownOverride == 0
				? 0 : ActivationCooldowns.remainingTicks(player, ability);
		if (ActivationCooldowns.blocks(remaining,
				ability.mayReactivateDuringCooldown(player, data, remaining))) {
			PowerMessages.send(player, "ability.powers.cooldown", 4, seconds(remaining));
			return Result.FAILED;
		}
		PreparedMagicCast magic = ServerMagicCasts.prepare(
				player, ability.magicActionId(player, data), source);
		if (!magic.allowed() || !data.spendEnergy(player, ability)) return Result.FAILED;
		int cooldown = cooldownOverride == null
				? ability.cooldownTicksFor(player, data) : cooldownOverride;
		boolean activated = ServerMagicCasts.execute(magic,
				() -> AbilityActivationContext.withCooldown(cooldownOverride, operation));
		if (!activated) {
			data.refundEnergy(ability);
		} else {
			ActivationCooldowns.start(player, ability, cooldown);
			var presenceId = ServerMagicCasts.commit(magic, player);
			ability.bindPhysicalPresence(player, data, presenceId);
			SkillQuestTracker.recordPowerUse(player, ability);
		}
		PowersPackets.syncTo(player);
		return activated ? Result.ACTIVATED : Result.FAILED;
	}

	private static boolean passesCasterChecks(ServerPlayer player) {
		return MagicUseGate.passes(player, true);
	}

	private static Result toggle(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			Ability ability, String toggleKey, CastSource source) {
		if (toggleKey == null || toggleKey.isBlank()) return Result.FAILED;
		if (data.isToggleActive(toggleKey)) {
			ability.activateToggleOff(player, data);
			data.setToggleActive(player, toggleKey, false);
			PowerMessages.overlay(player, Component.translatable("ability.powers.toggle_off", ability.name()));
			PowersPackets.syncTo(player);
			return Result.ACTIVATED;
		}
		// A player may own the same toggle normally and through an artifact. Only
		// one invocation may own its physical modifier/state at a time.
		for (String activeKey : data.getActiveToggles()) {
			if (!activeKey.equals(toggleKey) && ToggleKeyRules.ownsAbility(activeKey, ability.id())) {
				ability.activateToggleOff(player, data);
				data.setToggleActive(player, activeKey, false);
			}
		}

		PreparedMagicCast magic = ServerMagicCasts.prepare(
				player, ability.magicActionId(player, data), source);
		if (!magic.allowed()) return Result.FAILED;
		boolean paid = data.spendEnergy(player, ability);
		boolean activated = paid && ServerMagicCasts.execute(magic, () -> ability.activateToggleOn(player, data));
		if (activated) {
			data.setToggleActive(player, toggleKey, true);
			var presenceId = ServerMagicCasts.commit(magic, player);
			ability.bindPhysicalPresence(player, data, presenceId);
			SkillQuestTracker.recordPowerUse(player, ability);
			PowerMessages.overlay(player, Component.translatable("ability.powers.toggle_on", ability.name()));
		} else if (paid) {
			data.refundEnergy(ability);
		}
		PowersPackets.syncTo(player);
		return activated ? Result.ACTIVATED : Result.FAILED;
	}

	private static String seconds(int ticks) {
		return String.valueOf((ticks + 19) / 20);
	}
}
