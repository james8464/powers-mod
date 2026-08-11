package com.powers.companion;

import com.mojang.authlib.GameProfile;
import com.powers.entity.AbstractPlayerLikeMob;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/** Real unarmed player-shaped body for one Darkness user's persistent Shadow. */
public final class ShadowCompanionEntity extends AbstractPlayerLikeMob {
	private static final String OWNER_KEY = "PowersShadowOwner";
	private static final String ENERGY_KEY = "PowersShadowEnergy";
	private static final String REVEALED_KEY = "PowersShadowRevealed";
	private static final EntityDataAccessor<ResolvableProfile> PROFILE =
			SynchedEntityData.defineId(ShadowCompanionEntity.class,
					EntityDataSerializers.RESOLVABLE_PROFILE);
	private static final EntityDataAccessor<Boolean> REVEALED =
			SynchedEntityData.defineId(ShadowCompanionEntity.class, EntityDataSerializers.BOOLEAN);

	private UUID ownerId;
	private int energy = ShadowCompanionRules.MAX_ENERGY;

	public ShadowCompanionEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setCanPickUpLoot(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PROFILE, ResolvableProfile.createUnresolved("Shadow"));
		builder.define(REVEALED, false);
	}

	/** Binds one newly created body to its owner and persistent state. */
	public void configure(ServerPlayer owner, ShadowCompanionData data) {
		ownerId = owner.getUUID();
		energy = ShadowCompanionRules.energy(data.energy());
		getEntityData().set(PROFILE, ResolvableProfile.createResolved(owner.getGameProfile()));
		setCustomName(Component.literal("Shadow of " + owner.getScoreboardName()));
		setCustomNameVisible(false);
		setPersistenceRequired();
		setRevealed(data.revealed());
	}

	public UUID ownerId() {
		return ownerId;
	}

	/** Profile synchronized to the renderer for the owner's current wide/slim skin. */
	public GameProfile ownerProfile() {
		return getEntityData().get(PROFILE).partialProfile();
	}

	public boolean revealed() {
		return getEntityData().get(REVEALED);
	}

	/** Visibility changes presentation/physics only; health and effects are preserved. */
	public void setRevealed(boolean revealed) {
		getEntityData().set(REVEALED, revealed);
		var presentation = ShadowCompanionRules.presentation(revealed);
		setInvisible(!presentation.globallyVisible());
		noPhysics = !presentation.collidable();
		setNoGravity(!presentation.collidable());
		setInvulnerable(!presentation.externallyVulnerable());
	}

	public int energy() {
		return energy;
	}

	public void setEnergy(int value) {
		energy = ShadowCompanionRules.energy(value);
	}

	@Override
	protected void registerTargetGoals() {
		// The bounded Shadow combat controller owns targets and tactical decisions.
	}

	@Override
	protected boolean usesSharedRangedCombat() {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return ownerId != null && !ownerId.equals(target.getUUID()) && super.canAttack(target);
	}

	@Override
	public boolean isPushable() {
		return revealed() && super.isPushable();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (ownerId == null || tickCount % 10 != 0 || getTarget() != null) return;
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || owner.level() != level || !owner.isAlive()) return;
		if (ShadowCompanionRules.shouldFollow(distanceToSqr(owner))) {
			getNavigation().moveTo(owner, 1.15);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (ownerId != null) output.putString(OWNER_KEY, ownerId.toString());
		output.putInt(ENERGY_KEY, energy);
		output.putBoolean(REVEALED_KEY, revealed());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String owner = input.getStringOr(OWNER_KEY, "");
		try {
			ownerId = owner.isBlank() ? null : UUID.fromString(owner);
		} catch (IllegalArgumentException ignored) {
			ownerId = null;
		}
		if (ownerId != null) getEntityData().set(PROFILE, ResolvableProfile.createUnresolved(ownerId));
		setEnergy(input.getIntOr(ENERGY_KEY, ShadowCompanionRules.MAX_ENERGY));
		setRevealed(input.getBooleanOr(REVEALED_KEY, false));
	}
}
