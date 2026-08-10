package com.powers.entity;

import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared player-scale movement, attributes, and bounded lightning/fireball attacks. */
public abstract class AbstractPlayerLikeMob extends Monster {
	protected AbstractPlayerLikeMob(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.ARMOR, 12.0)
				.add(Attributes.ATTACK_DAMAGE, 16.0)
				.add(Attributes.ATTACK_SPEED, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected final void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
		goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		registerTargetGoals();
	}

	protected abstract void registerTargetGoals();

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || distanceToSqr(target) > 32.0 * 32.0
				|| !getSensing().hasLineOfSight(target)
				|| PowerProtection.isSafeZone(level, target.position())) {
			return;
		}
		switch (PlayerLikeMobRules.castAt(tickCount)) {
			case LIGHTNING -> castLightning(level, target);
			case FIREBALL -> castFireball(level, target);
			case NONE -> {
				// The cadence deliberately leaves most AI ticks free of ranged work.
			}
		}
	}

	private void castLightning(ServerLevel level, LivingEntity target) {
		if (AmethystDampening.isDampened(target)
				|| SpellFieldManager.isSanctuaryProtected(level, target)) {
			PowerFx.cancelled(level, target.position().add(0.0, 1.0, 0.0), 0x7C68FF);
			return;
		}
		var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(target.position());
			level.addFreshEntity(bolt);
		}
		target.hurtServer(level, PowerDamage.source(this), 28.0F);
		PowerFx.beam(level, getEyePosition(), target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 16);
		PowerFx.rune(level, target.position().add(0.0, 0.08, 0.0), 1.5, 0x7C68FF, 20, 0.0);
		PowerFx.sound(level, target.position(), PowersSounds.INTERACTION_CLASH, 1.4F, 0.65F);
	}

	private void castFireball(ServerLevel level, LivingEntity target) {
		Vec3 direction = target.getEyePosition().subtract(getEyePosition()).normalize();
		DarknessFireballProjectile fireball = new DarknessFireballProjectile(level, this, direction);
		fireball.setPos(getEyePosition().add(direction.scale(1.2)));
		level.addFreshEntity(fireball);
		PowerFx.rune(level, getEyePosition(), 0.65, 0x7B173E, 14, tickCount * 0.1);
		PowerFx.sound(level, getEyePosition(), PowersSounds.DARK_WHISPER, 1.1F, 0.8F);
	}
}
