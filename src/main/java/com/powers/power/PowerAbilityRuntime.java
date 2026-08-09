package com.powers.power;

import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.BreezyBashAbility;
import com.powers.power.abilities.EnergyBeamAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.FireballAbility;
import com.powers.power.abilities.GravityDisplacementAbility;
import com.powers.power.abilities.SlowWorldAbility;
import com.powers.power.abilities.SpeedBurstAbility;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Coordinates transient active-power state at server and player lifecycle boundaries. */
public final class PowerAbilityRuntime {
	private PowerAbilityRuntime() {
	}

	/** Clears state that cannot follow a player entity across a respawn replacement. */
	public static void afterRespawn(MinecraftServer server, UUID oldOwner) {
		GravityDisplacementAbility.clear(server, oldOwner);
		BreezyBashAbility.clear(server, oldOwner);
		FireballAbility.clear(server, oldOwner);
		SuperSpeedAbility.clear(server, oldOwner);
		SpeedBurstAbility.clear(oldOwner);
		EnergyBeamAbility.clear(oldOwner);
		VoidBeamAbility.clear(oldOwner);
	}

	/** Clears every active ability session owned by a disconnecting player. */
	public static void onDisconnect(MinecraftServer server, ServerPlayer player) {
		UUID owner = player.getUUID();
		TeleportAbility.clearMarking(player);
		TimeFreezeToggleAbility.clear(owner);
		ForcefieldAbility.clear(owner);
		GravityDisplacementAbility.clear(server, owner);
		BreezyBashAbility.clear(server, owner);
		FireballAbility.clear(server, owner);
		SuperSpeedAbility.clear(server, owner);
		VesselPossessionAbility.clear(player);
		AstralProjectionAbility.clear(owner);
		DreamwalkingAbility.clear(player);
		ChronoStopAbility.clear(owner);
		InfernoAbility.clear(owner);
		SoulLinkAbility.clear(owner);
		SizeShiftAbility.clear(owner);
		SlowWorldAbility.clear(owner);
		SpeedBurstAbility.clear(owner);
		EnergyBeamAbility.clear(owner);
		VoidBeamAbility.clear(owner);
		SpaceTimeAbility.clear(owner);
		EnergyDrainAbility.clear(owner);
	}

	/** Releases all server-owned ability state before world references are discarded. */
	public static void onServerStopped(MinecraftServer server) {
		TeleportAbility.clearAllMarking();
		TimeFreezeToggleAbility.clearAll();
		ForcefieldAbility.clearAll();
		GravityDisplacementAbility.clearAll(server);
		BreezyBashAbility.clearAll(server);
		FireballAbility.clearAll(server);
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
		SlowWorldAbility.clearAll();
		SpeedBurstAbility.clearAll();
		EnergyBeamAbility.clearAll();
		VoidBeamAbility.clearAll();
	}

	/** Advances every ability with persistent server-owned state exactly once per tick. */
	public static void tick(MinecraftServer server) {
		SlowWorldAbility.tickAll(server);
		VesselPossessionAbility.tickAll(server);
		AstralProjectionAbility.tickAll(server);
		EnergyDrainAbility.tickAll(server);
		SpeedBurstAbility.tickAll(server);
		EnergyBeamAbility.tickAll(server);
		VoidBeamAbility.tickAll(server);
		GravityDisplacementAbility.tickAll(server);
		BreezyBashAbility.tickAll(server);
		FireballAbility.tickAll(server);
		SuperSpeedAbility.tickAll(server);
		SpaceTimeAbility.tickAll(server);
		DreamwalkingAbility.tickAll(server);
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
		return TeleportAbility.MARKING.containsKey(owner) || AstralProjectionAbility.isActive(owner);
	}
}
