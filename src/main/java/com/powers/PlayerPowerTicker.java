package com.powers;

import com.powers.companion.PrivateCompanionManager;
import com.powers.fx.GodlyPunishment;
import com.powers.item.ArtifactInventoryRuntime;
import com.powers.item.ImportedArtifactRuntime;
import com.powers.mind.BodyProxyManager;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.PlayerTickCadence;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.Power;
import com.powers.power.PowerAbilityRuntime;
import com.powers.power.PowerEnergy;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.progression.PowerScalingService;
import com.powers.realm.RealmDimensionRules;
import com.powers.util.PowerMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns the single bounded per-player gameplay pass and its session-only state. */
final class PlayerPowerTicker {
	private static final Map<UUID, Boolean> WAS_SLEEPING = new HashMap<>();

	private PlayerPowerTicker() {
	}

	static void tick(ServerPlayer player, int tick, PlayerTickCadence cadence) {
		enforceRealmGamemode(player);
		// The server-end callback still runs while Minecraft's global tick is frozen.
		if (!GlobalTimeStopManager.mayAct(player)) {
			ArtifactInventoryRuntime.reconcileOwnership(player);
			PrivateCompanionManager.reconcileEligibility(player);
			return;
		}
		if (cadence.passiveRefresh()) PowersPackets.syncTo(player);
		ArtifactInventoryRuntime.tickPlayer(player, tick);
		ImportedArtifactRuntime.tickPlayer(player, tick);
		PrivateCompanionManager.tickPlayer(player, tick);
		tickToggles(player, tick);

		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean sleeping = player.isSleeping();
		boolean wasSleeping = WAS_SLEEPING.getOrDefault(player.getUUID(), false);
		WAS_SLEEPING.put(player.getUUID(), sleeping);
		if (cadence.passiveRefresh()) {
			SkillSystem.syncPathVisibility(player);
			SkillSystem.refresh(player);
		}
		if (wasSleeping && !sleeping) {
			data.restoreEnergy();
			PowersPackets.syncTo(player);
		} else if (cadence.second()) {
			int regen = 1;
			if (SkillSystem.hasDarknessTag(player)) {
				boolean inDarkRealm = SkillSystem.isDarkRealm(player.level().dimension());
				long timeOfDay = Math.floorMod(player.level().getDefaultClockTime(), 24_000L);
				boolean night = timeOfDay >= 13_000L || timeOfDay < 2_300L;
				regen = PowerEnergy.darknessRegen(inDarkRealm || night);
			}
			if (data.regenerateEnergy(PowerScalingService.regeneration(player, regen))) {
				PowersPackets.syncTo(player);
			}
		}
		if (cadence.fiveTick()) {
			if (cadence.second()) AmethystDampening.update(player);
			drainExhaustionEnergy(player);
		}
		if (cadence.second()) drainToggleEnergy(player);
	}

	static void forget(UUID playerId) {
		WAS_SLEEPING.remove(playerId);
	}

	static void clear() {
		WAS_SLEEPING.clear();
	}

	private static void enforceRealmGamemode(ServerPlayer player) {
		if (PowerAbilityRuntime.usesDetachedBody(player.getUUID())) return;
		boolean inRealm = RealmDimensionRules.isMindscape(
				player.level().dimension().identifier().toString());
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		GameType previous = data.previousGameMode();
		if (inRealm) {
			// Never snapshot forced adventure or a relog would erase the real mode.
			if (previous == null && player.gameMode() != GameType.ADVENTURE) {
				data.setPreviousGameMode(player.gameMode());
			}
			if (player.gameMode() != GameType.ADVENTURE) player.setGameMode(GameType.ADVENTURE);
		} else if (previous != null) {
			data.setPreviousGameMode(null);
			if (player.gameMode() == GameType.ADVENTURE) player.setGameMode(previous);
		}
	}

	private static void tickToggles(ServerPlayer player, int serverTick) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) continue;
			Ability ability = power.ability();
			if (ability != null && ability.isToggle() && data.isToggleActive(power.id().toString())
					&& serverTick % Math.max(1, ability.activeTickInterval()) == 0) {
				ability.tickActive(player, data);
			}
		}
	}

	private static void drainToggleEnergy(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean drainedOut = false;
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null || power.ability() == null || !power.ability().isToggle()
					|| !data.isToggleActive(power.id().toString())) continue;
			int cost = PowerEnergy.ongoingCost(player, power.ability());
			if (cost > 0 && !data.consumeEnergy(cost)) {
				power.ability().activateToggleOff(player, data);
				data.setToggleActive(player, power.id().toString(), false);
				drainedOut = true;
			}
		}
		if (drainedOut) {
			energyBacklash(player);
			PowersPackets.syncTo(player);
		}
	}

	private static void drainExhaustionEnergy(ServerPlayer player) {
		MobEffectInstance exhaustion = player.getEffect(PowersEffects.EXHAUSTION);
		if (exhaustion == null) return;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int drain = Math.max(1, data.energyCapacity() / 20) * (1 + exhaustion.getAmplifier());
		int before = data.energy();
		data.drainEnergy(drain);
		if (data.energy() != before) PowersPackets.syncTo(player);
	}

	private static void energyBacklash(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		if (player.isAlive()) {
			player.hurtServer(level, player.damageSources().magic(), player.getMaxHealth() * 0.7F);
		}
		GodlyPunishment.strike(level, player, 0xFFD700, true);
		PowerMessages.sendImportant(player, "energy.powers.backlash", 6);
	}
}
