package com.powers.util;

import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.function.Consumer;

/** Re-enters asynchronous work only through the still-active server lifecycle epoch. */
public final class ServerCallbackGate {
	private static final LifecycleCallbackGate<MinecraftServer> SERVERS = new LifecycleCallbackGate<>();

	private ServerCallbackGate() {
	}

	public static long bind(MinecraftServer server) {
		return SERVERS.bind(server);
	}

	public static long capture(MinecraftServer server) {
		return SERVERS.bind(server);
	}

	public static void execute(long epoch, Consumer<MinecraftServer> action) {
		Objects.requireNonNull(action, "action");
		SERVERS.resolve(epoch).ifPresent(server -> server.execute(() ->
				SERVERS.resolve(epoch).filter(current -> current == server).ifPresent(action)));
	}

	public static void clear(MinecraftServer server) {
		SERVERS.clear(server);
	}
}
