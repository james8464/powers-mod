package com.powers.player;

import com.mojang.serialization.Codec;
import com.powers.network.PowersPackets;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PassiveEffect;
import com.powers.power.Ability;
import com.powers.power.PowerEnergy;
import com.powers.PowersEffects;
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
 * assignment survives restarts and never changes between logins (unless the
 * player uses the Rainbow Crystal to re-roll).
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
						.initializer(() -> PowerEnergy.MAX)
						.persistent(Codec.INT)
						.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_INT,
								AttachmentSyncPredicate.targetOnly()));

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

		public int energy() {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return 0;
			return Math.max(0, Math.min(PowerEnergy.MAX,
					target.getAttachedOrElse(ENERGY, PowerEnergy.MAX)));
		}

		public boolean spendEnergy(ServerPlayer player, Ability ability) {
			int cost = PowerEnergy.cost(ability);
			int current = energy();
			if (current < cost) {
				player.sendSystemMessage(Component.translatable("energy.powers.empty"));
				return false;
			}
			target.setAttached(ENERGY, current - cost);
			return true;
		}

		public boolean consumeEnergy(int amount) {
			if (amount <= 0) return true;
			int current = energy();
			if (current < amount) return false;
			target.setAttached(ENERGY, current - amount);
			return true;
		}

		public void refundEnergy(int amount) {
			target.setAttached(ENERGY, Math.min(PowerEnergy.MAX, energy() + amount));
		}

		public boolean regenerateEnergy(int amount) {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return false;
			int current = energy();
			int updated = Math.min(PowerEnergy.MAX, current + Math.max(0, amount));
			if (updated == current) return false;
			target.setAttached(ENERGY, updated);
			return true;
		}

		public void restoreEnergy() {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return;
			target.setAttached(ENERGY, PowerEnergy.MAX);
		}

		public void emptyEnergy() {
			target.setAttached(ENERGY, 0);
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
		 * Assigns three distinct random powers. Called on first join and on
		 * re-roll. Does nothing if the player is already assigned unless
		 * {@code force} is true.
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
		 * Sets the exact slot contents. {@link #validateSlots(List)} must
		 * have been called first. Toggles belonging to dropped powers are
		 * turned off so nothing keeps running after a re-roll.
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

		public static boolean validateSlots(List<String> ids) {
			if (ids == null || ids.size() != SLOT_COUNT) {
				return false;
			}
			List<String> seen = new ArrayList<>();
			for (String id : ids) {
				if (!PowerRegistry.contains(id) || seen.contains(id)) {
					return false;
				}
				seen.add(id);
			}
			return true;
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
