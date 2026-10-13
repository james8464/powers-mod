package com.powers.companion;

import com.powers.fx.PowerFx;
import com.powers.PowersSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Shared manifestation and collapse cues, independent of session lifecycle ownership. */
final class ShadowBodyPresentation {
	private ShadowBodyPresentation() { }

	static void manifestBody(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.3, 0x55265F, 24, 0.0);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				com.powers.PowersParticles.ECLIPSE, 18, 0.55, 0.02);
	}

	static void collapse(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.4, 0x55265F, 24, Math.PI);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				com.powers.PowersParticles.ECLIPSE, 26, 0.7, 0.03);
		PowerFx.sound(level, position, PowersSounds.DARK_WHISPER, 1.1F, 0.55F);
	}

}
