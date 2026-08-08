package com.powers.client.fx;

import com.powers.PowersParticles;
import com.powers.fx.FxGeometry;
import com.powers.magic.fx.FxChoreography;
import com.powers.magic.fx.FxFrame;
import com.powers.magic.fx.FxMotif;
import com.powers.network.MagicFxPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Expands compact server cues into deterministic, distance-bounded local choreography. */
public final class ClientMagicFx {
	private static final int MAX_RECENT_EVENTS = 256;
	private static final int MAX_PENDING_EVENTS = 32;
	private static final Set<Long> RECENT_EVENTS = new LinkedHashSet<>();
	private static final List<PendingFx> PENDING = new ArrayList<>();

	private ClientMagicFx() {
	}

	public static void handle(MagicFxPackets.MagicFxPayload payload) {
		if (!remember(payload.eventId()) || PENDING.size() >= MAX_PENDING_EVENTS) return;
		PENDING.add(new PendingFx(payload, 0));
	}

	/** Advances the event-specific readable beats without blocking client threads. */
	public static void tick() {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			PENDING.clear();
			return;
		}
		for (Iterator<PendingFx> iterator = PENDING.iterator(); iterator.hasNext();) {
			PendingFx pending = iterator.next();
			boolean reducedMotion = FxAccessibility.reducedMotion(client);
			FxChoreography.frame(pending.payload().kind(), pending.age(), reducedMotion)
					.ifPresent(frame -> spawn(client, pending.payload(), frame, reducedMotion));
			if (FxChoreography.finished(pending.payload().kind(), pending.age())) iterator.remove();
			else pending.advance();
		}
	}

	private static void spawn(Minecraft client, MagicFxPackets.MagicFxPayload payload,
			FxFrame frame, boolean reducedMotion) {
		Vec3 origin = new Vec3(payload.x(), payload.y(), payload.z());
		double distance = client.player.position().distanceTo(origin);
		double scale = FxAccessibility.effectScale(client) * frame.budgetScale();
		int budget = FxGeometry.budget(distance, payload.intensity(), scale);
		FxMotif requested = frame.motifOverride().orElseGet(() -> FxMotif.fromCue(payload.motif()));
		FxMotif motif = FxGeometry.accessibleMotif(requested, reducedMotion);
		var orientation = frame.orientation().resolve(motif);
		Vec3 viewer = client.player.position();
		double billboardAngle = Math.atan2(-(viewer.x - origin.x), viewer.z - origin.z);
		var points = FxGeometry.points(motif, payload.glyphSeed(), payload.intensity(), budget);
		SimpleParticleType sprite = particleFor(motif);
		for (int index = 0; index < points.size(); index++) {
			FxGeometry.Point scaled = FxGeometry.scale(points.get(index), frame.geometryScale());
			FxGeometry.Point point = FxGeometry.transform(scaled, orientation, billboardAngle);
			double x = origin.x + point.x();
			double y = origin.y + frame.verticalOffset() + point.y();
			double z = origin.z + point.z();
			double velocity = frame.velocityScale();
			client.level.addParticle(sprite, x, y, z, point.x() * 0.006 * velocity,
					0.008 * velocity, point.z() * 0.006 * velocity);
			if (index % 4 == 0) {
				int color = index % 8 == 0 ? payload.primaryColor() : payload.secondaryColor();
				ParticleOptions tint = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
						0xD9000000 | (color & 0xFFFFFF));
				client.level.addParticle(tint, x, y, z, 0.0, 0.004, 0.0);
			}
		}
	}

	public static void reset() {
		RECENT_EVENTS.clear();
		PENDING.clear();
	}

	private static synchronized boolean remember(long eventId) {
		if (!RECENT_EVENTS.add(eventId)) return false;
		while (RECENT_EVENTS.size() > MAX_RECENT_EVENTS) {
			RECENT_EVENTS.remove(RECENT_EVENTS.iterator().next());
		}
		return true;
	}

	private static SimpleParticleType particleFor(FxMotif motif) {
		return switch (motif) {
			case RING -> PowersParticles.MOTE;
			case SPIRAL -> PowersParticles.RIBBON;
			case TETHER -> PowersParticles.RIBBON;
			case FORK -> PowersParticles.SPARK;
			case SHARD -> PowersParticles.SHARD;
			case GLYPH -> PowersParticles.GLYPH;
			case ROOT -> PowersParticles.ROOT;
			case ECLIPSE -> PowersParticles.ECLIPSE;
			case FRACTURE -> PowersParticles.FRACTURE;
		};
	}

	/** Mutable age is isolated inside the client-only pending queue. */
	private static final class PendingFx {
		private final MagicFxPackets.MagicFxPayload payload;
		private int age;

		private PendingFx(MagicFxPackets.MagicFxPayload payload, int age) {
			this.payload = payload;
			this.age = age;
		}

		private MagicFxPackets.MagicFxPayload payload() { return payload; }
		private int age() { return age; }
		private void advance() { age++; }
	}
}
