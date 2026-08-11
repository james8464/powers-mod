package com.powers.protection;

import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.player.PlayerPowers;
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
		PowersConfig config = PowersConfigLoader.get();
		return blockDecision(config, isSafeZone(level, Vec3.atCenterOf(pos)), level.getBlockEntity(pos) != null);
	}

	public static ProtectionDecision blockDecision(PowersConfig config, boolean safeZone, boolean blockEntity) {
		if (!config.allowTerrainDamage()) return ProtectionDecision.DENY_TERRAIN;
		if (safeZone) return ProtectionDecision.DENY_SAFE_ZONE;
		if (blockEntity && !config.allowBlockEntityDamage()) return ProtectionDecision.DENY_BLOCK_ENTITY;
		return ProtectionDecision.ALLOW;
	}

	public static boolean mayAffectBlock(ServerPlayer caster, ServerLevel level, BlockPos pos) {
		return blockDecision(caster, level, pos) == ProtectionDecision.ALLOW;
	}

	public static boolean mayForceMove(ServerPlayer caster, ServerPlayer target) {
		PowersConfig config = PowersConfigLoader.get();
		boolean ordinary = !config.requireTeleportConsent() || config.hostileForcedMovement()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.TELEPORT);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.TELEPORT, ordinary);
	}

	/** Entity-safe overload used by knockback, levitation and time powers. */
	public static boolean mayForceMove(ServerPlayer caster, LivingEntity target) {
		if (caster == target) return true;
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		return !(target instanceof ServerPlayer player) || mayForceMove(caster, player);
	}

	/** Safe zones prevent offensive power damage regardless of target type. */
	public static boolean mayHarm(ServerPlayer caster, LivingEntity target) {
		return caster == target || !isSafeZone((ServerLevel) target.level(), target.position());
	}

	public static boolean mayLocate(ServerPlayer caster, ServerPlayer target) {
		boolean ordinary = !PowersConfigLoader.get().requireLocatorConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.LOCATOR);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.LOCATOR, ordinary);
	}

	public static boolean mayBringCompanion(ServerPlayer caster, ServerPlayer target) {
		boolean ordinary = !PowersConfigLoader.get().requireCompanionConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.COMPANION);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.COMPANION, ordinary);
	}

	public static boolean mayDreamwalk(ServerPlayer caster, ServerPlayer target) {
		boolean ordinary = !PowersConfigLoader.get().requireDreamwalkConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.DREAMWALK);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.DREAMWALK, ordinary);
	}

	/** Keeps player consent while allowing named or aimed mobs outside safe zones. */
	public static boolean mayDreamwalk(ServerPlayer caster, LivingEntity target) {
		if (target instanceof ServerPlayer player) return mayDreamwalk(caster, player);
		return caster != target && !isSafeZone((ServerLevel) target.level(), target.position());
	}

	public static boolean mayPossess(ServerPlayer caster, ServerPlayer target) {
		boolean ordinary = !PowersConfigLoader.get().requirePossessionConsent()
				|| PlayerPowers.get(target).allowsConsent(ConsentKind.POSSESSION);
		return ConsentOverrideRuntime.authorize(caster, target, ConsentKind.POSSESSION, ordinary);
	}

	/** Keeps player consent intact while allowing suitable mobs outside safe zones. */
	public static boolean mayPossess(ServerPlayer caster, LivingEntity target) {
		if (target instanceof ServerPlayer player) return mayPossess(caster, player);
		if (caster == target) return false;
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		return true;
	}
}
