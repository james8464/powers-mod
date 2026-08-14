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
	void qualifiedMenuAliasesPreserveEveryCanonicalNamespace() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		assertTrue(catalogue.reloadAliases(Map.of(
				"innate/old_fire", "innate/fireball",
				"crystal/old_inferno", "crystal/inferno",
				"unique/old_call", "unique/call_hollowed",
				"dominion/old_host", "dominion/host_heaven")));

		assertEquals("innate/fireball", catalogue.snapshot().resolveKey("innate/old_fire"));
		assertEquals("crystal/inferno", catalogue.snapshot().resolveKey("crystal/old_inferno"));
		assertEquals("unique/call_hollowed", catalogue.snapshot().resolveKey("unique/old_call"));
		assertEquals("dominion/host_heaven", catalogue.snapshot().resolveKey("dominion/old_host"));
		assertEquals(new MagicActionId("fireball"), catalogue.snapshot().resolve("fireball"));
	}

	@Test
	void qualifiedCanonicalKeysRemainCollisionAndCycleSafe() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		ActionRegistrySnapshot before = catalogue.snapshot();

		assertFalse(catalogue.reloadAliases(Map.of("innate/fireball", "innate/lightning_strike")));
		assertFalse(catalogue.reloadAliases(Map.of(
				"unique/old_a", "unique/old_b", "unique/old_b", "unique/old_a")));
		assertSame(before, catalogue.snapshot());
	}

	@Test
	void submissionMustMatchRevisionAndCanonicalKey() {
		ActionRegistrySnapshot snapshot = MagicActionCatalogue.defaults().snapshot();

		assertEquals(ActionSubmissionValidation.ACCEPT,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision(), "fireball"));
		assertEquals(ActionSubmissionValidation.ACCEPT,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision(), "innate/fireball"));
		assertEquals(ActionSubmissionValidation.REFRESH,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision() - 1L, "fireball"));
		assertEquals(ActionSubmissionValidation.REFRESH,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision() + 1L, "fireball"));
		assertEquals(ActionSubmissionValidation.REFRESH,
				ActionSubmissionValidation.validate(snapshot, snapshot.revision(), "missing"));
	}
}
