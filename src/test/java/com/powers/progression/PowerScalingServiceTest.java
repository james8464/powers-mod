package com.powers.progression;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.CastAdjustment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerScalingServiceTest {
	private final PowerScalingService service = new PowerScalingService();
	private final MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

	@Test
	void branchSelectionChangesVariantsWithoutReintroducingGenericPercentageScaling() {
		RankGraph graph = graph(
				perkNode("root", "origin", RankPerkType.ENERGY_CAPACITY, 0.01),
				perkNode("motion", "motion", RankPerkType.MOVEMENT, 0.20));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("root", "motion"), "motion"));

		ScaledMagicValues step = service.scale(action("super_speed"), profile, 2);
		ScaledMagicValues healing = service.scale(action("plant_healing_acceleration"), profile, 2);

		assertEquals(InnatePowerLevels.forPower("super_speed", 2).rangeMultiplier(),
				step.rangeMultiplier(), 0.0001);
		assertEquals(InnatePowerLevels.forPower("plant_healing_acceleration", 2).rangeMultiplier(),
				healing.rangeMultiplier(), 0.0001);
	}

	@Test
	void oldScopedPercentagePerksCannotOverrideAuthoredPowerIdentity() {
		RankGraph graph = graph(
				scopedNode("fire", RankPerkType.POWER_DAMAGE, 0.20, "flame"),
				scopedNode("beam", RankPerkType.POWER_DAMAGE, 0.10, "void_beam"));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("fire", "beam"), ""));

		assertEquals(1.0, service.scale(action("fireball"), profile, 0).potencyMultiplier(), 0.0001);
		assertEquals(1.0, service.scale(action("lightning_strike"), profile, 0).potencyMultiplier(), 0.0001);
		assertEquals(1.0, service.scale(action("void_beam"), profile, 0).potencyMultiplier(), 0.0001);
	}

	@Test
	void costsCooldownsAndOutputsRemainFiniteAndCapped() {
		RankGraph graph = graph(perkNode("cap", "dominion", RankPerkType.POWER_DAMAGE, 9.0),
				perkNode("cost", "dominion", RankPerkType.ENERGY_COST_REDUCTION, 9.0),
				perkNode("cooldown", "dominion", RankPerkType.COOLDOWN_REDUCTION, 9.0));
		RankProfile profile = new RankProfileService().profile(graph,
				new RankProgress(Set.of("cap", "cost", "cooldown"), "cap"));
		ScaledMagicValues values = service.scale(action("fireball"), profile, 10);

		assertEquals(InnatePowerLevels.forPower("fireball", 10).damageMultiplier(),
				values.potencyMultiplier(), 0.0001);
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

	@Test
	void invocationSourcePreventsEquipmentRoutesFromScalingAnInnateDefinition() {
		RankGraph graph = graph(perkNode(
				"might", "might", RankPerkType.POWER_DAMAGE, 0.20));
		RankProfile profile = new RankProfileService().profile(
				graph, new RankProgress(Set.of("might"), "might"));
		MagicActionDefinition fireball = action("fireball");

		assertTrue(service.scaleForSource(
				fireball, profile, 10, CastSource.INNATE).potencyMultiplier() > 1.0);
		for (CastSource source : List.of(
				CastSource.ARTIFACT, CastSource.CRYSTAL, CastSource.SPELL)) {
			ScaledMagicValues equipment = service.scaleForSource(fireball, profile, 10, source);
			assertEquals(1.0, equipment.potencyMultiplier(), 0.0001, source.name());
			assertEquals(1.0, equipment.rangeMultiplier(), 0.0001, source.name());
			assertTrue(equipment.unlockedVariants().isEmpty(), source.name());
		}
	}

	@Test
	void interactionAdjustmentsPreserveBossScaleAuthoredMultipliers() {
		MagicActionDefinition fireball = action("fireball");
		ScaledMagicValues ranked = service.scale(fireball, RankProfile.EMPTY, 10);
		ScaledMagicValues adjusted = PowerScalingService.applyInteraction(fireball, ranked,
				new CastAdjustment(true, 1.2, 1.1, 1.1, List.of()));

		assertEquals(8.4, adjusted.potencyMultiplier(), 0.0001);
		assertTrue(adjusted.rangeMultiplier() > 2.0);
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
