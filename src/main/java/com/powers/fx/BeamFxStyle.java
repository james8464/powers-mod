package com.powers.fx;

import com.powers.PowersParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Objects;

/** Stable semantic styles used to recreate straight beams on each client. */
public enum BeamFxStyle {
	MOTE,
	RIBBON,
	SPARK,
	FRACTURE,
	ECLIPSE,
	GLYPH,
	SHARD,
	ROOT,
	ELECTRIC,
	FLAME,
	SOUL,
	END_ROD,
	ENCHANT,
	CLOUD,
	PORTAL,
	COLORED;

	public int networkId() {
		return ordinal();
	}

	public static BeamFxStyle fromNetworkId(int id) {
		if (id < 0 || id >= values().length) throw new IllegalArgumentException("Unknown beam style: " + id);
		return values()[id];
	}

	/** Collapses arbitrary server particle options to a bounded authored style. */
	public static BeamFxStyle from(ParticleOptions particle) {
		Objects.requireNonNull(particle, "particle");
		if (particle instanceof ColorParticleOption || particle instanceof DustParticleOptions) return COLORED;
		if (particle.getType() == ParticleTypes.ELECTRIC_SPARK) return ELECTRIC;
		if (particle.getType() == ParticleTypes.FLAME) return FLAME;
		if (particle.getType() == ParticleTypes.SOUL_FIRE_FLAME
				|| particle.getType() == ParticleTypes.SOUL) return SOUL;
		if (particle.getType() == ParticleTypes.END_ROD) return END_ROD;
		if (particle.getType() == ParticleTypes.ENCHANT) return ENCHANT;
		if (particle.getType() == ParticleTypes.CLOUD) return CLOUD;
		if (particle.getType() == ParticleTypes.PORTAL
				|| particle.getType() == ParticleTypes.REVERSE_PORTAL) return PORTAL;
		if (particle.getType() == PowersParticles.RIBBON) return RIBBON;
		if (particle.getType() == PowersParticles.SPARK) return SPARK;
		if (particle.getType() == PowersParticles.FRACTURE) return FRACTURE;
		if (particle.getType() == PowersParticles.ECLIPSE) return ECLIPSE;
		if (particle.getType() == PowersParticles.GLYPH) return GLYPH;
		if (particle.getType() == PowersParticles.SHARD) return SHARD;
		if (particle.getType() == PowersParticles.ROOT) return ROOT;
		return MOTE;
	}

	/** Extracts a 24-bit tint when the source option carries one. */
	public static int color(ParticleOptions particle) {
		float redChannel;
		float greenChannel;
		float blueChannel;
		if (particle instanceof ColorParticleOption color) {
			redChannel = color.getRed();
			greenChannel = color.getGreen();
			blueChannel = color.getBlue();
		} else if (particle instanceof DustParticleOptions dust) {
			redChannel = dust.getColor().x();
			greenChannel = dust.getColor().y();
			blueChannel = dust.getColor().z();
		} else return 0xFFFFFF;
		int red = Math.clamp(Math.round(redChannel * 255.0F), 0, 255);
		int green = Math.clamp(Math.round(greenChannel * 255.0F), 0, 255);
		int blue = Math.clamp(Math.round(blueChannel * 255.0F), 0, 255);
		return red << 16 | green << 8 | blue;
	}
}
