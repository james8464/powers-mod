package com.powers.testing;

import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.spell.SpellRegistry;
import com.powers.item.artifact.ArtifactActionCatalogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Exhaustive player-action response contract for every shipped major boss. */
public final class BossActionResponseMatrix {
	public enum Boss { LIGHT_HERALD, DARK_HERALD, FIRST_VESSEL }
	public enum Response { EFFECTIVE, RESISTED_WITH_FEEDBACK, SETUP_EFFECTIVE, DELIBERATELY_IMMUNE, UNCLASSIFIED }
	public record Entry(String actionId, Boss boss, Response response) { }

	private static final java.util.Set<String> DIRECT_ACTIONS = java.util.Set.of(
			"starfall", "void_beam", "fireball", "lightning_strike", "thunderclap",
			"telekinesis", "energy_beam", "breezy_bash", "gravity_displacement",
			"energy_drain", "ice_manipulation");
	private BossActionResponseMatrix() { }

	/** Keeps every selectable family/alignment route visible, including unverified responses. */
	public static List<Entry> generated() {
		List<Entry> entries = new ArrayList<>();
		for (Boss boss : Boss.values()) {
			PowerRegistry.getAll().forEach(power -> entries.add(new Entry(
					"innate/" + power.id().getPath(), boss, responseFor(power.id().getPath(), boss))));
			SpellRegistry.defaults().definitions().stream().flatMap(book -> book.spells().stream())
					.forEach(spell -> entries.add(new Entry("spell/" + spell.id(), boss, responseFor("spell/" + spell.id(), boss))));
			CrystalAbilityCatalog.defaults().values().stream().flatMap(List::stream).distinct()
					.forEach(id -> entries.add(new Entry("crystal/" + id, boss, responseFor("crystal/" + id, boss))));
			ArtifactActionCatalogue.all().forEach(action -> entries.add(new Entry("artifact/"
					+ action.alignment().name().toLowerCase(Locale.ROOT) + "/" + action.key(),
					boss, responseFor("artifact/" + action.alignment().serializedName() + "/" + action.key(), boss))));
		}
		return List.copyOf(entries);
	}

