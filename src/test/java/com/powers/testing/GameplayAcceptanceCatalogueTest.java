package com.powers.testing;

import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.spell.SpellRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the live/manual verification surface synchronized with every gameplay registry. */
class GameplayAcceptanceCatalogueTest {
	@BeforeAll
	static void bootstrapRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		PowerRegistry.initialize();
	}

	@Test
	void everyAcceptanceIdentityIsUniqueAndNamesConcreteEvidence() {
		var entries = GameplayAcceptanceCatalogue.entries();
		Set<String> identities = new HashSet<>();
		for (var entry : entries) {
			assertTrue(identities.add(entry.family() + ":" + entry.id()), entry.toString());
			assertFalse(entry.id().isBlank(), entry.toString());
			assertFalse(entry.evidence().isBlank(), entry.toString());
		}
	}

	@Test
	void catalogueTracksEveryActionRegistryExactly() {
		assertEquals(PowerRegistry.getAll().stream().map(power -> power.id().getPath())
				.collect(Collectors.toUnmodifiableSet()), ids(GameplayAcceptanceCatalogue.Family.INNATE));
		assertEquals(SpellRegistry.defaults().definitions().stream().flatMap(book -> book.spells().stream())
				.map(spell -> spell.id()).collect(Collectors.toUnmodifiableSet()),
				ids(GameplayAcceptanceCatalogue.Family.SPELL));
		assertEquals(CrystalAbilityCatalog.defaults().values().stream().flatMap(java.util.List::stream)
				.collect(Collectors.toUnmodifiableSet()), ids(GameplayAcceptanceCatalogue.Family.CRYSTAL));
		assertEquals(ArtifactActionCatalogue.all().size(),
				ids(GameplayAcceptanceCatalogue.Family.ARTIFACT).size());
	}

	@Test
	void criticalWorldAndPresentationFamiliesRemainVisible() {
		Set<String> systems = ids(GameplayAcceptanceCatalogue.Family.SYSTEM);
		for (String required : Set.of("light_realm", "dark_realm", "middleworld", "mind_body",
				"living_forces", "amethyst", "rank_maze", "energy_hud", "shadow_companion",
				"celestial_ruin", "magic_collisions", "multiplayer_soak")) {
			assertTrue(systems.contains(required), required);
		}
		Set<String> entities = ids(GameplayAcceptanceCatalogue.Family.ENTITY);
		assertEquals(Set.of("darkness_creature", "power_test_actor", "radiant_sentinel",
				"dark_herald", "light_herald", "first_vessel", "echo_clone",
				"shadow_companion"), entities);
	}

	private static Set<String> ids(GameplayAcceptanceCatalogue.Family family) {
		return GameplayAcceptanceCatalogue.entries().stream()
				.filter(entry -> entry.family() == family).map(GameplayAcceptanceCatalogue.Entry::id)
				.collect(Collectors.toUnmodifiableSet());
	}
}
