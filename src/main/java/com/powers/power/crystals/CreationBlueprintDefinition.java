package com.powers.power.crystals;

import net.minecraft.resources.Identifier;

import java.util.List;

/** Immutable, validated server-data definition for Creativity Manifestation. */
public record CreationBlueprintDefinition(
		String id, String name, List<BlockEntry> blocks, Identifier template) {
	public record Offset(int x, int y, int z) { }
	public record BlockEntry(Offset offset, Identifier block) { }

	public CreationBlueprintDefinition {
		blocks = List.copyOf(blocks);
	}

	public boolean isVanillaTemplate() {
		return template != null;
	}
}
