package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedSphereCursorTest {
	@Test
	void batchesVisitEveryIntegerPositionInTheSphereExactlyOnce() {
		BoundedSphereCursor cursor = new BoundedSphereCursor(1);
		Set<BoundedSphereCursor.Offset> offsets = new HashSet<>();
		while (!cursor.finished()) {
			offsets.addAll(cursor.take(2));
		}

		assertEquals(7, offsets.size());
		assertTrue(offsets.contains(new BoundedSphereCursor.Offset(0, 0, 0)));
	}
}
