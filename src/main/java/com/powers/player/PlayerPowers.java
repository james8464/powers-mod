package com.powers.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.network.PowersPackets;
import com.powers.mind.MindBodyState;
import com.powers.migration.SaveMigrationRules;
import com.powers.progression.RankProgress;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PowerToggleLifecycle;
import com.powers.power.abilities.SizeMorphRules;
import com.powers.power.Ability;
import com.powers.power.PowerEnergy;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.powers.player.PlayerPowerAttachments.ACTIVE_TOGGLES;
import static com.powers.player.PlayerPowerAttachments.COOLDOWNS;
import static com.powers.player.PlayerPowerAttachments.CRYSTAL_SELECTIONS;
import static com.powers.player.PlayerPowerAttachments.DARKNESS_LEVEL;
import static com.powers.player.PlayerPowerAttachments.DARKNESS_PREFIX_HIDDEN;
import static com.powers.player.PlayerPowerAttachments.DIMENSIONAL_ANCHOR;
import static com.powers.player.PlayerPowerAttachments.FLIGHT_SNAPSHOT;
import static com.powers.player.PlayerPowerAttachments.GUIDE_RECEIVED;
import static com.powers.player.PlayerPowerAttachments.LAST_DEATH;
import static com.powers.player.PlayerPowerAttachments.MIND_BODY;
import static com.powers.player.PlayerPowerAttachments.POWER_SLOTS;
import static com.powers.player.PlayerPowerAttachments.PREVIOUS_GAMEMODE;
import static com.powers.player.PlayerPowerAttachments.SKILL_LEVEL;
import static com.powers.player.PlayerPowerAttachments.SIZE_MORPH_OPTION;
import static com.powers.player.PlayerPowerAttachments.SPELL_SELECTIONS;

/**
 * Persistent per-player power slots, toggles, energy, and skill levels.
 * Copy-on-death attachments survive restarts, relogs, and deaths while each
 * character's assigned powers remain fixed.
 */
