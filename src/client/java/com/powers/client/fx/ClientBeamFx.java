package com.powers.client.fx;

import com.powers.PowersParticles;
import com.powers.fx.BeamFxStyle;
import com.powers.network.MagicFxPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Expands one semantic beam packet into an exact deterministic line locally. */
public final class ClientBeamFx {
	private ClientBeamFx() {
	}

	public static void handle(MagicFxPackets.BeamFxPayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		Vec3 from = new Vec3(payload.fromX(), payload.fromY(), payload.fromZ());
		Vec3 delta = new Vec3(payload.toX(), payload.toY(), payload.toZ()).subtract(from);
		ParticleOptions particle = particle(payload.style(), payload.color());
		int count = Math.max(1, (int) Math.round(payload.count() * FxAccessibility.effectScale(client)));
		for (int index = 1; index <= count; index++) {
			Vec3 point = from.add(delta.scale(index / (double) count));
			client.level.addParticle(particle, point.x, point.y, point.z, 0.0, 0.0, 0.0);
		}
	}

	private static ParticleOptions particle(BeamFxStyle style, int color) {
		return switch (style) {
			case MOTE -> PowersParticles.MOTE;
			case RIBBON -> PowersParticles.RIBBON;
			case SPARK -> PowersParticles.SPARK;
			case FRACTURE -> PowersParticles.FRACTURE;
			case ECLIPSE -> PowersParticles.ECLIPSE;
			case GLYPH -> PowersParticles.GLYPH;
			case SHARD -> PowersParticles.SHARD;
			case ROOT -> PowersParticles.ROOT;
			case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
			case FLAME -> ParticleTypes.FLAME;
			case SOUL -> PowersParticles.SHARD;
			case END_ROD -> PowersParticles.MOTE;
			case ENCHANT -> ParticleTypes.ENCHANT;
			case CLOUD -> PowersParticles.FRACTURE;
			case PORTAL -> PowersParticles.ECLIPSE;
			case COLORED -> new DustParticleOptions(color & 0xFFFFFF, 1.0F);
		};
	}
}
