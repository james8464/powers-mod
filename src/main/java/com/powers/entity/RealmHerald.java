package com.powers.entity;

import com.powers.PowersEntities;
import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.realm.RealmHeraldRules;
import com.powers.realm.RealmHeraldSavedData;
import com.powers.realm.RealmKind;
import com.powers.spell.SpellFieldManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Player-shaped realm boss that embodies one opposed primordial force. */
public final class RealmHerald extends AbstractPlayerLikeMob {
	private final ServerBossEvent bossBar;
	private boolean recordedDefeat;

	public RealmHerald(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		RealmKind kind = realmKind();
		if (kind == RealmKind.DARK) addTag(SkillSystem.DARKNESS_TAG);
		bossBar = new ServerBossEvent(getUUID(), name(kind),
				kind == RealmKind.LIGHT ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PURPLE,
				BossEvent.BossBarOverlay.NOTCHED_10);
		bossBar.setDarkenScreen(kind == RealmKind.DARK);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				// 1024 is Minecraft's hard MAX_HEALTH attribute ceiling.
				.add(Attributes.MAX_HEALTH, 1_024.0)
				.add(Attributes.ARMOR, 26.0)
				.add(Attributes.ARMOR_TOUGHNESS, 12.0)
				.add(Attributes.ATTACK_DAMAGE, 38.0)
				.add(Attributes.ATTACK_SPEED, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
	}

	public RealmKind realmKind() {
		return getType() == PowersEntities.LIGHT_HERALD ? RealmKind.LIGHT : RealmKind.DARK;
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<LivingEntity>(this,
				LivingEntity.class, 3, true, false, (target, level) -> target != this
						&& RealmHeraldRules.mayTarget(realmKind(),
						target.entityTags().contains(SkillSystem.DARKNESS_TAG))));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && RealmHeraldRules.mayTarget(realmKind(),
				target.entityTags().contains(SkillSystem.DARKNESS_TAG));
	}

	@Override
	protected boolean radiantCombat() {
		return realmKind() == RealmKind.LIGHT;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		bossBar.setProgress(getHealth() / getMaxHealth());
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || tickCount % 80 != 0
				|| distanceToSqr(target) > 48.0 * 48.0
				|| PowerProtection.isSafeZone(level, target.position())) return;
		if (AmethystDampening.isDampened(target)
				|| SpellFieldManager.isSanctuaryProtected(level, target)) {
			PowerFx.cancelled(level, target.getEyePosition(), 0x9B78FF);
			return;
		}
		float damage = getHealth() <= getMaxHealth() * 0.35F ? 72.0F : 52.0F;
		target.hurtServer(level, PowerDamage.source(this), damage);
		int color = realmKind() == RealmKind.LIGHT ? 0xFFF2A8 : 0x54206E;
		PowerFx.beam(level, getEyePosition(), target.getEyePosition(),
				realmKind() == RealmKind.LIGHT
						? net.minecraft.core.particles.ParticleTypes.END_ROD
						: net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, 24);
		PowerFx.rune(level, target.position(), 3.4, color, 38, tickCount * 0.05);
		PowerFx.spiral(level, target.position(), 0.8, 5.5, color, 30, 0.0);
		PowerFx.sound(level, target.position(), realmKind() == RealmKind.LIGHT
				? PowersSounds.LIGHT_CHORUS : PowersSounds.DARK_WHISPER, 2.0F,
				realmKind() == RealmKind.LIGHT ? 0.85F : 0.48F);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	public void die(DamageSource source) {
		if (!recordedDefeat && level() instanceof ServerLevel level) {
			recordedDefeat = true;
			RealmHeraldSavedData data = level.getServer().overworld().getDataStorage()
					.computeIfAbsent(RealmHeraldSavedData.TYPE);
			data.recordDefeat(level.dimension().identifier().toString(), level.getGameTime());
		}
		super.die(source);
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 2_500;
	}

	private static Component name(RealmKind kind) {
		return Component.translatable(kind == RealmKind.LIGHT
				? "entity.powers.light_herald" : "entity.powers.dark_herald");
	}
}
