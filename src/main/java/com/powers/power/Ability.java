package com.powers.power;

import com.powers.player.PlayerPowers;
import com.powers.progression.PowerScalingService;
import com.powers.progression.ScaledMagicValues;
import com.powers.progression.InnatePowerLevel;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.MagicPresenceId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * An active power the player triggers. Instant abilities fire right on the
 * key press; input abilities open a client-side screen first (like the
 * teleport pad) and the chosen coordinates come back through
 * {@link #activateTeleport}
 */
public abstract class Ability {
	private final Identifier id;
	private final Component name;
	private final int cooldownTicks;
	private final boolean requiresInput;
	private final boolean rankScaling;

	protected Ability(Identifier id, Component name, int cooldownTicks, boolean requiresInput) {
		this(id, name, cooldownTicks, requiresInput, true);
	}

	/**
	 * Creates an ability with an explicit progression policy. Crystal abilities
	 * pass {@code false}; innate player powers keep the default ranked policy.
	 */
	protected Ability(Identifier id, Component name, int cooldownTicks, boolean requiresInput,
			boolean rankScaling) {
		this.id = id;
		this.name = name;
		this.cooldownTicks = cooldownTicks;
		this.requiresInput = requiresInput;
		this.rankScaling = rankScaling;
	}

	public Identifier id() {
		return this.id;
	}

	public Component name() {
		return this.name;
	}

	public int cooldownTicks() {
		return this.cooldownTicks;
	}

	public boolean requiresInput() {
		return this.requiresInput;
	}

	/** True only for innate player powers affected by the rank maze. */
	public final boolean usesRankScaling() {
		return rankScaling;
	}

	/** Number of explicit options exposed by the crouch-key selection menu. */
	public int selectionOptionCount() {
		return 0;
	}

	/** Localized label for one validated explicit selection option. */
	public Component selectionOptionName(int option) {
		return Component.empty();
	}

	/** Applies one server-validated option without spending energy or starting cooldown. */
	public boolean selectOption(ServerPlayer player, PlayerPowers.PlayerPowersData data, int option) {
		return false;
	}

	/**
	 * Returns the server-derived canonical action used for collision resolution.
	 * Stateful abilities may override this without accepting packet-selected IDs.
	 */
	public String magicActionId(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return id.getPath();
	}

	/** True when this interaction only changes an ability mode and must be free. */
	public boolean isSelectionAction(ServerPlayer player) {
		return false;
	}

	/**
	 * Fires the ability when the player hits the key, only when it's ready;
	 * returning true means it went off, so the caller starts the cooldown
	 */
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return false;
	}

	/**
	 * Fires an input ability at the coordinates the player picked in the
	 * client screen, moving the subject (usually the caster) there
	 */
	public boolean activateTeleport(ServerPlayer caster, LivingEntity subject, PlayerPowers.PlayerPowersData data,
			ResourceKey<Level> dimension, double x, double y, double z) {
		return false;
	}

	/** true for toggle abilities like flight, which have no cooldown */
	public boolean isToggle() {
		return false;
	}

	/** turns the toggle on when the player presses the key */
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return false;
	}

	/** turns the toggle off when the player presses the key again */
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
	}

	/** runs every few server ticks while the toggle is on, to drain energy or keep the effect going */
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
	}

	/** Server tick cadence for active toggles; propulsion flight requires every tick. */
	public int activeTickInterval() {
		return 5;
	}

	/**
	 * The cooldown that actually applies after a successful activation,
	 * defaulting to the fixed cooldown unless a stateful ability shortens it
	 */
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return CastScalingContext.currentSource().appliesPlayerRank(rankScaling)
				? PowerScalingService.cooldown(player, id.getPath(), cooldownTicks)
				: cooldownTicks;
	}

	/**
	 * Allows one server-owned ability state to pass an otherwise active cooldown.
	 * Suppression, payment, execution, and cooldown restart still run normally.
	 */
	public boolean mayReactivateDuringCooldown(ServerPlayer player,
			PlayerPowers.PlayerPowersData data, int remainingTicks) {
		return false;
	}

	/** Returns the synchronized legal-reactivation window for this HUD slot. */
	public int reactivationTicks(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return 0;
	}

	/** Reanchors the committed collision residue to a physical projectile, beam, or field. */
	public void bindPhysicalPresence(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			MagicPresenceId presenceId) {
	}

	/** Removes partial runtime work when a later transaction phase fails. */
	public void rollbackFailedActivation(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		PowerAbilityRuntime.rollbackFailedActivation(player, id.getPath());
	}

	/** Returns the single canonical rank profile for this ability's action. */
	protected final ScaledMagicValues scaling(ServerPlayer player) {
		return CastScalingContext.currentSource().appliesPlayerRank(rankScaling)
				? PowerScalingService.forPlayer(player, id.getPath())
				: PowerScalingService.unranked(id.getPath());
	}

	/** Returns ability-specific authored capacity, destruction, and transformation data. */
	protected final InnatePowerLevel innateLevel(ServerPlayer player) {
		return PowerScalingService.innateLevel(player, id.getPath());
	}

	/** Scales an implementation-specific range through the canonical action. */
	protected final double scaledRange(ServerPlayer player, double baseRange) {
		return Math.max(0, baseRange) * scaling(player).rangeMultiplier();
	}

	/** Scales an implementation-specific duration through the canonical action. */
	protected final int scaledDuration(ServerPlayer player, int baseTicks) {
		return baseTicks <= 0 ? 0 : Math.max(1, (int) Math.round(baseTicks * scaling(player).durationMultiplier()));
	}

	/** Scales damage, healing, or force strength through canonical potency. */
	protected final float scaledPotency(ServerPlayer player, float baseValue) {
		return (float) (Math.max(0, baseValue) * scaling(player).potencyMultiplier());
	}
}
