package com.powers.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.SkillSystem;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Neutral player-shaped test opponent that fights hostile mobs or retaliates when struck. */
public final class PowerTestActor extends AbstractPlayerLikeMob implements PlayerLikeTarget {
	private static final String USERNAME_KEY = "PowersTestUsername";
	private static final String PROFILE_RANK_KEY = "PowersTestRank";
	private static final String PROFILE_ALIGNMENT_KEY = "PowersTestAlignment";
	private static final String PROFILE_CONSENT_KEY = "PowersTestConsent";
	private static final String PROFILE_ARMORED_KEY = "PowersTestArmored";
	private static final String PROFILE_MOBILE_KEY = "PowersTestMobile";
	private static final String PROFILE_ACTION_KEY = "PowersTestAction";
	private String testingUsername;
	private TestActorProfile profile = TestActorProfile.defaults();

	public PowerTestActor(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		applyUsername(TestActorIdentity.defaultUsername(getUUID()));
		configure(profile);
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

	/** Applies every player-compatible test dimension without impersonating a connected client. */
	public void configure(TestActorProfile requested) {
		profile = requested == null ? TestActorProfile.defaults() : requested;
		if (profile.alignment() == ArtifactAlignment.DARKNESS) addTag(SkillSystem.DARKNESS_TAG);
		else removeTag(SkillSystem.DARKNESS_TAG);
		setNoAi(!profile.mobile());
		getAttribute(Attributes.ARMOR).setBaseValue(profile.armored() ? 20.0 : 0.0);
	}

	public TestActorProfile profile() {
		return profile;
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
		output.putInt(PROFILE_RANK_KEY, profile.rank());
		output.putString(PROFILE_ALIGNMENT_KEY, profile.alignment().serializedName());
		output.putBoolean(PROFILE_CONSENT_KEY, profile.consents());
		output.putBoolean(PROFILE_ARMORED_KEY, profile.armored());
		output.putBoolean(PROFILE_MOBILE_KEY, profile.mobile());
		output.putString(PROFILE_ACTION_KEY, profile.scriptedAction());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		applyUsername(TestActorIdentity.normalize(input.getStringOr(USERNAME_KEY, ""), getUUID()));
		ArtifactAlignment alignment;
		try {
			alignment = ArtifactAlignment.fromSerialized(input.getStringOr(PROFILE_ALIGNMENT_KEY, "light"));
		} catch (IllegalArgumentException ignored) {
			alignment = ArtifactAlignment.LIGHT;
		}
		configure(TestActorProfile.of(input.getIntOr(PROFILE_RANK_KEY, 0), alignment,
				input.getBooleanOr(PROFILE_CONSENT_KEY, true),
				input.getBooleanOr(PROFILE_ARMORED_KEY, false),
				input.getBooleanOr(PROFILE_MOBILE_KEY, true),
				input.getStringOr(PROFILE_ACTION_KEY, "")));
	}
}
