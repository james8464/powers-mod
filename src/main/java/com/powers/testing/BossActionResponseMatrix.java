package com.powers.testing;

import com.powers.power.Power;
import com.powers.power.PowerRegistry;

import java.util.List;

/** Exhaustive player-action response contract for every shipped major boss. */
public final class BossActionResponseMatrix {
	public enum Boss { LIGHT_HERALD, DARK_HERALD, FIRST_VESSEL }
	public enum Response { EFFECTIVE, RESISTED_WITH_FEEDBACK, SETUP_EFFECTIVE, UNCLASSIFIED }
	public record Entry(String actionId, Boss boss, Response response) { }

	private static final java.util.Set<String> DIRECT_ACTIONS = java.util.Set.of(
			"starfall", "void_beam", "fireball", "lightning_strike", "thunderclap",
			"telekinesis", "energy_beam", "breezy_bash", "gravity_displacement",
			"energy_drain", "ice_manipulation");
	private BossActionResponseMatrix() { }

	/** Builds the matrix from the registry so a newly-shipped action cannot silently evade review. */
	public static List<Entry> generated() {
		return PowerRegistry.getAll().stream().flatMap(power -> java.util.Arrays.stream(Boss.values())
				.map(boss -> new Entry(path(power), boss, responseFor(path(power), boss)))).toList();
	}

	public static Response responseFor(String actionId, Boss boss) {
		if (DIRECT_ACTIONS.contains(actionId)) return Response.EFFECTIVE;
		return switch (actionId) {
			case "size_shift", "time_shift", "flight", "speed_burst", "super_speed",
					"invisibility", "time_freeze", "forcefield", "astral_projection",
					"plant_healing_acceleration", "double_health", "vessel_possession" -> Response.SETUP_EFFECTIVE;
			default -> Response.UNCLASSIFIED;
		};
	}

	private static String path(Power power) {
		return power.id().getPath();
	}
}
