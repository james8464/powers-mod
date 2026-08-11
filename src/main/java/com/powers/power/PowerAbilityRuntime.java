package com.powers.power;

import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.BreezyBashAbility;
import com.powers.power.abilities.EnergyBeamAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.FireballAbility;
import com.powers.power.abilities.GravityDisplacementAbility;
import com.powers.power.abilities.LightningStrikeAbility;
import com.powers.power.abilities.SpeedBurstAbility;
import com.powers.power.abilities.StarfallAbility;
import com.powers.power.abilities.SuperSpeedAbility;
import com.powers.power.abilities.TeleportAbility;
import com.powers.power.abilities.TimeFreezeToggleAbility;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.power.abilities.VoidBeamAbility;
import com.powers.power.crystals.ChronoStopAbility;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.power.crystals.InfernoAbility;
import com.powers.power.crystals.SizeShiftAbility;
import com.powers.power.crystals.SoulLinkAbility;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.player.PlayerPowers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Coordinates transient active-power state at server and player lifecycle boundaries. */
public final class PowerAbilityRuntime {
	private PowerAbilityRuntime() {
	}

	/** Clears state that cannot follow a player entity across a respawn replacement. */
	public static void afterRespawn(MinecraftServer server, ServerPlayer oldPlayer,
			ServerPlayer newPlayer) {
		clearPlayerState(server, oldPlayer);
		clearRuntimeOnlyToggles(newPlayer);
	}

	/** Death ends every innate toggle so a replacement body cannot silently reactivate it. */
	public static void deactivateToggles(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null || power.ability() == null || !power.ability().isToggle()) continue;
			String key = power.id().toString();
			if (!data.isToggleActive(key)) continue;
			power.ability().activateToggleOff(player, data);
			data.setToggleActive(player, key, false);
		}
	}

	/** Clears every active ability session owned by a disconnecting player. */
	public static void onDisconnect(MinecraftServer server, ServerPlayer player) {
		clearPlayerState(server, player);
		com.powers.testing.TestingOverrides.clear(player.getUUID());
	}

	/** One complete lifecycle boundary prevents UUID-keyed casts leaking onto replacement entities. */
	private static void clearPlayerState(MinecraftServer server, ServerPlayer player) {
		UUID owner = player.getUUID();
		TeleportAbility.clearMarking(player);
		TeleportAbility.clearStorm(owner);
		TimeFreezeToggleAbility.clear(server, owner);
		clearRuntimeOnlyToggles(player);
		ForcefieldAbility.clear(owner);
		GravityDisplacementAbility.clear(server, owner);
		BreezyBashAbility.clear(server, owner);
		FireballAbility.clear(server, owner);
		LightningStrikeAbility.clear(server, owner);
		StarfallAbility.clear(server, owner);
		SuperSpeedAbility.clear(server, owner);
		VesselPossessionAbility.clear(player);
		AstralProjectionAbility.clear(owner);
		DreamwalkingAbility.clear(player);
		ChronoStopAbility.clear(owner);
		InfernoAbility.clear(owner);
		SoulLinkAbility.clear(owner);
		SizeShiftAbility.clear(player);
		SpeedBurstAbility.clear(owner);
		EnergyBeamAbility.clear(owner);
		VoidBeamAbility.clear(owner);
		SpaceTimeAbility.clear(owner);
		EnergyDrainAbility.clear(owner);
	}

	/** Global clock ownership cannot survive a player-entity or connection replacement. */
	private static void clearRuntimeOnlyToggles(ServerPlayer player) {
		PlayerPowers.get(player).clearToggleOwnership(player,
				com.powers.PowersMod.id("time_freeze"));
	}

	/** Releases all server-owned ability state before world references are discarded. */
	public static void onServerStopped(MinecraftServer server) {
		com.powers.mind.ParticipantPowerLock.clear();
		com.powers.testing.TestingOverrides.clearAll();
		com.powers.entity.TestActorPowerState.clearAll();
		TeleportAbility.clearAllMarking();
		TeleportAbility.clearAllStorms();
		TimeFreezeToggleAbility.clearAll(server);
		ForcefieldAbility.clearAll();
		GravityDisplacementAbility.clearAll(server);
		BreezyBashAbility.clearAll(server);
		FireballAbility.clearAll(server);
		LightningStrikeAbility.clearAll(server);
		StarfallAbility.clearAll(server);
		SuperSpeedAbility.clearAll(server);
		VesselPossessionAbility.clearAll();
		AstralProjectionAbility.clearAll();
		EnergyDrainAbility.clearAll();
		SpaceTimeAbility.clearAll();
		CrystalPowerRegistry.clearAllSelections();
		EntityFreezeController.clearAll();
		DreamwalkingAbility.clearAll(server);
		ChronoStopAbility.clearAll();
		InfernoAbility.clearAll();
		SoulLinkAbility.clearAll();
		SizeShiftAbility.clearAll();
		SpeedBurstAbility.clearAll();
		EnergyBeamAbility.clearAll();
		VoidBeamAbility.clearAll();
	}

	/** Advances every ability with persistent server-owned state exactly once per tick. */
	public static void tick(MinecraftServer server) {
		GlobalTimeStopManager.tick(server);
		VesselPossessionAbility.tickAll(server);
		AstralProjectionAbility.tickAll(server);
		EnergyDrainAbility.tickAll(server);
		SpeedBurstAbility.tickAll(server);
		EnergyBeamAbility.tickAll(server);
		VoidBeamAbility.tickAll(server);
		GravityDisplacementAbility.tickAll(server);
		BreezyBashAbility.tickAll(server);
		FireballAbility.tickAll(server);
		LightningStrikeAbility.tickAll(server);
		StarfallAbility.tickAll(server);
		SuperSpeedAbility.tickAll(server);
		SpaceTimeAbility.tickAll(server);
		DreamwalkingAbility.tickAll(server);
		SizeShiftAbility.tickAll(server);
	}

	/** Refreshes defensive auras on the shared five-tick cadence. */
	public static void tickFrequent(MinecraftServer server) {
		ForcefieldAbility.tickAll(server);
	}

	/** Advances the teleport marking session after body and field managers settle. */
	public static void tickTeleportMarking() {
		TeleportAbility.tickMarking();
	}

	/** Returns whether the player is currently operating away from their physical body. */
	public static boolean usesDetachedBody(UUID owner) {
		return TeleportAbility.isMarking(owner) || AstralProjectionAbility.isActive(owner);
	}
}
