package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowAgendaTest {
	private static final String FACT = "A ward blocks the cast before energy is spent.";

	@Test
	void threePhasesChangePersuasionWithoutChangingTheVerifiedFact() {
		ShadowConversationMemory whisper = ShadowConversationMemory.empty().withInfluence(0);
		ShadowConversationMemory invitation = whisper.withInfluence(34);
		ShadowConversationMemory covenant = whisper.withInfluence(67);

		String early = ShadowAgendaVoice.compose(FACT, whisper);
		String middle = ShadowAgendaVoice.compose(FACT, invitation);
		String late = ShadowAgendaVoice.compose(FACT, covenant);

		assertEquals(ShadowAgendaPhase.WHISPER, whisper.agendaPhase());
		assertEquals(ShadowAgendaPhase.INVITATION, invitation.agendaPhase());
		assertEquals(ShadowAgendaPhase.COVENANT, covenant.agendaPhase());
		assertTrue(early.startsWith(FACT));
		assertTrue(middle.startsWith(FACT));
		assertTrue(late.startsWith(FACT));
		assertNotEquals(early, middle);
		assertNotEquals(middle, late);
		assertTrue(middle.contains("Verify it yourself"));
		assertTrue(late.contains("safety still permit"));
	}

	@Test
	void agendaProgressIsBoundedAndPersistsIndependentlyOfTheRuntimeBody() {
		ShadowConversationMemory memory = ShadowConversationMemory.empty();
		for (int turn = 0; turn < 40; turn++) memory = memory.advanceAgenda("owner question");
		assertEquals(100, memory.influence());
		assertEquals(ShadowAgendaPhase.COVENANT, memory.agendaPhase());
		assertEquals(memory, memory.afterBodyDeath());

		ShadowCompanionData stored = ShadowCompanionData.defaults().withMemory(memory)
				.withStance(ShadowStance.GUARD).withoutBody();
		assertEquals(100, stored.memory().influence());
		assertEquals(ShadowAgendaPhase.COVENANT, stored.memory().agendaPhase());
		assertEquals(100, memory.advanceAgenda("").influence());
	}
}
