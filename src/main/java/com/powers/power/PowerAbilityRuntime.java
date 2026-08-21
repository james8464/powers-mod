package com.powers.power;

import com.powers.magic.runtime.MagicRayCollisionRuntime;
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
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.power.crystals.InfernoAbility;
import com.powers.power.crystals.SizeShiftAbility;
import com.powers.power.crystals.SoulLinkAbility;
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
		// A resource reload can briefly leave a saved slot unresolved while its
		// authoritative toggle key remains. Death must never preserve that ownership.
		for (String key : java.util.List.copyOf(data.getActiveToggles())) {
			data.setToggleActive(player, key, false);
		}
	}

	/** Clears every active ability session owned by a disconnecting player. */
	public static void onDisconnect(MinecraftServer server, ServerPlayer player) {
		clearPlayerState(server, player);
		com.powers.testing.TestingOverrides.clear(player.getUUID());
	}

	/** Compensates partial ability work without clearing unrelated power families. */
	public static void rollbackFailedActivation(ServerPlayer player, String abilityId) {
		MinecraftServer server = player.level().getServer();
		UUID owner = player.getUUID();
		switch (abilityId) {
			case "time_shift" -> {
				TeleportAbility.clearMarking(player);
				TeleportAbility.clearStorm(server, owner);
			}
			case "time_freeze" -> TimeFreezeToggleAbility.clear(server, owner);
			case "forcefield" -> ForcefieldAbility.clear(owner);
			case "gravity_displacement" -> GravityDisplacementAbility.clear(server, owner);
			case "breezy_bash" -> BreezyBashAbility.clear(server, owner);
			case "fireball" -> FireballAbility.clear(server, owner);
			case "lightning_strike" -> LightningStrikeAbility.clear(server, owner);
			case "starfall" -> StarfallAbility.clear(server, owner);
			case "super_speed" -> SuperSpeedAbility.clear(server, owner);
			case "vessel_possession" -> VesselPossessionAbility.clear(player);
			case "astral_projection" -> AstralProjectionAbility.clear(server, owner);
			case "speed_burst" -> SpeedBurstAbility.clear(owner);
			case "energy_beam" -> EnergyBeamAbility.clear(owner);
			case "void_beam" -> VoidBeamAbility.clear(owner);
			case "energy_drain" -> EnergyDrainAbility.clear(owner);
			default -> {
				// Instant stateless abilities have nothing to compensate.
			}
		}
	}

	/** One complete lifecycle boundary prevents UUID-keyed casts leaking onto replacement entities. */
	private static void clearPlayerState(MinecraftServer server, ServerPlayer player) {
		UUID owner = player.getUUID();
		TeleportAbility.clearMarking(player);
		TeleportAbility.clearStorm(server, owner);
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
		AstralProjectionAbility.clear(server, owner);
		DreamwalkingAbility.clear(player);
		InfernoAbility.clear(owner);
		SoulLinkAbility.clear(owner);
		SizeShiftAbility.clear(player);
		SpeedBurstAbility.clear(owner);
		EnergyBeamAbility.clear(owner);
		VoidBeamAbility.clear(owner);
		MagicRayCollisionRuntime.clear(owner);
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
		TeleportAbility.clearAllMarking(server);
		TeleportAbility.clearAllStorms(server);
		TimeFreezeToggleAbility.clearAll(server);
		ForcefieldAbility.clearAll();
		GravityDisplacementAbility.clearAll(server);
		BreezyBashAbility.clearAll(server);
		FireballAbility.clearAll(server);
		LightningStrikeAbility.clearAll(server);
		StarfallAbility.clearAll(server);
		SuperSpeedAbility.clearAll(server);
		VesselPossessionAbility.clearAll();
		AstralProjectionAbility.clearAll(server);
		EnergyDrainAbility.clearAll();
		CrystalPowerRegistry.clearAllSelections();
		com.powers.power.crystals.MindscapeCrystalAbility.clearAll(server);
		EntityFreezeController.clearAll();
		DreamwalkingAbility.clearAll(server);
		InfernoAbility.clearAll();
		SoulLinkAbility.clearAll();
		SizeShiftAbility.clearAll();
		SpeedBurstAbility.clearAll();
		EnergyBeamAbility.clearAll();
		VoidBeamAbility.clearAll();
		MagicRayCollisionRuntime.clearAll();
	}

	/** Advances every ability with persistent server-owned state exactly once per tick. */
	public static void tick(MinecraftServer server) {
		GlobalTimeStopManager.tick(server);
		MagicRayCollisionRuntime.tick(server.getTickCount());
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
		DreamwalkingAbility.tickAll(server);
		SizeShiftAbility.tickAll(server);
	}

	/** Refreshes defensive auras on the shared five-tick cadence. */
	public static void tickFrequent(MinecraftServer server) {
		ForcefieldAbility.tickAll(server);
	}

	/** Advances the teleport marking session after body and field managers settle. */
	public static void tickTeleportMarking(MinecraftServer server) {
		TeleportAbility.tickMarking(server);
	}

	/** Returns whether the player is currently operating away from their physical body. */
	public static boolean usesDetachedBody(UUID owner) {
		return TeleportAbility.isMarking(owner) || AstralProjectionAbility.isActive(owner);
	}
}
