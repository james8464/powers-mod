package com.powers.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactFavouriteRules;
import com.powers.item.artifact.ArtifactSelectionMigration;
import com.powers.power.PowerAffinity;
import com.powers.power.PowerRegistry;
import com.powers.spell.SpellSelectionMigration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SaveMigrationCorpusTest {
	private static final Path CORPUS = Path.of(System.getProperty("user.dir"),
			"src/test/resources/save-migrations/corpus.json");

	@BeforeAll
	static void registerPowers() {
		PowerRegistry.initialize();
	}

	@Test
	void everyReleasedAndCorruptFixtureBecomesCanonicalAndIdempotent() throws IOException {
		JsonArray fixtures = JsonParser.parseString(Files.readString(CORPUS)).getAsJsonArray();
		assertEquals(6, fixtures.size());
		for (var element : fixtures) {
			JsonObject fixture = element.getAsJsonObject();
			Canonical first = migrate(fixture.getAsJsonObject("input"));
			Canonical second = migrate(first);
			assertEquals(first, second, fixture.get("name").getAsString());
			assertEquals(expected(fixture.getAsJsonObject("expected")), first,
					fixture.get("name").getAsString());
			assertFalse(first.opaque().isEmpty(), "Unknown save data must survive: " + fixture.get("name"));
		}
	}

	private static Canonical migrate(JsonObject input) {
		PowerAffinity affinity = PowerAffinity.valueOf(input.get("affinity").getAsString());
		int rank = input.get("rank").getAsInt();
		List<String> powers = SaveMigrationRules.canonicalPowerSlots(strings(input.getAsJsonArray("powers")), affinity);
		Map<String, Integer> spells = new LinkedHashMap<>();
		input.getAsJsonObject("spells").entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(entry -> spells.put(entry.getKey(),
						SpellSelectionMigration.canonicalIndex(entry.getKey(), entry.getValue().getAsInt())));
		String darkSelection = ArtifactSelectionMigration.migrate(ArtifactAlignment.DARKNESS,
				input.get("dark_selection").getAsString(), rank);
		List<String> darkFavourites = ArtifactFavouriteRules.reconcile(
				strings(input.getAsJsonArray("dark_favourites")),
				ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS),
				ArtifactAlignment.DARKNESS, rank, darkSelection);
		Map<String, String> opaque = new LinkedHashMap<>();
		input.getAsJsonObject("opaque").entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(entry -> opaque.put(entry.getKey(), entry.getValue().getAsString()));
		return new Canonical("1.0.2", affinity, rank, powers, Map.copyOf(spells),
				darkSelection, darkFavourites, Map.copyOf(opaque));
	}

	private static Canonical migrate(Canonical input) {
		List<String> powers = SaveMigrationRules.canonicalPowerSlots(input.powers(), input.affinity());
		Map<String, Integer> spells = new LinkedHashMap<>();
		input.spells().entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(entry -> spells.put(entry.getKey(),
						SpellSelectionMigration.canonicalIndex(entry.getKey(), entry.getValue())));
		String selected = ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, input.darkSelection(), input.rank());
		List<String> favourites = ArtifactFavouriteRules.reconcile(input.darkFavourites(),
				ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS),
				ArtifactAlignment.DARKNESS, input.rank(), selected);
		return new Canonical("1.0.2", input.affinity(), input.rank(), powers,
				Map.copyOf(spells), selected, favourites, input.opaque());
	}

	private static Canonical expected(JsonObject value) {
		Map<String, Integer> spells = new LinkedHashMap<>();
		value.getAsJsonObject("spells").entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(entry -> spells.put(entry.getKey(), entry.getValue().getAsInt()));
		Map<String, String> opaque = new LinkedHashMap<>();
		value.getAsJsonObject("opaque").entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(entry -> opaque.put(entry.getKey(), entry.getValue().getAsString()));
		return new Canonical("1.0.2", PowerAffinity.valueOf(value.get("affinity").getAsString()),
				value.get("rank").getAsInt(), strings(value.getAsJsonArray("powers")),
				Map.copyOf(spells), value.get("dark_selection").getAsString(),
				strings(value.getAsJsonArray("dark_favourites")), Map.copyOf(opaque));
	}

	private static List<String> strings(JsonArray array) {
		List<String> result = new ArrayList<>();
		array.forEach(value -> result.add(value.getAsString()));
		return List.copyOf(result);
	}

	private record Canonical(String version, PowerAffinity affinity, int rank,
			List<String> powers, Map<String, Integer> spells, String darkSelection,
			List<String> darkFavourites, Map<String, String> opaque) {
	}
}
