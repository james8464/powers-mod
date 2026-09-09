package com.powers.progression;

import com.powers.entity.FirstVessel;
import com.powers.entity.RealmHerald;
import com.powers.player.SkillQuestTracker;
import com.powers.player.SkillSystem;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Server-thread runtime for bounded, once-per-boss contribution credit. */
public final class BossContributionManager {
	private static final BossContributionLedger LEDGER = new BossContributionLedger();

	private BossContributionManager() { }

	public static boolean isSupportedBoss(LivingEntity entity) {
		return entity instanceof RealmHerald || entity instanceof FirstVessel;
	}

	public static void recordDamage(LivingEntity victim, DamageSource source, float damageTaken) {
		if (!isSupportedBoss(victim) || !(source.getEntity() instanceof ServerPlayer player)
				|| !eligibleAt(victim, player) || !BossContributionRules.validAmount(damageTaken)) return;
		LEDGER.noteDamage(victim.getUUID(), player.getUUID(), damageTaken,
				victim.level().getServer().getTickCount());
	}

	/** Credits real support for a player already participating in a nearby encounter. */
	public static void recordSupport(ServerPlayer actor, ServerPlayer beneficiary, float amount) {
		if (actor == null || beneficiary == null || actor == beneficiary
				|| actor.level() != beneficiary.level() || !BossContributionRules.validAmount(amount)
				|| !(actor.level() instanceof ServerLevel level)) return;
		long tick = level.getServer().getTickCount();
		for (LivingEntity boss : nearbyBosses(level, actor)) {
			if (!eligibleAt(boss, beneficiary)
					|| !LEDGER.hasActivity(boss.getUUID(), beneficiary.getUUID())) continue;
			LEDGER.noteSupport(boss.getUUID(), actor.getUUID(), amount, tick);
		}
	}

	/** Records only control duration that the caller actually applied to a supported boss. */
	public static void recordControl(ServerPlayer actor, LivingEntity boss, int appliedTicks) {
		if (actor == null || !isSupportedBoss(boss) || !eligibleAt(boss, actor)
				|| appliedTicks <= 0) return;
		LEDGER.noteControl(boss.getUUID(), actor.getUUID(), appliedTicks,
				boss.level().getServer().getTickCount());
	}

	/** Resolves one encounter and removes its ledger before granting any rewards. */
	public static int resolve(LivingEntity boss, DamageSource source) {
		if (!isSupportedBoss(boss) || !(boss.level() instanceof ServerLevel level)) return 0;
		if (source != null && source.getEntity() instanceof ServerPlayer killer && eligibleAt(boss, killer)) {
			LEDGER.noteDamage(boss.getUUID(), killer.getUUID(),
					BossContributionRules.DAMAGE_THRESHOLD, level.getServer().getTickCount());
		}
		List<java.util.UUID> qualified = LEDGER.resolve(boss.getUUID(),
				level.getServer().getTickCount(), id -> {
					ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
					return player != null && eligibleAt(boss, player);
				});
		for (java.util.UUID id : qualified) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) SkillQuestTracker.recordBossContribution(player);
		}
		return qualified.size();
	}

	public static void clear() {
		LEDGER.clear();
	}

	private static List<LivingEntity> nearbyBosses(ServerLevel level, ServerPlayer actor) {
		double radius = BossContributionRules.ENCOUNTER_RADIUS;
		return BoundedEntityCandidates.living(level,
				AABB.ofSize(actor.position(), radius * 2.0, radius * 2.0, radius * 2.0),
				8, BossContributionManager::isSupportedBoss);
	}

	private static boolean eligibleAt(LivingEntity boss, ServerPlayer player) {
		double radius = BossContributionRules.ENCOUNTER_RADIUS;
		return player.isAlive() && !player.isSpectator() && player.level() == boss.level()
				&& !SkillSystem.hasDarknessTag(player)
				&& player.distanceToSqr(boss) <= radius * radius;
	}
}
