package com.powers.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/** Unarmed player-shaped Orange Crystal echo carrying its owner's real skin. */
public final class EchoClone extends AbstractPlayerLikeMob {
	private static final EntityDataAccessor<ResolvableProfile> PROFILE =
			SynchedEntityData.defineId(EchoClone.class, EntityDataSerializers.RESOLVABLE_PROFILE);
	private static final int FOLLOW_PULSE_TICKS = 10;
	private UUID ownerId;
	private int remainingTicks;

	public EchoClone(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PROFILE, ResolvableProfile.createUnresolved("Echo"));
	}

	public void configure(ServerPlayer owner, int lifetimeTicks) {
		ownerId = owner.getUUID();
		remainingTicks = Math.max(1, lifetimeTicks);
		getEntityData().set(PROFILE,
				ResolvableProfile.createResolved(owner.getGameProfile()));
		setCustomName(Component.literal(owner.getGameProfile().name() + "'s Echo"));
		setCustomNameVisible(true);
		setPersistenceRequired();
	}

	/** Profile synchronized to clients for exact owner skin lookup. */
	public GameProfile ownerProfile() {
		return getEntityData().get(PROFILE).partialProfile();
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<Monster>(this,
				Monster.class, 5, true, false, (target, level) -> mayAttackTarget(target)));
	}

	@Override
	public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
		if (ownerId != null && ownerId.equals(target.getUUID())) return false;
		return super.canAttack(target) && mayAttackTarget(target);
	}

	private boolean mayAttackTarget(net.minecraft.world.entity.LivingEntity target) {
		if (target == this || ownerId == null) return false;
		ServerPlayer owner = level().getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null) return false;
		boolean sameOwner = target instanceof AbstractPlayerLikeMob guardian
				&& java.util.Objects.equals(ownerId, guardian.guardianOwner())
				|| target instanceof EchoClone echo && java.util.Objects.equals(ownerId, echo.ownerId);
		return EchoCloneRules.mayTarget(
				com.powers.player.SkillSystem.hasDarknessTag(owner),
				target.entityTags().contains(com.powers.player.SkillSystem.DARKNESS_TAG),
				target instanceof RadiantSentinel || target instanceof RealmHerald herald
						&& herald.realmKind() == com.powers.realm.RealmKind.LIGHT,
				sameOwner);
	}

	@Override
	protected boolean usesSharedRangedCombat() {
		return false;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (--remainingTicks <= 0) {
			com.powers.fx.PowerFx.burst(level, position().add(0.0, 1.0, 0.0),
					net.minecraft.core.particles.ParticleTypes.POOF, 14, 0.7, 0.2);
			discard();
			return;
		}
		if (ownerId == null || tickCount % FOLLOW_PULSE_TICKS != 0) return;
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || owner.level() != level || !owner.isAlive()) {
			discard();
			return;
		}
		double distance = distanceToSqr(owner);
		if (EchoCloneRules.shouldTeleport(distance)) {
			teleportNear(level, owner);
		} else if (getTarget() == null && EchoCloneRules.shouldFollow(distance)) {
			getNavigation().moveTo(owner, 1.2);
		}
	}

	private void teleportNear(ServerLevel level, ServerPlayer owner) {
		for (int offset = 0; offset < 8; offset++) {
			double angle = Math.PI * 2.0 * offset / 8.0;
			double x = owner.getX() + Math.cos(angle) * 2.0;
			double z = owner.getZ() + Math.sin(angle) * 2.0;
			setPos(x, owner.getY(), z);
			if (level.noCollision(this, getBoundingBox())) return;
		}
		setPos(owner.getX(), owner.getY(), owner.getZ());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (ownerId != null) output.putString("PowersEchoOwner", ownerId.toString());
		output.putInt("PowersEchoLife", remainingTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			ownerId = UUID.fromString(input.getStringOr("PowersEchoOwner", ""));
			getEntityData().set(PROFILE, ResolvableProfile.createUnresolved(ownerId));
		} catch (IllegalArgumentException ignored) {
			ownerId = null;
		}
		remainingTicks = Math.max(1, input.getIntOr("PowersEchoLife", 1));
	}
}
