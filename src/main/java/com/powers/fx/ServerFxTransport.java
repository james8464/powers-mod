package com.powers.fx;

import com.powers.config.PowersConfigLoader;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.network.FxPayloadBatch;
import com.powers.network.MagicFxPackets;
import com.powers.network.EventAudioPackets;
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
	private static final double BURST_RANGE = 128.0;
	private static final double MAX_SEMANTIC_RANGE = FxLodScope.EVENT_SCALE.maximumRange();
	private static final Map<MinecraftServer, ViewerParticleBudget> BUDGETS = new WeakHashMap<>();
	private static final Map<ServerLevel, CachedViewers> VIEWERS = new WeakHashMap<>();
	private static final Map<MinecraftServer, AdaptiveState> ADAPTIVE = new WeakHashMap<>();
	private static final Map<MinecraftServer, LodCounters> LOD_METRICS = new WeakHashMap<>();

	private record CachedViewers(long tick, ViewerSpatialIndex<ServerPlayer> index) {
	}
	private record AdaptiveState(AdaptiveFxBudget budget, long tick, double scale) { }
	private static final class LodCounters {
		private long nearDeliveries;
		private long midDeliveries;
		private long farDeliveries;
		private long nearSamples;
		private long midSamples;
		private long farSamples;
		private long nearAudio;
		private long midAudio;
		private long farAudio;

		private void record(FxLodTier tier, int samples) {
			switch (tier) {
				case NEAR -> { nearDeliveries++; nearSamples += samples; }
				case MID -> { midDeliveries++; midSamples += samples; }
				case FAR -> { farDeliveries++; farSamples += samples; }
				case HIDDEN -> { }
			}
		}

		private PowerFx.LodSnapshot snapshot() {
			return new PowerFx.LodSnapshot(nearDeliveries, midDeliveries, farDeliveries,
					nearSamples, midSamples, farSamples, nearAudio, midAudio, farAudio);
		}

		private void recordAudio(FxLodTier tier) {
			switch (tier) {
				case NEAR -> nearAudio++;
				case MID -> midAudio++;
				case FAR -> farAudio++;
				case HIDDEN -> { }
			}
		}
	}

	private ServerFxTransport() {
	}

	static void burst(ServerLevel level, Vec3 position, ParticleOptions particle,
			int count, double spread, double speed, boolean protectFirstPerson) {
		if (count <= 0) return;
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		int adaptiveCount = AdaptiveFxBudget.scaleCount(count, adaptiveScale(level), 1);
		for (ServerPlayer viewer : nearby(level, position, BURST_RANGE)) {
			Vec3 eye = viewer.getEyePosition();
			double distanceSquared = eye.distanceToSqr(position);
			Vec3 offset = position.subtract(eye);
			double viewDot = offset.lengthSqr() < 1.0E-8 ? 1.0
					: viewer.getLookAngle().dot(offset.normalize());
			int requested = protectFirstPerson
					? ParticleBudget.viewerCount(adaptiveCount, distanceSquared, viewDot) : adaptiveCount;
			int granted = budget.claim(tick, viewer.getUUID(), requested, distanceSquared);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			level.sendParticles(viewer, particle, false, false, position.x, position.y, position.z,
					granted, spread, spread, spread, speed);
		}
	}

	static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, int steps) {
		beam(level, from, to, particle, steps, FxLodScope.LOCAL);
	}

	static void eventBeam(ServerLevel level, Vec3 from, Vec3 to,
			ParticleOptions particle, int steps) {
		beam(level, from, to, particle, steps, FxLodScope.EVENT_SCALE);
	}

	private static void beam(ServerLevel level, Vec3 from, Vec3 to,
			ParticleOptions particle, int steps, FxLodScope scope) {
		if (steps <= 0 || !finite(from) || !finite(to)) return;
		int requested = AdaptiveFxBudget.scaleCount(Math.min(64, steps), adaptiveScale(level), 4);
		if (scope != FxLodScope.LOCAL) requested = Math.max(FxShapeFamily.BEAM.minimumSamples(), requested);
		Vec3 midpoint = from.add(to).scale(0.5);
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		BeamFxStyle style = BeamFxStyle.from(particle);
		int color = BeamFxStyle.color(particle);
		long eventId = Integer.toUnsignedLong(Objects.hash(tick, from, to, style, color));
		FxPayloadBatch.Beam payloads = FxPayloadBatch.beam(eventId, style,
				from.x, from.y, from.z, to.x, to.y, to.z, color);
		for (ServerPlayer viewer : nearby(level, midpoint, scope.maximumRange())) {
			int granted = claim(budget, tick, viewer, midpoint, requested,
					scope, FxShapeFamily.BEAM);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			boolean override = Math.max(viewer.getEyePosition().distanceToSqr(from),
					viewer.getEyePosition().distanceToSqr(to)) > 32.0 * 32.0;
			MagicFxPackets.sendBeam(viewer, payloads.forCount(granted, override));
		}
	}

	static void shape(ServerLevel level, Vec3 center, double radius, double height,
			int rgb, int points, double phase, ShapeFxKind kind) {
		shape(level, center, radius, height, rgb, points, phase, kind, FxLodScope.LOCAL);
	}

	static void eventShape(ServerLevel level, Vec3 center, double radius, double height,
			int rgb, int points, double phase, ShapeFxKind kind) {
		shape(level, center, radius, height, rgb, points, phase, kind, FxLodScope.EVENT_SCALE);
	}

	static void eventAudio(ServerLevel level, Vec3 position, EventAudioPackets.Cue cue,
			float positionalVolume, float pitch) {
		if (!finite(position)) return;
		double ordinaryRange = Math.max(16.0, Math.max(0.0F, positionalVolume) * 16.0);
		long tick = level.getServer().getTickCount();
		for (ServerPlayer viewer : nearby(level, position, FxLodScope.EVENT_SCALE.maximumRange())) {
			double distance = viewer.getEyePosition().distanceTo(position);
			if (distance <= ordinaryRange) continue;
			var lod = FxLodPolicy.decide(distance, 1, FxLodScope.EVENT_SCALE, FxShapeFamily.MAGIC);
			if (!lod.visible() || !EventAudioPackets.send(viewer, cue, lod.tier(), pitch)) continue;
			LOD_METRICS.computeIfAbsent(level.getServer(), ignored -> new LodCounters())
					.recordAudio(lod.tier());
			ServerRuntimeMetrics.recordPacket(level.getServer(), tick);
		}
	}

	private static void shape(ServerLevel level, Vec3 center, double radius, double height,
			int rgb, int points, double phase, ShapeFxKind kind, FxLodScope scope) {
		int requested = kind.requestedParticles(points);
		if (requested <= 0 || !finite(center) || !Double.isFinite(radius)
				|| !Double.isFinite(height) || !Double.isFinite(phase)) return;
		requested = AdaptiveFxBudget.scaleCount(requested, adaptiveScale(level), 6);
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		long eventId = Integer.toUnsignedLong(Objects.hash(tick, center, radius,
				height, rgb, phase, kind));
		FxPayloadBatch.Shape payloads = FxPayloadBatch.shape(eventId, kind,
				center.x, center.y, center.z, radius, height, rgb, phase);
		FxShapeFamily family = switch (kind) {
			case RING -> FxShapeFamily.RING;
			case RUNE -> FxShapeFamily.RUNE;
			case SPIRAL -> FxShapeFamily.SPIRAL;
		};
		if (scope != FxLodScope.LOCAL) requested = Math.max(family.minimumSamples(), requested);
		for (ServerPlayer viewer : nearby(level, center, scope.maximumRange())) {
			int granted = claim(budget, tick, viewer, center, requested, scope, family);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			double centerDistance = viewer.getEyePosition().distanceTo(center);
			boolean override = requiresDistanceOverride(centerDistance, radius, height);
			MagicFxPackets.sendShape(viewer, payloads.forCount(granted, override));
		}
	}

	static boolean requiresDistanceOverride(double centerDistance, double radius, double height) {
		if (!Double.isFinite(centerDistance) || !Double.isFinite(radius) || !Double.isFinite(height)) {
			return true;
		}
		return centerDistance + Math.hypot(radius, height) > 32.0;
	}

	static void clear() {
		BUDGETS.clear();
		VIEWERS.clear();
		ADAPTIVE.clear();
		LOD_METRICS.clear();
	}

	static void resetLodMetrics(MinecraftServer server) {
		LOD_METRICS.remove(server);
		// Fabric's parallel GameTests reuse one mock-player UUID. Clear only that
		// server's ephemeral allowance so another test cannot consume this proof's budget.
		BUDGETS.remove(server);
	}

	static PowerFx.LodSnapshot lodSnapshot(MinecraftServer server) {
		LodCounters counters = LOD_METRICS.get(server);
		return counters == null ? new PowerFx.LodSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0)
				: counters.snapshot();
	}

	private static int claim(ViewerParticleBudget budget, long tick, ServerPlayer viewer,
			Vec3 position, int requested, FxLodScope scope, FxShapeFamily family) {
		double distanceSquared = viewer.getEyePosition().distanceToSqr(position);
		var lod = FxLodPolicy.decide(Math.sqrt(distanceSquared), requested, scope, family);
		if (!lod.visible()) return 0;
		int granted = budget.claim(tick, viewer.getUUID(),
				ParticleBudget.viewerCount(lod.particleCount(), distanceSquared), distanceSquared);
		if (granted > 0) LOD_METRICS.computeIfAbsent(viewer.level().getServer(),
				ignored -> new LodCounters()).record(lod.tier(), granted);
		return granted;
	}

	private static ViewerParticleBudget budget(ServerLevel level) {
		int serverLimit = PowersConfigLoader.get().maxParticlesPerTick();
		int viewerLimit = Math.min(VIEWER_LIMIT, Math.max(1, serverLimit));
		ViewerParticleBudget budget = BUDGETS.get(level.getServer());
		if (budget == null || budget.serverLimit() != serverLimit
				|| budget.viewerLimit() != viewerLimit) {
			budget = new ViewerParticleBudget(serverLimit, viewerLimit, MAX_SEMANTIC_RANGE);
			BUDGETS.put(level.getServer(), budget);
		}
		return budget;
	}

	private static List<ServerPlayer> nearby(ServerLevel level, Vec3 center, double range) {
		long tick = level.getServer().getTickCount();
		CachedViewers cached = VIEWERS.get(level);
		if (cached == null || cached.tick() != tick) {
			ViewerSpatialIndex<ServerPlayer> index = new ViewerSpatialIndex<>(16);
			for (ServerPlayer player : level.players()) index.put(player, player.getX(), player.getZ());
			cached = new CachedViewers(tick, index);
			VIEWERS.put(level, cached);
		}
		return cached.index().nearby(center.x, center.z, range);
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
