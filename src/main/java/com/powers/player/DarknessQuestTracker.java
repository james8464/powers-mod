package com.powers.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.EnumSet;
import java.util.Map;

/** Records server-authoritative kills and grants consecutive darkness rites. */
public final class DarknessQuestTracker {
	private DarknessQuestTracker() {
	}

	/** Classifies one actual death; projectiles resolve to their owning player. */
	public static void recordKill(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer)
				|| !SkillSystem.hasDarknessTag(killer)
				|| victim.entityTags().contains(SkillSystem.DARKNESS_TAG)) {
			return;
		}
		EnumSet<DarknessDeed> deeds = classify(victim);
		if (deeds.isEmpty()) {
			return;
		}

		PlayerPowers.PlayerPowersData data = PlayerPowers.get(killer);
		Map<DarknessDeed, Integer> totals = data.addDarknessDeeds(deeds);
		int current = data.darknessLevel();
		int completed = DarknessQuestRules.highestContiguousLevel(current, totals);
		for (int level = current + 1; level <= completed; level++) {
			SkillSystem.awardDarknessRite(killer, level);
		}
		if (DarknessQuestRules.progressed(current, completed)) {
			SkillSystem.refresh(killer);
		}
	}

	private static EnumSet<DarknessDeed> classify(LivingEntity victim) {
		EnumSet<DarknessDeed> deeds = EnumSet.noneOf(DarknessDeed.class);
		if (victim instanceof Animal && !(victim instanceof Wolf)) {
			deeds.add(DarknessDeed.PASSIVE);
		}
		if (victim instanceof Wolf) {
			deeds.add(DarknessDeed.WOLF);
		}
		if (victim instanceof Villager villager) {
			deeds.add(DarknessDeed.VILLAGER);
			if (villager.isBaby()) {
				deeds.add(DarknessDeed.BABY_VILLAGER);
			}
		}
		if (victim instanceof IronGolem) {
			deeds.add(DarknessDeed.IRON_GOLEM);
		}
		return deeds;
	}
}
