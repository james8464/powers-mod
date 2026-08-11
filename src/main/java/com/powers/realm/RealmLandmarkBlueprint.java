package com.powers.realm;

import com.powers.PowersBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic, bounded architecture for the six Light/Dark mindscape sites. */
public final class RealmLandmarkBlueprint {
	public static final int MAX_PLACEMENTS = 2_048;

	private record Palette(Block floor, Block wall, Block accent, Block lamp,
			Block shelves, Block core, Block hazard) {
	}

	private RealmLandmarkBlueprint() {
	}

	public static List<RealmBlockPlacement> create(RealmKind kind, MemorySite site, int floorY) {
		return create(site, floorY, palette(kind));
	}

	/** Registry-safe geometry preview used by ordinary JVM tests before mod registration. */
	public static List<RealmBlockPlacement> preview(RealmKind kind, MemorySite site, int floorY) {
		Palette preview = kind == RealmKind.LIGHT
				? new Palette(Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BRICKS, Blocks.GOLD_BLOCK,
						Blocks.SEA_LANTERN, Blocks.CHISELED_BOOKSHELF, Blocks.BEACON, Blocks.POWDER_SNOW)
				: new Palette(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS,
						Blocks.CRYING_OBSIDIAN, Blocks.SOUL_LANTERN, Blocks.BOOKSHELF,
						Blocks.RESPAWN_ANCHOR, Blocks.MAGMA_BLOCK);
		return create(site, floorY, preview);
	}

	private static List<RealmBlockPlacement> create(MemorySite site, int floorY, Palette palette) {
		Map<BlockPos, Block> blocks = new LinkedHashMap<>();
		switch (site.landmarkType()) {
			case ARCHIVE -> archive(blocks, site, floorY, palette);
			case LABYRINTH -> labyrinth(blocks, site, floorY, palette);
			case SHRINE -> shrine(blocks, site, floorY, palette);
			case SETTLEMENT -> settlement(blocks, site, floorY, palette);
			case FONT -> font(blocks, site, floorY, palette);
			case HERALD_COURT -> court(blocks, site, floorY, palette);
		}
		put(blocks, site, floorY + 1, 0, 0, palette.core());
		if (blocks.size() > MAX_PLACEMENTS) {
			throw new IllegalStateException("realm landmark exceeded placement budget: " + site.id());
		}
		return blocks.entrySet().stream()
				.map(entry -> new RealmBlockPlacement(entry.getKey(), entry.getValue()))
				.sorted(java.util.Comparator
						.comparingInt((RealmBlockPlacement placement) -> {
							int dx = placement.position().getX() - site.x();
							int dz = placement.position().getZ() - site.z();
							return dx * dx + dz * dz;
						})
						.thenComparingInt(placement -> placement.position().getY()))
				.toList();
	}

	private static Palette palette(RealmKind kind) {
		return kind == RealmKind.LIGHT
				? new Palette(PowersBlocks.PURE_LIGHT, Blocks.QUARTZ_BRICKS,
						Blocks.GOLD_BLOCK, Blocks.SEA_LANTERN, Blocks.CHISELED_BOOKSHELF,
						PowersBlocks.LIGHT_MEMORY_OBELISK, Blocks.POWDER_SNOW)
				: new Palette(PowersBlocks.DARKNESS, Blocks.POLISHED_BLACKSTONE_BRICKS,
						Blocks.CRYING_OBSIDIAN, Blocks.SOUL_LANTERN, Blocks.BOOKSHELF,
						PowersBlocks.DARK_MEMORY_OBELISK, Blocks.MAGMA_BLOCK);
	}

