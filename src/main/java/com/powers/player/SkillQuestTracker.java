package com.powers.player;

import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/** Records durable normal-path mastery and grants every newly completed rite. */
public final class SkillQuestTracker {
	private SkillQuestTracker() {
	}

	/** Counts only successful innate casts; artifacts, crystals, and spells never inherit rank mastery. */
	public static void recordPowerUse(ServerPlayer player, Ability ability) {
		if (SkillSystem.hasDarknessTag(player) || ability == null || !ability.usesRankScaling()) return;
		record(player, SkillDeed.POWER_USE);
	}

	/** Credits server-authenticated power deaths, including sufficiently durable bosses from other mods. */
	public static void recordKill(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer)
				|| SkillSystem.hasDarknessTag(killer) || !PowerDamage.isPowerDamage(source)) return;
		Map<SkillDeed, Integer> totals = SkillDeedStore.increment(killer, SkillDeed.POWER_KILL);
		if (isBoss(victim)) totals = SkillDeedStore.increment(killer, SkillDeed.BOSS_KILL);
		evaluate(killer, totals);
	}

	/** Light memories are a normal-path requirement; dark memories intentionally do not substitute. */
	public static void recordLightMemory(ServerPlayer player) {
		if (!SkillSystem.hasDarknessTag(player)) record(player, SkillDeed.LIGHT_MEMORY);
	}

	private static void record(ServerPlayer player, SkillDeed deed) {
		evaluate(player, SkillDeedStore.increment(player, deed));
	}

	private static void evaluate(ServerPlayer player, Map<SkillDeed, Integer> totals) {
		int current = PlayerPowers.get(player).skillLevel();
		int completed = SkillQuestRules.highestContiguousLevel(current, totals);
		for (int level = current + 1; level <= completed; level++) {
			SkillSystem.awardSkillRite(player, level);
		}
		if (completed > current) SkillSystem.refresh(player);
	}

	private static boolean isBoss(LivingEntity victim) {
		String type = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getPath();
		return type.equals("ender_dragon") || type.equals("wither") || type.equals("warden")
				|| type.equals("elder_guardian") || type.equals("ravager")
				|| victim.getMaxHealth() >= 200.0F;
	}
}
