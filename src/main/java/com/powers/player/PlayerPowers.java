package com.powers.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.network.PowersPackets;
import com.powers.mind.MindBodyState;
import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankProgress;
import com.powers.player.SkillSystem;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PassiveEffect;
import com.powers.power.Ability;
import com.powers.power.PowerEnergy;
import com.powers.PowersEffects;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Per-player power state: three power slots, active toggles, energy, and
 * skill levels, kept as persistent copy-on-death attachments on the player
 * so the assignment survives restarts, relogs and deaths alike, and stays
 * fixed for the life of the character.
 */
public final class PlayerPowers {
	public static final int SLOT_COUNT = 3;
	public enum ConsentKind { TELEPORT, LOCATOR, COMPANION, DREAMWALK, POSSESSION }
	public record AnchorState(String dimensionId, long expiresAt) {
		private static final Codec<AnchorState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("dimension").forGetter(AnchorState::dimensionId),
				Codec.LONG.fieldOf("expires_at").forGetter(AnchorState::expiresAt)
		).apply(instance, AnchorState::new));
	}

	// every attachment here has to survive death as well as logout. attachments
	// are dropped when the player entity is rebuilt on respawn unless
	// copyOnDeath() is set, and losing POWER_SLOTS that way would silently
	// re-roll a player's permanent loadout the next time they log in.
	// the client mirror is driven entirely by PowerStatePayload, so none of
	// these need a sync codec on top of that
	private static final AttachmentType<List<String>> POWER_SLOTS = AttachmentRegistry.create(
			com.powers.PowersMod.id("power_slots"),
			builder -> builder
					.initializer(ArrayList::new)
					.persistent(Codec.STRING.listOf())
					.copyOnDeath());

	private static final AttachmentType<List<String>> ACTIVE_TOGGLES = AttachmentRegistry.create(
			com.powers.PowersMod.id("active_toggles"),
			builder -> builder
					.initializer(ArrayList::new)
					.persistent(Codec.STRING.listOf())
					.copyOnDeath());

	private static final AttachmentType<Integer> ENERGY = AttachmentRegistry.create(
				com.powers.PowersMod.id("energy"),
				builder -> builder
						.initializer(() -> PowerEnergy.BASE_MAX)
						.persistent(Codec.INT)
						.copyOnDeath());

	private static final AttachmentType<Integer> DARKNESS_ENERGY = AttachmentRegistry.create(
				com.powers.PowersMod.id("darkness_energy"),
				builder -> builder
						.initializer(() -> PowerEnergy.darknessMaxCapacity(0))
						.persistent(Codec.INT)
						.copyOnDeath());

	private static final AttachmentType<Integer> SKILL_LEVEL = AttachmentRegistry.create(
			com.powers.PowersMod.id("skill_level"),
			builder -> builder
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.copyOnDeath());

	private static final AttachmentType<Integer> DARKNESS_LEVEL = AttachmentRegistry.create(
			com.powers.PowersMod.id("darkness_level"),
			builder -> builder
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.copyOnDeath());

	private static final AttachmentType<List<String>> RANK_NODES = persistentStringList("rank_nodes");
	private static final AttachmentType<List<String>> DARK_RANK_NODES = persistentStringList("dark_rank_nodes");
	private static final AttachmentType<String> RANK_FOCUS = persistentString("rank_focus");
	private static final AttachmentType<String> DARK_RANK_FOCUS = persistentString("dark_rank_focus");

	private static AttachmentType<List<String>> persistentStringList(String name) {
		return AttachmentRegistry.create(com.powers.PowersMod.id(name), builder -> builder
				.initializer(ArrayList::new).persistent(Codec.STRING.listOf()).copyOnDeath());
	}

	private static AttachmentType<String> persistentString(String name) {
		return AttachmentRegistry.create(com.powers.PowersMod.id(name), builder -> builder
				.initializer(() -> "").persistent(Codec.STRING).copyOnDeath());
	}

	// darkness users may hide their real title and show the normal-ladder name instead
	private static final AttachmentType<Boolean> DARKNESS_PREFIX_HIDDEN = AttachmentRegistry.create(
			com.powers.PowersMod.id("darkness_prefix_hidden"),
			builder -> builder
					.initializer(() -> Boolean.FALSE)
					.persistent(Codec.BOOL)
					.copyOnDeath());

	private static final AttachmentType<Integer> ELEMENTAL_PHASE = AttachmentRegistry.create(
			com.powers.PowersMod.id("elemental_phase"),
			builder -> builder
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.copyOnDeath());

	private static final AttachmentType<Map<String, Long>> COOLDOWNS = AttachmentRegistry.create(
			com.powers.PowersMod.id("cooldowns"),
			builder -> builder
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
					.copyOnDeath());

	private static final AttachmentType<AnchorState> DIMENSIONAL_ANCHOR = AttachmentRegistry.create(
			com.powers.PowersMod.id("dimensional_anchor"),
			builder -> builder.persistent(AnchorState.CODEC).copyOnDeath());

	private static final AttachmentType<MindBodyState> MIND_BODY = AttachmentRegistry.create(
			com.powers.PowersMod.id("mind_body"),
			builder -> builder.persistent(MindBodyState.CODEC).copyOnDeath());

	// -1 means the power does not own a snapshot. Remaining bits preserve the
	// flags that existed before the power changed them, even across a relog.
	private static final AttachmentType<Integer> FLIGHT_SNAPSHOT = persistentInt("flight_snapshot", -1);
	private static final AttachmentType<Integer> INVISIBILITY_SNAPSHOT = persistentInt("invisibility_snapshot", -1);

	private static AttachmentType<Integer> persistentInt(String name, int initial) {
		return AttachmentRegistry.create(com.powers.PowersMod.id(name), builder -> builder
				.initializer(() -> initial).persistent(Codec.INT).copyOnDeath());
	}

	private static final AttachmentType<Boolean> TELEPORT_CONSENT = consentAttachment("teleport_consent");
	private static final AttachmentType<Boolean> LOCATOR_CONSENT = consentAttachment("locator_consent");
	private static final AttachmentType<Boolean> COMPANION_CONSENT = consentAttachment("companion_consent");
	private static final AttachmentType<Boolean> DREAMWALK_CONSENT = consentAttachment("dreamwalk_consent");
	private static final AttachmentType<Boolean> POSSESSION_CONSENT = consentAttachment("possession_consent");

	private static AttachmentType<Boolean> consentAttachment(String name) {
		return AttachmentRegistry.create(com.powers.PowersMod.id(name), builder -> builder
				.initializer(() -> Boolean.FALSE).persistent(Codec.BOOL).copyOnDeath());
	}

	// the game mode a player held before stepping into a realm dimension. this
	// has to persist too: a player who logs out inside a realm would otherwise
	// have adventure mode recorded as their "previous" mode on rejoin and stay
	// stuck in it forever after leaving
	private static final AttachmentType<String> PREVIOUS_GAMEMODE = AttachmentRegistry.create(
			com.powers.PowersMod.id("previous_gamemode"),
			builder -> builder
					.persistent(Codec.STRING)
					.copyOnDeath());

	private PlayerPowers() {
	}

	public static PlayerPowersData get(AttachmentTarget target) {
		return new PlayerPowersData(target);
	}

	public record PlayerPowersData(AttachmentTarget target) {
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

		public int invisibilitySnapshot() {
			return target.getAttachedOrElse(INVISIBILITY_SNAPSHOT, -1);
		}

		public void setInvisibilitySnapshot(int snapshot) {
			target.setAttached(INVISIBILITY_SNAPSHOT, snapshot);
		}

		public boolean allowsConsent(ConsentKind kind) {
			return target.getAttachedOrElse(consentType(kind), Boolean.FALSE);
		}

		public void setConsent(ConsentKind kind, boolean allowed) {
			target.setAttached(consentType(kind), allowed);
		}

		private static AttachmentType<Boolean> consentType(ConsentKind kind) {
			return switch (kind) {
				case TELEPORT -> TELEPORT_CONSENT;
				case LOCATOR -> LOCATOR_CONSENT;
				case COMPANION -> COMPANION_CONSENT;
				case DREAMWALK -> DREAMWALK_CONSENT;
				case POSSESSION -> POSSESSION_CONSENT;
			};
		}
		public List<String> getSlotIds() {
			List<String> slots = target.getAttachedOrElse(POWER_SLOTS, List.of());
			if (slots.size() != SLOT_COUNT) {
				return List.of();
			}
			// slots pointing at powers that no longer exist (say, after a
			// registry update) count as unassigned so the player re-rolls
			for (String id : slots) {
				if (!PowerRegistry.contains(id)) {
					return List.of();
				}
			}
			return List.copyOf(slots);
		}

		// darkness users draw from their own separate pool
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

		// capacity grows with the player's skill ladder
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

		public RankProgress rankProgress(boolean darkness) {
			AttachmentType<List<String>> nodesType = darkness ? DARK_RANK_NODES : RANK_NODES;
			AttachmentType<String> focusType = darkness ? DARK_RANK_FOCUS : RANK_FOCUS;
			List<String> completed = target.getAttachedOrElse(nodesType, List.of());
			RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
			if (completed.isEmpty()) {
				RankProgress migrated = RankProgress.migrateLegacy(graph,
						darkness ? darknessLevel() : skillLevel());
				target.setAttached(nodesType, new ArrayList<>(migrated.completed()));
				target.setAttached(focusType, migrated.focus());
				return migrated;
			}
			return new RankProgress(new java.util.LinkedHashSet<>(completed),
					target.getAttachedOrElse(focusType, ""));
		}

		public boolean unlockRankNode(boolean darkness, String nodeId) {
			RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
			RankProgress progress = rankProgress(darkness);
			int earnedDepth = darkness ? darknessLevel() : skillLevel();
			if (!graph.unlockable(progress.completed(), earnedDepth).contains(nodeId)) return false;
			java.util.LinkedHashSet<String> updated = new java.util.LinkedHashSet<>(progress.completed());
			updated.add(nodeId);
			target.setAttached(darkness ? DARK_RANK_NODES : RANK_NODES, new ArrayList<>(updated));
			target.setAttached(darkness ? DARK_RANK_FOCUS : RANK_FOCUS, nodeId);
			return true;
		}

		public boolean setRankFocus(boolean darkness, String nodeId) {
			RankProgress progress = rankProgress(darkness);
			if (!progress.completed().contains(nodeId)) return false;
			target.setAttached(darkness ? DARK_RANK_FOCUS : RANK_FOCUS, nodeId);
			return true;
		}

		public void respecRankMaze(boolean darkness) {
			RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
			RankProgress migrated = RankProgress.migrateLegacy(graph, darkness ? darknessLevel() : skillLevel());
			target.setAttached(darkness ? DARK_RANK_NODES : RANK_NODES,
					new ArrayList<>(migrated.completed()));
			target.setAttached(darkness ? DARK_RANK_FOCUS : RANK_FOCUS, migrated.focus());
		}

		public boolean isDarknessPrefixHidden() {
			return target.getAttachedOrElse(DARKNESS_PREFIX_HIDDEN, Boolean.FALSE);
		}

		/**
		 * The game mode this player held before entering a realm dimension, or
		 * null when they were not in one. Stored by name so an unknown value
		 * from an older save simply reads back as "no snapshot".
		 */
		public GameType previousGameMode() {
			String name = target.getAttachedOrElse(PREVIOUS_GAMEMODE, "");
			if (name.isEmpty()) {
				return null;
			}
			for (GameType mode : GameType.values()) {
				if (mode.getName().equals(name)) {
					return mode;
				}
			}
			return null;
		}

		/** Records (or, with null, forgets) the game mode to restore on leaving a realm. */
		public void setPreviousGameMode(GameType mode) {
			target.setAttached(PREVIOUS_GAMEMODE, mode == null ? "" : mode.getName());
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
				// too broke: tell the player and show a cancelled spark burst
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

		// the exhaustion effect blocks all natural regen
		public boolean regenerateEnergy(int amount) {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return false;
			int current = energy();
			int updated = Math.min(energyCapacity(), current + Math.max(0, amount));
			if (updated == current) return false;
			setStoredEnergy(updated);
			return true;
		}

		// exhausted players can't even refill by sleeping
		public void restoreEnergy() {
			if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return;
			setStoredEnergy(energyCapacity());
		}

		public void emptyEnergy() {
			setStoredEnergy(0);
		}

		/** Drains the pool, clamped so it never goes below zero. */
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
		 * Rolls three distinct random powers. Runs on first join and via the
		 * re-roll command; skips players who already have powers unless
		 * {@code force} is set.
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
		 * Sets the exact slot contents. Toggles for dropped powers are turned
		 * off so nothing keeps draining after a re-roll.
		 */
		public void setSlots(ServerPlayer player, List<String> ids) {
			List<String> newIds = new ArrayList<>(ids);
			// A passive may be shared with a potion or another mod. It is allowed
			// to expire naturally instead of removing the entire effect type.
			for (String id : new ArrayList<>(getActiveToggles())) {
				Power power = PowerRegistry.get(id);
				if (power != null && power.ability().isToggle()) {
					power.ability().activateToggleOff(player, this);
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

		/** Advances the elemental blast phase (0-3) and returns the new one. */
		public int nextPhase() {
			int next = (getPhase() + 1) % 4;
			target.setAttached(ELEMENTAL_PHASE, next);
			return next;
		}

		/** The current elemental blast phase: 0 fire, 1 frost, 2 storm, 3 earth. */
		public int getPhase() {
			return target.getAttachedOrElse(ELEMENTAL_PHASE, 0);
		}
	}
}
