package com.powers.power.crystals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict parser for the deliberately small Creativity Manifestation format. */
public final class CreationBlueprintParser {
	public static final int MAX_BLOCKS = 256;
	public static final int MAX_SPAN = 16;
	private static final Set<String> SAFE_BLOCKS = Set.of(
			"minecraft:stone", "minecraft:cobblestone", "minecraft:stone_bricks",
			"minecraft:oak_planks", "minecraft:oak_log", "minecraft:oak_stairs",
			"minecraft:spruce_planks", "minecraft:spruce_log", "minecraft:spruce_stairs",
			"minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:sandstone_stairs",
			"minecraft:terracotta", "minecraft:orange_terracotta", "minecraft:orange_concrete",
			"minecraft:glass", "minecraft:orange_stained_glass", "minecraft:glowstone",
			"minecraft:torch", "minecraft:lantern");
	private static final Set<String> APPROVED_TEMPLATES = Set.of(
			"minecraft:village/plains/houses/plains_small_house_1",
			"minecraft:village/desert/houses/desert_small_house_1");

	private CreationBlueprintParser() { }

	public static CreationBlueprintDefinition parse(String id, JsonObject json) {
		String name = requiredString(json, "name");
		if (name.isBlank() || name.length() > 64) {
			throw new IllegalArgumentException("Blueprint name must be 1-64 characters in " + id);
		}
		boolean hasBlocks = json.has("blocks");
		boolean hasTemplate = json.has("template");
		if (hasBlocks == hasTemplate) {
			throw new IllegalArgumentException("Blueprint must define exactly one of blocks or template in " + id);
		}
		if (hasTemplate) {
			String raw = requiredString(json, "template");
			if (!APPROVED_TEMPLATES.contains(raw)) {
				throw new IllegalArgumentException("Template is not approved: " + raw);
			}
			return new CreationBlueprintDefinition(id, name, List.of(), Identifier.parse(raw));
		}
		JsonArray array = json.getAsJsonArray("blocks");
		if (array.isEmpty() || array.size() > MAX_BLOCKS) {
			throw new IllegalArgumentException("Blueprint block count outside 1-" + MAX_BLOCKS + " in " + id);
		}
		List<CreationBlueprintDefinition.BlockEntry> blocks = new ArrayList<>(array.size());
		Set<CreationBlueprintDefinition.Offset> occupied = new HashSet<>();
		for (var element : array) {
			JsonObject block = element.getAsJsonObject();
			JsonArray pos = block.getAsJsonArray("pos");
			if (pos == null || pos.size() != 3) throw new IllegalArgumentException("Invalid pos in " + id);
			var offset = new CreationBlueprintDefinition.Offset(
					pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt());
			if (Math.abs(offset.x()) >= MAX_SPAN || Math.abs(offset.y()) >= MAX_SPAN
					|| Math.abs(offset.z()) >= MAX_SPAN) {
				throw new IllegalArgumentException("Blueprint offset exceeds origin budget in " + id);
			}
			if (!occupied.add(offset)) throw new IllegalArgumentException("Duplicate pos in " + id);
			String blockId = requiredString(block, "block");
			if (!SAFE_BLOCKS.contains(blockId)) {
				throw new IllegalArgumentException("Block is not an approved construction material: " + blockId);
			}
			blocks.add(new CreationBlueprintDefinition.BlockEntry(offset, Identifier.parse(blockId)));
		}
		validateSpan(id, occupied);
		return new CreationBlueprintDefinition(id, name, blocks, null);
	}

	public static boolean approvedMaterial(Identifier id) {
		return SAFE_BLOCKS.contains(id.toString());
	}

	private static void validateSpan(String id, Set<CreationBlueprintDefinition.Offset> offsets) {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (var pos : offsets) {
			minX = Math.min(minX, pos.x()); maxX = Math.max(maxX, pos.x());
			minY = Math.min(minY, pos.y()); maxY = Math.max(maxY, pos.y());
			minZ = Math.min(minZ, pos.z()); maxZ = Math.max(maxZ, pos.z());
		}
		if (maxX - minX + 1 > MAX_SPAN || maxY - minY + 1 > MAX_SPAN || maxZ - minZ + 1 > MAX_SPAN) {
			throw new IllegalArgumentException("Blueprint exceeds " + MAX_SPAN + " block span in " + id);
		}
	}

	private static String requiredString(JsonObject json, String key) {
		if (!json.has(key) || !json.get(key).isJsonPrimitive()
				|| !json.get(key).getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Missing string " + key);
		}
		return json.get(key).getAsString();
	}
}
