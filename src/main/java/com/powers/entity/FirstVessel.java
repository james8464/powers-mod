package com.powers.entity;

import com.powers.PowersSounds;
import com.powers.boss.FirstVesselCombat;
import com.powers.boss.FirstVesselPhase;
import com.powers.boss.FirstVesselPowerAction;
import com.powers.boss.FirstVesselPowerCatalogue;
import com.powers.boss.FirstVesselRules;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * Player-shaped tactical raid boss whose 28 adapters mirror every innate power.
 * The planner inspects a maximum of 24 actions on a ten-tick cadence and all
 * area queries use the same hard entity cap.
 */
public final class FirstVessel extends AbstractPlayerLikeMob {
	private static final int RECONSTITUTION_TICKS = 100;
	private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(),
			Component.translatable("entity.powers.first_vessel"),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
	private final Map<String, Integer> lastActionAt = new HashMap<>();
	private FirstVesselPhase phase = FirstVesselPhase.AWAKENING;
	private int actionCursor;
	private int lastCastAt = -1_000;
	private int scaledPlayers;
	private float effectiveMaximumHealth = FirstVesselRules.BASE_HEALTH;
	private float effectiveHealth = FirstVesselRules.BASE_HEALTH;
	private boolean reconstitutionUsed;
	private int reconstitutionTicks;
	private float reconstitutionDamage;
	private boolean lastFirmamentUsed;

