package com.powers.fx;

import com.powers.PowersMod;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityActivationService;
import com.powers.power.PowerRegistry;
import com.powers.power.ToggleAbility;
import com.powers.progression.InnatePowerLevels;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Live server acceptance for the VFX-005 successful-cast boundary and bounded admission policy. */
public final class RankTenSilhouetteGameTests {
	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void everyExactRankTenInnateIdTraversesTheCommittedCastHook(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 10);
		TestingOverrides.setAll(caster.getUUID(), true);
		RankTenSilhouetteService.clear(helper.getLevel().getServer());
		try {
			Set<String> exactIds = InnatePowerLevels.powerIds();
			helper.assertTrue(exactIds.size() == 23, "Rank-ten catalogue did not contain 23 IDs");
			for (String id : exactIds) {
				helper.assertTrue(PowerRegistry.get(id) != null, "Missing production innate " + id);
				AbilityActivationService.Result result = AbilityActivationService.activate(
						caster, new ProbeAbility(id, 0, false, true), "powers:" + id);
				helper.assertTrue(result == AbilityActivationService.Result.ACTIVATED,
						"Committed cast probe failed for " + id);
			}
			var diagnostics = RankTenSilhouetteService.diagnostics(helper.getLevel().getServer());
			helper.assertTrue(diagnostics.acceptedEvents() == 23,
					"Expected one accepted event for every production ID");
			helper.assertTrue(diagnostics.acceptedProfiles().equals(exactIds),
					"Accepted profiles differed from InnatePowerLevels.powerIds()");
			helper.assertTrue(diagnostics.chunkTicketsRequested() == 0,
					"Presentation requested a chunk ticket");
		} finally {
			cleanup(caster, helper);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void inputToggleAndFailureRoutesRetainExactEmissionBoundaries(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 10);
		TestingOverrides.setAll(caster.getUUID(), true);
		RankTenSilhouetteService.clear(helper.getLevel().getServer());
		try {
			ProbeAbility input = new ProbeAbility("time_shift", 0, true, true);
			helper.assertTrue(AbilityActivationService.activateInput(caster, input, true, () -> true)
					== AbilityActivationService.Result.ACTIVATED, "Input cast did not commit");
			ProbeToggle toggle = new ProbeToggle("flight");
			helper.assertTrue(AbilityActivationService.activate(caster, toggle, "powers:flight")
					== AbilityActivationService.Result.ACTIVATED, "Toggle-on did not commit");
			long afterSuccess = accepted(helper);
			helper.assertTrue(afterSuccess == 2, "Input and toggle-on did not emit exactly once each");
			helper.assertTrue(AbilityActivationService.activate(caster, toggle, "powers:flight")
					== AbilityActivationService.Result.ACTIVATED, "Toggle-off did not commit");
			helper.assertTrue(accepted(helper) == afterSuccess, "Toggle-off emitted presentation");

			ProbeAbility selection = new SelectionProbe("size_shift");
			helper.assertTrue(AbilityActivationService.activate(caster, selection, "powers:size_shift")
					== AbilityActivationService.Result.ACTIVATED, "Selection probe did not resolve");
			helper.assertTrue(accepted(helper) == afterSuccess, "Selection emitted presentation");

			ProbeAbility artifact = new ProbeAbility("fireball", 0, false, true);
			helper.assertTrue(AbilityActivationService.activateWithCooldown(
					caster, artifact, "powers:fireball", 0, CastSource.ARTIFACT)
					== AbilityActivationService.Result.ACTIVATED, "Artifact probe did not commit");
			helper.assertTrue(accepted(helper) == afterSuccess, "Artifact cast emitted innate presentation");

			data.setSkillLevel(caster, 9);
			helper.assertTrue(AbilityActivationService.activate(caster,
					new ProbeAbility("starfall", 0, false, true), "powers:starfall", true)
					== AbilityActivationService.Result.ACTIVATED, "Rank-nine gameplay probe did not commit");
			helper.assertTrue(accepted(helper) == afterSuccess, "Rank-nine cast emitted presentation");
			helper.assertTrue(RankTenSilhouetteService.diagnostics(helper.getLevel().getServer()).belowRank() == 1,
					"Rank-nine offer was not rejected by the service");

			data.setSkillLevel(caster, 10);
			ProbeAbility failed = new ProbeAbility("void_beam", 0, false, false);
			helper.assertTrue(AbilityActivationService.activate(caster, failed, "powers:void_beam", true)
					== AbilityActivationService.Result.FAILED, "Failed execution unexpectedly committed");
			helper.assertTrue(accepted(helper) == afterSuccess, "Failed execution emitted presentation");

			TestingOverrides.setEnergyDisabled(caster.getUUID(), false);
			data.emptyEnergy();
			helper.assertTrue(AbilityActivationService.activate(caster,
					new ProbeAbility("energy_beam", 0, false, true), "powers:energy_beam", true)
					== AbilityActivationService.Result.FAILED, "Empty-energy cast unexpectedly committed");
			helper.assertTrue(accepted(helper) == afterSuccess, "Insufficient energy emitted presentation");
		} finally {
			cleanup(caster, helper);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void cooldownFailureCannotProduceASecondSilhouette(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 10);
		data.forceRestoreEnergy();
		RankTenSilhouetteService.clear(helper.getLevel().getServer());
		ProbeAbility ability = new ProbeAbility("lightning_strike", 200, false, true);
		try {
			helper.assertTrue(AbilityActivationService.activate(caster, ability, "powers:lightning_strike")
					== AbilityActivationService.Result.ACTIVATED, "Initial cooldown probe did not commit");
			long afterFirst = accepted(helper);
			helper.assertTrue(afterFirst == 1, "Initial cooldown probe did not emit");
			helper.assertTrue(AbilityActivationService.activate(caster, ability, "powers:lightning_strike")
					== AbilityActivationService.Result.FAILED, "Cooldown did not reject repeat cast");
			helper.assertTrue(accepted(helper) == afterFirst, "Cooldown rejection emitted presentation");
		} finally {
			cleanup(caster, helper);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void admissionCapsSessionsRangeAndTicketsRemainFailClosed(GameTestHelper helper) {
		String overworld = helper.getLevel().dimension().identifier().toString();
		var state = RankTenSilhouetteService.initialPolicy(1);
		List<RankTenSilhouetteService.Decision> decisions = new ArrayList<>();
		for (int index = 0; index < 33; index++) {
			var decision = RankTenSilhouetteService.offer(state,
					cast(20, new UUID(0xA11CEL, index + 1L), "flight", overworld), List.of());
			decisions.add(decision);
			state = decision.state();
		}
		helper.assertTrue(decisions.stream().filter(RankTenSilhouetteService.Decision::accepted).count() == 32,
				"Global admission cap did not stop the 33rd offer");
		helper.assertFalse(decisions.getLast().accepted(), "Over-cap offer was accepted");
		helper.assertTrue(state.diagnostics().budgetRejected() == 1, "Over-cap rejection was not diagnosed");

		var observers = List.of(
				observer(101, overworld, 384.0, true, true),
				observer(102, overworld, 384.01, true, true),
				observer(103, "powers:dark_realm", 1.0, true, true),
				observer(104, overworld, 1.0, false, true),
				observer(105, overworld, 1.0, true, false));
		var filtered = RankTenSilhouetteService.offer(RankTenSilhouetteService.initialPolicy(1),
				cast(21, new UUID(0xA11CEL, 200), "fireball", overworld), observers);
		helper.assertTrue(filtered.accepted(), "Boundary observer offer was rejected");
		helper.assertTrue(filtered.recipients().equals(List.of(new UUID(0xA11CEL, 101))),
				"Range/session/dimension filtering selected the wrong observer");
		helper.assertTrue(filtered.diagnostics().rangeObservers() == 1
				&& filtered.diagnostics().dimensionObservers() == 1
				&& filtered.diagnostics().unsupportedObservers() == 1
				&& filtered.diagnostics().staleObservers() == 1,
				"Observer rejection diagnostics were incomplete");

		var exhausted = RankTenSilhouetteService.offer(
				RankTenSilhouetteService.initialPolicy(Long.MAX_VALUE),
				cast(22, new UUID(0xA11CEL, 201), "double_health", overworld), List.of());
		helper.assertFalse(exhausted.accepted(), "Exhausted event ID was accepted");
		helper.assertTrue(exhausted.diagnostics().exhaustedEvents() == 1,
				"Event exhaustion was not diagnosed");
		helper.assertTrue(state.diagnostics().chunkTicketsRequested() == 0
				&& filtered.diagnostics().chunkTicketsRequested() == 0
				&& exhausted.diagnostics().chunkTicketsRequested() == 0,
				"Presentation admission requested chunk tickets");
		helper.succeed();
	}

	private static long accepted(GameTestHelper helper) {
		return RankTenSilhouetteService.diagnostics(helper.getLevel().getServer()).acceptedEvents();
	}

	private static RankTenSilhouetteService.CastOffer cast(long tick, UUID caster,
			String powerId, String dimension) {
		return new RankTenSilhouetteService.CastOffer(tick, caster, 10, powerId, dimension,
				0, 0, 0, 0, 0, 0, 0x5EED);
	}

	private static RankTenSilhouetteService.ObserverOffer observer(int id, String dimension,
			double x, boolean supported, boolean live) {
		return new RankTenSilhouetteService.ObserverOffer(new UUID(0xA11CEL, id), dimension,
				x, 0, 0, supported, live);
	}

	private static void cleanup(ServerPlayer caster, GameTestHelper helper) {
		TestingOverrides.clear(caster.getUUID());
		MagicRuntime.global().clearOwner(caster.getUUID());
		RankTenSilhouetteService.clear(helper.getLevel().getServer());
	}

	private static class ProbeAbility extends Ability {
		private final boolean result;

		private ProbeAbility(String id, int cooldown, boolean input, boolean result) {
			super(PowersMod.id(id), Component.literal("VFX-005 " + id), cooldown, input);
			this.result = result;
		}

		@Override
		public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
			return result;
		}
	}

	private static final class SelectionProbe extends ProbeAbility {
		private SelectionProbe(String id) {
			super(id, 0, false, true);
		}

		@Override
		public boolean isSelectionAction(ServerPlayer player) {
			return true;
		}
	}

	private static final class ProbeToggle extends ToggleAbility {
		private ProbeToggle(String id) {
			super(PowersMod.id(id), Component.literal("VFX-005 " + id));
		}

		@Override
		public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
			return true;
		}
	}
}
