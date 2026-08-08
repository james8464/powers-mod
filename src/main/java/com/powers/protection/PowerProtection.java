package com.powers.protection;

import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.player.PlayerPowers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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
		if (caster == target) return true;
		PowersConfig config = PowersConfigLoader.get();
		if (isSafeZone((ServerLevel) target.level(), target.position())) return false;
		if (!config.requireTeleportConsent() || config.hostileForcedMovement()) return true;
		return PlayerPowers.get(target).allowsConsent(PlayerPowers.ConsentKind.TELEPORT);
	}

	public static boolean mayLocate(ServerPlayer caster, ServerPlayer target) {
		if (caster == target || !PowersConfigLoader.get().requireLocatorConsent()) return true;
		return PlayerPowers.get(target).allowsConsent(PlayerPowers.ConsentKind.LOCATOR);
	}

	public static boolean mayBringCompanion(ServerPlayer caster, ServerPlayer target) {
		if (caster == target || !PowersConfigLoader.get().requireCompanionConsent()) return true;
		return PlayerPowers.get(target).allowsConsent(PlayerPowers.ConsentKind.COMPANION);
	}
}
