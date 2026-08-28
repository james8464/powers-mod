package com.powers.client.fx;

import com.powers.PowersParticles;
import com.powers.client.audio.ClientLayeredAudioMixer;
import com.powers.network.CelestialRuinPackets;
import com.powers.spell.CelestialRuinPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders enormous Heavenfall columns and the local flash without server particle floods. */
public final class ClientCelestialRuinFx {
	private static final Map<Long, Column> COLUMNS = new HashMap<>();
	private static final int MAX_RINGING_EVENTS = 4;
	private static final Map<Long, Ringing> RINGING_EVENTS = new LinkedHashMap<>();
	private static int flashTicks;
	private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> flashDimension;
	private static int clientTick;

	private ClientCelestialRuinFx() {
	}

	public static void handle(CelestialRuinPackets.Payload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		var dimension = client.level.dimension();
		long key = key(dimension.identifier().hashCode(), payload.x(), payload.y(), payload.z());
		if (payload.phase() == CelestialRuinPackets.Phase.END) {
			COLUMNS.remove(key);
			return;
		}
		if (payload.phase() == CelestialRuinPackets.Phase.DETONATE) {
			flashTicks = CelestialRuinPresentation.FLASH_TICKS;
			flashDimension = dimension;
			while (RINGING_EVENTS.size() >= MAX_RINGING_EVENTS) {
				RINGING_EVENTS.remove(RINGING_EVENTS.keySet().iterator().next());
			}
			RINGING_EVENTS.put(key, new Ringing(CelestialRuinPresentation.RINGING_TICKS,
					payload.lod(), dimension));
			if (client.player != null) ClientLayeredAudioMixer.playLocalCelestial(
					CelestialRuinPresentation.audioGain(payload.lod()), 1.0F);
			return;
		}
		COLUMNS.put(key, new Column(new Vec3(payload.x(), payload.y(), payload.z()),
				CelestialRuinPresentation.BEAM_LEASE_TICKS, payload.age(), dimension, payload.lod()));
	}

	public static void tick() {
		clientTick++;
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			COLUMNS.clear();
			RINGING_EVENTS.clear();
			flashTicks = 0;
			flashDimension = null;
			return;
		}
		if (flashDimension != null && !flashDimension.equals(client.level.dimension())) {
			flashTicks = 0;
			flashDimension = null;
		} else if (flashTicks > 0) flashTicks--;
		tickRinging(client);
		for (Iterator<Column> iterator = COLUMNS.values().iterator(); iterator.hasNext();) {
			Column column = iterator.next();
			if (!column.dimension.equals(client.level.dimension()) || --column.lease <= 0) {
				iterator.remove();
				continue;
			}
			if (client.player.position().distanceToSqr(column.center)
					> (double) CelestialRuinPresentation.BEAM_VIEW_RADIUS
					* CelestialRuinPresentation.BEAM_VIEW_RADIUS) continue;
			spawnColumn(client, column);
		}
	}

	private static void tickRinging(Minecraft client) {
		float strongestVolume = 0.0F;
		float strongestPitch = 1.0F;
		for (Iterator<Ringing> iterator = RINGING_EVENTS.values().iterator(); iterator.hasNext();) {
			Ringing ringing = iterator.next();
			if (!ringing.dimension.equals(client.level.dimension()) || --ringing.remainingTicks <= 0) {
				iterator.remove();
				continue;
			}
			if (ringing.remainingTicks % 20 != 0) continue;
			float volume = CelestialRuinPresentation.ringingVolume(ringing.remainingTicks)
					* CelestialRuinPresentation.audioGain(ringing.lod);
			if (volume <= strongestVolume) continue;
			strongestVolume = volume;
			strongestPitch = 1.18F + (CelestialRuinPresentation.RINGING_TICKS
					- ringing.remainingTicks) * 0.002F;
		}
		if (strongestVolume > 0.0F) {
			ClientLayeredAudioMixer.playLocalCelestial(strongestVolume, strongestPitch);
		}
	}

	private static void spawnColumn(Minecraft client, Column column) {
		DustParticleOptions warm = new DustParticleOptions(0xFFF3C4, 3.5F);
		DustParticleOptions white = new DustParticleOptions(0xFFFFFF, 4.0F);
		int minY = client.level.getMinY() + 1;
		int maxY = client.level.getMaxY() - 2;
		var density = CelestialRuinPresentation.columnDensity(column.lod);
		if (density.particleCount() == 0) return;
		double pulse = 48.0 + Math.sin((clientTick + column.age) * 0.08) * 2.0;
		double verticalSpan = 264.0;
		for (int slice = 0; slice < density.verticalSlices(); slice++) {
			double progress = slice / (double) Math.max(1, density.verticalSlices() - 1);
			double y = Math.clamp(client.player.getY() - 36.0 + progress * verticalSpan, minY, maxY);
			for (int index = 0; index < density.particlesPerSlice(); index++) {
				double fraction = ((index * 37L + slice * 53L + clientTick * 3L) & 127) / 127.0;
				double radius = pulse * Math.sqrt(fraction);
				double angle = index * 2.399963 + clientTick * 0.025 + slice * 0.37;
				double x = column.center.x + Math.cos(angle) * radius;
				double z = column.center.z + Math.sin(angle) * radius;
				client.level.addAlwaysVisibleParticle(index % 4 == 0 ? white : warm, true,
						x, y, z, 0.0, 0.02, 0.0);
				if (slice == 1 && index % 2 == 0) client.level.addAlwaysVisibleParticle(
						PowersParticles.MOTE, true, x, y, z, 0.0, 0.01, 0.0);
			}
		}
		double boundaryY = Math.clamp(client.player.getY() + 0.5, minY, maxY);
		for (int index = 0; index < density.boundaryParticles(); index++) {
			double angle = Math.PI * 2.0 * index
					/ density.boundaryParticles() + clientTick * 0.018;
			client.level.addAlwaysVisibleParticle(white, true,
					column.center.x + Math.cos(angle) * pulse,
					boundaryY, column.center.z + Math.sin(angle) * pulse, 0.0, 0.025, 0.0);
		}
	}

	public static void renderFlash(GuiGraphicsExtractor graphics) {
		int alpha = CelestialRuinPresentation.flashAlpha(flashTicks);
		if (alpha <= 0) return;
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
	}

	public static void reset() {
		COLUMNS.clear();
		RINGING_EVENTS.clear();
		flashTicks = 0;
		flashDimension = null;
		clientTick = 0;
	}

	/** Bounded receiver state used by the real-client overlapping-event proof. */
	public static int activeRingingCount() {
		return RINGING_EVENTS.size();
	}

	private static long key(int dimension, double x, double y, double z) {
		return java.util.Objects.hash(dimension, Math.floor(x), Math.floor(y), Math.floor(z));
	}

	private static final class Column {
		private final Vec3 center;
		private final int age;
		private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
		private final com.powers.fx.FxLodTier lod;
		private int lease;

		private Column(Vec3 center, int lease, int age,
				net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
				com.powers.fx.FxLodTier lod) {
			this.center = center;
			this.lease = lease;
			this.age = age;
			this.dimension = dimension;
			this.lod = lod;
		}
	}

	private static final class Ringing {
		private int remainingTicks;
		private final com.powers.fx.FxLodTier lod;
		private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;

		private Ringing(int remainingTicks, com.powers.fx.FxLodTier lod,
				net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
			this.remainingTicks = remainingTicks;
			this.lod = lod;
			this.dimension = dimension;
		}
	}
}
