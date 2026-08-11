package com.powers.client;

import com.powers.network.ProtocolHandshakePackets;
import com.powers.network.ProtocolHandshakeRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;

/** Answers the mandatory server challenge while the connection is configuring. */
final class ClientProtocolHandshake {
	private ClientProtocolHandshake() {
	}

	static void initialize() {
		ClientConfigurationNetworking.registerGlobalReceiver(
				ProtocolHandshakePackets.Challenge.TYPE,
				(payload, context) -> context.responseSender().sendPacket(
						new ProtocolHandshakePackets.Response(
								ProtocolHandshakeRules.CURRENT_PROTOCOL,
								ProtocolHandshakePackets.modVersion())));
	}
}
