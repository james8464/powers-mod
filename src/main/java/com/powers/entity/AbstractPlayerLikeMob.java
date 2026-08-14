package com.powers.entity;

import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.power.artifact.ArtifactGuardianSummons;
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
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Shared player-scale movement, attributes, and bounded lightning/fireball attacks. */
public abstract class AbstractPlayerLikeMob extends Monster {
	private static final double NORMAL_GUARDIAN_HEALTH = 100.0;
	private static final double NORMAL_GUARDIAN_ARMOR = 12.0;
	private static final double NORMAL_GUARDIAN_DAMAGE = 16.0;
	private static final double ELITE_GUARDIAN_HEALTH = 240.0;
	private static final double ELITE_GUARDIAN_ARMOR = 22.0;
	private static final double ELITE_GUARDIAN_DAMAGE = 34.0;
	private @Nullable LongLivedSummonRecord summonRecord;

	protected AbstractPlayerLikeMob(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, NORMAL_GUARDIAN_HEALTH)
				.add(Attributes.ARMOR, NORMAL_GUARDIAN_ARMOR)
				.add(Attributes.ATTACK_DAMAGE, NORMAL_GUARDIAN_DAMAGE)
				.add(Attributes.ATTACK_SPEED, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected final void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new GuardianTacticalGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
		goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		registerTargetGoals();
	}

	protected abstract void registerTargetGoals();

	/** Configures a bounded owned summon; natural realm creatures remain unowned. */
	public final void configureGuardian(@Nullable UUID ownerId, int lifetimeTicks, boolean elite) {
		ArtifactGuardianSummons.rebindLoaded(this, () -> {
			summonRecord = LongLivedSummonRecord.create(getUUID(), ownerId,
					ownerId == null ? LongLivedSummonRecord.Task.INVADE
							: LongLivedSummonRecord.Task.GUARD,
					elite ? LongLivedSummonRecord.Archetype.ELITE
							: LongLivedSummonRecord.Archetype.NORMAL,
					level().getGameTime(), lifetimeTicks);
			setPersistenceRequired();
			applyGuardianArchetype(elite, true);
		});
	}

	private void applyGuardianArchetype(boolean elite, boolean refillElite) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(
				elite ? ELITE_GUARDIAN_HEALTH : NORMAL_GUARDIAN_HEALTH);
		getAttribute(Attributes.ARMOR).setBaseValue(
				elite ? ELITE_GUARDIAN_ARMOR : NORMAL_GUARDIAN_ARMOR);
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
				elite ? ELITE_GUARDIAN_DAMAGE : NORMAL_GUARDIAN_DAMAGE);
		setHealth(refillElite && elite ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
	}

	public final @Nullable UUID guardianOwner() {
		return summonRecord == null ? null : summonRecord.ownerId();
	}

	public final boolean eliteGuardian() {
		return summonRecord != null
				&& summonRecord.archetype() == LongLivedSummonRecord.Archetype.ELITE;
	}

	/** True for finite artifact summons; natural realm creatures are not globally capped. */
	public final boolean temporaryGuardian() {
		return summonRecord != null;
	}

	/** Exposes the immutable persisted contract without exposing derived guardian indexes. */
	public final @Nullable LongLivedSummonRecord summonRecord() {
		return summonRecord;
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
		if (summonRecord != null) {
			UUID guardianOwner = summonRecord.ownerId();
			var owner = guardianOwner == null ? null
					: level.getServer().getPlayerList().getPlayer(guardianOwner);
			ArtifactAlignment alignment = radiantCombat()
					? ArtifactAlignment.LIGHT : ArtifactAlignment.DARKNESS;
			boolean ownerPresent = guardianOwner == null || owner != null && owner.level() == level
					&& ArtifactWeaponManager.carries(owner, alignment)
					&& ArtifactWeaponManager.authorized(owner, alignment);
			if (summonRecord.expiredAt(level.getGameTime()) || !ownerPresent) {
				discard();
				return;
			}
		}
		if (guardianOwner() != null && GuardianFieldRules.pulseAt(tickCount, eliteGuardian())) {
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
		if (summonRecord != null) summonRecord.write(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		long gameTime = level().getGameTime();
		summonRecord = LongLivedSummonRecord.read(input, getUUID(), gameTime).orElseGet(() -> {
			int legacyLifetime = GuardianFactionRules.normalizeLifetime(
					input.getIntOr("PowersGuardianLifetime", -1));
			if (legacyLifetime < 0) return null;
			String encodedOwner = input.getStringOr("PowersGuardianOwner", "");
			UUID ownerId;
			try {
				ownerId = encodedOwner.isBlank() ? null : UUID.fromString(encodedOwner);
			} catch (IllegalArgumentException ignored) {
				ownerId = null;
			}
			return LongLivedSummonRecord.fromLegacy(getUUID(), ownerId, legacyLifetime,
					input.getBooleanOr("PowersEliteGuardian", false), gameTime);
		});
		if (summonRecord != null) applyGuardianArchetype(eliteGuardian(), false);
	}
}
