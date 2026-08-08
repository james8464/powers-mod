package com.powers.client.fx.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Lightweight full-bright particle shared by the eight authored shape sprites. */
public final class ArcaneParticle extends SimpleAnimatedParticle {
	private ArcaneParticle(ClientLevel level, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ, SpriteSet sprites, float scale) {
		super(level, x, y, z, sprites, -0.006f);
		this.xd = velocityX;
		this.yd = velocityY;
		this.zd = velocityZ;
		this.quadSize *= scale;
		this.lifetime = 18 + random.nextInt(11);
		this.friction = 0.94f;
		this.hasPhysics = false;
		this.setSpriteFromAge(sprites);
	}

	/** Sprite-aware provider registered after the particle atlas is available. */
	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;
		private final float scale;

		public Provider(SpriteSet sprites, float scale) {
			this.sprites = sprites;
			this.scale = scale;
		}

		@Override
		public Particle createParticle(SimpleParticleType options, ClientLevel level,
				double x, double y, double z, double velocityX, double velocityY,
				double velocityZ, RandomSource random) {
			return new ArcaneParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites, scale);
		}
	}
}
