package com.powers.player;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import com.powers.power.Ability;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SkillSystem {
	public static final String DARKNESS_TAG = "darkness";
	public static final int MAX_LEVEL = 21;
	public static final int DARKNESS_MAX_LEVEL = 30;

	private record Tier(String name, int color) {}

	private static final Tier[] TIERS = {
			new Tier("Unawakened", 0xFF777777), new Tier("Spark", 0xFFFFB74D),
			new Tier("Awakened", 0xFFFFD54F), new Tier("Channeler", 0xFF81D4FA),
			new Tier("Adept", 0xFF4FC3F7), new Tier("Weaver", 0xFF4DB6AC),
			new Tier("Arcanist", 0xFF9575CD), new Tier("Vanguard", 0xFFEF5350),
			new Tier("Luminary", 0xFFFFEE58), new Tier("Riftwalker", 0xFFAB47BC),
			new Tier("Starforged", 0xFF64B5F6), new Tier("Soulbound", 0xFFCE93D8),
			new Tier("Chronarch", 0xFF26C6DA), new Tier("Astral", 0xFFB39DDB),
			new Tier("Voidcaller", 0xFF5C6BC0), new Tier("Paragon", 0xFFFF7043),
			new Tier("Ascendant", 0xFF26A69A), new Tier("Transcendent", 0xFFEC407A),
			new Tier("Mythic", 0xFFFFCA28), new Tier("Apex", 0xFFF5F5F5),
			new Tier("Origin", 0xFFFF80AB)
	};

	private static final Tier[] DARKNESS_TIERS = {
			new Tier("Acolyte of Gloom", 0xFF4B2E50), new Tier("Shade Initiate", 0xFF5A3F69),
			new Tier("Nightbound", 0xFF3C2753), new Tier("Shadowpriest", 0xFF5B2C62),
			new Tier("Duskwarden", 0xFF3E2044), new Tier("Umbra Cultist", 0xFF4A2A4E),
			new Tier("Voidborn", 0xFF352040), new Tier("Abyssal Adept", 0xFF4F2D56),
			new Tier("Witch of Wraiths", 0xFF6A3B74), new Tier("Sable Seeker", 0xFF3F1E34),
			new Tier("Obsidian Oracle", 0xFF1F1B24), new Tier("Coven Herald", 0xFF502B5B),
			new Tier("Malediction Master", 0xFF4F2A5B), new Tier("Onyx Savant", 0xFF2F223C),
			new Tier("Ravenous Shade", 0xFF4D2E4F), new Tier("Nightmare Binder", 0xFF512E55),
			new Tier("Sinister Paragon", 0xFF3A253B), new Tier("Midnight Marshall", 0xFF33233A),
			new Tier("Soulblight", 0xFF462741), new Tier("Ebon Sovereign", 0xFF251C29),
			new Tier("Gravecaller", 0xFF432C3F), new Tier("Umbral Tyrant", 0xFF342239),
			new Tier("Dread Reaver", 0xFF4E2E53), new Tier("Cryptic Overlord", 0xFF2D1F30),
			new Tier("Void Emperor", 0xFF3A2641), new Tier("Darkstar Primarch", 0xFF56386F),
			new Tier("Nocturne Lord", 0xFF312238), new Tier("Abyssal Archon", 0xFF432A40),
			new Tier("Eclipsed Herald", 0xFF4A2F50), new Tier("Nightfall Sovereign", 0xFF200E20)
	};

	private SkillSystem() {
	}

	public static void refresh(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int highest = data.skillLevel();
		for (int level = 1; level <= MAX_LEVEL; level++) {
			AdvancementHolder holder = ((ServerLevel) player.level()).getServer().getAdvancements()
					.get(PowersMod.id("skill/level_" + String.format("%02d", level)));
			if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
				highest = Math.max(highest, level);
			}
		}
		if (highest != data.skillLevel()) {
			data.setSkillLevel(player, highest);
			player.sendSystemMessage(Component.translatable("skill.powers.advanced", rank(highest)));
			PowersPackets.syncTo(player);
		}

		int highestDarkness = data.darknessLevel();
		if (hasDarknessTag(player)) {
			for (int level = 1; level <= DARKNESS_MAX_LEVEL; level++) {
				AdvancementHolder holder = ((ServerLevel) player.level()).getServer().getAdvancements()
					.get(PowersMod.id("darkness/level_" + String.format("%02d", level)));
				if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
					highestDarkness = Math.max(highestDarkness, level);
				}
			}
			if (highestDarkness != data.darknessLevel()) {
				data.setDarknessLevel(player, highestDarkness);
				player.sendSystemMessage(Component.translatable("skill.powers.darkness_advanced", darknessRank(highestDarkness)));
				PowersPackets.syncTo(player);
			}
		}
		applyRank(player);
	}

	public static String rank(int level) {
		return TIERS[Math.max(0, Math.min(MAX_LEVEL - 1, level))].name();
	}

	public static int color(int level) {
		return TIERS[Math.max(0, Math.min(MAX_LEVEL - 1, level))].color();
	}

	public static String darknessRank(int level) {
		return DARKNESS_TIERS[Math.max(0, Math.min(DARKNESS_MAX_LEVEL - 1, level))].name();
	}

	public static int darknessColor(int level) {
		return DARKNESS_TIERS[Math.max(0, Math.min(DARKNESS_MAX_LEVEL - 1, level))].color();
	}

	public static Component prefix(int level, boolean darkness) {
		if (darkness) {
			return Component.literal("[" + darknessRank(level) + "] ")
					.withStyle(style -> style.withColor(darknessColor(level)));
		}
		return Component.literal("[" + rank(level) + "] ")
				.withStyle(style -> style.withColor(color(level)));
	}

	public static Component prefix(ServerPlayer player) {
		boolean darkness = player.entityTags().contains("darkness");
		int level = darkness ? PlayerPowers.get(player).darknessLevel() : PlayerPowers.get(player).skillLevel();
		return prefix(level, darkness);
	}

	public static float damage(ServerPlayer player, float base) {
		return base * (1.0f + effectiveLevel(player) * 0.025f);
	}

	public static double range(ServerPlayer player, double base) {
		return base * (1.0 + effectiveLevel(player) * 0.01);
	}

	public static int energyCost(Ability ability, int skill) {
		return Math.max(1, com.powers.power.PowerEnergy.cost(ability));
	}

	public static boolean hasDarknessTag(ServerPlayer player) {
		return player.entityTags().contains(DARKNESS_TAG);
	}

	public static int effectiveLevel(ServerPlayer player) {
		return hasDarknessTag(player)
			? PlayerPowers.get(player).darknessLevel()
			: PlayerPowers.get(player).skillLevel();
	}

	public static boolean isDarkRealm(ResourceKey<Level> dimension) {
		return dimension.identifier().equals(PowersMod.id("dark_realm"));
	}

	public static boolean canTraverseDarknessDimension(ServerPlayer subject, boolean assisted) {
		if (hasDarknessTag(subject)) {
			return true;
		}
		if (assisted) {
			return true;
		}
		return effectiveLevel(subject) >= 10;
	}

	private static void applyRank(ServerPlayer player) {
		player.setCustomName(prefix(player).copy().append(player.getName()));
		player.setCustomNameVisible(true);
	}
}
