package com.powers.protection;

import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.config.ResolvedPowerPolicy;
import com.powers.player.PlayerPowers;
import com.powers.magic.participant.MagicConsentAuthority;
import com.powers.magic.participant.MagicParticipants;
import com.powers.power.abilities.ControlResistance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Central server-authoritative policy for safe zones, consent, and terrain mutation. */
public final class PowerProtection {
	private PowerProtection() {
	}

	public static boolean isSafeZone(ServerLevel level, Vec3 position) {
		String dimension = level.dimension().identifier().toString();
		for (PowersConfig.SafeZone zone : PowersConfigLoader.get().safeZones()) {
			if (!zone.dimension().equals(dimension)) continue;
			double dx = position.x - zone.x();
			double dy = position.y - zone.y();
			double dz = position.z - zone.z();
			if (dx * dx + dy * dy + dz * dz <= zone.radius() * zone.radius()) return true;
		}
		return false;
	}

	public static ProtectionDecision blockDecision(ServerPlayer caster, ServerLevel level, BlockPos pos) {
		return blockDecision((LivingEntity) caster, level, pos);
	}

	/** Entity-safe terrain decision used by Shadow, guardians, and player powers. */
	public static ProtectionDecision blockDecision(LivingEntity caster, ServerLevel level, BlockPos pos) {
		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(level);
		ProtectionDecision builtIn = blockDecision(policy, isSafeZone(level, Vec3.atCenterOf(pos)),
				level.getBlockEntity(pos) != null);
		if (builtIn != ProtectionDecision.ALLOW) return builtIn;
		boolean adapterAllows = adapterAllows(ProtectionAction.TERRAIN,
				caster == null ? null : caster.getUUID(), null, level, pos);
		return blockDecision(policy, false, false, adapterAllows);
	}

	public static ProtectionDecision blockDecision(PowersConfig config, boolean safeZone, boolean blockEntity) {
		if (safeZone) return ProtectionDecision.DENY_SAFE_ZONE;
		if (!config.allowTerrainDamage()) return ProtectionDecision.DENY_TERRAIN;
		if (blockEntity && !config.allowBlockEntityDamage()) return ProtectionDecision.DENY_BLOCK_ENTITY;
		return ProtectionDecision.ALLOW;
	}

	public static ProtectionDecision blockDecision(ResolvedPowerPolicy policy,
			boolean safeZone, boolean blockEntity) {
		return blockDecision(policy, safeZone, blockEntity, true);
	}

	public static ProtectionDecision blockDecision(ResolvedPowerPolicy policy,
			boolean safeZone, boolean blockEntity, boolean adapterAllows) {
		if (safeZone) return ProtectionDecision.DENY_SAFE_ZONE;
		if (!policy.allowTerrainDamage()) return ProtectionDecision.DENY_TERRAIN;
		if (blockEntity && !policy.allowBlockEntityDamage()) return ProtectionDecision.DENY_BLOCK_ENTITY;
		return adapterAllows ? ProtectionDecision.ALLOW : ProtectionDecision.DENY_ADAPTER;
	}

	public static boolean mayAffectBlock(ServerPlayer caster, ServerLevel level, BlockPos pos) {
		return blockDecision(caster, level, pos) == ProtectionDecision.ALLOW;
	}

	/** Entity-safe terrain mutation check. */
	public static boolean mayAffectBlock(LivingEntity caster, ServerLevel level, BlockPos pos) {
		return blockDecision(caster, level, pos) == ProtectionDecision.ALLOW;
	}

	/** Unowned persistent/spread mutation check. */
	public static boolean mayAffectBlock(ServerLevel level, BlockPos pos) {
		return blockDecision((LivingEntity) null, level, pos) == ProtectionDecision.ALLOW;
	}

