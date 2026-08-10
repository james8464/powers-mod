package com.powers.companion;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoreDialogueEngineTest {
	@Test
	void speaksFromFictionalContextWithoutImmediateRepetition() {
		LoreDialogueEngine engine = new LoreDialogueEngine();
		UUID owner = UUID.randomUUID();
		LoreDialogueContext context = new LoreDialogueContext(
				"dark_realm", true, true, 10, "darkness", "event_horizon", true, true, "first_gate");
		var lines = new HashSet<String>();
		for (int i = 0; i < 8; i++) lines.add(engine.line(owner, context, false));
		assertEquals(8, lines.size());
		assertFalse(lines.stream().anyMatch(line -> line.length() > 256));
	}

	@Test
	void bossVoiceUsesASeparateOriginalRegister() {
		LoreDialogueEngine engine = new LoreDialogueEngine();
		LoreDialogueContext context = LoreDialogueContext.calm("overworld", 4);
		String companion = engine.line(UUID.randomUUID(), context, false);
		String boss = engine.line(UUID.randomUUID(), context, true);
		assertFalse(companion.equals(boss));
	}
}
