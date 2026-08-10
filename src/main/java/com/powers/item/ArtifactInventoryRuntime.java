package com.powers.item;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.PowerEnergy;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.power.artifact.ArtifactCovenantManager;
import com.powers.power.artifact.ArtifactDecreeManager;
import com.powers.power.artifact.ArtifactFieldManager;
import com.powers.power.artifact.ArtifactGateManager;
import com.powers.power.artifact.ArtifactGroundWorkQueue;
import com.powers.power.artifact.ArtifactGuardianSummons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Consolidates artifact auras, curses, regeneration, toggles, and lifecycle cleanup. */
public final class ArtifactInventoryRuntime {
	private static final Map<ArtifactAlignment, Map<UUID, Long>> LAST_GUARDIAN =
			new EnumMap<>(ArtifactAlignment.class);

	static {
		for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
			LAST_GUARDIAN.put(alignment, new HashMap<>());
		}
	}

	private ArtifactInventoryRuntime() {
	}

	public static void tickPlayer(ServerPlayer player, int serverTick) {
		for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
			boolean carries = ArtifactWeaponManager.carries(player, alignment);
			if (!carries || !ArtifactWeaponManager.authorized(player, alignment)) {
				stopToggles(player, alignment);
				if (carries) tickUnauthorized(player, alignment, serverTick);
				else LAST_GUARDIAN.get(alignment).remove(player.getUUID());
				continue;
			}
			LAST_GUARDIAN.get(alignment).remove(player.getUUID());
			tickAuthorized(player, alignment, serverTick);
		}
	}

	private static void tickAuthorized(ServerPlayer player, ArtifactAlignment alignment, int tick) {
		tickToggles(player, alignment, tick);
		if (tick % 10 == 0) {
			ServerLevel level = (ServerLevel) player.level();
			PowerFx.burst(level, player.position().add(0.0, 1.0, 0.0),
					alignment == ArtifactAlignment.DARKNESS ? ParticleTypes.REVERSE_PORTAL
							: ParticleTypes.END_ROD,
					ArtifactWeaponManager.rank(player, alignment) >= 10 ? 2 : 1, 0.25, 0.01);
		}
		if (tick % 20 != 0) return;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int rank = ArtifactWeaponManager.rank(player, alignment);
		int regeneration = alignment == ArtifactAlignment.DARKNESS
				? rank >= 10 ? 900 : 80 + rank * 35
				: rank >= 10 ? 300 : 40 + rank * 15;
		boolean changed = data.regenerateEnergy(regeneration);
		changed |= drainToggles(player, alignment, data);
		if (changed) PowersPackets.syncTo(player);
	}

	private static void tickUnauthorized(ServerPlayer player, ArtifactAlignment alignment, int tick) {
		if (tick % 20 == 0) {
			if (alignment == ArtifactAlignment.DARKNESS) {
				player.addEffect(PowerStatusEffects.hidden(MobEffects.BLINDNESS, 60, 1, false, true));
				player.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 60, 2, false, true));
			} else {
				player.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 60, 0, false, true));
				player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), 8.0F);
			}
		}
		Map<UUID, Long> summons = LAST_GUARDIAN.get(alignment);
		long last = summons.getOrDefault(player.getUUID(), -1_000L);
		if (tick - last < 200) return;
		if (ArtifactGuardianSummons.summon(player, alignment, 2, false, player, false) > 0) {
			summons.put(player.getUUID(), (long) tick);
		}
	}

	private static void tickToggles(ServerPlayer player, ArtifactAlignment alignment, int tick) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (ArtifactWeaponManager.Action action : ArtifactWeaponManager.actions(alignment)) {
			String key = ArtifactWeaponManager.toggleKey(action);
			if (action.ability().isToggle() && data.isToggleActive(key)
					&& tick % Math.max(1, action.ability().activeTickInterval()) == 0) {
				action.ability().tickActive(player, data);
			}
		}
	}

	private static boolean drainToggles(ServerPlayer player, ArtifactAlignment alignment,
			PlayerPowers.PlayerPowersData data) {
		boolean changed = false;
		for (ArtifactWeaponManager.Action action : ArtifactWeaponManager.actions(alignment)) {
			String key = ArtifactWeaponManager.toggleKey(action);
			if (!action.ability().isToggle() || !data.isToggleActive(key)) continue;
			int cost = PowerEnergy.ongoingCost(player, action.ability());
			if (cost > 0 && !data.consumeEnergy(cost)) {
				action.ability().activateToggleOff(player, data);
				data.setToggleActive(player, key, false);
				changed = true;
			}
		}
		return changed;
	}

	private static void stopToggles(ServerPlayer player, ArtifactAlignment alignment) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (ArtifactWeaponManager.Action action : ArtifactWeaponManager.actions(alignment)) {
			String key = ArtifactWeaponManager.toggleKey(action);
			if (!action.ability().isToggle() || !data.isToggleActive(key)) continue;
			action.ability().activateToggleOff(player, data);
			data.setToggleActive(player, key, false);
		}
	}

	public static void tickServer(MinecraftServer server) {
		ArtifactDecreeManager.tick(server);
		ArtifactFieldManager.tick(server);
		ArtifactGateManager.tick(server);
		ArtifactGroundWorkQueue.tick(server);
	}

	public static void forget(ServerPlayer player) {
		UUID playerId = player.getUUID();
		LAST_GUARDIAN.values().forEach(map -> map.remove(playerId));
		ArtifactDecreeManager.forget(playerId);
		ArtifactFieldManager.forget(playerId);
		ArtifactGateManager.forget(playerId);
		ArtifactGroundWorkQueue.forget(playerId);
		ArtifactGuardianSummons.forget(playerId);
		ArtifactDeathWardManager.forget(playerId);
		ArtifactCovenantManager.forget(playerId);
	}

	public static void clear() {
		LAST_GUARDIAN.values().forEach(Map::clear);
		ArtifactDecreeManager.clear();
		ArtifactFieldManager.clear();
		ArtifactGateManager.clear();
		ArtifactGroundWorkQueue.clear();
		ArtifactGuardianSummons.clear();
		ArtifactDeathWardManager.clear();
		ArtifactCovenantManager.clear();
	}
}
