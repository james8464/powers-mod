package com.powers.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Neutral player-shaped test opponent that fights hostile mobs or retaliates when struck. */
public final class PowerTestActor extends AbstractPlayerLikeMob {
	public PowerTestActor(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10,
				true, false, (target, level) -> target != this && !(target instanceof PowerTestActor)));
	}
}
