package com.powers.command;

import net.minecraft.commands.CommandSourceStack;

import java.util.Optional;

/** Runtime bridge for permission mods; works safely with no adapter installed. */
public final class PermissionNodes {
	@FunctionalInterface
	public interface Provider {
		Optional<Boolean> decision(CommandSourceStack source, PermissionNode node);
	}

	private static volatile Provider provider;

	private PermissionNodes() { }

	public static boolean allows(CommandSourceStack source, PermissionNode node) {
		Optional<Boolean> decision = Optional.empty();
		Provider active = provider;
		if (active != null) {
			try {
				decision = active.decision(source, node);
				if (decision == null) decision = Optional.empty();
			} catch (RuntimeException ignored) {
				decision = Optional.empty();
			}
		}
		return PermissionNodePolicy.allowed(PowerCommand.hasVanillaAdmin(source), decision);
	}

	public static void install(Provider adapter) { provider = adapter; }
	public static void clear() { provider = null; }
}
