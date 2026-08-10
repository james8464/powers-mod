package com.powers.item;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.PowerEnergy;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Advances inventory consequences, rapid affinity, and sword-owned toggles once per player. */
public final class ShadowSwordRuntime {
	private static final Set<UUID> UNAUTHORIZED_CARRIERS = new HashSet<>();
	private static final Map<UUID, Long> LAST_GUARDIAN_SUMMON = new HashMap<>();

	private ShadowSwordRuntime() {
	}

	public static void tickPlayer(ServerPlayer player, int serverTick) {
		boolean carriesSword = ShadowSwordPowerManager.carriesSword(player);
		if (!carriesSword) {
			UNAUTHORIZED_CARRIERS.remove(player.getUUID());
			LAST_GUARDIAN_SUMMON.remove(player.getUUID());
			stopSwordToggles(player);
			return;
		}
		if (!ShadowSwordPowerManager.authorized(player)) {
			stopSwordToggles(player);
			tickUnauthorized(player, serverTick);
			return;
		}

		UNAUTHORIZED_CARRIERS.remove(player.getUUID());
		LAST_GUARDIAN_SUMMON.remove(player.getUUID());
		tickSwordToggles(player, serverTick);
		if (serverTick % 20 == 0) {
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			boolean changed = data.regenerateEnergy(ShadowSwordRules.regenerationPerSecond(data.darknessLevel()));
			changed |= drainSwordToggles(player, data);
			if (changed) PowersPackets.syncTo(player);
		}
	}

	private static void tickUnauthorized(ServerPlayer player, long tick) {
		UUID playerId = player.getUUID();
		boolean newlyCarrying = UNAUTHORIZED_CARRIERS.add(playerId);
		if (tick % 20 == 0 || newlyCarrying) {
			player.addEffect(PowerStatusEffects.hidden(MobEffects.BLINDNESS, 60, 1, false, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 60, 2, false, true));
		}
		long lastSummon = LAST_GUARDIAN_SUMMON.getOrDefault(playerId, Long.MIN_VALUE / 2);
		if (!newlyCarrying && tick - lastSummon < 200) return;
		int nearby = ShadowSwordWorldActions.nearbyGuardians(player, 32.0);
		int count = ShadowSwordRules.protectorsToSummon(nearby);
		if (count > 0 && ShadowSwordWorldActions.summonGuardians(player, player, count) > 0) {
			LAST_GUARDIAN_SUMMON.put(playerId, tick);
		}
	}

	private static void tickSwordToggles(ServerPlayer player, int serverTick) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (ShadowSwordPowerManager.Action action : ShadowSwordPowerManager.actions()) {
			if (action.ability().isToggle() && data.isToggleActive(
					ShadowSwordPowerManager.toggleKey(action))
					&& serverTick % Math.max(1, action.ability().activeTickInterval()) == 0) {
				action.ability().tickActive(player, data);
			}
		}
	}

	private static boolean drainSwordToggles(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		boolean changed = false;
		for (ShadowSwordPowerManager.Action action : ShadowSwordPowerManager.actions()) {
			String key = ShadowSwordPowerManager.toggleKey(action);
			if (!action.ability().isToggle() || !data.isToggleActive(key)) continue;
			int cost = PowerEnergy.ongoingCost(player, action.ability());
			if (cost > 0 && !data.consumeEnergy(cost)) {
				action.ability().activateToggleOff(player, data);
				data.setToggleActive(player, key, false);
				changed = true;
				ServerLevel level = (ServerLevel) player.level();
				PowerFx.cancelled(level, player.position().add(0.0, 1.0, 0.0), 0x3A0B52);
				PowerFx.burst(level, player.position().add(0.0, 1.0, 0.0),
						ParticleTypes.REVERSE_PORTAL, 14, 0.5, 0.03);
			}
		}
		return changed;
	}

	private static void stopSwordToggles(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (ShadowSwordPowerManager.Action action : ShadowSwordPowerManager.actions()) {
			String key = ShadowSwordPowerManager.toggleKey(action);
			if (!action.ability().isToggle() || !data.isToggleActive(key)) continue;
			action.ability().activateToggleOff(player, data);
			data.setToggleActive(player, key, false);
		}
	}

	public static void forget(ServerPlayer player) {
		UNAUTHORIZED_CARRIERS.remove(player.getUUID());
		LAST_GUARDIAN_SUMMON.remove(player.getUUID());
	}

	public static void clear() {
		UNAUTHORIZED_CARRIERS.clear();
		LAST_GUARDIAN_SUMMON.clear();
	}
}
