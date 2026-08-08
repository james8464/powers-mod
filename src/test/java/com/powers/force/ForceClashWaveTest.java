package com.powers.force;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceClashWaveTest {
	@Test
	void cursorVisitsEverySphereCoordinateOnceWithinBatchLimits() {
		ForceClashWave.Cursor cursor = new ForceClashWave.Cursor(2);
		List<ForceClashWave.Offset> visited = new ArrayList<>();

		while (!cursor.finished()) {
			List<ForceClashWave.Offset> batch = cursor.take(5);
			assertTrue(batch.size() <= 5);
			assertTrue(batch.stream().allMatch(offset -> offset.distanceSquared() <= 4));
			visited.addAll(batch);
		}

		assertEquals(33, visited.size());
		assertEquals(visited.size(), new HashSet<>(visited).size());
	}

	@Test
	void cursorTreatsTheRadiusBoundaryAsPartOfTheWave() {
		ForceClashWave.Cursor cursor = new ForceClashWave.Cursor(1);
		List<ForceClashWave.Offset> offsets = cursor.take(20);

		assertEquals(7, offsets.size());
		assertTrue(offsets.contains(new ForceClashWave.Offset(1, 0, 0)));
		assertTrue(offsets.contains(new ForceClashWave.Offset(-1, 0, 0)));
		assertTrue(cursor.finished());
		assertTrue(cursor.take(0).isEmpty());
		assertFalse(offsets.contains(new ForceClashWave.Offset(1, 1, 0)));
	}

	@Test
	void cursorExpandsFromTheEpicentreWithoutReturningToInnerShells() {
		ForceClashWave.Cursor cursor = new ForceClashWave.Cursor(3);
		List<ForceClashWave.Offset> visited = new ArrayList<>(cursor.take(1));
		assertEquals(0, cursor.frontierRadius());
		while (!cursor.finished()) visited.addAll(cursor.take(4));

		assertEquals(new ForceClashWave.Offset(0, 0, 0), visited.getFirst());
		assertEquals(123, visited.size());
		assertEquals(visited.size(), new HashSet<>(visited).size());
		assertEquals(3, cursor.frontierRadius());
		int previousShell = -1;
		for (ForceClashWave.Offset offset : visited) {
			int shell = (int) Math.ceil(Math.sqrt(offset.distanceSquared()));
			assertTrue(shell >= previousShell);
			previousShell = shell;
		}
	}
}
