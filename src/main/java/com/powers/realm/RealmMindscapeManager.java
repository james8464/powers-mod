package com.powers.realm;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillQuestTracker;
import com.powers.power.state.GlobalTimeStopManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Builds and animates the explorable memories inside both mental realms. */
public final class RealmMindscapeManager {
	private RealmMindscapeManager() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 5 != 0) return;
		Map<ServerLevel, RealmKind> activeRealms = new LinkedHashMap<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RealmKind kind = kind(player);
			if (kind == null) continue;
			ServerLevel level = (ServerLevel) player.level();
			activeRealms.put(level, kind);
			if (!GlobalTimeStopManager.mayAct(player)) continue;
			RealmEventManager.tickPlayer(player, level, kind);
			enforceTether(player, level, kind);
			discoverMemories(player, level, kind);
			ambient(player, level, kind, server.getTickCount());
		}
		for (Map.Entry<ServerLevel, RealmKind> entry : activeRealms.entrySet()) {
			RealmLandmarkSavedData data = server.overworld().getDataStorage()
					.computeIfAbsent(RealmLandmarkSavedData.TYPE);
			RealmLandmarkConstruction.tick(entry.getKey(), entry.getValue(), data);
			RealmHeraldManager.tick(entry.getKey(), entry.getValue());
		}
	}

	private static RealmKind kind(ServerPlayer player) {
		var id = player.level().dimension().identifier();
		if (id.equals(PowersMod.id("light_realm"))) return RealmKind.LIGHT;
		if (id.equals(PowersMod.id("dark_realm"))) return RealmKind.DARK;
		return null;
	}

	private static void enforceTether(ServerPlayer player, ServerLevel level, RealmKind kind) {
		double dx = player.getX() - RealmLayout.ENTRY_X;
		double dz = player.getZ() - RealmLayout.ENTRY_Z;
		if (dx * dx + dz * dz <= RealmLayout.TETHER_RADIUS * RealmLayout.TETHER_RADIUS) return;
		Vec3 from = player.position().add(0, 1, 0);
		PowerFx.rune(level, from, 1.8, kind == RealmKind.LIGHT ? 0xFFFFFF : 0x2A143D, 20, 0);
		player.teleportTo(level, RealmLayout.ENTRY_X,
				RealmTerrain.arrivalY(level, (int) RealmLayout.ENTRY_X, (int) RealmLayout.ENTRY_Z),
				RealmLayout.ENTRY_Z,
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
			Vec3 center = new Vec3(site.x() + 0.5,
					RealmTerrain.arrivalY(level, site.x(), site.z()) + 1.0, site.z() + 0.5);
			PowerFx.rune(level, center, 3.2, color, 30, 0);
			PowerFx.spiral(level, center, 1.0, 7.0, color, 28, 0);
			PowerFx.burst(level, center, kind == RealmKind.LIGHT ? com.powers.PowersParticles.GLYPH : com.powers.PowersParticles.MOTE,
					24, 1.1, 0.08);
			PowerFx.sound(level, center, SoundEvents.END_PORTAL_SPAWN, 0.8f, kind == RealmKind.LIGHT ? 1.4f : 0.55f);
			player.sendSystemMessage(Component.translatable("realm.powers.landmark_discovered",
					Component.translatable("realm.powers.landmark."
							+ site.landmarkType().name().toLowerCase(java.util.Locale.ROOT))));
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
			PowerFx.burst(level, pos, com.powers.PowersParticles.GLYPH, 2, 5.5, 0.01);
			PowerFx.coloredBurst(level, pos, 0xFFF5D6, 1, 4.0);
		} else {
			PowerFx.burst(level, pos, com.powers.PowersParticles.MOTE, 2, 5.0, 0.01);
			PowerFx.burst(level, pos, com.powers.PowersParticles.ECLIPSE, 1, 4.0, 0.0);
		}
		if (tick % 200 == 0) {
			PowerFx.sound(level, pos, kind == RealmKind.LIGHT ? SoundEvents.AMETHYST_BLOCK_CHIME
					: SoundEvents.SCULK_SHRIEKER_SHRIEK, kind == RealmKind.LIGHT ? 0.25f : 0.12f,
					kind == RealmKind.LIGHT ? 1.6f : 0.45f);
		}
	}

	public static void clearAll() {
		RealmLandmarkConstruction.clear();
		RealmEventManager.clear();
	}
}
