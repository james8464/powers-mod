package com.powers.entity;

import com.powers.player.SkillSystem;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Black player-shaped guardian that attacks every living being outside darkness. */
public final class DarknessCreature extends AbstractPlayerLikeMob {
	public DarknessCreature(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		addTag(SkillSystem.DARKNESS_TAG);
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new GuardianPerceptionTargetGoal(this));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && GuardianFactionRules.mayTarget(
				ArtifactAlignment.DARKNESS, guardianOwner(), target.getUUID(),
				target.entityTags().contains(SkillSystem.DARKNESS_TAG));
	}
}
