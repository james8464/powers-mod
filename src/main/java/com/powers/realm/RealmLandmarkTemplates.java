package com.powers.realm;

import com.powers.PowersMod;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/** Generated bounded piece catalogue for the twelve authored realm structures. */
public final class RealmLandmarkTemplates {
	public static final int MAX_SITE_BLOCKS = 2_048;
	public static final int MAX_PIECE_BLOCKS = 128;
	private static final Map<String, List<Piece>> PIECES = Map.ofEntries(
		Map.entry("light_memory_1", List.of(
				new Piece(PowersMod.id("realm/light/archive/piece_00"), 128, -9, 0, -9),
				new Piece(PowersMod.id("realm/light/archive/piece_01"), 128, -7, 0, -8),
				new Piece(PowersMod.id("realm/light/archive/piece_02"), 128, -7, 1, -5),
				new Piece(PowersMod.id("realm/light/archive/piece_03"), 128, -7, 1, -5),
				new Piece(PowersMod.id("realm/light/archive/piece_04"), 128, -7, 3, -5),
				new Piece(PowersMod.id("realm/light/archive/piece_05"), 38, -3, 6, -5))),
		Map.entry("light_memory_2", List.of(
				new Piece(PowersMod.id("realm/light/labyrinth/piece_00"), 128, -10, 0, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_01"), 128, -4, 0, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_02"), 128, 2, 0, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_03"), 128, -10, 0, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_04"), 128, -4, 1, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_05"), 128, -10, 1, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_06"), 128, -4, 2, -10),
				new Piece(PowersMod.id("realm/light/labyrinth/piece_07"), 21, 10, 2, -10))),
		Map.entry("light_memory_3", List.of(
				new Piece(PowersMod.id("realm/light/shrine/piece_00"), 128, -8, 0, -8),
				new Piece(PowersMod.id("realm/light/shrine/piece_01"), 128, -6, 0, -7),
				new Piece(PowersMod.id("realm/light/shrine/piece_02"), 128, -6, 1, -6),
				new Piece(PowersMod.id("realm/light/shrine/piece_03"), 112, -6, 2, -6))),
		Map.entry("light_memory_4", List.of(
				new Piece(PowersMod.id("realm/light/settlement/piece_00"), 128, -10, 0, -9),
				new Piece(PowersMod.id("realm/light/settlement/piece_01"), 128, -2, 0, -10),
				new Piece(PowersMod.id("realm/light/settlement/piece_02"), 128, -9, 0, -8),
				new Piece(PowersMod.id("realm/light/settlement/piece_03"), 128, -9, 1, -7),
				new Piece(PowersMod.id("realm/light/settlement/piece_04"), 128, -9, 2, -7),
				new Piece(PowersMod.id("realm/light/settlement/piece_05"), 128, -9, 4, -7),
				new Piece(PowersMod.id("realm/light/settlement/piece_06"), 59, -5, 6, -7))),
		Map.entry("light_memory_5", List.of(
				new Piece(PowersMod.id("realm/light/font/piece_00"), 128, -10, 0, -9),
				new Piece(PowersMod.id("realm/light/font/piece_01"), 128, -2, 0, -10),
				new Piece(PowersMod.id("realm/light/font/piece_02"), 128, -6, 0, -8),
				new Piece(PowersMod.id("realm/light/font/piece_03"), 93, -7, 1, -7))),
		Map.entry("light_memory_6", List.of(
				new Piece(PowersMod.id("realm/light/herald_court/piece_00"), 128, -12, 0, -11),
				new Piece(PowersMod.id("realm/light/herald_court/piece_01"), 128, -4, 0, -12),
				new Piece(PowersMod.id("realm/light/herald_court/piece_02"), 128, 2, 0, -11),
				new Piece(PowersMod.id("realm/light/herald_court/piece_03"), 128, -12, 0, -12),
				new Piece(PowersMod.id("realm/light/herald_court/piece_04"), 53, -12, 1, -12))),
		Map.entry("dark_memory_1", List.of(
				new Piece(PowersMod.id("realm/dark/archive/piece_00"), 128, -9, 0, -9),
				new Piece(PowersMod.id("realm/dark/archive/piece_01"), 128, -7, 0, -8),
				new Piece(PowersMod.id("realm/dark/archive/piece_02"), 128, -7, 1, -5),
				new Piece(PowersMod.id("realm/dark/archive/piece_03"), 128, -7, 1, -5),
				new Piece(PowersMod.id("realm/dark/archive/piece_04"), 128, -7, 3, -5),
				new Piece(PowersMod.id("realm/dark/archive/piece_05"), 38, -3, 6, -5))),
		Map.entry("dark_memory_2", List.of(
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_00"), 128, -10, 0, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_01"), 128, -4, 0, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_02"), 128, 2, 0, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_03"), 128, -10, 0, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_04"), 128, -4, 1, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_05"), 128, -10, 1, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_06"), 128, -4, 2, -10),
				new Piece(PowersMod.id("realm/dark/labyrinth/piece_07"), 21, 10, 2, -10))),
		Map.entry("dark_memory_3", List.of(
				new Piece(PowersMod.id("realm/dark/shrine/piece_00"), 128, -8, 0, -8),
				new Piece(PowersMod.id("realm/dark/shrine/piece_01"), 128, -6, 0, -7),
				new Piece(PowersMod.id("realm/dark/shrine/piece_02"), 128, -6, 1, -6),
				new Piece(PowersMod.id("realm/dark/shrine/piece_03"), 112, -6, 2, -6))),
		Map.entry("dark_memory_4", List.of(
				new Piece(PowersMod.id("realm/dark/settlement/piece_00"), 128, -10, 0, -9),
				new Piece(PowersMod.id("realm/dark/settlement/piece_01"), 128, -2, 0, -10),
				new Piece(PowersMod.id("realm/dark/settlement/piece_02"), 128, -9, 0, -8),
				new Piece(PowersMod.id("realm/dark/settlement/piece_03"), 128, -9, 1, -7),
				new Piece(PowersMod.id("realm/dark/settlement/piece_04"), 128, -9, 2, -7),
				new Piece(PowersMod.id("realm/dark/settlement/piece_05"), 128, -9, 4, -7),
				new Piece(PowersMod.id("realm/dark/settlement/piece_06"), 59, -5, 6, -7))),
		Map.entry("dark_memory_5", List.of(
				new Piece(PowersMod.id("realm/dark/font/piece_00"), 128, -10, 0, -9),
				new Piece(PowersMod.id("realm/dark/font/piece_01"), 128, -2, 0, -10),
				new Piece(PowersMod.id("realm/dark/font/piece_02"), 128, -6, 0, -8),
				new Piece(PowersMod.id("realm/dark/font/piece_03"), 93, -7, 1, -7))),
		Map.entry("dark_memory_6", List.of(
				new Piece(PowersMod.id("realm/dark/herald_court/piece_00"), 128, -12, 0, -11),
				new Piece(PowersMod.id("realm/dark/herald_court/piece_01"), 128, -4, 0, -12),
				new Piece(PowersMod.id("realm/dark/herald_court/piece_02"), 128, 2, 0, -11),
				new Piece(PowersMod.id("realm/dark/herald_court/piece_03"), 128, -12, 0, -12),
				new Piece(PowersMod.id("realm/dark/herald_court/piece_04"), 53, -12, 1, -12)))
	);

	private RealmLandmarkTemplates() {
	}

	/** Ordered pieces for one stable MemorySite identifier. */
	public static List<Piece> pieces(String siteId) {
		return PIECES.getOrDefault(siteId, List.of());
	}

	/** One structure-template piece and its placement offset from site centre/floor. */
	public record Piece(Identifier template, int blocks, int offsetX, int offsetY, int offsetZ) {
		public Piece {
			if (template == null || blocks < 1 || blocks > MAX_PIECE_BLOCKS) {
				throw new IllegalArgumentException("Invalid realm template piece");
			}
		}
	}
}
