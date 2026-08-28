package com.powers.gametest;

import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import com.powers.audio.LayeredAudioService;
import com.powers.client.audio.ClientLayeredAudioAudit;
import com.powers.client.audio.ClientLayeredAudioMixer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.List;

/** Production server-to-client proof for exact layered-audio bands and lifecycle reset. */
public final class LayeredAudioClientAcceptance {
	private LayeredAudioClientAcceptance() {
	}

	public static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		context.runOnClient(client -> ClientLayeredAudioMixer.resetConnectionEpoch());
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			var eye = player.getEyePosition();
			for (double distance : List.of(20.0, 96.0, 256.0)) {
				LayeredAudioService.emit(player.level(), eye.add(distance, 0, 0),
						LayeredAudioCue.LIGHT_CHORUS, 1.0F, 1.0F);
			}
		});
		context.waitFor(client -> ClientLayeredAudioAudit.rows().size() == 3);
		context.runOnClient(client -> {
			var rows = ClientLayeredAudioAudit.rows();
			if (!rows.stream().map(ClientLayeredAudioAudit.Row::layer).toList()
					.equals(List.of(LayeredAudioLayer.NEAR, LayeredAudioLayer.MID,
							LayeredAudioLayer.FAR))
					|| rows.stream().anyMatch(row -> !row.result().equals("admitted"))) {
				throw new AssertionError("Exact live layered-audio bands were not admitted: " + rows);
			}
			ClientLayeredAudioMixer.resetConnectionEpoch();
			if (!ClientLayeredAudioAudit.rows().isEmpty()) {
				throw new AssertionError("Audio audit survived connection lifecycle reset");
			}
		});
	}
}
