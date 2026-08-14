package com.powers.magic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionRegistrySnapshotTest {
	@Test
	void successfulReloadPublishesOneNewImmutableRevision() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		ActionRegistrySnapshot captured = catalogue.snapshot();

		assertTrue(catalogue.reloadAliases(Map.of("old_fire", "fireball")));

		assertEquals(0L, captured.revision());
		assertEquals(new MagicActionId("fireball"), captured.resolve("fireball"));
		assertEquals(1L, catalogue.snapshot().revision());
		assertEquals(new MagicActionId("fireball"), catalogue.snapshot().resolve("old_fire"));
		assertFalse(captured.aliases().containsKey("old_fire"));
	}

	@Test
	void invalidReloadLeavesPriorSnapshotAndRevisionUntouched() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		assertTrue(catalogue.reloadAliases(Map.of("old_fire", "fireball")));
		ActionRegistrySnapshot accepted = catalogue.snapshot();

		assertFalse(catalogue.reloadAliases(Map.of("cycle_a", "cycle_b", "cycle_b", "cycle_a")));

		assertSame(accepted, catalogue.snapshot());
		assertEquals(1L, catalogue.snapshot().revision());
		assertEquals(new MagicActionId("fireball"), catalogue.snapshot().resolve("old_fire"));
	}

	@Test
	void aliasesAreCollisionSafeAndMustResolveToCanonicalDefinitions() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		ActionRegistrySnapshot before = catalogue.snapshot();

		assertFalse(catalogue.reloadAliases(Map.of("fireball", "lightning_strike")));
		assertFalse(catalogue.reloadAliases(Map.of("retired", "missing")));
		assertSame(before, catalogue.snapshot());
	}

	@Test
	void submissionMustMatchRevisionAndCanonicalKey() {
		ActionRegistrySnapshot snapshot = MagicActionCatalogue.defaults().snapshot();

		assertEquals(ActionSubmissionValidation.ACCEPT,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision(), "fireball"));
		assertEquals(ActionSubmissionValidation.REFRESH,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision() - 1L, "fireball"));
		assertEquals(ActionSubmissionValidation.REFRESH,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision() + 1L, "fireball"));
		assertEquals(ActionSubmissionValidation.REFRESH,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision(), "missing"));
	}
}
