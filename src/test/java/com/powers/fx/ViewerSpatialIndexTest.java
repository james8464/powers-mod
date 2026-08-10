package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewerSpatialIndexTest {
	@Test
	void returnsOnlyViewersInsideTheExactHorizontalRange() {
		ViewerSpatialIndex<String> index = new ViewerSpatialIndex<>(16);
		index.put("near", 8.0, 8.0);
		index.put("edge", 128.0, 0.0);
		index.put("far", 129.0, 0.0);

		assertEquals(Set.of("near", "edge"), Set.copyOf(index.nearby(0.0, 0.0, 128.0)));
	}

	@Test
	void clearDropsEveryCachedViewer() {
		ViewerSpatialIndex<Integer> index = new ViewerSpatialIndex<>(16);
		index.put(1, -17.0, -17.0);
		index.clear();
		assertEquals(0, index.size());
		assertEquals(0, index.nearby(-17.0, -17.0, 1.0).size());
	}
}
