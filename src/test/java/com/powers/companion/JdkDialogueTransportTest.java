package com.powers.companion;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkDialogueTransportTest {
	@Test
	void omitsAuthorizationForCredentiallessLocalOllama() {
		var request = JdkDialogueTransport.buildRequest(
				URI.create("http://127.0.0.1:11434/v1/chat/completions"),
				"qwen3.5:9b", "", "fictional prompt", Duration.ofSeconds(1));
		assertTrue(request.headers().firstValue("Authorization").isEmpty());
		assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
	}

	@Test
	void sendsBearerCredentialForHostedEndpoint() {
		var request = JdkDialogueTransport.buildRequest(
				URI.create("https://example.invalid/v1/chat/completions"),
				"lore", "secret", "fictional prompt", Duration.ofSeconds(1));
		assertEquals("Bearer secret", request.headers().firstValue("Authorization").orElseThrow());
	}
}
