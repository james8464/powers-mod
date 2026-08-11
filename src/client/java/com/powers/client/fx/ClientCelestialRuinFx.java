package com.powers.client.fx;

import com.powers.PowersSounds;
import com.powers.network.CelestialRuinPackets;
import com.powers.spell.CelestialRuinPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Renders enormous Heavenfall columns and the local flash without server particle floods. */
public final class ClientCelestialRuinFx {
	private static final Map<Long, Column> COLUMNS = new HashMap<>();
	private static int flashTicks;
	private static int ringingTicks;
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
			ringingTicks = CelestialRuinPresentation.RINGING_TICKS;
			flashDimension = dimension;
			if (client.player != null) {
				client.player.playSound(PowersSounds.CELESTIAL_RING, 1.0F, 1.0F);
			}
			return;
		}
		COLUMNS.put(key, new Column(new Vec3(payload.x(), payload.y(), payload.z()),
				CelestialRuinPresentation.BEAM_LEASE_TICKS, payload.age(), dimension));
	}

	public static void tick() {
		clientTick++;
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			COLUMNS.clear();
			flashTicks = 0;
			ringingTicks = 0;
			flashDimension = null;
			return;
		}
		if (flashDimension != null && !flashDimension.equals(client.level.dimension())) {
			flashTicks = 0;
			ringingTicks = 0;
			flashDimension = null;
		} else if (flashTicks > 0) flashTicks--;
		if (ringingTicks > 0) {
			ringingTicks--;
			if (ringingTicks > 0 && ringingTicks % 20 == 0) {
				client.player.playSound(PowersSounds.CELESTIAL_RING,
						CelestialRuinPresentation.ringingVolume(ringingTicks),
						1.18F + (CelestialRuinPresentation.RINGING_TICKS - ringingTicks) * 0.002F);
			}
		}
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

	private static void spawnColumn(Minecraft client, Column column) {
		DustParticleOptions warm = new DustParticleOptions(0xFFF3C4, 3.5F);
		DustParticleOptions white = new DustParticleOptions(0xFFFFFF, 4.0F);
		int minY = client.level.getMinY() + 1;
		int maxY = client.level.getMaxY() - 2;
		double pulse = 48.0 + Math.sin((clientTick + column.age) * 0.08) * 2.0;
		for (int slice = 0; slice < CelestialRuinPresentation.BEAM_VERTICAL_SLICES; slice++) {
			double y = Math.clamp(client.player.getY() - 36.0 + slice * 24.0, minY, maxY);
			for (int index = 0; index < CelestialRuinPresentation.BEAM_PARTICLES_PER_SLICE; index++) {
				double fraction = ((index * 37L + slice * 53L + clientTick * 3L) & 127) / 127.0;
				double radius = pulse * Math.sqrt(fraction);
				double angle = index * 2.399963 + clientTick * 0.025 + slice * 0.37;
				double x = column.center.x + Math.cos(angle) * radius;
				double z = column.center.z + Math.sin(angle) * radius;
				client.level.addParticle(index % 4 == 0 ? white : warm,
						x, y, z, 0.0, 0.02, 0.0);
				if (slice == 1 && index % 2 == 0) client.level.addParticle(ParticleTypes.END_ROD,
						x, y, z, 0.0, 0.01, 0.0);
			}
		}
		double boundaryY = Math.clamp(client.player.getY() + 0.5, minY, maxY);
		for (int index = 0; index < CelestialRuinPresentation.BEAM_BOUNDARY_PARTICLES; index++) {
			double angle = Math.PI * 2.0 * index
					/ CelestialRuinPresentation.BEAM_BOUNDARY_PARTICLES + clientTick * 0.018;
			client.level.addParticle(white, column.center.x + Math.cos(angle) * pulse,
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
		flashTicks = 0;
		ringingTicks = 0;
		flashDimension = null;
		clientTick = 0;
	}

	private static long key(int dimension, double x, double y, double z) {
		return java.util.Objects.hash(dimension, Math.floor(x), Math.floor(y), Math.floor(z));
	}

	private static final class Column {
		private final Vec3 center;
		private final int age;
		private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
		private int lease;

		private Column(Vec3 center, int lease, int age,
				net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
			this.center = center;
			this.lease = lease;
			this.age = age;
			this.dimension = dimension;
		}
	}
}
