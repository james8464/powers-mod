package com.powers;

import com.powers.magic.runtime.MagicRuntime;
import com.powers.network.RankTenSilhouettePackets;
import com.powers.network.CastingPosePackets;
import com.powers.power.PowerRegistry;
import com.powers.util.ScheduledTaskQueue;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stable Fabric entrypoint and compatibility facade for shared scheduling helpers. */
public final class PowersMod implements ModInitializer {
	public static final String MOD_ID = "powers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Realm palette echoed by a scheduled magical storm. */
	public enum StormTheme { NONE, DARK, LIGHT }

	/** Callback supplied with the currently active server and stable task identity. */
	@FunctionalInterface
	public interface DelayedServerAction {
		void run(MinecraftServer server, ScheduledTaskQueue.TaskDescriptor task);
	}

	@Override
	public void onInitialize() {
		PowersBootstrap.initialize();
		RankTenSilhouettePackets.initialize();
		CastingPosePackets.initialize();
		PowersServerLifecycle.initialize();
		LOGGER.info("Magic collision kernel loaded: {} actions, {} exhaustive interactions",
				MagicRuntime.catalogue().definitions().size(), MagicRuntime.global().interactionCount());
		LOGGER.info("POWERS framework initialized with {} power(s)", PowerRegistry.getAll().size());
	}

	/** Starts a visual lightning storm at a fixed point. */
	public static void startStorm(ServerLevel level, Vec3 position, int ticks) {
		startStorm(level, position, null, ticks, 0, StormTheme.NONE);
	}

	/** Starts a fixed storm using one realm's visual palette. */
	public static void startStorm(ServerLevel level, Vec3 position, int ticks, StormTheme theme) {
		startStorm(level, position, null, ticks, 0, theme);
	}

	/** Starts a storm that follows a player for a bounded number of ticks. */
	public static void startStorm(ServerLevel level, Vec3 position,
			ServerPlayer follow, int ticks, int followTicks) {
		startStorm(level, position, follow, ticks, followTicks, StormTheme.NONE);
	}

	/** Starts a bounded, optionally following storm with an explicit realm palette. */
	public static void startStorm(ServerLevel level, Vec3 position, ServerPlayer follow,
			int ticks, int followTicks, StormTheme theme) {
		ServerMagicScheduler.startStorm(level, position, follow, ticks, followTicks, theme);
	}

	/** Runs owned work after a bounded delay without retaining entity or level instances. */
	public static ScheduledTaskQueue.TaskToken scheduleDelayed(
			MinecraftServer server, int ticks, UUID subjectId, ResourceKey<Level> dimension,
			UUID cancellationOwner, String purpose, DelayedServerAction action) {
		return ServerMagicScheduler.schedule(server, ticks, subjectId, dimension,
				cancellationOwner, purpose, action);
	}

	/** Cancels all delayed work owned by one disconnecting or dying principal. */
	public static int cancelDelayedTasks(UUID cancellationOwner) {
		return ServerMagicScheduler.cancelOwner(cancellationOwner);
	}

	/** Stable delayed-work identities for bounded operator diagnostics. */
	public static List<ScheduledTaskQueue.TaskDescriptor> delayedTasks() {
		return ServerMagicScheduler.delayedTasks();
	}

	/** Creates one identifier in the stable POWERS namespace. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
