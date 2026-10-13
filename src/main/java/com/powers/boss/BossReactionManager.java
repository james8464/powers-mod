package com.powers.boss;

import com.powers.PowerStatusEffects;
import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.entity.FirstVessel;
import com.powers.entity.RealmHerald;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.protection.PowerProtection;
import com.powers.realm.RealmKind;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;

/** Applies finite, readable setup rewards to the two authored boss families. */
public final class BossReactionManager {
	private static final int MAX_OBSERVERS = 32;

	private BossReactionManager() {
	}

	public static int onArtifactActivated(ServerPlayer caster, ArtifactAlignment alignment) {
		ServerLevel level = (ServerLevel) caster.level();
		AABB bounds = caster.getBoundingBox().inflate(BossReactionRules.ARTIFACT_RANGE);
		int reacted = 0;
		for (LivingEntity candidate : BoundedEntityCandidates.living(level, bounds,
				BossReactionRules.MAX_BOSS_CANDIDATES,
				entity -> entity.isAlive() && entity instanceof BossReactionTarget,
				Comparator.comparingDouble((LivingEntity entity) -> entity.distanceToSqr(caster))
						.thenComparing(entity -> entity.getUUID().toString()))) {
			if (!PowerProtection.mayHarm(caster, candidate)) continue;
			if (react(level, candidate, artifactStimulus(alignment))) reacted++;
		}
		return reacted;
	}

	/** Makes either aligned Concord a valid setup against the neutral First Vessel. */
	public static boolean acceptsConcordTarget(LivingEntity target, boolean darkness) {
		BossReactionRules.BossKind kind = kind(target);
		return kind != null && BossReactionRules.reaction(kind, concordStimulus(darkness))
				!= BossReactionRules.Reaction.NONE;
	}

	/** Called only after the authoritative Concord damage transaction succeeds. */
	public static boolean onConcordImpact(ServerLevel level, LivingEntity target,
			boolean darkness) {
		return react(level, target, concordStimulus(darkness));
	}

	private static boolean react(ServerLevel level, LivingEntity target,
			BossReactionRules.Stimulus stimulus) {
		if (!(target instanceof BossReactionTarget state)) return false;
		BossReactionRules.Reaction reaction = BossReactionRules.reaction(
				state.bossReactionKind(), stimulus);
		long tick = level.getServer().getTickCount();
		if (reaction == BossReactionRules.Reaction.NONE
				|| !BossReactionRules.offCooldown(tick, state.lastBossReactionAt())) return false;
		state.markBossReaction(tick);
		int duration = BossReactionRules.reactionDurationTicks(reaction);
		target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, duration, 0, false, true));
		target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, duration, 1, false, true));
		target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, duration, 1, false, true));
		int color = reaction == BossReactionRules.Reaction.COVENANT_BREACH
				? 0xD8B4FE : 0xFFF0A8;
		PowerFx.rune(level, target.position(), 3.8, color, 36, tick * 0.08);
		PowerFx.burst(level, target.getEyePosition(), PowersParticles.FRACTURE, 12, 0.8, 0.08);
		PowerFx.sound(level, target.position(), reaction == BossReactionRules.Reaction.COVENANT_BREACH
				? PowersSounds.DARK_WHISPER : PowersSounds.LIGHT_CHORUS, 1.4F, 0.72F);
		announce(level, target, reaction);
		return true;
	}

	private static void announce(ServerLevel level, LivingEntity target,
			BossReactionRules.Reaction reaction) {
		AABB bounds = target.getBoundingBox().inflate(64.0);
		for (ServerPlayer observer : BoundedEntityCandidates.ofClass(level, ServerPlayer.class,
				bounds, MAX_OBSERVERS, player -> !player.isSpectator())) {
			PowerMessages.overlay(observer, Component.translatable(reaction
					== BossReactionRules.Reaction.COVENANT_BREACH
					? "boss.powers.reaction.covenant_breach"
					: "boss.powers.reaction.opposition_stagger"));
		}
	}

	private static BossReactionRules.Stimulus artifactStimulus(ArtifactAlignment alignment) {
		return alignment == ArtifactAlignment.DARKNESS
				? BossReactionRules.Stimulus.DARK_ARTIFACT
				: BossReactionRules.Stimulus.LIGHT_ARTIFACT;
	}

	private static BossReactionRules.Stimulus concordStimulus(boolean darkness) {
		return darkness ? BossReactionRules.Stimulus.UMBRAL_CONCORD
				: BossReactionRules.Stimulus.RADIANT_CONCORD;
	}

	private static BossReactionRules.BossKind kind(LivingEntity target) {
		if (target instanceof FirstVessel) return BossReactionRules.BossKind.FIRST_VESSEL;
		if (target instanceof RealmHerald herald) {
			return herald.realmKind() == RealmKind.LIGHT
					? BossReactionRules.BossKind.LIGHT_HERALD
					: BossReactionRules.BossKind.DARK_HERALD;
		}
		return null;
	}
}
