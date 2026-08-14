package com.powers.client.fx;

import com.powers.network.EventAudioPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** Plays one restrained local layer when an authored event is beyond positional sound range. */
public final class ClientEventAudio {
	private static int handledCount;

	private ClientEventAudio() {
	}

	public static void handle(EventAudioPackets.Payload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
				payload.cue().sound(), payload.pitch(), payload.cue().volume(payload.lod())));
		handledCount++;
	}

	/** Number of production receiver cues handled in this client session. */
	public static int handledCount() {
		return handledCount;
	}

	public static void resetMetrics() {
		handledCount = 0;
	}
}
