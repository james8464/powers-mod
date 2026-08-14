package com.powers.testing;

import com.powers.progression.QuestTelemetryLedger;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTelemetryCampaignPlanTest {
	@Test
	void suppliesTenDistinctHumanCadenceSessionsForEachAlignment() {
		for (QuestTelemetryLedger.Alignment alignment : QuestTelemetryLedger.Alignment.values()) {
			var profiles = QuestTelemetryCampaignPlan.profiles(alignment);
			assertEquals(10, profiles.size());
			assertEquals(10, new HashSet<>(profiles).size());
			for (var profile : profiles) {
				assertTrue(profile.maximumCompletionTick(alignment) >= 6L * 60L * 60L * 20L);
				assertTrue(profile.maximumCompletionTick(alignment) <= 12L * 60L * 60L * 20L);
			}
		}
	}

	@Test
	void everyThresholdHasANondecreasingExpectedCompletionTime() {
		for (QuestTelemetryLedger.Alignment alignment : QuestTelemetryLedger.Alignment.values()) {
			for (var profile : QuestTelemetryCampaignPlan.profiles(alignment)) {
				long previous = 0L;
				for (int level = 1; level <= 10; level++) {
					long completion = profile.expectedCompletionTick(alignment, level);
					assertTrue(completion >= previous,
							alignment + " level " + level + " regressed below its prerequisite");
					previous = completion;
				}
			}
		}
	}

	@Test
	void cadenceEventsNeverFireAtSessionStartOrOffInterval() {
		var profile = QuestTelemetryCampaignPlan.profiles(
				QuestTelemetryLedger.Alignment.LIGHT).getFirst();
		int interval = profile.interval("power_use");
		assertFalse(profile.due("power_use", 0L));
		assertFalse(profile.due("power_use", interval - 1L));
		assertTrue(profile.due("power_use", interval));
	}
}
