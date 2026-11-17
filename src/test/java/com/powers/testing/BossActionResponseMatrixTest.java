package com.powers.testing;

import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.spell.SpellRegistry;
import com.powers.item.artifact.ArtifactActionCatalogue;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BossActionResponseMatrixTest {
	@Test
	void heavenGateTransportsEveryBossWithinTheCohort() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/light/dominion/heaven_gate", boss));
		}
	}

	@Test
	void nightfallDamagesNonDarknessBossesAndBuffsCasterNearDarkHerald() {
		String route = "artifact/darkness/unique/nightfall_dominion";
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			var expected = boss == BossActionResponseMatrix.Boss.DARK_HERALD
					? BossActionResponseMatrix.Response.SETUP_EFFECTIVE : BossActionResponseMatrix.Response.EFFECTIVE;
			assertEquals(expected, BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
		}
	}

	@Test
	void covenantChainPullsEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/light/dominion/covenant_chain", boss));
		}
	}

	@Test
	void secondDawnProvidesLethalHitProtectionAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/light/dominion/second_dawn", boss));
		}
	}

	@Test
	void guardianCallsGroundRitesAndHostOfHeavenProvideSetupNearEveryBoss() {
		for (String route : java.util.List.of("artifact/light/dominion/call_radiant",
				"artifact/darkness/unique/call_hollowed", "artifact/light/dominion/consecrate_ground",
				"artifact/darkness/unique/blight_ground", "artifact/light/dominion/host_heaven")) {
			for (var boss : BossActionResponseMatrix.Boss.values()) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void artifactDaybreakWaveDamagesDarkHeraldAndCleansesOtherBosses() {
		String route = "artifact/light/dominion/daybreak_wave";
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			var expected = boss == BossActionResponseMatrix.Boss.DARK_HERALD
					? BossActionResponseMatrix.Response.EFFECTIVE : BossActionResponseMatrix.Response.SETUP_EFFECTIVE;
			assertEquals(expected, BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
		}
	}

	@Test
	void solarFirmamentDamagesDarkHeraldAndHealsOtherBosses() {
		String route = "artifact/light/dominion/solar_firmament";
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			var expected = boss == BossActionResponseMatrix.Boss.DARK_HERALD
					? BossActionResponseMatrix.Response.EFFECTIVE : BossActionResponseMatrix.Response.SETUP_EFFECTIVE;
			assertEquals(expected, BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
		}
	}

	@Test
	void artifactTimeShiftCarriesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/time_shift", boss));
		}
	}

	@Test
	void artifactAstralProjectionProvidesSetupAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/astral_projection", boss));
		}
	}

	@Test
	void artifactSuperSpeedProvidesSetupAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/super_speed", boss));
		}
	}

	@Test
	void artifactSpeedBurstImpactDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/speed_burst", boss));
		}
	}

	@Test
	void artifactTimeFreezeSuspendsEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/time_freeze", boss));
		}
	}

	@Test
	void artifactInvisibilityProvidesSetupAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/invisibility", boss));
		}
	}

	@Test
	void artifactFlightProvidesSetupAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("darkness", "light")) {
				String route = "artifact/" + alignment + "/innate/flight";
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void artifactInnateSizeMorphProvidesSetupAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/size_shift", boss));
		}
	}

	@Test
	void artifactPlantHealingSupportsPlayersAroundEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("darkness", "light")) {
				String route = "artifact/" + alignment + "/innate/plant_healing_acceleration";
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void artifactIceDamagesAndChillsEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/ice_manipulation", boss));
		}
	}

	@Test
	void artifactEnergyDrainDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/energy_drain", boss));
		}
	}

	@Test
	void artifactGravityPullMovesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/gravity_displacement", boss));
		}
	}

	@Test
	void artifactBreezyBashMovesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/breezy_bash", boss));
		}
	}

	@Test
	void artifactFireballDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/fireball", boss));
		}
	}

	@Test
	void artifactVoidBeamDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/innate/void_beam", boss));
		}
	}

	@Test
	void artifactStarfallDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("darkness", "light")) {
				String route = "artifact/" + alignment + "/innate/starfall";
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void artifactThunderclapDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("darkness", "light")) {
				String route = "artifact/" + alignment + "/innate/thunderclap";
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void artifactLightningDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("darkness", "light")) {
				String route = "artifact/" + alignment + "/innate/lightning_strike";
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void artifactEnergyBeamDamagesEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("darkness", "light")) {
				String route = "artifact/" + alignment + "/innate/energy_beam";
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void forcefieldRoutesProtectCasterAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("forcefield", "artifact/darkness/innate/forcefield",
					"artifact/light/innate/forcefield")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void realmTravelRoutesCarryEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of(
					"crystal/middleworld",
					"crystal/light_crystal",
					"crystal/dark_crystal",
					"artifact/darkness/crystal/middleworld",
					"artifact/darkness/crystal/light_crystal",
					"artifact/darkness/crystal/dark_crystal",
					"artifact/light/crystal/light_crystal")) {
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route + " / " + boss);
			}
		}
	}

	@Test
	void chronoStopRoutesSuspendEveryBossUntilTheirBoundedDeadline() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("crystal/chrono_stop", "artifact/darkness/crystal/chrono_stop")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route);
			}
		}
	}

	@Test
	void soulLinkRoutesMirrorRealWoundsToEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("crystal/soul_link", "artifact/darkness/crystal/soul_link")) {
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route);
			}
		}
	}

	@Test
	void creationRoutesBuildEncounterTerrainWithoutDirectBossDamage() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("crystal/creativity_manifestation",
					"artifact/light/crystal/creativity_manifestation", "artifact/darkness/crystal/creativity_manifestation")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route);
			}
		}
	}

	@Test
	void crystalSizeRoutesSupportEveryBossEncounterWithoutDirectDamage() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("crystal/size_shift", "artifact/darkness/crystal/size_shift")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route);
			}
		}
	}

	@Test
	void cloneSwarmSupportsItsFactionAndDamagesHostileBosses() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("crystal/clone_swarm", boss));
			assertEquals(boss == BossActionResponseMatrix.Boss.DARK_HERALD
					? BossActionResponseMatrix.Response.SETUP_EFFECTIVE : BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("artifact/darkness/crystal/clone_swarm", boss));
		}
	}

	@Test
	void infernoRoutesHaveVerifiedFiniteBossDamage() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("crystal/inferno", "artifact/darkness/crystal/inferno")) {
				assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
						BossActionResponseMatrix.responseFor(route, boss), route);
			}
		}
	}

	@Test
	void heavenfallHasVerifiedCatastrophicDamageAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("spell/celestial_ruin", boss));
		}
	}

	@Test
	void locatorsObserveNamedBossesAndDiscoverTheSurroundingWorld() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.responseFor("spell/soul_compass", boss));
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("spell/cartographers_star", boss));
		}
	}

	@Test
	void wardBreakingChangesTheEncounterFieldRatherThanBossHealth() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
							&& entry.actionId().equals("spell/ward_breaking_ritual"))
							.findFirst().orElseThrow().response());
		}
	}

	@Test
	void anchorExplicitlyDocumentsUnsupportedBossTargets() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals("DELIBERATELY_IMMUNE", BossActionResponseMatrix.generated().stream()
					.filter(entry -> entry.boss() == boss && entry.actionId().equals("spell/dimensional_anchor"))
					.findFirst().orElseThrow().response().name());
		}
	}

	@Test
	void informationSpellsSupportTheCasterNearEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("spell/augury", "spell/grave_recall")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
								&& entry.actionId().equals(route)).findFirst().orElseThrow().response(), route);
			}
		}
	}

	@Test
	void wildSupportSpellsHaveVerifiedBossCombatResponses() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("spell/purification_circle", "spell/hearth_sanctuary",
					"spell/verdant_tending")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
								&& entry.actionId().equals(route)).findFirst().orElseThrow().response(), route);
			}
		}
	}

	@Test
	void bloodReadingIsEffectiveInformationAgainstEveryBoss() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
							&& entry.actionId().equals("spell/blood_reading")).findFirst().orElseThrow().response());
		}
	}

	@Test
	void crystalAndArtifactControlRoutesReportBossResistance() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String route : java.util.List.of("crystal/dreamwalking",
					"artifact/darkness/crystal/dreamwalking", "artifact/darkness/innate/vessel_possession")) {
				assertEquals(BossActionResponseMatrix.Response.RESISTED_WITH_FEEDBACK,
						BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
								&& entry.actionId().equals(route)).findFirst().orElseThrow().response(), route);
			}
		}
	}

	@Test
	void possessionReportsResistedControlRatherThanCombatSupport() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.RESISTED_WITH_FEEDBACK,
					BossActionResponseMatrix.responseFor("vessel_possession", boss));
		}
	}

	@Test
	void bothArtifactsRetainDoubleHealthCombatSupport() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("light", "darkness")) {
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
								&& entry.actionId().equals("artifact/" + alignment + "/innate/double_health"))
								.findFirst().orElseThrow().response());
			}
		}
	}

	@Test
	void dispelIsEffectiveAgainstEveryBossBuff() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.EFFECTIVE,
					BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
							&& entry.actionId().equals("spell/dispel")).findFirst().orElseThrow().response());
		}
	}

	@Test
	void artifactTelekinesisKeepsItsBossResistanceClassification() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.RESISTED_WITH_FEEDBACK,
					BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
							&& entry.actionId().equals("artifact/darkness/innate/telekinesis"))
							.findFirst().orElseThrow().response());
		}
	}

	@Test
	void alignedArtifactLifeBloomRoutesAreVerifiedCombatSupport() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			for (String alignment : java.util.List.of("light", "darkness")) {
				String key = "artifact/" + alignment + "/crystal/life_bloom";
				assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
						BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
								&& entry.actionId().equals(key)).findFirst().orElseThrow().response());
			}
		}
	}

	@Test
	void lifeBloomIsCombatSupportNotAnUnclassifiedCrystal() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.responseFor("crystal/life_bloom", boss));
			assertEquals(BossActionResponseMatrix.Response.SETUP_EFFECTIVE,
					BossActionResponseMatrix.generated().stream().filter(entry -> entry.boss() == boss
							&& entry.actionId().equals("crystal/life_bloom")).findFirst().orElseThrow().response());
		}
	}

	@Test
	void telekinesisReportsBossResistanceRatherThanFullEffectiveness() {
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			assertEquals(BossActionResponseMatrix.Response.RESISTED_WITH_FEEDBACK,
					BossActionResponseMatrix.responseFor("telekinesis", boss));
		}
	}

	@Test
	void matrixIncludesEverySpellCrystalAndAlignedArtifactActionNotOnlyInnates() {
		PowerRegistry.initialize();
		Set<String> expected = new HashSet<>();
		PowerRegistry.getAll().forEach(power -> expected.add("innate/" + power.id().getPath()));
		SpellRegistry.defaults().definitions().stream().flatMap(book -> book.spells().stream())
				.forEach(spell -> expected.add("spell/" + spell.id()));
		CrystalAbilityCatalog.defaults().values().stream().flatMap(java.util.List::stream)
				.forEach(id -> expected.add("crystal/" + id));
		ArtifactActionCatalogue.all().forEach(action -> expected.add("artifact/"
				+ action.alignment().name().toLowerCase(Locale.ROOT) + "/" + action.key()));
		for (var boss : BossActionResponseMatrix.Boss.values()) {
			Set<String> actual = BossActionResponseMatrix.generated().stream()
					.filter(entry -> entry.boss() == boss)
					.map(BossActionResponseMatrix.Entry::actionId).collect(Collectors.toSet());
			assertEquals(expected, actual, "Missing player-action coverage for " + boss);
		}
	}

	@Test
	void everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss() {
		PowerRegistry.initialize();
		var matrix = BossActionResponseMatrix.generated();
		assertEquals(matrix.size(), matrix.stream().map(entry -> entry.boss() + ":" + entry.actionId())
				.distinct().count(), "Duplicate boss/action routes must not inflate coverage");
		var missing = matrix.stream()
				.filter(entry -> entry.response() == BossActionResponseMatrix.Response.UNCLASSIFIED)
				.collect(Collectors.groupingBy(BossActionResponseMatrix.Entry::actionId,
						java.util.TreeMap::new, Collectors.mapping(BossActionResponseMatrix.Entry::boss, Collectors.toList())));
		assertEquals(java.util.Map.of(), missing, "Routes without a classified boss response");
	}
}
