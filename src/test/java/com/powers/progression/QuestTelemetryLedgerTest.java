package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTelemetryLedgerTest {
	@Test
	void completionsAreAnonymousBoundedAndRoundTripExactly() {
		QuestTelemetryLedger ledger = new QuestTelemetryLedger(3);
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		ledger.noteActivity(first, QuestTelemetryLedger.Alignment.LIGHT, 100L);
		ledger.noteActivity(second, QuestTelemetryLedger.Alignment.LIGHT, 120L);
		ledger.complete(first, QuestTelemetryLedger.Alignment.LIGHT, 1, "mastery", 500L);
		ledger.complete(second, QuestTelemetryLedger.Alignment.LIGHT, 1, "pilgrimage", 820L);
		ledger.complete(first, QuestTelemetryLedger.Alignment.LIGHT, 2, "mastery", 1_000L);
		ledger.complete(first, QuestTelemetryLedger.Alignment.DARK, 1, "atrocity", 1_100L);

		assertEquals(3, ledger.samples().size());
		assertTrue(ledger.encodedSamples().stream().noneMatch(row -> row.contains(first.toString())));
		QuestTelemetryLedger decoded = QuestTelemetryLedger.decode(
				3, ledger.encodedStarts(), ledger.encodedSamples());
		assertEquals(ledger.encodedStarts(), decoded.encodedStarts());
		assertEquals(ledger.encodedSamples(), decoded.encodedSamples());
	}

	@Test
	void summariesPublishMedianP90AndSufficiencyWithoutPlayerIdentity() {
		QuestTelemetryLedger ledger = new QuestTelemetryLedger(64);
		for (int index = 1; index <= 20; index++) {
			UUID player = new UUID(0L, index);
			ledger.noteActivity(player, QuestTelemetryLedger.Alignment.DARK, 0L);
			ledger.complete(player, QuestTelemetryLedger.Alignment.DARK, 5,
					index % 2 == 0 ? "dominion" : "atrocity", index * 100L);
		}

		QuestTelemetryLedger.Summary summary = ledger.summary(
				QuestTelemetryLedger.Alignment.DARK, 5);
		assertEquals(20, summary.samples());
		assertEquals(1_050L, summary.medianTicks());
		assertEquals(1_800L, summary.p90Ticks());
		assertTrue(summary.sufficient(20));
		assertFalse(summary.sufficient(21));
		assertEquals(List.of("atrocity", "dominion"), summary.routes());
	}

	@Test
	void malformedRowsAreIgnoredAndClockRegressionCannotCreateNegativeDuration() {
		QuestTelemetryLedger ledger = QuestTelemetryLedger.decode(8,
				List.of("broken", UUID.randomUUID() + ";LIGHT;500"),
				List.of("bad", "LIGHT;1;mastery;-7"));
		UUID player = UUID.randomUUID();
		ledger.noteActivity(player, QuestTelemetryLedger.Alignment.LIGHT, 900L);
		ledger.complete(player, QuestTelemetryLedger.Alignment.LIGHT, 1, "mastery", 100L);
		assertEquals(0L, ledger.samples().getLast().elapsedTicks());
	}

	@Test
	void oneDeedCompletingSeveralLevelsDoesNotManufactureZeroDurationSamples() {
		QuestTelemetryLedger ledger = new QuestTelemetryLedger(8);
		UUID player = UUID.randomUUID();
		ledger.noteActivity(player, QuestTelemetryLedger.Alignment.LIGHT, 100L);
		List<QuestTelemetryLedger.Sample> samples = ledger.completeBatch(player,
				QuestTelemetryLedger.Alignment.LIGHT, List.of(
						new QuestTelemetryLedger.Completion(1, "mastery"),
						new QuestTelemetryLedger.Completion(2, "pilgrimage")), 500L);

		assertEquals(2, samples.size());
		assertEquals(List.of(400L, 400L), samples.stream()
				.map(QuestTelemetryLedger.Sample::elapsedTicks).toList());
	}
}
