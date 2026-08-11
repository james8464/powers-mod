package com.powers.command;

import net.minecraft.commands.CommandSourceStack;

import java.util.Optional;
import java.util.function.Predicate;

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

	/** Installs Fabric Permission API/LuckPerms routing when that optional module is present. */
	public static boolean installFabricAdapterIfPresent() {
		try {
			Class<?> predicates = Class.forName(
					"net.fabricmc.fabric.api.permission.v1.PermissionPredicates");
			var require = predicates.getMethod("require", net.minecraft.resources.Identifier.class,
					net.minecraft.server.permissions.PermissionLevel.class);
			install((source, node) -> {
				try {
					var level = net.minecraft.server.permissions.PermissionLevel.byId(
							CommandPermissionRules.tier(
									com.powers.config.PowersConfigLoader.get().adminPermissionLevel()));
					@SuppressWarnings("unchecked")
					Predicate<CommandSourceStack> predicate = (Predicate<CommandSourceStack>) require.invoke(
							null, net.minecraft.resources.Identifier.parse(node.id()), level);
					return Optional.of(predicate.test(source));
				} catch (ReflectiveOperationException | RuntimeException error) {
					return Optional.empty();
				}
			});
			return true;
		} catch (ReflectiveOperationException | LinkageError absent) {
			return false;
		}
	}
}
