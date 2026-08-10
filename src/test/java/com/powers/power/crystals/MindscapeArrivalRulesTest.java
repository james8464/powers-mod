package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindscapeArrivalRulesTest {
	@Test
	void searchBeginsAtTheLandmarkAndStaysInsideTheLoadedTicketFootprint() {
		var offsets = MindscapeArrivalRules.horizontalOffsets();

		assertEquals(new MindscapeArrivalRules.Offset(0, 0), offsets.getFirst());
		assertTrue(offsets.stream().allMatch(offset -> Math.abs(offset.x()) <= 8
				&& Math.abs(offset.z()) <= 8));
		assertEquals(offsets.size(), offsets.stream().distinct().count());
	}
}
