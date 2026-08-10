package com.powers.realm;

import com.powers.PowerStatusEffects;
import com.powers.PowersBlocks;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillQuestTracker;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Builds and animates the explorable memories inside both mental realms. */
public final class RealmMindscapeManager {
	private RealmMindscapeManager() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 5 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RealmKind kind = kind(player);
			if (kind == null) continue;
			ServerLevel level = (ServerLevel) player.level();
			ensureLandmarks(level, kind);
			enforceTether(player, level, kind);
			discoverMemories(player, level, kind);
			ambient(player, level, kind, server.getTickCount());
		}
	}

	private static RealmKind kind(ServerPlayer player) {
		var id = player.level().dimension().identifier();
		if (id.equals(PowersMod.id("light_realm"))) return RealmKind.LIGHT;
		if (id.equals(PowersMod.id("dark_realm"))) return RealmKind.DARK;
		return null;
	}

	private static void ensureLandmarks(ServerLevel level, RealmKind kind) {
		RealmLandmarkSavedData data = level.getServer().overworld().getDataStorage()
				.computeIfAbsent(RealmLandmarkSavedData.TYPE);
		String dimension = level.dimension().identifier().toString();
		Set<String> missing = Set.copyOf(data.missing(dimension,
				RealmLayout.sites(kind).stream().map(MemorySite::id).toList()));
		if (missing.isEmpty()) return;
		int floorY = level.getMinY();
		Block floor = kind == RealmKind.LIGHT ? PowersBlocks.PURE_LIGHT : PowersBlocks.DARKNESS;
		Block obelisk = kind == RealmKind.LIGHT
				? PowersBlocks.LIGHT_MEMORY_OBELISK : PowersBlocks.DARK_MEMORY_OBELISK;
		Block accent = kind == RealmKind.LIGHT ? Blocks.GOLD_BLOCK : Blocks.CRYING_OBSIDIAN;
		for (MemorySite site : RealmLayout.sites(kind)) {
			if (!missing.contains(site.id())) continue;
			BlockPos center = new BlockPos(site.x(), floorY + 1, site.z());
			if (!LoadedChunks.contains(level, center)) continue;
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					int distance = dx * dx + dz * dz;
					if (distance > 10) continue;
					BlockPos floorPos = center.offset(dx, -1, dz);
					Block pattern = distance == 9 || (Math.abs(dx) == Math.abs(dz) && distance <= 8)
							? accent : floor;
					if (!level.getBlockState(floorPos).is(pattern)) {
						level.setBlockAndUpdate(floorPos, pattern.defaultBlockState());
					}
				}
			}
			if (!level.getBlockState(center).is(obelisk)) {
				level.setBlockAndUpdate(center, obelisk.defaultBlockState());
			}
			for (BlockPos marker : List.of(center.north(3), center.south(3), center.east(3), center.west(3))) {
				Block markerBlock = kind == RealmKind.LIGHT ? Blocks.END_ROD : Blocks.SOUL_LANTERN;
				if (level.getBlockState(marker).isAir()) level.setBlockAndUpdate(marker, markerBlock.defaultBlockState());
			}
			data.complete(dimension, site.id());
		}
	}

	private static void enforceTether(ServerPlayer player, ServerLevel level, RealmKind kind) {
		double dx = player.getX() - RealmLayout.ENTRY_X;
		double dz = player.getZ() - RealmLayout.ENTRY_Z;
		if (dx * dx + dz * dz <= RealmLayout.TETHER_RADIUS * RealmLayout.TETHER_RADIUS) return;
		Vec3 from = player.position().add(0, 1, 0);
		PowerFx.rune(level, from, 1.8, kind == RealmKind.LIGHT ? 0xFFFFFF : 0x2A143D, 20, 0);
		player.teleportTo(level, RealmLayout.ENTRY_X, level.getMinY() + 1, RealmLayout.ENTRY_Z,
				Set.of(), player.getYRot(), player.getXRot(), false);
		player.sendSystemMessage(Component.translatable("realm.powers.tether"));
	}

	private static void discoverMemories(ServerPlayer player, ServerLevel level, RealmKind kind) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (MemorySite site : RealmLayout.sites(kind)) {
			double dx = player.getX() - (site.x() + 0.5);
			double dz = player.getZ() - (site.z() + 0.5);
			if (dx * dx + dz * dz > 4.5 * 4.5 || !data.discoverRealmMemory(site.id())) continue;
			int color = kind == RealmKind.LIGHT ? 0xFFF4C2 : 0x5B2C83;
			Vec3 center = new Vec3(site.x() + 0.5, level.getMinY() + 2.0, site.z() + 0.5);
			PowerFx.rune(level, center, 3.2, color, 30, 0);
			PowerFx.spiral(level, center, 1.0, 7.0, color, 28, 0);
			PowerFx.burst(level, center, kind == RealmKind.LIGHT ? ParticleTypes.END_ROD : ParticleTypes.SOUL,
					24, 1.1, 0.08);
			PowerFx.sound(level, center, SoundEvents.END_PORTAL_SPAWN, 0.8f, kind == RealmKind.LIGHT ? 1.4f : 0.55f);
			player.sendSystemMessage(Component.translatable(site.memoryKey()));
			player.sendSystemMessage(Component.translatable("realm.powers.path_offer",
					Component.translatable(site.pathKey()), site.offeredPath()));
			data.refundEnergy(site.rewardEnergy());
			if (kind == RealmKind.LIGHT) SkillQuestTracker.recordLightMemory(player);
			player.sendSystemMessage(Component.translatable("realm.powers.energy_restored", site.rewardEnergy()));
			PowersPackets.syncTo(player);
			long found = data.realmMemories().stream().filter(id -> id.startsWith(kind == RealmKind.LIGHT ? "light_" : "dark_")).count();
			if (found == RealmLayout.sites(kind).size()) {
				player.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 1200, 2, true, true));
				player.sendSystemMessage(Component.translatable(kind == RealmKind.LIGHT
						? "realm.powers.light_complete" : "realm.powers.dark_complete"));
			}
		}
	}

	private static void ambient(ServerPlayer player, ServerLevel level, RealmKind kind, int tick) {
		if (tick % 10 != 0) return;
		Vec3 pos = player.position().add(0, 1, 0);
		if (kind == RealmKind.LIGHT) {
			PowerFx.burst(level, pos, ParticleTypes.END_ROD, 2, 5.5, 0.01);
			PowerFx.coloredBurst(level, pos, 0xFFF5D6, 1, 4.0);
		} else {
			PowerFx.burst(level, pos, ParticleTypes.SOUL, 2, 5.0, 0.01);
			PowerFx.burst(level, pos, ParticleTypes.REVERSE_PORTAL, 1, 4.0, 0.0);
		}
		if (tick % 200 == 0) {
			PowerFx.sound(level, pos, kind == RealmKind.LIGHT ? SoundEvents.AMETHYST_BLOCK_CHIME
					: SoundEvents.SCULK_SHRIEKER_SHRIEK, kind == RealmKind.LIGHT ? 0.25f : 0.12f,
					kind == RealmKind.LIGHT ? 1.6f : 0.45f);
		}
	}

	public static void clearAll() {
		// SavedData owns landmark completion; no process-local construction cache remains.
	}
}
