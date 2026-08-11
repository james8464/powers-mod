package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowConversationMemoryTest {
	@Test
	void historyReferentsAndInfluenceAreHardBounded() {
		ShadowConversationMemory memory = ShadowConversationMemory.empty();
		for (int index = 0; index < 40; index++) {
			memory = memory.remember("owner token=" + index, "answer " + index)
					.withInfluence(index * 9).withRelationship(-index * 9);
		}
		assertEquals(24, memory.turns().size());
		assertEquals(100, memory.influence());
		assertEquals(-100, memory.relationship());
		assertEquals(24, memory.redactedSummary().lines().count());
	}

	@Test
	void redactionAndDeathSafeSnapshotNeverRetainSecretsOrRuntimeEntities() {
		ShadowConversationMemory memory = ShadowConversationMemory.empty()
				.remember("server 192.168.1.2 token=secret password=hunter2", "I heard you")
				.rememberReferent(ShadowConversationMemory.ReferentType.ITEM, "minecraft:torch")
				.rememberFailure("powers:lightning:no_target");
		String summary = memory.redactedSummary();
		assertEquals(false, summary.contains("192.168"));
		assertEquals(false, summary.contains("secret"));
		assertEquals("minecraft:torch", memory.recent(ShadowConversationMemory.ReferentType.ITEM));
		assertEquals("powers:lightning:no_target", memory.recentFailure());
		assertEquals(memory, memory.afterBodyDeath());
	}
}
