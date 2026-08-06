package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * An active ability. Instant abilities are triggered directly by the keybind;
 * input abilities first open a client-side input screen (e.g. teleport
 * coordinates) whose result is delivered through {@link #activateTeleport}.
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
	 * Executes the ability. Called only when the ability is ready (not on
	 * cooldown). The cooldown is started by the caller if this returns true.
	 */
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return false;
	}

	/**
	 * Executes an input-driven ability with the coordinates supplied by the
	 * client's input screen.
	 */
	public boolean activateTeleport(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			ResourceKey<Level> dimension, double x, double y, double z) {
		return false;
	}

	/** True for toggle abilities (e.g. flight), which have no cooldown. */
	public boolean isToggle() {
		return false;
	}

	/** Turns a toggle ability on. Called by the server when the key is pressed. */
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return false;
	}

	/** Turns a toggle ability off. Called by the server when the key is pressed. */
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
	}

	/** Called every few server ticks while a toggle ability is active. */
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
	}

	/**
	 * Cooldown actually applied after a successful activation. Defaults to
	 * {@link #cooldownTicks()}; stateful abilities may override it.
	 */
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return this.cooldownTicks;
	}
}
