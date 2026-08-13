package com.powers.spell;

import com.powers.fx.CelestialRuinFx;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Maintains the visible Heavenfall omen while bounded impact chunks load. */
final class CelestialRuinWarningRuntime {
	private CelestialRuinWarningRuntime() {
	}

	/** Returns whether detonation must wait after refreshing any due warning beam. */
	static boolean awaitingChunks(ServerLevel level, BlockPos center, int countdownRemaining,
			boolean detonated, boolean chunksReady) {
		if (chunksReady) return false;
		if (CelestialRuinStagingRules.shouldSustainWarning(
				countdownRemaining, detonated, false) && level.getGameTime() % 10 == 0) {
			int holdAge = CelestialRuinRules.COUNTDOWN_TICKS + (int) (level.getGameTime() % 200L);
			CelestialRuinFx.beam(level, Vec3.atCenterOf(center), CelestialRuinRules.BEAM_RADIUS, holdAge);
		}
		return true;
	}

	static void warnCaster(ServerLevel level, UUID caster, int elapsed) {
		int remainingSeconds = (CelestialRuinRules.COUNTDOWN_TICKS - elapsed + 19) / 20;
		if (remainingSeconds != 60 && remainingSeconds != 30 && remainingSeconds != 10
				&& remainingSeconds > 5 || elapsed % 20 != 0) return;
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(caster);
		if (player != null) {
			PowerMessages.overlay(player, Component.translatable(
					"spell.powers.celestial_ruin_countdown", remainingSeconds));
		}
	}
}
