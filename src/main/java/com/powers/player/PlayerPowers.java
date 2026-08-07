package com.powers.player;

import com.mojang.serialization.Codec;
import com.powers.network.PowersPackets;
import com.powers.player.SkillSystem;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PassiveEffect;
import com.powers.power.Ability;
import com.powers.power.PowerEnergy;
import com.powers.PowersEffects;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Per-player power state: three power slots and active toggles, stored
 * as persistent + synced data attachments on the player entity so the
 * assignment survives restarts and never changes between logins.
 */
public final class PlayerPowers {
	public static final int SLOT_COUNT = 3;

	private static final AttachmentType<List<String>> POWER_SLOTS = AttachmentRegistry.create(
			com.powers.PowersMod.id("power_slots"),
			builder -> builder
					.initializer(ArrayList::new)
					.persistent(Codec.STRING.listOf())
					.syncWith(ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
							AttachmentSyncPredicate.targetOnly()));

	private static final AttachmentType<List<String>> ACTIVE_TOGGLES = AttachmentRegistry.create(
			com.powers.PowersMod.id("active_toggles"),
			builder -> builder
					.initializer(ArrayList::new)
					.persistent(Codec.STRING.listOf())
					.syncWith(ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
							AttachmentSyncPredicate.targetOnly()));

	private static final AttachmentType<Integer> ENERGY = AttachmentRegistry.create(
				com.powers.PowersMod.id("energy"),
				builder -> builder
						.initializer(() -> PowerEnergy.BASE_MAX)
						.persistent(Codec.INT)
						.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_INT,
								AttachmentSyncPredicate.targetOnly()));

	private static final AttachmentType<Integer> DARKNESS_ENERGY = AttachmentRegistry.create(
				com.powers.PowersMod.id("darkness_energy"),
				builder -> builder
						.initializer(() -> PowerEnergy.darknessMaxCapacity(0))
						.persistent(Codec.INT)
						.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_INT,
								AttachmentSyncPredicate.targetOnly()));

	private static final AttachmentType<Integer> SKILL_LEVEL = AttachmentRegistry.create(
			com.powers.PowersMod.id("skill_level"),
			builder -> builder
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	private static final AttachmentType<Integer> DARKNESS_LEVEL = AttachmentRegistry.create(
			com.powers.PowersMod.id("darkness_level"),
			builder -> builder
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/** Darkness users may hide their real title and show the normal-ladder name instead. */
	private static final AttachmentType<Boolean> DARKNESS_PREFIX_HIDDEN = AttachmentRegistry.create(
			com.powers.PowersMod.id("darkness_prefix_hidden"),
			builder -> builder
					.initializer(() -> Boolean.FALSE)
					.persistent(Codec.BOOL)
					.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly()));

	private static final AttachmentType<Integer> ELEMENTAL_PHASE = AttachmentRegistry.create(
			com.powers.PowersMod.id("elemental_phase"),
			builder -> builder
					.initializer(() -> 0)
					.persistent(Codec.INT));

	private PlayerPowers() {
	}

	public static PlayerPowersData get(AttachmentTarget target) {
		return new PlayerPowersData(target);
	}

	public record PlayerPowersData(AttachmentTarget target) {
		public List<String> getSlotIds() {
			List<String> slots = target.getAttachedOrElse(POWER_SLOTS, List.of());
			if (slots.size() != SLOT_COUNT) {
				return List.of();
			}
			// Slots referencing powers that no longer exist (e.g. after a
			// registry update) are treated as unassigned so they re-roll.
			for (String id : slots) {
				if (!PowerRegistry.contains(id)) {
					return List.of();
				}
			}
			return List.copyOf(slots);
		}

		private boolean usesDarknessEnergy() {
			return target instanceof ServerPlayer player && SkillSystem.hasDarknessTag(player);
		}

		public boolean isDarknessUser() {
			return target instanceof ServerPlayer player && SkillSystem.hasDarknessTag(player);
		}

		private int storedEnergy() {
			if (usesDarknessEnergy()) {
				return target.getAttachedOrElse(DARKNESS_ENERGY, PowerEnergy.darknessMaxCapacity(0));
			}
			return target.getAttachedOrElse(ENERGY, PowerEnergy.BASE_MAX);
		}

		private void setStoredEnergy(int value) {
			int clamped = Math.max(0, value);
			if (usesDarknessEnergy()) {
				target.setAttached(DARKNESS_ENERGY, clamped);
			} else {
				target.setAttached(ENERGY, clamped);
			}
		}

		public int energy() {
			return Math.max(0, Math.min(energyCapacity(), storedEnergy()));
		}

		public int energyCapacity() {
			if (target instanceof ServerPlayer player) {
				int level = SkillSystem.effectiveLevel(player);
				return usesDarknessEnergy() ? PowerEnergy.darknessMaxCapacity(level)
					: PowerEnergy.maxCapacity(level);
			}
			return PowerEnergy.maxCapacity(0);
		}

		public int skillLevel() {
			return Math.max(0, Math.min(SkillSystem.MAX_LEVEL,
				target.getAttachedOrElse(SKILL_LEVEL, 0)));
		}

		public int darknessLevel() {
			return Math.max(0, Math.min(SkillSystem.DARKNESS_MAX_LEVEL,
				target.getAttachedOrElse(DARKNESS_LEVEL, 0)));
		}

		public boolean isDarknessPrefixHidden() {
			return target.getAttachedOrElse(DARKNESS_PREFIX_HIDDEN, Boolean.FALSE);
		}

		public void setDarknessPrefixHidden(boolean hidden) {
			target.setAttached(DARKNESS_PREFIX_HIDDEN, hidden);
		}

		public void setSkillLevel(ServerPlayer player, int level) {
			target.setAttached(SKILL_LEVEL, Math.max(0, Math.min(SkillSystem.MAX_LEVEL, level)));
		}

		public void setDarknessLevel(ServerPlayer player, int level) {
			target.setAttached(DARKNESS_LEVEL, Math.max(0, Math.min(SkillSystem.DARKNESS_MAX_LEVEL, level)));
		}

		public boolean spendEnergy(ServerPlayer player, Ability ability) {
			int cost = PowerEnergy.cost(ability);
			int current = energy();
			if (current < cost) {
				PowerMessages.send(player, "energy.powers.empty", 6);
				if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
					com.powers.fx.PowerFx.cancelled(level, player.position().add(0, 1, 0), 0x40E0D0);
					com.powers.fx.PowerFx.burst(level, player.position().add(0, 1, 0),
							net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 8, 0.3, 0.05);
				}
				return false;
			}
			setStoredEnergy(current - cost);
			return true;
		}

		public boolean consumeEnergy(int amount) {
			if (amount <= 0) return true;
			int current = energy();
			if (current < amount) return false;
			setStoredEnergy(current - amount);
			return true;
		}

		public void refundEnergy(int amount) {
			setStoredEnergy(Math.min(energyCapacity(), energy() + amount));
		}

		public void refundEnergy(Ability ability) {
			refundEnergy(PowerEnergy.cost(ability));
		}

		public boolean regenerateEnergy(int amount) {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return false;
			int current = energy();
			int updated = Math.min(energyCapacity(), current + Math.max(0, amount));
			if (updated == current) return false;
			setStoredEnergy(updated);
			return true;
		}

		public void restoreEnergy() {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return;
			setStoredEnergy(energyCapacity());
		}

		public void emptyEnergy() {
			setStoredEnergy(0);
		}

		/** Drains the pool without going below zero. */
		public void drainEnergy(int amount) {
			if (amount <= 0) return;
			setStoredEnergy(storedEnergy() - amount);
		}

		public Power getPower(int slot) {
			List<String> slots = getSlotIds();
			if (slot < 0 || slot >= slots.size()) {
				return null;
			}
			return PowerRegistry.get(slots.get(slot));
		}

		public boolean hasAssigned() {
			return getSlotIds().size() == SLOT_COUNT;
		}

		/**
		 * Assigns three distinct random powers. Called on first join and by
		 * the re-roll command. Does nothing if the player is already assigned
		 * unless {@code force} is true.
		 */
		public void assignRandom(ServerPlayer player, boolean force) {
			if (hasAssigned() && !force) {
				return;
			}
			List<String> ids = new ArrayList<>();
			for (Power power : PowerRegistry.randomDistinct(SLOT_COUNT, new Random())) {
				ids.add(power.id().toString());
			}
			setSlots(player, ids);
		}

		/**
		 * Sets the exact slot contents. Toggles belonging to dropped powers
		 * are turned off so nothing keeps running after a re-roll.
		 */
		public void setSlots(ServerPlayer player, List<String> ids) {
			List<String> newIds = new ArrayList<>(ids);
			for (String oldId : getSlotIds()) {
				Power oldPower = PowerRegistry.get(oldId);
				if (oldPower != null) {
					for (PassiveEffect passive : oldPower.passives()) {
						player.removeEffect(passive.effect());
					}
				}
			}
			for (String id : new ArrayList<>(getActiveToggles())) {
				if (!newIds.contains(id)) {
					Power power = PowerRegistry.get(id);
					if (power != null && power.ability().isToggle()) {
						power.ability().activateToggleOff(player, this);
					}
				}
			}
			target.setAttached(ACTIVE_TOGGLES, new ArrayList<>());
			target.setAttached(POWER_SLOTS, newIds);
			PowersPackets.syncTo(player);
		}

		public List<String> getActiveToggles() {
			return target.getAttachedOrElse(ACTIVE_TOGGLES, List.of());
		}

		public boolean isToggleActive(String powerId) {
			return getActiveToggles().contains(powerId);
		}

		/** Adds or removes a power from the active toggle set and syncs it. */
		public void setToggleActive(ServerPlayer player, String powerId, boolean active) {
			List<String> updated = new ArrayList<>(getActiveToggles());
			if (active && !updated.contains(powerId)) {
				updated.add(powerId);
			}
			if (!active) {
				updated.remove(powerId);
			}
			target.setAttached(ACTIVE_TOGGLES, updated);
			PowersPackets.syncTo(player);
		}

		/** Advances and returns the elemental blast phase (0-3). */
		public int nextPhase() {
			int next = (getPhase() + 1) % 4;
			target.setAttached(ELEMENTAL_PHASE, next);
			return next;
		}

		/** The current elemental blast phase (0 = fire, 1 = frost, 2 = storm, 3 = earth). */
		public int getPhase() {
			return target.getAttachedOrElse(ELEMENTAL_PHASE, 0);
		}
	}
}
