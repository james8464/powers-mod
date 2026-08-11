package com.powers.spell;

import com.powers.PowersMod;
import com.powers.force.LivingForceKind;
import com.powers.force.LivingForceManager;
import com.powers.realm.RealmEventRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Immutable server-observed facts revealed by Augury. */
public record AuguryReport(Weather weather, String moon, long ticksUntilRealmEvent,
		boolean darknessNear, boolean pureLightNear) {
	public enum Weather { CLEAR, RAIN, THUNDER }

	public static AuguryReport create(ServerLevel level, BlockPos origin) {
		long day = level.getOverworldClockTime();
		int moon = (int) Math.floorMod(day / 24_000L, 8L);
		boolean realm = level.dimension().identifier().equals(PowersMod.id("dark_realm"))
				|| level.dimension().identifier().equals(PowersMod.id("light_realm"));
		return new AuguryReport(weather(level.isRaining(), level.isThundering()), moonName(moon),
				realm ? ticksUntilRealmEvent(level.getGameTime()) : -1L,
				LivingForceManager.isNearForce(level, origin, 64, LivingForceKind.DARKNESS),
				LivingForceManager.isNearForce(level, origin, 64, LivingForceKind.PURE_LIGHT));
	}

	public static Weather weather(boolean raining, boolean thundering) {
		if (thundering) return Weather.THUNDER;
		return raining ? Weather.RAIN : Weather.CLEAR;
	}

	public static String moonName(int phase) {
		return switch (Math.floorMod(phase, 8)) {
			case 0 -> "full";
			case 1 -> "waning_gibbous";
			case 2 -> "last_quarter";
			case 3 -> "waning_crescent";
			case 4 -> "new";
			case 5 -> "waxing_crescent";
			case 6 -> "first_quarter";
			default -> "waxing_gibbous";
		};
	}

	public static long ticksUntilRealmEvent(long gameTime) {
		long phase = Math.floorMod(gameTime, RealmEventRules.CYCLE_TICKS);
		if (phase < RealmEventRules.EVENT_START_TICK) {
			return RealmEventRules.EVENT_START_TICK - phase;
		}
		if (phase == RealmEventRules.EVENT_START_TICK) return 0L;
		return RealmEventRules.CYCLE_TICKS - phase + RealmEventRules.EVENT_START_TICK;
	}
}