public final class PlayerPowers {
	public static final int SLOT_COUNT = 3;
	public record AnchorState(String dimensionId, long expiresAt) {
		static final Codec<AnchorState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("dimension").forGetter(AnchorState::dimensionId),
				Codec.LONG.fieldOf("expires_at").forGetter(AnchorState::expiresAt)
		).apply(instance, AnchorState::new));
	}

	private PlayerPowers() {
	}

	public static PlayerPowersData get(AttachmentTarget target) {
		return new PlayerPowersData(target);
	}

	public record PlayerPowersData(AttachmentTarget target) {
		/** Returns a typed immutable view of every persistent darkness deed counter. */
		public Map<DarknessDeed, Integer> darknessDeeds() {
			return DarknessDeedStore.read(target);
		}

		/** Increments all categories caused by one kill using one attachment write. */
		public Map<DarknessDeed, Integer> addDarknessDeeds(Set<DarknessDeed> deeds) {
			return DarknessDeedStore.increment(target, deeds);
		}

		public boolean discoverRealmMemory(String memoryId) {
			return RealmMemoryStore.discover(target, memoryId);
		}

		public List<String> realmMemories() {
			return RealmMemoryStore.read(target);
		}

		/** Most recent recorded death, or null before this character has died. */
		public LastDeathRecord lastDeath() {
			return target.getAttached(LAST_DEATH);
		}

		/** Captures the current server-authoritative death point before respawn. */
		public void recordDeath(ServerPlayer player) {
			target.setAttached(LAST_DEATH, LastDeathRecord.at(
					player.level().dimension().identifier().toString(),
					player.getX(), player.getY(), player.getZ()));
		}

		public int selectedSpell(String grimoireKey, int spellCount) {
			if (spellCount <= 0) return 0;
			int selected = target.getAttachedOrElse(SPELL_SELECTIONS, Map.of()).getOrDefault(grimoireKey, 0);
			return Math.floorMod(selected, spellCount);
		}

		public int selectedCrystalMode(String crystalKey, int modeCount) {
			if (modeCount <= 0) return 0;
			return com.powers.power.crystals.CrystalModeState.current(
					target.getAttachedOrElse(CRYSTAL_SELECTIONS, Map.of())
							.getOrDefault(crystalKey, 0), modeCount);
		}

		public void setSelectedCrystalMode(String crystalKey, int selected) {
			Map<String, Integer> updated = new HashMap<>(
					target.getAttachedOrElse(CRYSTAL_SELECTIONS, Map.of()));
			updated.put(crystalKey, Math.max(0, selected));
			target.setAttached(CRYSTAL_SELECTIONS, updated);
		}

		/** Returns the stored page before catalogue migration. */
		public int rawSelectedSpell(String grimoireKey) {
			return target.getAttachedOrElse(SPELL_SELECTIONS, Map.of()).getOrDefault(grimoireKey, 0);
		}

		/** Stores one canonical spell page after migration or explicit selection. */
		public void setSelectedSpell(String grimoireKey, int selected) {
			Map<String, Integer> updated = new HashMap<>(
					target.getAttachedOrElse(SPELL_SELECTIONS, Map.of()));
			updated.put(grimoireKey, Math.max(0, selected));
			target.setAttached(SPELL_SELECTIONS, updated);
		}

		public int cycleSpell(String grimoireKey, int spellCount) {
			if (spellCount <= 0) return 0;
			int selected = (selectedSpell(grimoireKey, spellCount) + 1) % spellCount;
			setSelectedSpell(grimoireKey, selected);
			return selected;
		}
		public long cooldownReadyAt(String abilityId) {
			return target.getAttachedOrElse(COOLDOWNS, Map.of()).getOrDefault(abilityId, 0L);
		}

		public void setCooldown(String abilityId, long readyAt) {
			Map<String, Long> updated = new HashMap<>(target.getAttachedOrElse(COOLDOWNS, Map.of()));
			updated.put(abilityId, readyAt);
			target.setAttached(COOLDOWNS, updated);
		}

		public void clearCooldown(String abilityId) {
			Map<String, Long> current = target.getAttachedOrElse(COOLDOWNS, Map.of());
			if (!current.containsKey(abilityId)) return;
			Map<String, Long> updated = new HashMap<>(current);
			updated.remove(abilityId);
			target.setAttached(COOLDOWNS, updated);
		}

		/** Drops every saved recovery deadline when persistence is disabled by server policy. */
		public void clearCooldowns() {
			if (!target.getAttachedOrElse(COOLDOWNS, Map.of()).isEmpty()) {
				target.setAttached(COOLDOWNS, Map.of());
			}
		}

		public AnchorState dimensionalAnchor() {
			return target.getAttached(DIMENSIONAL_ANCHOR);
		}

		public MindBodyState mindBody() {
			return target.getAttached(MIND_BODY);
		}

		public void setMindBody(MindBodyState state) {
			if (state == null) target.removeAttached(MIND_BODY);
			else target.setAttached(MIND_BODY, state);
		}

		public void setDimensionalAnchor(String dimensionId, long expiresAt) {
			target.setAttached(DIMENSIONAL_ANCHOR, new AnchorState(dimensionId, expiresAt));
		}

		public void clearDimensionalAnchor() {
			target.removeAttached(DIMENSIONAL_ANCHOR);
		}

		public int flightSnapshot() {
			return target.getAttachedOrElse(FLIGHT_SNAPSHOT, -1);
		}

		public void setFlightSnapshot(int snapshot) {
			target.setAttached(FLIGHT_SNAPSHOT, snapshot);
		}

		public boolean allowsConsent(com.powers.protection.ConsentKind kind) {
			return target.getAttachedOrElse(PlayerConsentAttachments.type(kind), Boolean.FALSE);
		}

		public void setConsent(com.powers.protection.ConsentKind kind, boolean allowed) {
			target.setAttached(PlayerConsentAttachments.type(kind), allowed);
		}
		public List<String> getSlotIds() {
			List<String> slots = target.getAttachedOrElse(POWER_SLOTS, List.of());
			if (slots.isEmpty()) return List.of();
			boolean canonical = slots.size() == SLOT_COUNT
					&& slots.stream().distinct().count() == SLOT_COUNT
					&& slots.stream().allMatch(PowerRegistry::contains);
			if (canonical) return List.copyOf(slots);
			if (!(target instanceof ServerPlayer player)) return List.of();
			List<String> migrated = SaveMigrationRules.canonicalPowerSlots(
					slots, PlayerPowerAffinity.allegiance(player));
			target.setAttached(ACTIVE_TOGGLES, PowerToggleLifecycle.deactivateInnate(
					player, this, List.copyOf(getActiveToggles())));
			target.setAttached(POWER_SLOTS, migrated);
			return migrated;
		}

		public boolean isDarknessUser() {
			return PlayerEnergyStorage.usesDarkness(target);
		}

		public int energy() {
			return PlayerEnergyStorage.energy(target);
		}

		// capacity grows with the player's skill ladder
		public int energyCapacity() {
			return PlayerEnergyStorage.capacity(target);
		}

		public int skillLevel() {
			return Math.max(0, Math.min(SkillSystem.MAX_LEVEL,
				target.getAttachedOrElse(SKILL_LEVEL, 0)));
		}

		public int darknessLevel() {
			return Math.max(0, Math.min(SkillSystem.DARKNESS_MAX_LEVEL,
				target.getAttachedOrElse(DARKNESS_LEVEL, 0)));
		}

		public RankProgress rankProgress(boolean darkness) {
			return PlayerRankState.progress(target, darkness, darkness ? darknessLevel() : skillLevel());
		}

		public boolean unlockRankNode(boolean darkness, String nodeId) {
			int earnedDepth = darkness ? darknessLevel() : skillLevel();
			return PlayerRankState.unlock(target, darkness, earnedDepth, nodeId);
		}

		public boolean setRankFocus(boolean darkness, String nodeId) {
			int legacyLevel = darkness ? darknessLevel() : skillLevel();
			return PlayerRankState.focus(target, darkness, legacyLevel, nodeId);
		}

		public void respecRankMaze(boolean darkness) {
			PlayerRankState.resetToLegacyPath(target, darkness, darkness ? darknessLevel() : skillLevel());
		}

		public boolean isDarknessPrefixHidden() {
			return target.getAttachedOrElse(DARKNESS_PREFIX_HIDDEN, Boolean.FALSE);
		}

		/** Whether this persistent character has already received the authored guide. */
		public boolean hasReceivedGuide() {
			return target.getAttachedOrElse(GUIDE_RECEIVED, Boolean.FALSE);
		}

		/** Permanently records delivery so reconnects and respawns cannot duplicate the book. */
		public void markGuideReceived() {
			target.setAttached(GUIDE_RECEIVED, Boolean.TRUE);
		}

		/** Raw save-compatible value written by builds that forced mindscapes to Adventure. */
		public String legacyPreviousGameModeName() {
			return target.getAttachedOrElse(PREVIOUS_GAMEMODE, "");
		}

		/** Erases the retired snapshot after its one compatibility decision. */
		public void clearLegacyPreviousGameMode() {
			target.removeAttached(PREVIOUS_GAMEMODE);
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
			int cost = PowerEnergy.cost(player, ability);
			if (!PlayerEnergyStorage.consume(target, cost)) {
				// too broke: tell the player and show a cancelled spark burst
				PowerMessages.send(player, "energy.powers.empty", 6);
				if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
					com.powers.fx.PowerFx.cancelled(level, player.position().add(0, 1, 0), 0x40E0D0);
					com.powers.fx.PowerFx.burst(level, player.position().add(0, 1, 0),
							net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 8, 0.3, 0.05);
				}
				return false;
			}
			return true;
		}

		public boolean consumeEnergy(int amount) {
			return PlayerEnergyStorage.consume(target, amount);
		}

		public void refundEnergy(int amount) {
			PlayerEnergyStorage.refund(target, amount);
		}

		public void refundEnergy(Ability ability) {
			int amount = target instanceof ServerPlayer player
					? PowerEnergy.cost(player, ability) : PowerEnergy.cost(ability);
			refundEnergy(amount);
		}

		// the exhaustion effect blocks all natural regen
		public boolean regenerateEnergy(int amount) {
			return PlayerEnergyStorage.regenerate(target, amount);
		}

		// exhausted players can't even refill by sleeping
		public void restoreEnergy() {
			PlayerEnergyStorage.restore(target);
		}

		public void emptyEnergy() {
			PlayerEnergyStorage.empty(target);
		}

		/** Refills even through exhaustion; reserved for explicit operator testing. */
		public void forceRestoreEnergy() {
			PlayerEnergyStorage.forceRestore(target);
		}

		/** Drains the pool, clamped so it never goes below zero. */
		public void drainEnergy(int amount) {
			PlayerEnergyStorage.drain(target, amount);
		}

		public Power getPower(int slot) {
			List<String> slots = getSlotIds();
			if (slot < 0 || slot >= slots.size()) {
				return null;
			}
			Power power = PowerRegistry.get(slots.get(slot));
			return target instanceof ServerPlayer player && !PlayerPowerAffinity.permits(player, power)
					? null : power;
		}

		public boolean hasAssigned() {
			return getSlotIds().size() == SLOT_COUNT;
		}

		/**
		 * Rolls three distinct random powers. Runs on first join and via the
		 * re-roll command; skips players who already have powers unless
		 * {@code force} is set.
		 */
		public void assignRandom(ServerPlayer player, boolean force) {
			if (hasAssigned() && !force) {
				reconcileAffinity(player);
				return;
			}
			List<String> ids = new ArrayList<>();
			for (Power power : PowerRegistry.randomDistinct(SLOT_COUNT, new Random(),
					PlayerPowerAffinity.allegiance(player))) {
				ids.add(power.id().toString());
			}
			setSlots(player, ids);
		}

		/** Replaces only slots made illegal by an allegiance change. */
		public void reconcileAffinity(ServerPlayer player) {
			List<String> current = getSlotIds();
			if (current.size() != SLOT_COUNT) return;
			List<String> reconciled = PlayerPowerAffinity.reconcile(player, current);
			if (!current.equals(reconciled)) setSlots(player, reconciled);
		}

		/**
		 * Sets the exact slot contents. Toggles for dropped powers are turned
		 * off so nothing keeps draining after a re-roll.
		 */
		public void setSlots(ServerPlayer player, List<String> ids) {
			List<String> newIds = new ArrayList<>(ids);
			// A passive may be shared with a potion or another mod. It is allowed
			// to expire naturally instead of removing the entire effect type.
			// Artifact toggles are item-owned and survive an unrelated slot reroll.
			target.setAttached(ACTIVE_TOGGLES, PowerToggleLifecycle.deactivateInnate(
					player, this, List.copyOf(getActiveToggles())));
			target.setAttached(POWER_SLOTS, newIds);
			PowersPackets.syncTo(player);
		}

		public List<String> getActiveToggles() {
			return target.getAttachedOrElse(ACTIVE_TOGGLES, List.of());
		}

		public boolean isToggleActive(String powerId) {
			return getActiveToggles().contains(powerId);
		}

		/** Adds or removes a power from the active toggle set and syncs the change. */
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

		/** Clears every route that owns one runtime-only toggle with a single attachment write. */
		public boolean clearToggleOwnership(ServerPlayer player,
				net.minecraft.resources.Identifier abilityId) {
			List<String> current = getActiveToggles();
			List<String> retained = com.powers.power.ToggleKeyRules.withoutAbility(current, abilityId);
			if (retained.equals(current)) return false;
			target.setAttached(ACTIVE_TOGGLES, retained);
			PowersPackets.syncTo(player);
			return true;
		}

		/** Selects one authored Size Morphing scale option. */
		public void setSizeMorphOption(int option) {
			target.setAttached(SIZE_MORPH_OPTION, option);
		}

		/** Returns the persisted Size Morphing option, normalizing corrupt saves to normal size. */
		public int getSizeMorphOption() {
			int option = target.getAttachedOrElse(SIZE_MORPH_OPTION, SizeMorphRules.normalOption());
			return SizeMorphRules.isValidOption(option) ? option : SizeMorphRules.normalOption();
		}
	}
}
