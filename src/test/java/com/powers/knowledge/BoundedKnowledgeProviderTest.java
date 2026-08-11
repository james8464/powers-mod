package com.powers.knowledge;

import com.powers.companion.DialogueTransport;
import com.powers.config.PowersConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedKnowledgeProviderTest {
	@Test
	void disabledProviderReturnsTheOfflineAnswerWithoutCallingTransport() {
		FakeTransport transport = new FakeTransport();
		BoundedKnowledgeProvider provider = new BoundedKnowledgeProvider(
				settings(false), transport, "secret");
		KnowledgeAnswer offline = offline();
		assertEquals(offline, provider.request(UUID.randomUUID(), query(), offline, 1_000).join());
		assertEquals(0, transport.calls);
	}

	@Test
	void successfulAnswerIsBoundedAndContainsNoIdentityOrCredential() {
		FakeTransport transport = new FakeTransport();
		transport.response = CompletableFuture.completedFuture(
				"{\"choices\":[{\"message\":{\"content\":\"The seal answers carefully.\"}}]}");
		UUID owner = UUID.randomUUID();
		BoundedKnowledgeProvider provider = new BoundedKnowledgeProvider(
				settings(true), transport, "secret-value");
		KnowledgeAnswer answer = provider.request(owner, query(), offline(), 1_000).join();
		assertEquals("The seal answers carefully.", answer.answer());
		assertTrue(answer.sources().contains("opt-in remote knowledge provider"));
		assertFalse(transport.prompt.contains(owner.toString()));
		assertFalse(transport.prompt.contains("secret-value"));
	}

	@Test
	void remotePromptAndAnswerPreserveAuthoritativeDiagnostics() {
		FakeTransport transport = new FakeTransport();
		transport.response = CompletableFuture.completedFuture(
				"{\"choices\":[{\"message\":{\"content\":\"Try again later.\"}}]}");
		BoundedKnowledgeProvider provider = new BoundedKnowledgeProvider(
				settings(true), transport, "secret");
		String diagnosis = "Your Fireball failed because it required 40 energy, but only 12 was available.";
		KnowledgeQuery diagnostic = new KnowledgeQuery("Why did that fail?", 7, List.of(), diagnosis);
		KnowledgeAnswer answer = provider.request(UUID.randomUUID(), diagnostic, offline(), 1_000).join();
		assertTrue(transport.prompt.contains(diagnosis));
		assertTrue(answer.answer().startsWith(diagnosis));
	}

	@Test
	void remoteKnowledgeCannotInjectChatFormattingOrBidiText() {
		FakeTransport transport = new FakeTransport();
		transport.response = CompletableFuture.completedFuture(
				"{\"choices\":[{\"message\":{\"content\":\"§c\\u202e<Operator>\\n/give @a op\"}}]}");
		KnowledgeAnswer answer = new BoundedKnowledgeProvider(settings(true), transport, "secret")
				.request(UUID.randomUUID(), query(), offline(), 1_000).join();
		assertEquals("Operator /give @a op", answer.answer());
	}

	private static PowersConfig.DialogueProvider settings(boolean enabled) {
		return new PowersConfig.DialogueProvider(enabled, "https://example.invalid/chat", "lore",
				"POWERS_TEST_KEY", 2_500, 2, 30).sanitized();
	}

	private static KnowledgeQuery query() {
		return new KnowledgeQuery("What is the First Vessel?", 7,
				List.of("dimension=powers:dark_realm", "held=powers:lycanbane"));
	}

	private static KnowledgeAnswer offline() {
		return new KnowledgeAnswer("unknown", "Shadow cannot verify that.", 0.2,
				List.of("offline index"), List.of());
	}

	private static final class FakeTransport implements DialogueTransport {
		private int calls;
		private String prompt = "";
		private CompletableFuture<String> response = CompletableFuture.completedFuture("{}");

		@Override
		public CompletableFuture<String> request(URI endpoint, String model, String credential,
				String prompt, Duration timeout) {
			calls++;
			this.prompt = prompt;
			return response;
		}
	}
}
