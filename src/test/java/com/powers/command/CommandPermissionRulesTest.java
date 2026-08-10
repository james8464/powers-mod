package com.powers.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandPermissionRulesTest {
	@Test
	void configuredOperatorLevelsMapToTheFiveVanillaTiers() {
		assertEquals(0, CommandPermissionRules.tier(-20));
		assertEquals(0, CommandPermissionRules.tier(0));
		assertEquals(1, CommandPermissionRules.tier(1));
		assertEquals(2, CommandPermissionRules.tier(2));
		assertEquals(3, CommandPermissionRules.tier(3));
		assertEquals(4, CommandPermissionRules.tier(99));
	}
}