	public static boolean mayForceMove(ServerPlayer caster, ServerPlayer target) {
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		if (!adaptersAllow(ProtectionAction.MOVEMENT, caster, target,
				(ServerLevel) target.level(), target.blockPosition())) return false;
		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve((ServerLevel) target.level());
		boolean ordinary = !policy.requireTeleportConsent() || policy.hostileForcedMovement()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.TELEPORT);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.TELEPORT, ordinary)
				&& controlAllowed(caster, target);
	}

	/** Entity-safe overload used by knockback, levitation and time powers. */
	public static boolean mayForceMove(ServerPlayer caster, LivingEntity target) {
		if (caster == target) return true;
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		if (target instanceof ServerPlayer player) return mayForceMove(caster, player);
		if (!adaptersAllow(ProtectionAction.MOVEMENT, caster, target,
				(ServerLevel) target.level(), target.blockPosition())) return false;
		var participant = MagicParticipants.resolve(target);
		boolean consent = participant.isEmpty()
				|| participant.get().consentAuthority() == MagicConsentAuthority.ALWAYS_ALLOW_TESTS
				|| participant.get().consentOwner().map(owner -> mayForceMove(caster, owner)).orElse(true);
		return consent && controlAllowed(caster, target);
	}

	/** Safe zones prevent offensive power damage regardless of target type. */
	public static boolean mayHarm(ServerPlayer caster, LivingEntity target) {
		return caster == target || !isSafeZone((ServerLevel) target.level(), target.position())
				&& adaptersAllow(ProtectionAction.DAMAGE, caster, target,
						(ServerLevel) target.level(), target.blockPosition());
	}

	/** Event-level safety net covering every POWERS damage source, including persisted unowned events. */
	public static boolean mayPowerDamage(net.minecraft.world.entity.Entity source, LivingEntity target) {
		if (source == target) return true;
		ServerLevel level = (ServerLevel) target.level();
		if (isSafeZone(level, target.position())) return false;
		ServerPlayer actor = source instanceof ServerPlayer player ? player : null;
		return adaptersAllow(ProtectionAction.DAMAGE, actor, target, level, target.blockPosition());
	}

	/** Shared destination hook for every non-recovery portal or teleport route. */
	public static boolean mayPortal(LivingEntity subject, ServerLevel target, BlockPos destination) {
		ServerPlayer actor = subject instanceof ServerPlayer player ? player : null;
		return adaptersAllow(ProtectionAction.PORTAL, actor, subject, target, destination);
	}

	/** Shared claim hook for all grimoire rituals after their target is locked. */
	public static boolean mayRitual(ServerPlayer caster, ServerLevel level, BlockPos focus) {
		return adaptersAllow(ProtectionAction.RITUAL, caster, null, level, focus);
	}

	private static boolean adaptersAllow(ProtectionAction action, ServerPlayer caster,
			LivingEntity target, ServerLevel level, BlockPos position) {
		return adapterAllows(action, caster == null ? null : caster.getUUID(),
				target == null ? null : target.getUUID(), level, position);
	}

	private static boolean adapterAllows(ProtectionAction action, java.util.UUID actor,
			java.util.UUID target, ServerLevel level, BlockPos position) {
		return PowerProtectionAdapters.allows(new ProtectionQuery(action, level, position, actor, target));
	}

	private static boolean controlAllowed(ServerPlayer caster, LivingEntity target) {
		ControlResistance.Outcome outcome = ControlResistance.evaluate(caster, target, "forced_movement");
		return outcome == ControlResistance.Outcome.FULL
				|| outcome == ControlResistance.Outcome.RESISTED;
	}

	public static boolean mayLocate(ServerPlayer caster, ServerPlayer target) {
		ServerLevel level = (ServerLevel) target.level();
		if (isSafeZone(level, target.position())
				|| !adaptersAllow(ProtectionAction.OBSERVE, caster, target,
						level, target.blockPosition())) return false;
		boolean ordinary = !ResolvedPowerPolicy.resolve(level).requireLocatorConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.LOCATOR);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.LOCATOR, ordinary);
	}

	/** Applies observation protection to named mobs as well as consent-aware players. */
	public static boolean mayLocate(ServerPlayer caster, LivingEntity target) {
		if (target instanceof ServerPlayer player) return mayLocate(caster, player);
		ServerLevel level = (ServerLevel) target.level();
		if (caster == target || isSafeZone(level, target.position())
				|| !adaptersAllow(ProtectionAction.OBSERVE, caster, target,
						level, target.blockPosition())) return false;
		var participant = MagicParticipants.resolve(target);
		if (participant.isEmpty()
				|| participant.get().consentAuthority() == MagicConsentAuthority.ALWAYS_ALLOW_TESTS) {
			return true;
		}
		return participant.get().consentOwner()
				.map(owner -> owner == caster || mayLocate(caster, owner)).orElse(true);
	}

	public static boolean mayBringCompanion(ServerPlayer caster, ServerPlayer target) {
		ServerLevel level = (ServerLevel) target.level();
		if (isSafeZone(level, target.position())
				|| !adaptersAllow(ProtectionAction.MOVEMENT, caster, target,
						level, target.blockPosition())) return false;
		boolean ordinary = !ResolvedPowerPolicy.resolve(level).requireCompanionConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.COMPANION);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.COMPANION, ordinary);
	}

	public static boolean mayDreamwalk(ServerPlayer caster, ServerPlayer target) {
		ServerLevel level = (ServerLevel) target.level();
		if (isSafeZone(level, target.position())
				|| !adaptersAllow(ProtectionAction.OBSERVE, caster, target,
						level, target.blockPosition())) return false;
		boolean ordinary = !ResolvedPowerPolicy.resolve(level).requireDreamwalkConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.DREAMWALK);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.DREAMWALK, ordinary);
	}

	/** Keeps player consent while allowing named or aimed mobs outside safe zones. */
	public static boolean mayDreamwalk(ServerPlayer caster, LivingEntity target) {
		if (target instanceof ServerPlayer player) return mayDreamwalk(caster, player);
		ServerLevel level = (ServerLevel) target.level();
		if (caster == target || isSafeZone(level, target.position())
				|| !adaptersAllow(ProtectionAction.OBSERVE, caster, target,
						level, target.blockPosition())) return false;
		var participant = MagicParticipants.resolve(target);
		if (participant.isEmpty()
				|| participant.get().consentAuthority() == MagicConsentAuthority.ALWAYS_ALLOW_TESTS) return true;
		return participant.get().consentOwner().map(owner -> mayDreamwalk(caster, owner)).orElse(true);
	}

	public static boolean mayPossess(ServerPlayer caster, ServerPlayer target) {
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		if (!adaptersAllow(ProtectionAction.MOVEMENT, caster, target,
				(ServerLevel) target.level(), target.blockPosition())) return false;
		boolean ordinary = !ResolvedPowerPolicy.resolve((ServerLevel) target.level())
				.requirePossessionConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.POSSESSION);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.POSSESSION, ordinary)
				&& controlAllowed(caster, target);
	}

	/** Keeps player consent intact while allowing suitable mobs outside safe zones. */
	public static boolean mayPossess(ServerPlayer caster, LivingEntity target) {
		if (target instanceof ServerPlayer player) return mayPossess(caster, player);
		if (caster == target) return false;
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		if (!adaptersAllow(ProtectionAction.MOVEMENT, caster, target,
				(ServerLevel) target.level(), target.blockPosition())) return false;
		var participant = MagicParticipants.resolve(target);
		boolean consent = participant.isEmpty()
				|| participant.get().consentAuthority() == MagicConsentAuthority.ALWAYS_ALLOW_TESTS
				|| participant.get().consentOwner().map(owner -> mayPossess(caster, owner)).orElse(true);
		return consent && controlAllowed(caster, target);
	}
}