	public FirstVessel(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setCustomName(Component.translatable("entity.powers.first_vessel"));
		setCustomNameVisible(true);
		setCanPickUpLoot(false);
		bossEvent.setDarkenScreen(true).setPlayBossMusic(true).setCreateWorldFog(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				// Vanilla clamps this attribute; the boss layers its 5000+ vitality above it.
				.add(Attributes.MAX_HEALTH, 1_024.0)
				.add(Attributes.ARMOR, 24.0)
				.add(Attributes.ARMOR_TOUGHNESS, 16.0)
				.add(Attributes.ATTACK_DAMAGE, 36.0)
				.add(Attributes.ATTACK_KNOCKBACK, 2.0)
				.add(Attributes.ATTACK_SPEED, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.FOLLOW_RANGE, 96.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.85);
	}

	@Override
	protected void registerTargetGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this,
				Player.class, 5, true, false,
				(target, level) -> target instanceof Player player
						&& !player.isCreative() && !player.isSpectator()));
	}

	@Override
	protected boolean usesSharedRangedCombat() {
		return false;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		updateBossState(level);
		if (reconstitutionTicks > 0) {
			tickReconstitution(level);
			return;
		}
		double healthRatio = effectiveHealthRatio();
		if (FirstVesselRules.shouldBeginReconstitution(healthRatio, reconstitutionUsed)) {
			beginReconstitution(level);
			return;
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) return;

		if (phase != FirstVesselPhase.AWAKENING && tickCount % 240 == 0) {
			announce(level, "boss.powers.first_vessel.world_suture");
			FirstVesselCombat.worldSuture(level, this);
			lastCastAt = tickCount;
			return;
		}
		if (phase == FirstVesselPhase.LAST_COVENANT && healthRatio < 0.15
				&& !lastFirmamentUsed) {
			lastFirmamentUsed = true;
			announce(level, "boss.powers.first_vessel.last_firmament");
			FirstVesselCombat.lastFirmament(level, this);
			lastCastAt = tickCount;
			return;
		}
		if (tickCount % 260 == 0 && target instanceof ServerPlayer player
				&& castStolenPower(level, player)) return;
		if (!FirstVesselRules.planningTick(tickCount)
				|| tickCount - lastCastAt < FirstVesselRules.castInterval(phase)) return;
		castFromDeck(level, target);
	}

	private void updateBossState(ServerLevel level) {
		bossEvent.setProgress(Math.clamp(effectiveHealthRatio(), 0.0F, 1.0F));
		if (tickCount % 100 == 0) updatePlayerScaling(level);
		FirstVesselPhase nextPhase = FirstVesselRules.phase(
				effectiveHealthRatio());
		if (nextPhase != phase) {
			phase = nextPhase;
			bossEvent.setColor(phase == FirstVesselPhase.LAST_COVENANT
					? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
			bossEvent.setName(Component.translatable("boss.powers.first_vessel.phase."
					+ phase.name().toLowerCase(java.util.Locale.ROOT)));
			announce(level, "boss.powers.first_vessel.phase_"
					+ phase.name().toLowerCase(java.util.Locale.ROOT));
			sevenfoldStep(level);
		}
		if (!hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE)
				&& getAttribute(Attributes.SCALE).getBaseValue() != 1.0) {
			getAttribute(Attributes.SCALE).setBaseValue(1.0);
		}
	}

	private void updatePlayerScaling(ServerLevel level) {
		int players = 0;
		for (ServerPlayer player : level.players()) {
			if (players >= FirstVesselRules.MAX_CANDIDATES) break;
			if (player.isAlive() && !player.isSpectator()
					&& player.distanceToSqr(this) <= 96.0 * 96.0) players++;
		}
		players = Math.max(1, players);
		if (players == scaledPlayers) return;
		float ratio = effectiveHealthRatio();
		effectiveMaximumHealth = (float) (FirstVesselRules.BASE_HEALTH
				* FirstVesselRules.playerScale(players));
		effectiveHealth = Math.max(1.0F, effectiveMaximumHealth * ratio);
		scaledPlayers = players;
	}

	private void castFromDeck(ServerLevel level, LivingEntity target) {
		var deck = FirstVesselPowerCatalogue.deck(phase);
		for (int checked = 0; checked < deck.size(); checked++) {
			int index = Math.floorMod(actionCursor + checked, deck.size());
			FirstVesselPowerAction action = deck.get(index);
			int readyAt = lastActionAt.getOrDefault(action.powerId(), -1_000)
					+ action.cooldownTicks();
			if (tickCount < readyAt) continue;
			actionCursor = index + 1;
			lastActionAt.put(action.powerId(), tickCount);
			lastCastAt = tickCount;
			FirstVesselCombat.cast(level, this, target, action, phase);
			return;
		}
	}

	private boolean castStolenPower(ServerLevel level, ServerPlayer target) {
		for (String slot : PlayerPowers.get(target).getSlotIds()) {
			String path = slot.indexOf(':') >= 0 ? slot.substring(slot.indexOf(':') + 1) : slot;
			FirstVesselPowerAction action = FirstVesselPowerCatalogue.actions().stream()
					.filter(candidate -> candidate.powerId().equals(path)).findFirst().orElse(null);
			if (action == null) continue;
			announce(level, "boss.powers.first_vessel.power_theft");
			FirstVesselCombat.cast(level, this, target, action, phase);
			lastActionAt.put(action.powerId(), tickCount);
			lastCastAt = tickCount;
			return true;
		}
		return false;
	}

	private void sevenfoldStep(ServerLevel level) {
		LivingEntity target = getTarget();
		if (target == null) return;
		for (int step = 0; step < 7; step++) {
			double angle = step * Math.PI * 2.0 / 7.0;
			PowerFx.spiral(level, target.position().add(Math.cos(angle) * 5.0, 0,
					Math.sin(angle) * 5.0), 0.65, 3.0, 0x8C4AA3, 12, angle);
		}
		double angle = (tickCount % 7) * Math.PI * 2.0 / 7.0;
		randomTeleport(target.getX() + Math.cos(angle) * 5.0, target.getY(),
				target.getZ() + Math.sin(angle) * 5.0, true);
	}

	private void beginReconstitution(ServerLevel level) {
		reconstitutionUsed = true;
		reconstitutionTicks = RECONSTITUTION_TICKS;
		reconstitutionDamage = 0.0F;
		announce(level, "boss.powers.first_vessel.reconstitution");
		PowerFx.sound(level, position(), PowersSounds.DARK_WHISPER, 2.0F, 0.4F);
	}

	private void tickReconstitution(ServerLevel level) {
		setDeltaMovement(0.0, 0.0, 0.0);
		if (FirstVesselRules.channelInterrupted(reconstitutionDamage, effectiveMaximumHealth)) {
			reconstitutionTicks = 0;
			announce(level, "boss.powers.first_vessel.interrupted");
			PowerFx.cancelled(level, position().add(0, 1, 0), 0xA878BC);
			return;
		}
		if (reconstitutionTicks % 5 == 0) {
			double progress = 1.0 - reconstitutionTicks / (double) RECONSTITUTION_TICKS;
			PowerFx.rune(level, position(), 2.5 + progress * 5.5, 0x69357A,
					28, level.getGameTime() * 0.09);
			PowerFx.spiral(level, position(), 1.8, 6.0, 0xC697D5, 24, progress * Math.PI * 2.0);
		}
		if (--reconstitutionTicks == 0) {
			heal(effectiveMaximumHealth * 0.25F);
			announce(level, "boss.powers.first_vessel.restored");
			PowerFx.burst(level, position().add(0, 1, 0),
					net.minecraft.core.particles.ColorParticleOption.create(
							ParticleTypes.FLASH, 0xFFEBD7FF), 6, 0.7, 0.0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (amount <= 0.0F || effectiveHealth <= 0.0F) return false;
		if (getHealth() <= 1.5F && effectiveHealth > 1.0F) setHealth(getMaxHealth());
		float beforeLayer = getHealth();
		float safeAmount = Math.min(amount, Math.max(0.5F, beforeLayer - 1.0F));
		boolean hurt = super.hurtServer(level, source, safeAmount);
		if (!hurt) return false;
		float layerLoss = Math.max(0.0F, beforeLayer - getHealth());
		float effectiveLoss = safeAmount > 0.0F && amount > safeAmount
				? layerLoss * amount / safeAmount : layerLoss;
		effectiveLoss = Math.max(0.01F, effectiveLoss);
		effectiveHealth = Math.max(0.0F, effectiveHealth - effectiveLoss);
		if (reconstitutionTicks > 0) reconstitutionDamage += effectiveLoss;
		if (effectiveHealth <= 0.0F) {
			setHealth(Math.max(1.0F, getHealth()));
			super.hurtServer(level, source, Float.MAX_VALUE);
		} else if (getHealth() <= 1.0F) {
			setHealth(Math.min(getMaxHealth(), effectiveHealth));
		}
		return true;
	}

	@Override
	public void heal(float amount) {
		if (amount <= 0.0F) return;
		effectiveHealth = Math.min(effectiveMaximumHealth, effectiveHealth + amount);
		super.heal(Math.min(amount, getMaxHealth() - getHealth()));
	}

	public float effectiveHealth() {
		return effectiveHealth;
	}

	public float effectiveMaximumHealth() {
		return effectiveMaximumHealth;
	}

	private float effectiveHealthRatio() {
		return effectiveHealth / Math.max(1.0F, effectiveMaximumHealth);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	private void announce(ServerLevel level, String key) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) <= 128.0 * 128.0) {
				PowerMessages.overlay(player, Component.translatable(key));
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("PowersFirstVesselPhase", phase.ordinal());
		output.putInt("PowersFirstVesselCursor", actionCursor);
		output.putBoolean("PowersFirstVesselReconstitution", reconstitutionUsed);
		output.putBoolean("PowersFirstVesselFirmament", lastFirmamentUsed);
		output.putFloat("PowersFirstVesselHealth", effectiveHealth);
		output.putFloat("PowersFirstVesselMaxHealth", effectiveMaximumHealth);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int storedPhase = Math.clamp(input.getIntOr("PowersFirstVesselPhase", 0),
				0, FirstVesselPhase.values().length - 1);
		phase = FirstVesselPhase.values()[storedPhase];
		actionCursor = Math.max(0, input.getIntOr("PowersFirstVesselCursor", 0));
		reconstitutionUsed = input.getBooleanOr("PowersFirstVesselReconstitution", false);
		lastFirmamentUsed = input.getBooleanOr("PowersFirstVesselFirmament", false);
		effectiveMaximumHealth = Math.max(FirstVesselRules.BASE_HEALTH,
				input.getFloatOr("PowersFirstVesselMaxHealth", FirstVesselRules.BASE_HEALTH));
		effectiveHealth = Math.clamp(input.getFloatOr("PowersFirstVesselHealth",
				effectiveMaximumHealth), 1.0F, effectiveMaximumHealth);
	}
}
