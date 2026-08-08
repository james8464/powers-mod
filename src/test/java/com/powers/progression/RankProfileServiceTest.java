package com.powers.progression;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankProfileServiceTest {
	private static final RankProfileService SERVICE = new RankProfileService();

	@BeforeAll
	static void loadGraphs() {
		RankGraphRegistry.initialize();
	}

	@Test
	void everyNodeHasAtLeastOneMechanicalPerk() {
		Stream.of(RankGraphRegistry.light(), RankGraphRegistry.darkness())
				.flatMap(graph -> graph.nodes().stream())
				.forEach(node -> assertFalse(node.perks().isEmpty(), node.id()));
	}

	@Test
	void focusStrengthensOneNodeWithoutRemovingOtherUnlockedPaths() {
		RankGraph graph = new RankGraph(List.of(
				node("root", "origin", RankPerkType.ENERGY_CAPACITY, 0.02),
				node("might", "might", RankPerkType.POWER_DAMAGE, 0.10),
				node("motion", "motion", RankPerkType.MOVEMENT, 0.10)));
		RankProfile focused = SERVICE.profile(graph,
				new RankProgress(Set.of("root", "might", "motion"), "might"));
		RankProfile unfocused = SERVICE.profile(graph,
				new RankProgress(Set.of("root", "might", "motion"), "motion"));

		assertEquals(0.15, focused.value(RankPerkType.POWER_DAMAGE), 0.0001);
		assertEquals(0.10, focused.value(RankPerkType.MOVEMENT), 0.0001);
		assertTrue(focused.value(RankPerkType.POWER_DAMAGE)
				> unfocused.value(RankPerkType.POWER_DAMAGE));
		assertTrue(focused.branchWeight("motion") > 0);
	}

	@Test
	void aggregationAppliesExplicitSafetyCapsAfterFocus() {
		RankGraph graph = new RankGraph(List.of(
				node("root", "origin", RankPerkType.POWER_DAMAGE, 0.30),
				node("one", "might", RankPerkType.POWER_DAMAGE, 0.30),
				node("two", "might", RankPerkType.COOLDOWN_REDUCTION, 0.40)));
		RankProfile profile = SERVICE.profile(graph,
				new RankProgress(Set.of("root", "one", "two"), "one"));

		assertEquals(0.40, profile.value(RankPerkType.POWER_DAMAGE), 0.0001);
		assertEquals(0.25, profile.value(RankPerkType.COOLDOWN_REDUCTION), 0.0001);
	}

	private static RankNode node(String id, String branch, RankPerkType type, double amount) {
		return new RankNode(id, 0, branch, id, List.of(), false,
				List.of(new RankPerk(type, amount, "")));
	}
}
