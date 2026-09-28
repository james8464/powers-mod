package com.powers.realm;

import com.powers.PowersMod;
import com.powers.PowersBlocks;
import com.powers.fx.PowerFx;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Builds compact loaded Middleworld libraries and their neutral non-trading Archivists. */
public final class MiddleworldOutpostManager {
	private static final ResourceKey<Level> MIDDLEWORLD = ResourceKey.create(Registries.DIMENSION,
			PowersMod.id("middleworld"));
	private static final String ARCHIVIST_TAG = "powers_middleworld_archivist";

	private MiddleworldOutpostManager() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) return;
		ServerLevel level = server.getLevel(MIDDLEWORLD);
		if (level == null || level.players().isEmpty()) return;
		int remaining = MiddleworldOutpostRules.maxBlocksPerPulse();
		for (MiddleworldOutpostRules.Outpost outpost : MiddleworldOutpostRules.outposts()) {
			BlockPos center = new BlockPos(outpost.x(), RealmTerrain.provisionalArrivalY(level), outpost.z());
			if (!LoadedChunks.contains(level, center)) continue;
			remaining -= buildLibrary(level, outpost, remaining);
			remaining -= buildRoad(level, outpost, remaining);
			ensureArchivist(level, outpost);
			if (remaining <= 0) break;
		}
		if (server.getTickCount() % 40 == 0) dreamWeather(level);
	}

	private static int buildLibrary(ServerLevel level, MiddleworldOutpostRules.Outpost outpost, int remaining) {
		int edits = 0;
		int y = RealmTerrain.floorY(level, outpost.x(), outpost.z());
		for (int x = -2; x <= 2 && edits < remaining; x++) {
			for (int z = -2; z <= 2 && edits < remaining; z++) {
				BlockPos position = new BlockPos(outpost.x() + x, y, outpost.z() + z);
				if (level.getBlockState(position).is(PowersBlocks.PURE_LIGHT)) {
					level.setBlock(position, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
					edits++;
				}
			}
		}
		for (int x : new int[] {-2, 2}) {
			for (int z : new int[] {-2, 2}) {
				if (edits >= remaining) return edits;
				BlockPos shelf = new BlockPos(outpost.x() + x, y + 1, outpost.z() + z);
				if (level.getBlockState(shelf).isAir()) {
					level.setBlock(shelf, Blocks.CHISELED_BOOKSHELF.defaultBlockState(), 3);
					edits++;
				}
			}
		}
		return edits;
	}

	private static int buildRoad(ServerLevel level, MiddleworldOutpostRules.Outpost outpost, int remaining) {
		int edits = 0;
		int y = RealmTerrain.floorY(level, outpost.x(), outpost.z());
		int x = (int) RealmLayout.ENTRY_X;
		int z = (int) RealmLayout.ENTRY_Z;
		while (x != outpost.x() && edits < remaining) {
			x += Integer.signum(outpost.x() - x);
			edits += placeRoadBlock(level, new BlockPos(x, y, z));
		}
		while (z != outpost.z() && edits < remaining) {
			z += Integer.signum(outpost.z() - z);
			edits += placeRoadBlock(level, new BlockPos(x, y, z));
		}
		return edits;
	}

	private static int placeRoadBlock(ServerLevel level, BlockPos position) {
		if (!LoadedChunks.contains(level, position) || !level.getBlockState(position).is(PowersBlocks.PURE_LIGHT)) return 0;
		level.setBlock(position, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
		return 1;
	}

	private static void ensureArchivist(ServerLevel level, MiddleworldOutpostRules.Outpost outpost) {
		BlockPos center = new BlockPos(outpost.x(), RealmTerrain.arrivalY(level, outpost.x(), outpost.z()), outpost.z());
		if (!level.getEntitiesOfClass(Villager.class, new AABB(center).inflate(5.0),
				villager -> villager.entityTags().contains(ARCHIVIST_TAG + '_' + outpost.id())).isEmpty()) return;
		Villager archivist = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
		if (archivist == null) return;
		archivist.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
		archivist.setCustomName(Component.translatable("entity.powers.archivist"));
		archivist.setCustomNameVisible(true);
		archivist.setNoAi(true);
		archivist.setPersistenceRequired();
		archivist.addTag(ARCHIVIST_TAG + '_' + outpost.id());
		level.addFreshEntity(archivist);
	}

	private static void dreamWeather(ServerLevel level) {
		MiddleworldOutpostRules.DreamWeather weather = MiddleworldOutpostRules.weatherAt(level.getGameTime());
		for (ServerPlayer player : level.players()) {
			Vec3 position = player.position().add(0.0, 1.0, 0.0);
			switch (weather) {
				case STILL -> PowerFx.coloredBurst(level, position, 0x8D82A9, 1, 4.0);
				case MOTES -> PowerFx.coloredBurst(level, position, 0x9D89C7, 4, 5.0);
				case ECHOES -> PowerFx.rune(level, position, 1.6, 0xB4A0DD, 8, 0.0);
			}
		}
	}
}
