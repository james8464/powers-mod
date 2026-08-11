package com.powers.companion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowRequestParserTest {
	private static final ShadowNameResolver NAMES = ShadowNameResolver.from(
			Map.of("lightning", "powers:lightning", "void beam", "powers:void_beam",
					"flight", "powers:flight"),
			Map.of("torch", "minecraft:torch", "dark crystal", "powers:dark_crystal"));

	@Test
	void recognizesEveryTaskFamilyWithoutConsumingOrdinaryChat() {
		ShadowConversationMemory memory = ShadowConversationMemory.empty();
		assertFalse(ShadowRequestParser.parse("hello shadow", memory, NAMES).addressed());
		assertEquals(ShadowRequest.Kind.SUMMON, parse("come to me", memory).kind());
		assertEquals(ShadowRequest.Kind.DISMISS, parse("dismiss", memory).kind());
		assertEquals(ShadowRequest.Kind.REVEAL, parse("reveal yourself", memory).kind());
		assertEquals(ShadowRequest.Kind.HIDE, parse("hide yourself", memory).kind());
		assertEquals(ShadowRequest.Kind.FOLLOW, parse("follow me", memory).kind());
		assertEquals(ShadowRequest.Kind.STAY, parse("stay here", memory).kind());
		assertEquals(ShadowRequest.Kind.GUARD, parse("guard this place", memory).kind());
		assertEquals(ShadowRequest.Kind.STOP, parse("stop", memory).kind());
		assertEquals(ShadowRequest.Kind.ATTACK, parse("attack that zombie", memory).kind());
		assertEquals(ShadowRequest.Kind.DEFEND, parse("defend me", memory).kind());
		assertEquals(ShadowRequest.Kind.USE_POWER, parse("use lightning", memory).kind());
		assertEquals(ShadowRequest.Kind.STOP_POWER, parse("stop flight", memory).kind());
		assertEquals(ShadowRequest.Kind.GET_ITEM, parse("bring me 16 torches", memory).kind());
		assertEquals(ShadowRequest.Kind.CONJURE_ITEM, parse("conjure a torch", memory).kind());
		assertEquals(ShadowRequest.Kind.SCOUT, parse("scout ahead", memory).kind());
		assertEquals(ShadowRequest.Kind.DIAGNOSE, parse("why did it fail?", memory).kind());
		assertEquals(ShadowRequest.Kind.CONVERSE, parse("what do you want?", memory).kind());
	}

	@Test
	void parsesCountsRegistryNamesAndRangePreferences() {
		ShadowConversationMemory memory = ShadowConversationMemory.empty();
		ShadowRequest get = parse("please bring me 16 minecraft:torch", memory);
		assertEquals(16, get.count());
		assertEquals("minecraft:torch", get.subject());
		ShadowRequest power = parse("use void beam", memory);
		assertEquals("powers:void_beam", power.subject());
		ShadowRequest range = parse("fight that boss from farther away", memory);
		assertEquals(ShadowRequest.Kind.RANGE_PREFERENCE, range.kind());
		assertEquals(ShadowRequest.Range.FAR, range.range());
	}

	@Test
	void recentReferentsResolveAndAmbiguityNeverGuesses() {
		ShadowConversationMemory memory = ShadowConversationMemory.empty()
				.rememberReferent(ShadowConversationMemory.ReferentType.ENTITY, "Vessel")
				.rememberReferent(ShadowConversationMemory.ReferentType.POWER, "powers:void_beam");
		assertEquals("Vessel", parse("attack it", memory).subject());
		assertEquals("powers:void_beam", parse("use that power", memory).subject());
		ShadowNameResolver ambiguous = ShadowNameResolver.from(
				Map.of("void beam", "powers:void_beam", "energy beam", "powers:energy_beam"), Map.of());
		ShadowRequest request = ShadowRequestParser.parse("shadow, use beam", memory, ambiguous);
		assertEquals(ShadowRequest.Kind.CLARIFY, request.kind());
		assertTrue(request.reason().contains("powers:void_beam"));
	}

	private static ShadowRequest parse(String message, ShadowConversationMemory memory) {
		return ShadowRequestParser.parse("shadow, " + message, memory, NAMES);
	}
}
