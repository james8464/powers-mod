package com.powers;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/** Registers the eight authored magic particle sprites used by client choreography. */
public final class PowersParticles {
	public static final SimpleParticleType MOTE = register("mote");
	public static final SimpleParticleType SHARD = register("shard");
	public static final SimpleParticleType GLYPH = register("glyph");
	public static final SimpleParticleType RIBBON = register("ribbon");
	public static final SimpleParticleType SPARK = register("spark");
	public static final SimpleParticleType ECLIPSE = register("eclipse");
	public static final SimpleParticleType ROOT = register("root");
	public static final SimpleParticleType FRACTURE = register("fracture");
	public static final List<SimpleParticleType> ALL = List.of(
			MOTE, SHARD, GLYPH, RIBBON, SPARK, ECLIPSE, ROOT, FRACTURE);

	private PowersParticles() {
	}

	public static void initialize() {
		// Static field initialization performs registration exactly once.
	}

	private static SimpleParticleType register(String path) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, PowersMod.id(path), FabricParticleTypes.simple());
	}
}
