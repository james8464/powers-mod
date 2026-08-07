package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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

	protected Ability(Identifier id, Component name, int cooldownTicks, boolean requiresInput) {
		this.id = id;
		this.name = name;
		this.cooldownTicks = cooldownTicks;
		this.requiresInput = requiresInput;
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
	public boolean activateTeleport(ServerPlayer caster, ServerPlayer subject, PlayerPowers.PlayerPowersData data,
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

	/**
	 * The cooldown that actually applies after a successful activation,
	 * defaulting to the fixed cooldown unless a stateful ability shortens it
	 */
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return this.cooldownTicks;
	}
}
