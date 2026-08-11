package com.powers.fx;

import com.powers.config.PowersConfigLoader;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.network.MagicFxPackets;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/** Budgeted, recipient-indexed transport for compact server-authored visual cues. */
final class ServerFxTransport {
	private static final int VIEWER_LIMIT = 128;
	private static final double RANGE = 128.0;
	private static final Map<MinecraftServer, ViewerParticleBudget> BUDGETS = new WeakHashMap<>();
	private static final Map<ServerLevel, CachedViewers> VIEWERS = new WeakHashMap<>();
	private static final Map<MinecraftServer, AdaptiveState> ADAPTIVE = new WeakHashMap<>();

	private record CachedViewers(long tick, ViewerSpatialIndex<ServerPlayer> index) {
	}
	private record AdaptiveState(AdaptiveFxBudget budget, long tick, double scale) { }

	private ServerFxTransport() {
	}

	static void burst(ServerLevel level, Vec3 position, ParticleOptions particle,
			int count, double spread, double speed, boolean protectFirstPerson) {
		if (count <= 0) return;
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		int adaptiveCount = AdaptiveFxBudget.scaleCount(count, adaptiveScale(level), 1);
		for (ServerPlayer viewer : nearby(level, position)) {
			double distanceSquared = viewer.getEyePosition().distanceToSqr(position);
			int requested = protectFirstPerson
					? ParticleBudget.viewerCount(adaptiveCount, distanceSquared) : adaptiveCount;
			int granted = budget.claim(tick, viewer.getUUID(), requested, distanceSquared);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			level.sendParticles(viewer, particle, false, false, position.x, position.y, position.z,
					granted, spread, spread, spread, speed);
		}
	}

	static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, int steps) {
		if (steps <= 0 || !finite(from) || !finite(to)) return;
		int requested = AdaptiveFxBudget.scaleCount(Math.min(64, steps), adaptiveScale(level), 4);
		Vec3 midpoint = from.add(to).scale(0.5);
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		BeamFxStyle style = BeamFxStyle.from(particle);
		int color = BeamFxStyle.color(particle);
		long eventId = Integer.toUnsignedLong(Objects.hash(tick, from, to, style, color));
		for (ServerPlayer viewer : nearby(level, midpoint)) {
			int granted = claim(budget, tick, viewer, midpoint, requested);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			MagicFxPackets.sendBeam(viewer, MagicFxPackets.pooled(new MagicFxPackets.BeamFxPayload(
					eventId, style, from.x, from.y, from.z, to.x, to.y, to.z, granted, color)));
		}
	}

	static void shape(ServerLevel level, Vec3 center, double radius, double height,
			int rgb, int points, double phase, ShapeFxKind kind) {
		int requested = kind.requestedParticles(points);
		if (requested <= 0 || !finite(center) || !Double.isFinite(radius)
				|| !Double.isFinite(height) || !Double.isFinite(phase)) return;
		requested = AdaptiveFxBudget.scaleCount(requested, adaptiveScale(level), 6);
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		long eventId = Integer.toUnsignedLong(Objects.hash(tick, center, radius,
				height, rgb, phase, kind));
		for (ServerPlayer viewer : nearby(level, center)) {
			int granted = claim(budget, tick, viewer, center, requested);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			MagicFxPackets.sendShape(viewer, MagicFxPackets.pooled(new MagicFxPackets.ShapeFxPayload(
					eventId, kind, center.x, center.y, center.z, radius, height, granted, rgb, phase)));
		}
	}

	static void clear() {
		BUDGETS.clear();
		VIEWERS.clear();
		ADAPTIVE.clear();
	}

	private static int claim(ViewerParticleBudget budget, long tick, ServerPlayer viewer,
			Vec3 position, int requested) {
		double distanceSquared = viewer.getEyePosition().distanceToSqr(position);
		return budget.claim(tick, viewer.getUUID(),
				ParticleBudget.viewerCount(requested, distanceSquared), distanceSquared);
	}

	private static ViewerParticleBudget budget(ServerLevel level) {
		int serverLimit = PowersConfigLoader.get().maxParticlesPerTick();
		int viewerLimit = Math.min(VIEWER_LIMIT, Math.max(1, serverLimit));
		ViewerParticleBudget budget = BUDGETS.get(level.getServer());
		if (budget == null || budget.serverLimit() != serverLimit
				|| budget.viewerLimit() != viewerLimit) {
			budget = new ViewerParticleBudget(serverLimit, viewerLimit, RANGE);
			BUDGETS.put(level.getServer(), budget);
		}
		return budget;
	}

	private static List<ServerPlayer> nearby(ServerLevel level, Vec3 center) {
		long tick = level.getServer().getTickCount();
		CachedViewers cached = VIEWERS.get(level);
		if (cached == null || cached.tick() != tick) {
			ViewerSpatialIndex<ServerPlayer> index = new ViewerSpatialIndex<>(16);
			for (ServerPlayer player : level.players()) index.put(player, player.getX(), player.getZ());
			cached = new CachedViewers(tick, index);
			VIEWERS.put(level, cached);
		}
		return cached.index().nearby(center.x, center.z, RANGE);
	}

	private static boolean finite(Vec3 point) {
		return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
	}

	private static double adaptiveScale(ServerLevel level) {
		MinecraftServer server = level.getServer();
		long tick = server.getTickCount();
		AdaptiveState state = ADAPTIVE.get(server);
		if (state != null && state.tick() == tick) return state.scale();
		AdaptiveFxBudget controller = state == null ? new AdaptiveFxBudget(100) : state.budget();
		double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
		double scale = controller.update(mspt);
		ADAPTIVE.put(server, new AdaptiveState(controller, tick, scale));
		return scale;
	}
}
