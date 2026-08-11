package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import com.powers.power.state.GlobalTimeStopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Innate global Time Stop. Vanilla's server tick manager suspends entities,
 * projectiles, block entities and scheduled ticks in every loaded dimension;
 * the owning player alone remains authorized to act.
 */
public class TimeFreezeToggleAbility extends ToggleAbility {
	public TimeFreezeToggleAbility() {
		super(PowersMod.id("time_freeze"),
				Component.translatable("ability.powers.time_freeze"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		double mspt = player.level().getServer().getAverageTickTimeNanos() / 1_000_000.0;
		TimeFreezeDrainRules.Forecast forecast = TimeFreezeDrainRules.forecast(
				data.energy(), data.energyCapacity(), mspt);
		player.sendSystemMessage(Component.translatable(
				"ability.powers.time_freeze.forecast",
				forecast.energyPerSecond(), forecast.safeSeconds()));
		if (forecast.lowTpsWarning()) {
			player.sendSystemMessage(Component.translatable(
					"ability.powers.time_freeze.low_tps", String.format(
							java.util.Locale.ROOT, "%.1f", mspt)));
		}
		// Load is advisory only; clock ownership remains the sole activation policy here.
		return GlobalTimeStopManager.start(player);
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		GlobalTimeStopManager.stop(player);
	}

	/** Called on disconnect/respawn so one player's freeze cannot outlive them. */
	public static void clear(MinecraftServer server, UUID owner) {
		GlobalTimeStopManager.clear(server, owner);
	}

	/** Called during shutdown to restore the global clock before state is dropped. */
	public static void clearAll(MinecraftServer server) {
		GlobalTimeStopManager.clearAll(server);
	}
}
