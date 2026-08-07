package com.powers.player;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import com.powers.util.PowerMessages;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SkillSystem {
	public static final String DARKNESS_TAG = "darkness";
	/** Ranks in each progression. Fewer, heavier jumps than before: 10. */
	public static final int MAX_LEVEL = 10;
	public static final int DARKNESS_MAX_LEVEL = 10;
	/** Unassisted dark-realm crossing now needs half of the darkness ladder. */
	public static final int DARKNESS_GATE_LEVEL = 5;

	private record Tier(String name, int color) {}

	private static final Tier[] TIERS = {
			new Tier("Unawakened", 0xFF777777), new Tier("Spark", 0xFFFFB74D),
			new Tier("Awakened", 0xFFFFD54F), new Tier("Adept", 0xFF4FC3F7),
			new Tier("Weaver", 0xFF4DB6AC), new Tier("Arcanist", 0xFF9575CD),
			new Tier("Luminary", 0xFFFFEE58), new Tier("Voidcaller", 0xFF5C6BC0),
			new Tier("Ascendant", 0xFF26A69A), new Tier("Origin", 0xFFFF80AB)
	};

	private static final Tier[] DARKNESS_TIERS = {
			new Tier("Acolyte of Gloom", 0xFF4B2E50), new Tier("Shadowpriest", 0xFF5B2C62),
			new Tier("Voidborn", 0xFF352040), new Tier("Sable Seeker", 0xFF3F1E34),
			new Tier("Malediction Master", 0xFF4F2A5B), new Tier("Nightmare Binder", 0xFF512E55),
			new Tier("Soulblight", 0xFF462741), new Tier("Umbral Tyrant", 0xFF342239),
			new Tier("Abyssal Archon", 0xFF432A40), new Tier("Nightfall Sovereign", 0xFF200E20)
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
			PowerMessages.send(player, "skill.powers.advanced", 3, rank(highest));
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
				PowerMessages.send(player, "skill.powers.darkness_advanced", 3, darknessRank(highestDarkness));
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

	public static Component prefix(ServerPlayer player) {
		boolean darkness = hasDarknessTag(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (darkness) {
			int level = data.darknessLevel();
			// A darkness user may hide the real title, showing the equivalent
			// rank name from the normal ladder instead.
			if (data.isDarknessPrefixHidden()) {
				return Component.literal("[" + rank(level) + "] ")
						.withStyle(style -> style.withColor(color(level)));
			}
			return Component.literal("[" + darknessRank(level) + "] ")
					.withStyle(style -> style.withColor(darknessColor(level)));
		}
		int level = data.skillLevel();
		return Component.literal("[" + rank(level) + "] ")
				.withStyle(style -> style.withColor(color(level)));
	}

	public static float damage(ServerPlayer player, float base) {
		// Fewer ranks, bigger per-rank steps: +5.25% per rank (same +52.5% cap).
		return base * (1.0f + effectiveLevel(player) * 0.0525f);
	}

	public static double range(ServerPlayer player, double base) {
		// +2.1% per rank (same +21% cap as the old 21-rank ladder).
		return base * (1.0 + effectiveLevel(player) * 0.021);
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
		return effectiveLevel(subject) >= DARKNESS_GATE_LEVEL;
	}

	/** Re-applies the visible name prefix (e.g. after toggling the hidden darkness title). */
	public static void refreshPrefix(ServerPlayer player) {
		applyRank(player);
	}

	private static void applyRank(ServerPlayer player) {
		player.setCustomName(prefix(player).copy().append(player.getName()));
		player.setCustomNameVisible(true);
	}
}
