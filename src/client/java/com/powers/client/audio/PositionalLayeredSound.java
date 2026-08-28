package com.powers.client.audio;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** One non-looping directional semantic cue anchored at its authoritative server origin. */
public final class PositionalLayeredSound extends AbstractSoundInstance {
	public PositionalLayeredSound(SoundEvent sound, float volume, float pitch,
			double x, double y, double z) {
		super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.volume = volume;
		this.pitch = pitch;
		this.x = x;
		this.y = y;
		this.z = z;
		this.looping = false;
		this.delay = 0;
		this.attenuation = Attenuation.LINEAR;
		this.relative = false;
	}
}
