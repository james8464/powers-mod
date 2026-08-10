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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Shared player-scale movement, attributes, and bounded lightning/fireball attacks. */
public abstract class AbstractPlayerLikeMob extends Monster {
	private UUID guardianOwner;
	private int guardianLifetime = -1;
	private boolean eliteGuardian;

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

	/** Configures a bounded owned summon; natural realm creatures remain unowned. */
	public final void configureGuardian(UUID ownerId, int lifetimeTicks, boolean elite) {
		guardianOwner = ownerId;
		guardianLifetime = GuardianFactionRules.normalizeLifetime(Math.max(1, lifetimeTicks));
		eliteGuardian = elite;
		setPersistenceRequired();
		if (elite) {
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(240.0);
			getAttribute(Attributes.ARMOR).setBaseValue(22.0);
			getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(34.0);
			setHealth(getMaxHealth());
		}
	}

	public final UUID guardianOwner() {
		return guardianOwner;
	}

	public final boolean eliteGuardian() {
		return eliteGuardian;
	}

	/** True for finite artifact summons; natural realm creatures are not globally capped. */
	public final boolean temporaryGuardian() {
		return guardianLifetime > 0;
	}

	protected boolean radiantCombat() {
		return false;
	}

	/** Boss subclasses own a complete tactical catalogue instead of this simple pair. */
	protected boolean usesSharedRangedCombat() {
		return true;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (guardianLifetime > 0) {
			guardianLifetime--;
			var owner = guardianOwner == null ? null
					: level.getServer().getPlayerList().getPlayer(guardianOwner);
			boolean ownerPresent = guardianOwner == null || owner != null && owner.level() == level;
			if (GuardianFactionRules.shouldExpire(guardianLifetime, ownerPresent)) {
				discard();
				return;
			}
		}
		if (guardianOwner != null && GuardianFieldRules.pulseAt(tickCount, eliteGuardian)) {
			GuardianAlignmentField.pulse(level, this, radiantCombat()
					? com.powers.item.artifact.ArtifactAlignment.LIGHT
					: com.powers.item.artifact.ArtifactAlignment.DARKNESS);
		}
		LivingEntity target = getTarget();
		if (!usesSharedRangedCombat()) return;
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
		DarknessFireballProjectile fireball = new DarknessFireballProjectile(
				level, this, direction, radiantCombat());
		fireball.setPos(getEyePosition().add(direction.scale(1.2)));
		level.addFreshEntity(fireball);
		PowerFx.rune(level, getEyePosition(), 0.65,
				radiantCombat() ? 0xFFE89B : 0x7B173E, 14, tickCount * 0.1);
		PowerFx.sound(level, getEyePosition(), radiantCombat()
				? PowersSounds.LIGHT_CHORUS : PowersSounds.DARK_WHISPER, 1.1F,
				radiantCombat() ? 1.35F : 0.8F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (guardianOwner != null) output.putString("PowersGuardianOwner", guardianOwner.toString());
		output.putInt("PowersGuardianLifetime", guardianLifetime);
		output.putBoolean("PowersEliteGuardian", eliteGuardian);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String owner = input.getStringOr("PowersGuardianOwner", "");
		try {
			guardianOwner = owner.isBlank() ? null : UUID.fromString(owner);
		} catch (IllegalArgumentException ignored) {
			guardianOwner = null;
		}
		guardianLifetime = GuardianFactionRules.normalizeLifetime(
				input.getIntOr("PowersGuardianLifetime", -1));
		eliteGuardian = input.getBooleanOr("PowersEliteGuardian", false);
	}
}
