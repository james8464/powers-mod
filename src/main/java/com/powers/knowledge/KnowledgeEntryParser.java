package com.powers.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Strict bounded parser for datapack {@code knowledge_entries} JSON files. */
public final class KnowledgeEntryParser {
	private KnowledgeEntryParser() {
	}

	public static KnowledgeEntry parse(String id, JsonObject json) {
		Identifier identifier = Identifier.tryParse(id);
		if (identifier == null || json == null) throw new IllegalArgumentException("Invalid knowledge entry id");
		String title = requiredString(json, "title", 128);
		String answer = requiredString(json, "answer", 4_096);
		List<String> keywords = stringList(json, "keywords", 32, 96);
		List<String> sources = new ArrayList<>(stringList(json, "sources", 16, 256));
		sources.add("data/" + identifier.getNamespace() + "/knowledge_entries/"
				+ identifier.getPath() + ".json");
		int revealRank = json.has("reveal_rank") ? json.get("reveal_rank").getAsInt() : 0;
		return new KnowledgeEntry(id, title, keywords, answer, sources, revealRank);
	}

	private static String requiredString(JsonObject json, String key, int maximum) {
		if (!json.has(key) || !json.get(key).isJsonPrimitive()
				|| !json.get(key).getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Knowledge entry requires string field: " + key);
		}
		String value = json.get(key).getAsString().strip();
		if (value.isBlank() || value.length() > maximum) {
			throw new IllegalArgumentException("Knowledge field is blank or oversized: " + key);
		}
		return value;
	}

	private static List<String> stringList(JsonObject json, String key, int maxEntries, int maxLength) {
		if (!json.has(key)) return List.of();
		JsonElement element = json.get(key);
		if (!element.isJsonArray()) throw new IllegalArgumentException("Knowledge field must be an array: " + key);
		JsonArray array = element.getAsJsonArray();
		if (array.size() > maxEntries) throw new IllegalArgumentException("Too many values in: " + key);
		List<String> result = new ArrayList<>(array.size());
		for (JsonElement value : array) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException("Knowledge list values must be strings: " + key);
			}
			String text = value.getAsString().strip();
			if (text.isBlank() || text.length() > maxLength) {
				throw new IllegalArgumentException("Knowledge list value is blank or oversized: " + key);
			}
			result.add(text);
		}
		return List.copyOf(result);
	}
}
