package com.powers.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Neutral player-shaped test opponent that fights hostile mobs or retaliates when struck. */
public final class PowerTestActor extends AbstractPlayerLikeMob implements PlayerLikeTarget {
	private static final String USERNAME_KEY = "PowersTestUsername";
	private String testingUsername;

	public PowerTestActor(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		applyUsername(TestActorIdentity.defaultUsername(getUUID()));
	}

	@Override
	public String testingUsername() {
		Component visibleName = getCustomName();
		String requested = visibleName == null ? testingUsername : visibleName.getString();
		String normalized = TestActorIdentity.normalize(requested, getUUID());
		if (!normalized.equals(testingUsername) || visibleName == null
				|| !normalized.equals(visibleName.getString())) applyUsername(normalized);
		return testingUsername;
	}

	/** Applies command/name-tag input while retaining a valid target username. */
	public void setTestingUsername(String username) {
		applyUsername(TestActorIdentity.normalize(username, getUUID()));
	}

	private void applyUsername(String username) {
		testingUsername = username;
		setCustomName(Component.literal(username));
		setCustomNameVisible(true);
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10,
				true, false, (target, level) -> target != this && !(target instanceof PowerTestActor)));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString(USERNAME_KEY, testingUsername());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		applyUsername(TestActorIdentity.normalize(input.getStringOr(USERNAME_KEY, ""), getUUID()));
	}
}
