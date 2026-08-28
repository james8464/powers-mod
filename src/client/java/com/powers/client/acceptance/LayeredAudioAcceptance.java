package com.powers.client.acceptance;

import com.powers.audio.LayeredAudioCue;
import com.powers.client.audio.ClientAudioComfortConfig;
import com.powers.client.audio.ClientLayeredAudioAudit;
import com.powers.client.audio.ClientLayeredAudioMixer;
import com.powers.network.LayeredAudioPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Development-only driver for auditable production-mixer acceptance scripts. */
public final class LayeredAudioAcceptance {
	private static long sequenceTick = Long.MIN_VALUE;
	private static int sequence;

	private LayeredAudioAcceptance() {
	}

	public static void emit(Minecraft client, String argument) {
		if (client.player == null || client.level == null) {
			throw new IllegalStateException("Audio acceptance requires a joined world");
		}
		String[] values = argument.split(" ");
		LayeredAudioCue cue = LayeredAudioCue.forSemanticName(values[0]).orElseThrow();
		double distance = Double.parseDouble(values[1]);
		boolean expectedObstructed = values[2].equals("wall");
		Vec3 camera = client.gameRenderer.mainCamera().position();
		Vec3 look = client.player.getLookAngle().normalize();
		Vec3 origin = camera.add(look.scale(distance));
		long gameTime = client.level.getGameTime();
		ClientLayeredAudioMixer.handle(new LayeredAudioPackets.Payload(nextEventId(gameTime), cue,
				client.level.dimension().identifier(), origin.x, origin.y, origin.z,
				1.0F, 1.0F, gameTime));
		ClientLayeredAudioAudit.Row row = ClientLayeredAudioAudit.last();
		if (row == null || row.obstructed() != expectedObstructed) {
			throw new AssertionError("Audio fixture obstruction did not match " + values[2]);
		}
	}

	public static void comfort(String argument) {
		ClientAudioComfortConfig.setAcceptanceOverride(argument.equals("reduced"));
	}

	public static void assertLast(String argument) {
		String[] values = argument.split(" ");
		ClientLayeredAudioAudit.Row row = ClientLayeredAudioAudit.last();
		if (row == null || !row.layer().serializedName().equals(values[0])
				|| !row.result().equals(values[1])) {
			throw new AssertionError("Layered audio assertion failed: expected " + argument
					+ " but was " + (row == null ? "none" : row.layer().serializedName()
					+ " " + row.result()));
		}
	}

	private static synchronized long nextEventId(long gameTime) {
		if (sequenceTick != gameTime) {
			sequenceTick = gameTime;
			sequence = 0;
		} else {
			sequence++;
		}
		return (gameTime << 16) | sequence;
	}
}
