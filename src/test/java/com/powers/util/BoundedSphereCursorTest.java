package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

	@Test
	void snapshotResumesAtTheExactNextCubePosition() {
		BoundedSphereCursor first = new BoundedSphereCursor(2);
		Set<BoundedSphereCursor.Offset> before = new HashSet<>(first.take(19));
		BoundedSphereCursor resumed = new BoundedSphereCursor(first.snapshot());
		Set<BoundedSphereCursor.Offset> after = new HashSet<>();
		while (!resumed.finished()) after.addAll(resumed.take(7));

		assertTrue(java.util.Collections.disjoint(before, after));
		assertFalse(after.isEmpty());
		before.addAll(after);
		assertEquals(33, before.size());
	}
}
