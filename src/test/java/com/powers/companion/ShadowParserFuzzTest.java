package com.powers.companion;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowParserFuzzTest {
	private static final ShadowNameResolver NAMES = ShadowNameResolver.from(
			Map.of("lightning", "powers:lightning_strike", "void beam", "powers:void_beam"),
			Map.of("torch", "minecraft:torch"));

	@Test
	void randomUnicodeAndControlInputNeverEscapesOutputBounds() {
		Random random = new Random(0x534841444F57L);
		for (int sample = 0; sample < 10_000; sample++) {
			int codePoints = random.nextInt(1_025);
			StringBuilder value = new StringBuilder(codePoints);
			if ((sample & 1) == 0) value.append("shadow,");
			for (int index = 0; index < codePoints; index++) {
				int codePoint;
				do {
					codePoint = random.nextInt(Character.MAX_CODE_POINT + 1);
				} while (!Character.isValidCodePoint(codePoint)
						|| Character.isSurrogate((char) codePoint));
				value.appendCodePoint(codePoint);
			}
			ShadowRequest request = assertDoesNotThrow(() -> ShadowRequestParser.parse(
					value.toString(), ShadowConversationMemory.empty(), NAMES));
			assertTrue(request.original().length() <= ShadowRequest.MAX_MESSAGE_LENGTH);
			assertTrue(request.subject().length() <= 128);
			assertTrue(request.reason().length() <= 512);
			assertTrue(request.count() >= 1 && request.count() <= 64);
		}
	}

	@Test
	void millionCharacterRequestIsRejectedBeforeCommandNormalization() {
		ShadowRequest request = assertDoesNotThrow(() -> ShadowRequestParser.parse(
				"shadow, " + "x".repeat(1_000_000), ShadowConversationMemory.empty(), NAMES));
		assertTrue(request.addressed());
		assertTrue(request.kind() == ShadowRequest.Kind.TOO_LONG);
		assertTrue(request.original().isEmpty());
	}

	@Test
	void confusablePrefixesNeverConsumeOrdinarySignedChat() {
		for (String confusable : new String[] {
				"shadоw, reveal yourself", // Cyrillic o
				"ѕhadow, reveal yourself", // Cyrillic dze
				"shadow\u200B, reveal yourself",
				"shadow； reveal yourself",
				"shadow: reveal yourself"
		}) {
			assertFalse(ShadowRequestParser.parse(confusable,
					ShadowConversationMemory.empty(), NAMES).addressed(), confusable);
		}
	}

	@Test
	void signedChatIsInspectedOnceAndRevealedSpeechIsSystemAuthored() throws IOException {
		Path root = Path.of(System.getProperty("user.dir"));
		String lifecycle = Files.readString(root.resolve(
				"src/main/java/com/powers/PowersServerLifecycle.java"));
		String messaging = Files.readString(root.resolve(
				"src/main/java/com/powers/companion/ShadowCompanionMessaging.java"));
		assertTrue(lifecycle.contains("message.signedContent()"));
		assertTrue(lifecycle.contains("!PrivateCompanionManager.handleChat"));
		assertTrue(messaging.contains("broadcastSystemMessage"));
		assertFalse(messaging.contains("PlayerChatMessage"));
	}

	@Test
	void noParserResultCanAuthorizeWithoutEveryOwnerFact() {
		assertTrue(PrivateCompanionRules.eligible(true, true, true, false, true));
		assertFalse(PrivateCompanionRules.eligible(false, true, true, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, false, true, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, true, false, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, true, true, false, false));
	}
}
