package com.powers.gametest;

import com.powers.player.PlayerPowers;
import com.powers.power.abilities.FlightAbility;
import com.powers.power.abilities.FlightRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Live server proof for PWR-006 survival propulsion and bounded stale-input braking. */
public final class FlightGameTests {
	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void pwr006MomentumFlightRemainsServerOwnedAndBoundsClientIntent(
			GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		GameType beforeMode = player.gameMode();
		var data = PlayerPowers.get(player);
		var flight = new FlightAbility();
		helper.assertTrue(flight.activateToggleOn(player, data), "Flight did not activate");
		Vec3 before = new Vec3(3.5, 0.6, 0.0);
		player.setDeltaMovement(before);

		flight.tickActive(player, data);
		Vec3 after = player.getDeltaMovement();
		helper.assertTrue(after.distanceTo(before) <= Math.sqrt(2.0) *
				FlightRules.MAX_AIR_ACCELERATION + 0.0001,
				"One retained client-input tick exceeded the server acceleration bound");
		helper.assertTrue(Double.isFinite(after.x) && Double.isFinite(after.y)
				&& Double.isFinite(after.z), "Flight produced invalid server velocity");
		helper.assertTrue(player.gameMode() == beforeMode,
				"Flight propulsion changed the player's game mode");
		flight.activateToggleOff(player, data);
		helper.assertTrue(data.flightSnapshot() == -1, "Flight cleanup left session state behind");
		helper.succeed();
	}
}
