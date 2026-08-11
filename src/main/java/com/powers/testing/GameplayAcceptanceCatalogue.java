package com.powers.testing;

import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.spell.SpellRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Runtime-synchronized index of what each verification layer actually proves. */
public final class GameplayAcceptanceCatalogue {
	public enum Family { INNATE, SPELL, CRYSTAL, ARTIFACT, ENTITY, SYSTEM }

	public enum Proof { LIVE_REGISTRY, LIVE_BEHAVIOR, UNIT_RULES, RESOURCE, SOAK }

	/** One gameplay identity and the strongest repeatable evidence currently attached to it. */
	public record Entry(Family family, String id, Proof proof, String evidence) {
		public Entry {
			Objects.requireNonNull(family, "family");
			Objects.requireNonNull(proof, "proof");
			if (id == null || id.isBlank()) throw new IllegalArgumentException("blank acceptance id");
			if (evidence == null || evidence.isBlank()) {
				throw new IllegalArgumentException("blank acceptance evidence");
			}
		}
	}

	private static final String LIVE_REGISTRY =
			"PowersGameTests#everyAdvertisedMagicRegistryResolvesInsideTheLiveServer";

	private GameplayAcceptanceCatalogue() {
	}

	/** Builds from the live registries so newly registered actions cannot disappear from coverage. */
	public static List<Entry> entries() {
		List<Entry> entries = new ArrayList<>();
		PowerRegistry.getAll().forEach(power -> entries.add(new Entry(Family.INNATE,
				power.id().getPath(), Proof.LIVE_REGISTRY, LIVE_REGISTRY)));
		SpellRegistry.defaults().definitions().stream().flatMap(book -> book.spells().stream())
				.forEach(spell -> entries.add(new Entry(Family.SPELL, spell.id(),
						Proof.LIVE_REGISTRY, LIVE_REGISTRY)));
		CrystalAbilityCatalog.defaults().values().stream().flatMap(List::stream).distinct()
				.forEach(id -> entries.add(new Entry(Family.CRYSTAL, id,
						Proof.LIVE_REGISTRY, LIVE_REGISTRY)));
		ArtifactActionCatalogue.all().forEach(action -> entries.add(new Entry(Family.ARTIFACT,
				action.alignment().name().toLowerCase(Locale.ROOT) + "/" + action.key(),
				Proof.LIVE_REGISTRY, LIVE_REGISTRY)));

		for (String entity : List.of("darkness_creature", "power_test_actor", "radiant_sentinel",
				"dark_herald", "light_herald", "first_vessel", "echo_clone")) {
			entries.add(new Entry(Family.ENTITY, entity, Proof.LIVE_BEHAVIOR,
					"PowersGameTests and LivingForceGameTests entity scenarios"));
		}

		entries.add(system("light_realm", Proof.RESOURCE, "RealmResourcesTest and dedicated-server boot"));
		entries.add(system("dark_realm", Proof.LIVE_BEHAVIOR,
				"PowersGameTests#darkCrystalMovesItsCasterIntoTheMindscape"));
		entries.add(system("middleworld", Proof.LIVE_REGISTRY, LIVE_REGISTRY));
		entries.add(system("mind_body", Proof.LIVE_BEHAVIOR,
				"PowersGameTests#forcefieldFollowsTheMindBodyTetherAndProtectsThePhysicalBody"));
		entries.add(system("living_forces", Proof.LIVE_BEHAVIOR, "LivingForceGameTests"));
		entries.add(system("amethyst", Proof.LIVE_BEHAVIOR,
				"LivingForceGameTests#poweredAmethystCeremonyCrystallisesLivingForce"));
		entries.add(system("rank_maze", Proof.RESOURCE, "RankMazePresentationResourcesTest"));
		entries.add(system("energy_hud", Proof.UNIT_RULES, "HudLayoutTest and HudMathTest"));
		entries.add(system("shadow_companion", Proof.LIVE_BEHAVIOR,
				"PowersGameTests#shadowChatOwnsVisibilityAndFormerBookKnowledge and #shadowExplainsTheExactLatestServerRecordedMagicFailure"));
		entries.add(system("shadow_diagnostics", Proof.LIVE_BEHAVIOR,
				"PowersGameTests#shadowExplainsTheExactLatestServerRecordedMagicFailure"));
		entries.add(system("artifact_energy", Proof.LIVE_BEHAVIOR,
				"PowersGameTests#soulstoneReservoirPaysEnergyShortfallsAtomically"));
		entries.add(system("celestial_ruin", Proof.LIVE_BEHAVIOR,
				"PowersGameTests#celestialRuinOverwhelmsTheFirstVesselsLayeredVitality"));
		entries.add(system("magic_collisions", Proof.UNIT_RULES,
				"MagicInteractionResolverTest exhaustive 2,080-pair matrix"));
		entries.add(system("multiplayer_soak", Proof.SOAK, "SyntheticMultiplayerSoakTest"));
		return List.copyOf(entries);
	}

	/** Compact operator-facing count used during a manual acceptance session. */
	public static String summary() {
		List<Entry> current = entries();
		StringBuilder result = new StringBuilder();
		for (Family family : Family.values()) {
			long count = current.stream().filter(entry -> entry.family() == family).count();
			if (!result.isEmpty()) result.append(", ");
			result.append(family.name().toLowerCase(Locale.ROOT)).append('=').append(count);
		}
		return result.toString();
	}

	private static Entry system(String id, Proof proof, String evidence) {
		return new Entry(Family.SYSTEM, id, proof, evidence);
	}
}
