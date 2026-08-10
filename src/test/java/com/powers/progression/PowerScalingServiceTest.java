package com.powers.progression;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerScalingServiceTest {
	private final PowerScalingService service = new PowerScalingService();
	private final MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

	@Test
	void motionFocusImprovesMovementWithoutLeakingIntoHealing() {
		RankGraph graph = graph(
				perkNode("root", "origin", RankPerkType.ENERGY_CAPACITY, 0.01),
				perkNode("motion", "motion", RankPerkType.MOVEMENT, 0.20));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("root", "motion"), "motion"));

		ScaledMagicValues step = service.scale(action("shadow_step"), profile, 2);
		ScaledMagicValues healing = service.scale(action("plant_healing_acceleration"), profile, 2);

		assertTrue(step.rangeMultiplier() > healing.rangeMultiplier());
		assertEquals(1.04, healing.rangeMultiplier(), 0.0001);
	}

	@Test
	void actionAndAspectScopedPerksApplyOnlyToMatchingActions() {
		RankGraph graph = graph(
				scopedNode("fire", RankPerkType.POWER_DAMAGE, 0.20, "flame"),
				scopedNode("beam", RankPerkType.POWER_DAMAGE, 0.10, "void_beam"));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("fire", "beam"), ""));

		assertTrue(service.scale(action("fireball"), profile, 0).potencyMultiplier()
				> service.scale(action("lightning_strike"), profile, 0).potencyMultiplier());
		assertTrue(service.scale(action("void_beam"), profile, 0).potencyMultiplier() > 1.0);
	}

	@Test
	void costsCooldownsAndOutputsRemainFiniteAndCapped() {
		RankGraph graph = graph(perkNode("cap", "dominion", RankPerkType.POWER_DAMAGE, 9.0),
				perkNode("cost", "dominion", RankPerkType.ENERGY_COST_REDUCTION, 9.0),
				perkNode("cooldown", "dominion", RankPerkType.COOLDOWN_REDUCTION, 9.0));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("cap", "cost", "cooldown"), "cap"));
		ScaledMagicValues values = service.scale(action("fireball"), profile, 10);

		assertEquals(1.90, values.potencyMultiplier(), 0.0001);
		assertTrue(values.energyCost() >= 0);
		assertTrue(values.cooldownTicks() >= 0);
		assertTrue(values.energyCost() >= Math.ceil(action("fireball").baseEnergy() * 0.65) - 1);
		assertTrue(values.cooldownTicks() >= Math.ceil(action("fireball").baseCooldownTicks() * 0.60) - 1);
	}

	@Test
	void insightAndAbyssBranchesExposeTheirNamedVariants() {
		RankGraph graph = graph(
				perkNode("oracle", "insight", RankPerkType.REVEAL, 0.10),
				perkNode("hunger", "abyss", RankPerkType.ENERGY_REGEN, 0.10));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("oracle", "hunger"), "oracle"));
		Set<String> variants = service.scale(action("soul_compass"), profile, 3).unlockedVariants();

		assertTrue(variants.contains("true_sight"));
		assertTrue(variants.contains("dark_resurgence"));
	}

	@Test
	void unrankedCrystalAndSpellBaselinesDoNotInheritPlayerProgression() {
		ScaledMagicValues crystal = PowerScalingService.unranked("inferno");
		ScaledMagicValues spell = PowerScalingService.unranked("hex");

		assertEquals(1.0, crystal.potencyMultiplier(), 0.0001);
		assertEquals(1.0, crystal.rangeMultiplier(), 0.0001);
		assertEquals(1.0, crystal.durationMultiplier(), 0.0001);
		assertTrue(crystal.unlockedVariants().isEmpty());
		assertEquals(1.0, spell.potencyMultiplier(), 0.0001);
		assertTrue(spell.unlockedVariants().isEmpty());
	}

	private MagicActionDefinition action(String id) {
		return catalogue.definitions().stream().filter(action -> action.id().value().equals(id)).findFirst().orElseThrow();
	}

	private static RankGraph graph(RankNode... nodes) {
		return new RankGraph(List.of(nodes));
	}

	private static RankNode perkNode(String id, String branch, RankPerkType type, double amount) {
		return scopedNode(id, type, amount, "", branch);
	}

	private static RankNode scopedNode(String id, RankPerkType type, double amount, String scope) {
		return scopedNode(id, type, amount, scope, "insight");
	}

	private static RankNode scopedNode(String id, RankPerkType type, double amount, String scope, String branch) {
		return new RankNode(id, 0, branch, id, List.of(), false,
				List.of(new RankPerk(type, amount, scope)));
	}
}
