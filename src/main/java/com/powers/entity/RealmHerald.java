package com.powers.entity;

import com.powers.PowersEntities;
import com.powers.PowersSounds;
import com.powers.PowerStatusEffects;
import com.powers.animation.CastingHand;
import com.powers.animation.CastingPose;
import com.powers.animation.CastingPoseMapping;
import com.powers.animation.CastingPoseService;
import com.powers.fx.PowerFx;
import com.powers.boss.BossReactionRules;
import com.powers.boss.BossReactionTarget;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.realm.RealmHeraldRules;
import com.powers.realm.RealmHeraldCourtRules;
import com.powers.realm.RealmHeraldSavedData;
import com.powers.realm.RealmKind;
import com.powers.spell.SpellFieldManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.Locale;

/** Player-shaped realm boss that embodies one opposed primordial force. */
public final class RealmHerald extends AbstractPlayerLikeMob implements BossReactionTarget {
	private final ServerBossEvent bossBar;
	private boolean recordedDefeat;
	private long lastBossReactionAt = Long.MIN_VALUE;
	private RealmHeraldCourtRules.Phase courtPhase = RealmHeraldCourtRules.Phase.VERDICT;
	private int scaledParticipants;
	private long nextSummonAt;
	private boolean courtEncounter;

	public RealmHerald(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		RealmKind kind = realmKind();
		if (kind == RealmKind.DARK) addTag(SkillSystem.DARKNESS_TAG);
		bossBar = new ServerBossEvent(getUUID(), phaseName(kind, courtPhase),
				kind == RealmKind.LIGHT ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PURPLE,
				BossEvent.BossBarOverlay.NOTCHED_10);
		applyBossPresentation();
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

	/** Exposes the persisted encounter phase for diagnostics and acceptance tests. */
	public RealmHeraldCourtRules.Phase courtPhase() {
		return courtPhase;
	}

	/** Marks a manager-spawned Herald as the persistent encounter for its completed Court. */
	public void beginCourtEncounter() {
		courtEncounter = true;
		setPersistenceRequired();
	}

	@Override
	public BossReactionRules.BossKind bossReactionKind() {
		return realmKind() == RealmKind.LIGHT ? BossReactionRules.BossKind.LIGHT_HERALD
				: BossReactionRules.BossKind.DARK_HERALD;
	}

	@Override
	public long lastBossReactionAt() {
		return lastBossReactionAt;
	}

	@Override
	public void markBossReaction(long tick) {
		lastBossReactionAt = tick;
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
	protected boolean usesSharedRangedCombat() {
		return false;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		RealmHeraldCourtRules.Profile profile = updateCourtState(level);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || tickCount % profile.beamCadenceTicks() != 0
				|| distanceToSqr(target) > 48.0 * 48.0
				|| PowerProtection.isSafeZone(level, target.position())) return;
		if (AmethystDampening.isDampened(target)
				|| SpellFieldManager.isSanctuaryProtected(level, target)) {
			PowerFx.cancelled(level, target.getEyePosition(), 0x9B78FF);
			return;
		}
		target.hurtServer(level, PowerDamage.source(this), profile.beamDamage());
		int color = realmKind() == RealmKind.LIGHT ? 0xFFF2A8 : 0x54206E;
		PowerFx.beam(level, getEyePosition(), target.getEyePosition(),
				realmKind() == RealmKind.LIGHT
						? com.powers.PowersParticles.GLYPH
						: net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, 24);
		PowerFx.rune(level, target.position(), 3.4, color, 38, tickCount * 0.05);
		PowerFx.spiral(level, target.position(), 0.8, 5.5, color, 30, 0.0);
		PowerFx.sound(level, target.position(), realmKind() == RealmKind.LIGHT
				? PowersSounds.LIGHT_CHORUS : PowersSounds.DARK_WHISPER, 2.0F,
				realmKind() == RealmKind.LIGHT ? 0.85F : 0.48F);
		CastingPoseService.start(this, CastingPose.CHANNEL, CastingPoseMapping.style(this),
				CastingHand.BOTH, 20);
	}

	private RealmHeraldCourtRules.Profile updateCourtState(ServerLevel level) {
		float healthRatio = Math.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F);
		bossBar.setProgress(healthRatio);
		if (scaledParticipants == 0) scaledParticipants = countParticipants(level);
		RealmHeraldCourtRules.Phase nextPhase = RealmHeraldCourtRules.phase(healthRatio);
		if (nextPhase != courtPhase) {
			courtPhase = nextPhase;
			applyBossPresentation();
		}
		RealmHeraldCourtRules.Profile profile = RealmHeraldCourtRules.profile(courtPhase,
				scaledParticipants == 0 ? 1 : scaledParticipants);
		if (courtEncounter) {
			if (tickCount % profile.hazardPulseTicks() == 0) pulseReversibleHazard(level, profile);
			maintainCourtSummons(level, profile);
		}
		return profile;
	}

	private int countParticipants(ServerLevel level) {
		int participants = 0;
		for (ServerPlayer player : level.players()) {
			if (participants >= RealmHeraldCourtRules.MAX_PARTICIPANTS) break;
			if (player.isAlive() && !player.isSpectator() && distanceToSqr(player) <= 96.0 * 96.0
					&& RealmHeraldRules.mayTarget(realmKind(),
					player.entityTags().contains(SkillSystem.DARKNESS_TAG))) participants++;
		}
		return participants;
	}