	private static void platform(Map<BlockPos, Block> blocks, MemorySite site, int y,
			Palette palette, int radius) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (x * x + z * z <= radius * radius) put(blocks, site, y, x, z, palette.floor());
			}
		}
	}

	private static void archive(Map<BlockPos, Block> blocks, MemorySite site, int y, Palette palette) {
		platform(blocks, site, y, palette, 9);
		for (int x = -7; x <= 7; x++) for (int z = -5; z <= 5; z++) {
			put(blocks, site, y + 1, x, z, palette.floor());
			boolean edge = Math.abs(x) == 7 || Math.abs(z) == 5;
			for (int dy = 2; edge && dy <= 5; dy++) {
				boolean doorway = z == -5 && Math.abs(x) <= 1 && dy <= 3;
				put(blocks, site, y + dy, x, z, doorway ? Blocks.AIR : palette.wall());
			}
			if ((Math.abs(x) <= 6 && (z == -4 || z == 4)) && x % 2 == 0) {
				put(blocks, site, y + 2, x, z, palette.shelves());
			}
			if ((x + z) % 3 == 0) put(blocks, site, y + 6, x, z, palette.wall());
		}
		for (int x : new int[] {-6, 6}) for (int z : new int[] {-4, 4}) {
			put(blocks, site, y + 5, x, z, palette.lamp());
		}
	}

	private static void labyrinth(Map<BlockPos, Block> blocks, MemorySite site, int y, Palette palette) {
		for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
			put(blocks, site, y, x, z, palette.floor());
			boolean boundary = Math.abs(x) == 10 || Math.abs(z) == 10;
			boolean inner = x % 2 == 0 && z % 2 == 0
					|| x % 4 == 0 && Math.floorMod(z + x * 3, 7) != 0
					|| z % 4 == 0 && Math.floorMod(x - z * 5, 7) != 0;
			boolean entrance = z == -10 && Math.abs(x) <= 1;
			if ((boundary || inner) && !entrance && !(Math.abs(x) <= 1 && Math.abs(z) <= 1)) {
				put(blocks, site, y + 1, x, z, palette.wall());
				put(blocks, site, y + 2, x, z, palette.wall());
			}
		}
	}

	private static void shrine(Map<BlockPos, Block> blocks, MemorySite site, int y, Palette palette) {
		platform(blocks, site, y, palette, 8);
		for (int radius = 6, dy = 1; radius >= 2; radius -= 2, dy++) {
			for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
				if (Math.max(Math.abs(x), Math.abs(z)) <= radius) {
					put(blocks, site, y + dy, x, z, dy == 3 ? palette.accent() : palette.wall());
				}
			}
		}
		for (int x : new int[] {-6, 6}) for (int z : new int[] {-6, 6}) {
			for (int dy = 1; dy <= 6; dy++) put(blocks, site, y + dy, x, z, palette.wall());
			put(blocks, site, y + 7, x, z, palette.lamp());
		}
	}

	private static void settlement(Map<BlockPos, Block> blocks, MemorySite site, int y, Palette palette) {
		platform(blocks, site, y, palette, 10);
		for (int[] offset : new int[][] {{-6, -4}, {6, -4}, {0, 6}}) {
			int ox = offset[0];
			int oz = offset[1];
			for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
				put(blocks, site, y + 1, ox + x, oz + z, palette.floor());
				boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
				for (int dy = 2; edge && dy <= 4; dy++) {
					boolean door = z == 3 && x == 0 && dy <= 3;
					put(blocks, site, y + dy, ox + x, oz + z, door ? Blocks.AIR : palette.wall());
				}
				if (Math.max(Math.abs(x), Math.abs(z)) <= 3) {
					put(blocks, site, y + 5 + Math.max(Math.abs(x), Math.abs(z)) / 3,
							ox + x, oz + z, palette.accent());
				}
			}
			put(blocks, site, y + 3, ox, oz, palette.lamp());
		}
	}

	private static void font(Map<BlockPos, Block> blocks, MemorySite site, int y, Palette palette) {
		platform(blocks, site, y, palette, 10);
		for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
			int distance = x * x + z * z;
			if (distance <= 45) put(blocks, site, y + 1, x, z,
					distance >= 32 ? palette.wall() : palette.hazard());
			if (distance == 49) put(blocks, site, y + 2, x, z, palette.accent());
		}
		for (int dy = 2; dy <= 7; dy++) put(blocks, site, y + dy, 0, 0, palette.accent());
		put(blocks, site, y + 8, 0, 0, palette.lamp());
	}

	private static void court(Map<BlockPos, Block> blocks, MemorySite site, int y, Palette palette) {
		platform(blocks, site, y, palette, 12);
		for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
			int distance = x * x + z * z;
			if (distance >= 121 && distance <= 144) {
				put(blocks, site, y + 1, x, z, palette.wall());
				if ((x + z) % 3 == 0) put(blocks, site, y + 2, x, z, palette.wall());
			}
		}
		for (int[] point : new int[][] {{-8, -8}, {-8, 8}, {8, -8}, {8, 8}}) {
			for (int dy = 1; dy <= 8; dy++) put(blocks, site, y + dy, point[0], point[1], palette.accent());
			put(blocks, site, y + 9, point[0], point[1], palette.lamp());
		}
	}

	private static void put(Map<BlockPos, Block> blocks, MemorySite site,
			int y, int relativeX, int relativeZ, Block block) {
		blocks.put(new BlockPos(site.x() + relativeX, y, site.z() + relativeZ), block);
	}
}
