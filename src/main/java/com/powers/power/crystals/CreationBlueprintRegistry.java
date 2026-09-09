package com.powers.power.crystals;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;

/** Validates an entire datapack snapshot before atomically publishing it. */
public final class CreationBlueprintRegistry {
	public record Document(String id, JsonObject json) { }
	private volatile List<CreationBlueprintDefinition> definitions = List.of();

	public boolean reload(List<Document> documents) {
		try {
			LinkedHashMap<String, CreationBlueprintDefinition> parsed = new LinkedHashMap<>();
			documents.stream().sorted(java.util.Comparator.comparing(Document::id)).forEach(document -> {
				var definition = CreationBlueprintParser.parse(document.id(), document.json());
				if (parsed.putIfAbsent(definition.id(), definition) != null) {
					throw new IllegalArgumentException("Duplicate blueprint " + definition.id());
				}
			});
			if (parsed.isEmpty()) throw new IllegalArgumentException("At least one blueprint is required");
			definitions = List.copyOf(parsed.values());
			return true;
		} catch (RuntimeException rejected) {
			return false;
		}
	}

	public List<CreationBlueprintDefinition> definitions() {
		return definitions;
	}

	public List<String> ids() {
		return definitions.stream().map(CreationBlueprintDefinition::id).toList();
	}

	public CreationBlueprintDefinition findOrFirst(String id) {
		return definitions.stream().filter(definition -> definition.id().equals(id)).findFirst()
				.orElseGet(() -> definitions.isEmpty() ? null : definitions.getFirst());
	}
}
