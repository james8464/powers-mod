package com.powers.entity;

import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.SkillSystem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Ivory-and-gold player-shaped guardian of normal-aligned Partisan wielders. */
public final class RadiantSentinel extends AbstractPlayerLikeMob {
	public RadiantSentinel(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new GuardianPerceptionTargetGoal(this));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && GuardianFactionRules.mayTarget(
				ArtifactAlignment.LIGHT, guardianOwner(), target.getUUID(),
				target.entityTags().contains(SkillSystem.DARKNESS_TAG));
	}

	@Override
	protected boolean radiantCombat() {
		return true;
	}
}
