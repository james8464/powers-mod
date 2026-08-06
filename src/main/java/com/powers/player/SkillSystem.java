package com.powers.player;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import com.powers.power.Ability;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;

public final class SkillSystem {
	public static final int MAX_LEVEL = 21;

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
		applyRank(player, highest);
	}

	public static String rank(int level) {
		return TIERS[Math.max(0, Math.min(MAX_LEVEL - 1, level))].name();
	}

	public static int color(int level) {
		return TIERS[Math.max(0, Math.min(MAX_LEVEL - 1, level))].color();
	}

	public static Component prefix(int level) {
		return Component.literal("[" + rank(level) + "] ")
				.withStyle(style -> style.withColor(color(level)));
	}

	public static float damage(ServerPlayer player, float base) {
		return base * (1.0f + PlayerPowers.get(player).skillLevel() * 0.025f);
	}

	public static double range(ServerPlayer player, double base) {
		return base * (1.0 + PlayerPowers.get(player).skillLevel() * 0.01);
	}

	public static int energyCost(Ability ability, int skill) {
		return Math.max(1, (int) Math.ceil(com.powers.power.PowerEnergy.cost(ability) *
				(1.0 - Math.min(0.4, skill * 0.02))));
	}

	private static void applyRank(ServerPlayer player, int level) {
		player.setCustomName(prefix(level).copy().append(player.getName()));
		player.setCustomNameVisible(true);
	}
}
