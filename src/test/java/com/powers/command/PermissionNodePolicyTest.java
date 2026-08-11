package com.powers.command;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionNodePolicyTest {
	@Test
	void absentProviderFallsBackToVanillaOperatorLevel() {
		assertTrue(PermissionNodePolicy.allowed(true, Optional.empty()));
		assertFalse(PermissionNodePolicy.allowed(false, Optional.empty()));
	}

	@Test
	void installedProviderCanSeparateEverySensitiveControl() {
		assertTrue(PermissionNodePolicy.allowed(false, Optional.of(true)));
		assertFalse(PermissionNodePolicy.allowed(true, Optional.of(false)));
		assertTrue(java.util.Arrays.stream(PermissionNode.values()).map(PermissionNode::id)
				.distinct().count() == 6L);
	}

	@Test
	void commandControlsMapToSixDistinctDocumentedNodes() {
		assertEquals(PermissionNode.DIAGNOSE, PermissionCommandRoutes.forControl("diagnose"));
		assertEquals(PermissionNode.TESTING, PermissionCommandRoutes.forControl("testing"));
		assertEquals(PermissionNode.TRAVEL, PermissionCommandRoutes.forControl("travel"));
		assertEquals(PermissionNode.ASSIGN, PermissionCommandRoutes.forControl("assign"));
		assertEquals(PermissionNode.RECOVER, PermissionCommandRoutes.forControl("recover"));
		assertEquals(PermissionNode.BOSS, PermissionCommandRoutes.forControl("boss"));
	}

	@Test
	void supportedFabricPermissionApiAdapterIsDiscovered() {
		assertTrue(PermissionNodes.installFabricAdapterIfPresent());
		PermissionNodes.clear();
	}
}
