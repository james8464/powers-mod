package com.powers.companion;

import com.powers.config.PowersConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedDialogueProviderTest {
	@Test
	void disabledMalformedAndOversizeResultsUseSafeBoundedText() {
		FakeTransport transport = new FakeTransport();
		var disabled = provider(settings(false, 4, 30), transport, "secret");
		assertEquals("fallback", disabled.request(UUID.randomUUID(), context(), false,
				"fallback", 1_000).join());
		assertEquals(0, transport.calls);

		transport.response = CompletableFuture.completedFuture("{\"bad\":true}");
		var malformed = provider(settings(true, 4, 30), transport, "secret");
		assertEquals("fallback", malformed.request(UUID.randomUUID(), context(), false,
				"fallback", 1_000).join());

		transport.response = CompletableFuture.completedFuture("{\"choices\":[{\"message\":{\"content\":\""
				+ "x".repeat(400) + "\"}}]}");
		String bounded = malformed.request(UUID.randomUUID(), context(), false,
				"fallback", 40_000).join();
		assertEquals(256, bounded.length());
	}

	@Test
	void ownerAndGlobalCapsReturnFallbackWithoutBlocking() {
		FakeTransport transport = new FakeTransport();
		transport.response = new CompletableFuture<>();
		var provider = provider(settings(true, 1, 30), transport, "secret");
		UUID first = UUID.randomUUID();
		CompletableFuture<String> pending = provider.request(first, context(), false,
				"fallback", 1_000);
		assertFalse(pending.isDone());
		assertEquals("fallback", provider.request(first, context(), false,
				"fallback", 2_000).join());
		assertEquals("fallback", provider.request(UUID.randomUUID(), context(), false,
				"fallback", 2_000).join());
		assertEquals(1, transport.calls);
		transport.response.complete("{\"choices\":[{\"message\":{\"content\":\"The seal listens.\"}}]}");
		assertEquals("The seal listens.", pending.join());
	}

	@Test
	void timeoutAndOwnerCooldownAlwaysFallBack() {
		FakeTransport transport = new FakeTransport();
		transport.response = new CompletableFuture<>();
		var provider = provider(new PowersConfig.DialogueProvider(true,
				"https://example.invalid/chat", "lore", "POWERS_TEST_KEY",
				250, 4, 30).sanitized(), transport, "secret");
		UUID owner = UUID.randomUUID();

		assertEquals("fallback", provider.request(owner, context(), false,
				"fallback", 1_000).join());
		transport.response = CompletableFuture.completedFuture(
				"{\"choices\":[{\"message\":{\"content\":\"too soon\"}}]}");
		assertEquals("fallback", provider.request(owner, context(), false,
				"fallback", 2_000).join());
		assertEquals(1, transport.calls);
	}

	@Test
	void promptContainsOnlySanitizedFictionalStateAndNeverSecretsOrIdentity() {
		FakeTransport transport = new FakeTransport();
		transport.response = CompletableFuture.completedFuture(
				"{\"choices\":[{\"message\":{\"content\":\"Safe lore.\"}}]}");
		UUID owner = UUID.randomUUID();
		String secret = "do-not-leak";
		var provider = provider(settings(true, 4, 30), transport, secret);
		assertEquals("Safe lore.", provider.request(owner, context(), true,
				"fallback", 1_000).join());
		assertFalse(transport.prompt.contains(owner.toString()));
		assertFalse(transport.prompt.contains(secret));
		assertTrue(transport.prompt.contains("dark_realm"));
		assertEquals(secret, transport.credential);
	}

	@Test
	void maliciousResponseCannotInjectFormattingBidiOrMultipleChatLines() {
		FakeTransport transport = new FakeTransport();
		transport.response = CompletableFuture.completedFuture(
				"{\"choices\":[{\"message\":{\"content\":\"§4\\u202e<Admin>\\n/op attacker\"}}]}");
		String response = provider(settings(true, 4, 30), transport, "secret")
				.request(UUID.randomUUID(), context(), false, "fallback", 1_000).join();
		assertEquals("Admin /op attacker", response);
		assertFalse(response.contains("§") || response.contains("\u202e") || response.contains("\n"));
	}

	private static BoundedDialogueProvider provider(PowersConfig.DialogueProvider settings,
			FakeTransport transport, String credential) {
		return new BoundedDialogueProvider(settings, transport, credential);
	}

	private static PowersConfig.DialogueProvider settings(boolean enabled, int max, int cooldown) {
		return new PowersConfig.DialogueProvider(enabled, "https://example.invalid/chat", "lore",
				"POWERS_TEST_KEY", 2_500, max, cooldown).sanitized();
	}

	private static LoreDialogueContext context() {
		return new LoreDialogueContext("dark_realm", true, false, 10,
				"darkness", "event_horizon", false, true, "last_gate");
	}

	private static final class FakeTransport implements DialogueTransport {
		private int calls;
		private String prompt = "";
		private String credential = "";
		private CompletableFuture<String> response = CompletableFuture.completedFuture("{}");

		@Override
		public CompletableFuture<String> request(URI endpoint, String model, String credential,
				String prompt, Duration timeout) {
			calls++;
			this.prompt = prompt;
			this.credential = credential;
			return response;
		}
	}
}