	public static Response responseFor(String actionId, Boss boss) {
		switch (actionId) {
			case "crystal/middleworld",
				"crystal/light_crystal",
				"crystal/dark_crystal",
				"artifact/darkness/crystal/middleworld",
				"artifact/darkness/crystal/light_crystal",
				"artifact/darkness/crystal/dark_crystal",
				"artifact/light/crystal/light_crystal" -> { return Response.EFFECTIVE; }
			default -> { }
		}
		if (actionId.equals("crystal/chrono_stop")
				|| actionId.equals("artifact/darkness/crystal/chrono_stop")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("crystal/soul_link")
				|| actionId.equals("artifact/darkness/crystal/soul_link")) return Response.EFFECTIVE;
		if (actionId.equals("crystal/creativity_manifestation")
				|| actionId.equals("artifact/light/crystal/creativity_manifestation")
				|| actionId.equals("artifact/darkness/crystal/creativity_manifestation")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("crystal/size_shift")
				|| actionId.equals("artifact/darkness/crystal/size_shift")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("crystal/clone_swarm")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/crystal/clone_swarm")) {
			return boss == Boss.DARK_HERALD ? Response.SETUP_EFFECTIVE : Response.EFFECTIVE;
		}
		if (actionId.equals("crystal/inferno")
				|| actionId.equals("artifact/darkness/crystal/inferno")) return Response.EFFECTIVE;
		if (actionId.equals("spell/celestial_ruin")) return Response.EFFECTIVE;
		// Soul Compass retains its unique custom-name eligibility requirement.
		if (actionId.equals("spell/soul_compass")) return Response.EFFECTIVE;
		if (actionId.equals("spell/cartographers_star")) return Response.SETUP_EFFECTIVE;
		// Bosses have no player-compatible anchor state; failed casts refund their reservation.
		if (actionId.equals("spell/dimensional_anchor")) return Response.DELIBERATELY_IMMUNE;
		if (actionId.equals("spell/purification_circle")
				|| actionId.equals("spell/ward_breaking_ritual")
				|| actionId.equals("spell/augury") || actionId.equals("spell/grave_recall")
				|| actionId.equals("spell/verdant_tending")
				|| actionId.equals("spell/hearth_sanctuary")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("crystal/dreamwalking")
				|| actionId.equals("artifact/darkness/crystal/dreamwalking")
				|| actionId.equals("artifact/darkness/innate/vessel_possession")) return Response.RESISTED_WITH_FEEDBACK;
		if (actionId.equals("spell/dispel") || actionId.equals("spell/blood_reading")) return Response.EFFECTIVE;
		if (actionId.equals("vessel_possession") || actionId.equals("telekinesis")
				|| actionId.equals("artifact/darkness/innate/telekinesis")) return Response.RESISTED_WITH_FEEDBACK;
		if (actionId.equals("crystal/life_bloom")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/light/crystal/life_bloom")
				|| actionId.equals("artifact/darkness/crystal/life_bloom")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/light/innate/double_health")
				|| actionId.equals("artifact/darkness/innate/double_health")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/light/innate/forcefield")
				|| actionId.equals("artifact/darkness/innate/forcefield")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/light/innate/energy_beam")
				|| actionId.equals("artifact/darkness/innate/energy_beam")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/light/innate/lightning_strike")
				|| actionId.equals("artifact/darkness/innate/lightning_strike")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/light/innate/thunderclap")
				|| actionId.equals("artifact/darkness/innate/thunderclap")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/light/innate/starfall")
				|| actionId.equals("artifact/darkness/innate/starfall")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/void_beam")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/fireball")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/breezy_bash")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/gravity_displacement")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/energy_drain")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/ice_manipulation")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/light/innate/plant_healing_acceleration")
				|| actionId.equals("artifact/darkness/innate/plant_healing_acceleration")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/size_shift")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/invisibility")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/time_freeze")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/speed_burst")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/super_speed")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/astral_projection")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/darkness/innate/time_shift")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/light/dominion/daybreak_wave")
				|| actionId.equals("artifact/light/dominion/solar_firmament")) {
			return boss == Boss.DARK_HERALD ? Response.EFFECTIVE : Response.SETUP_EFFECTIVE;
		}
		if (actionId.equals("artifact/light/dominion/covenant_chain")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/light/dominion/heaven_gate")) return Response.EFFECTIVE;
		if (actionId.equals("artifact/darkness/unique/nightfall_dominion")) {
			return boss == Boss.DARK_HERALD ? Response.SETUP_EFFECTIVE : Response.EFFECTIVE;
		}
		if (actionId.equals("artifact/light/dominion/call_radiant")
				|| actionId.equals("artifact/darkness/unique/call_hollowed")
				|| actionId.equals("artifact/light/dominion/consecrate_ground")
				|| actionId.equals("artifact/darkness/unique/blight_ground")
				|| actionId.equals("artifact/light/dominion/host_heaven")
				|| actionId.equals("artifact/light/dominion/second_dawn")) return Response.SETUP_EFFECTIVE;
		if (actionId.equals("artifact/light/innate/flight")
				|| actionId.equals("artifact/darkness/innate/flight")) return Response.SETUP_EFFECTIVE;
		if (DIRECT_ACTIONS.contains(actionId)) return Response.EFFECTIVE;
		return switch (actionId) {
			case "size_shift", "time_shift", "flight", "speed_burst", "super_speed",
					"invisibility", "time_freeze", "forcefield", "astral_projection",
					"plant_healing_acceleration", "double_health" -> Response.SETUP_EFFECTIVE;
			default -> Response.UNCLASSIFIED;
		};
	}

}