	private void pulseReversibleHazard(ServerLevel level,
			RealmHeraldCourtRules.Profile profile) {
		int affected = 0;
		double radiusSquared = profile.hazardRadius() * profile.hazardRadius();
		for (ServerPlayer player : level.players()) {
			if (affected >= RealmHeraldCourtRules.MAX_PARTICIPANTS) break;
			if (!player.isAlive() || player.isSpectator() || distanceToSqr(player) > radiusSquared
					|| !RealmHeraldRules.mayTarget(realmKind(),
					player.entityTags().contains(SkillSystem.DARKNESS_TAG))
					|| PowerProtection.isSafeZone(level, player.position())
					|| AmethystDampening.isDampened(player)
					|| SpellFieldManager.isSanctuaryProtected(level, player)) continue;
			player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS,
					profile.hazardDurationTicks(), courtPhase.ordinal(), false, true));
			player.addEffect(PowerStatusEffects.hidden(realmKind() == RealmKind.DARK
					? MobEffects.DARKNESS : MobEffects.GLOWING,
					profile.hazardDurationTicks(), 0, false, true));
			affected++;
		}
		int color = realmKind() == RealmKind.LIGHT ? 0xFFF2A8 : 0x54206E;
		PowerFx.rune(level, position(), profile.hazardRadius(), color,
				18 + courtPhase.ordinal() * 8, tickCount * 0.04);
	}

	private void maintainCourtSummons(ServerLevel level,
			RealmHeraldCourtRules.Profile profile) {
		long gameTime = level.getGameTime();
		if (gameTime < nextSummonAt) return;
		nextSummonAt = gameTime + profile.summonCadenceTicks();
		String tag = courtSummonTag();
		var summons = com.powers.util.BoundedEntityCandidates.ofClass(level,
				AbstractPlayerLikeMob.class, new AABB(blockPosition()).inflate(64.0),
				32, candidate -> candidate.isAlive() && candidate.entityTags().contains(tag));
		if (summons.size() >= profile.summonCap()) return;
		AbstractPlayerLikeMob summon = (realmKind() == RealmKind.LIGHT
				? PowersEntities.RADIANT_SENTINEL : PowersEntities.DARKNESS_CREATURE)
				.create(level, EntitySpawnReason.EVENT);
		if (summon == null) return;
		double angle = Math.floorMod(getUUID().getLeastSignificantBits()
				+ gameTime / profile.summonCadenceTicks(), 8L)
				* Math.PI / 4.0;
		summon.setPos(getX() + Math.cos(angle) * 5.0, getY(), getZ() + Math.sin(angle) * 5.0);
		summon.finalizeSpawn(level, level.getCurrentDifficultyAt(summon.blockPosition()),
				EntitySpawnReason.EVENT, null);
		summon.configureGuardian(null, profile.summonLifetimeTicks(),
				courtPhase == RealmHeraldCourtRules.Phase.LAST_COVENANT);
		summon.addTag(tag);
		level.addFreshEntity(summon);
	}

	private String courtSummonTag() {
		return "powers_herald_court_" + getUUID();
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
			cleanupLoadedCourtSummons(level);
		}
		bossBar.removeAllPlayers();
		super.die(source);
	}

	private void cleanupLoadedCourtSummons(ServerLevel level) {
		String tag = courtSummonTag();
		com.powers.util.BoundedEntityCandidates.ofClass(level, AbstractPlayerLikeMob.class,
				new AABB(blockPosition()).inflate(96.0), 64,
				candidate -> candidate.entityTags().contains(tag)).forEach(AbstractPlayerLikeMob::discard);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("PowersHeraldCourtPhase", courtPhase.ordinal());
		output.putInt("PowersHeraldCourtParticipants", scaledParticipants);
		output.putLong("PowersHeraldCourtNextSummon", nextSummonAt);
		output.putBoolean("PowersHeraldCourtEncounter", courtEncounter);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		courtPhase = RealmHeraldCourtRules.restoredPhase(
				input.getIntOr("PowersHeraldCourtPhase", 0));
		scaledParticipants = input.getIntOr("PowersHeraldCourtParticipants", 0);
		if (scaledParticipants != 0) {
			scaledParticipants = RealmHeraldCourtRules.normalizeParticipants(scaledParticipants);
		}
		nextSummonAt = Math.max(0L, input.getLongOr("PowersHeraldCourtNextSummon", 0L));
		courtEncounter = input.getBooleanOr("PowersHeraldCourtEncounter", false);
		applyBossPresentation();
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 2_500;
	}

	private void applyBossPresentation() {
		bossBar.setName(phaseName(realmKind(), courtPhase));
		bossBar.setColor(courtPhase == RealmHeraldCourtRules.Phase.LAST_COVENANT
				? BossEvent.BossBarColor.RED
				: realmKind() == RealmKind.LIGHT
						? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PURPLE);
		bossBar.setDarkenScreen(realmKind() == RealmKind.DARK);
	}

	private static Component phaseName(RealmKind kind, RealmHeraldCourtRules.Phase phase) {
		return Component.translatable("boss.powers.herald."
				+ kind.name().toLowerCase(Locale.ROOT) + "."
				+ phase.name().toLowerCase(Locale.ROOT));
	}
}
