package com.powers.spell;

import com.powers.PowersMod;
import com.powers.force.LivingForceKind;
import com.powers.force.LoadedFrontForecast;
import com.powers.realm.RealmEventType;
import com.powers.realm.RealmKind;
import com.powers.realm.RealmLayout;
import com.powers.realm.RealmEventRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Immutable, explicitly loaded-only server facts revealed by Augury. */
public record AuguryReport(Weather weather, String moon, RealmForecast realm,
		LoadedFrontForecast darknessFront, LoadedFrontForecast pureLightFront, RitualWindow ritualWindow,
		Uncertainty uncertainty) {
	private static final int FORCE_FORECAST_RADIUS = 96;

	public enum Weather { CLEAR, RAIN, THUNDER }
	public enum RitualSafety {
		PROVISIONAL_WINDOW, EVENT_HAZARD, LOADED_FRONT_HAZARD, INDETERMINATE_SAMPLE
	}
	public enum Uncertainty { LOADED_CHUNKS_ONLY }
	public record RealmForecast(RealmEventType event, boolean active, int severityTier,
			long ticksUntilTransition) { }
	public record RitualWindow(RitualSafety safety, long ticksUntilTransition,
			Uncertainty uncertainty) { }

	public static AuguryReport create(ServerLevel level, BlockPos origin) {
		long day = level.getOverworldClockTime();
		int moon = (int) Math.floorMod(day / 24_000L, 8L);
		RealmKind kind = realmKind(level);
		double dx = origin.getX() + 0.5 - RealmLayout.ENTRY_X;
		double dz = origin.getZ() + 0.5 - RealmLayout.ENTRY_Z;
		RealmForecast realm = realmForecast(kind, level.getGameTime(), Math.hypot(dx, dz));
		LoadedFrontForecast darkness = LoadedFrontForecast.read(
				level, origin, FORCE_FORECAST_RADIUS, LivingForceKind.DARKNESS);
		LoadedFrontForecast light = LoadedFrontForecast.read(
				level, origin, FORCE_FORECAST_RADIUS, LivingForceKind.PURE_LIGHT);
		return new AuguryReport(weather(level.isRaining(), level.isThundering()), moonName(moon),
				realm, darkness, light, ritualWindow(realm, darkness, light),
				Uncertainty.LOADED_CHUNKS_ONLY);
	}

	private static RealmKind realmKind(ServerLevel level) {
		if (level.dimension().identifier().equals(PowersMod.id("dark_realm"))) return RealmKind.DARK;
		if (level.dimension().identifier().equals(PowersMod.id("light_realm"))) return RealmKind.LIGHT;
		return null;
	}

	public static RealmForecast realmForecast(RealmKind kind, long gameTime, double distance) {
		if (kind == null) return new RealmForecast(RealmEventType.NONE, false, 0, -1L);
		long phase = Math.floorMod(gameTime, RealmEventRules.CYCLE_TICKS);
		boolean active = RealmEventRules.eventAt(kind, gameTime) != RealmEventType.NONE;
		RealmEventType event = kind == RealmKind.DARK
				? RealmEventType.DARK_ECLIPSE : RealmEventType.WHITEOUT;
		long untilTransition = active ? RealmEventRules.CYCLE_TICKS - phase
				: RealmEventRules.EVENT_START_TICK - phase;
		return new RealmForecast(event, active,
				RealmEventRules.pressureTier(distance, true), untilTransition);
	}

	public static RitualWindow ritualWindow(RealmForecast realm,
			LoadedFrontForecast darkness, LoadedFrontForecast pureLight) {
		if (realm.active()) {
			return new RitualWindow(RitualSafety.EVENT_HAZARD, realm.ticksUntilTransition(),
					Uncertainty.LOADED_CHUNKS_ONLY);
		}
		if (darkness.observed() || pureLight.observed()) {
			return new RitualWindow(RitualSafety.LOADED_FRONT_HAZARD, -1L,
					Uncertainty.LOADED_CHUNKS_ONLY);
		}
		if (darkness.truncated() || pureLight.truncated()) {
			return new RitualWindow(RitualSafety.INDETERMINATE_SAMPLE, -1L,
					Uncertainty.LOADED_CHUNKS_ONLY);
		}
		return new RitualWindow(RitualSafety.PROVISIONAL_WINDOW, realm.ticksUntilTransition(),
				Uncertainty.LOADED_CHUNKS_ONLY);
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
