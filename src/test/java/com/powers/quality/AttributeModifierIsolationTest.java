package com.powers.quality;

import com.powers.PowersMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cross-mod contract: cleanup is always by a stable POWERS ID, never by attribute-wide removal. */
class AttributeModifierIsolationTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void everyOwnedScaleSpeedHealthAndKnockbackRemovalPreservesForeignModifiers() {
		Identifier foreign = Identifier.fromNamespaceAndPath("compatibility_fixture", "foreign_modifier");
		for (String ownedPath : List.of("size_morph", "size_shift_shrink", "size_shift_grow",
				"size_shift_knockback", "super_speed", "double_health", "rank_movement",
				"rank_resilience", "rank_ward_stability")) {
			AttributeInstance instance = new AttributeInstance(Attributes.SCALE, ignored -> { });
			Identifier owned = PowersMod.id(ownedPath);
			instance.addTransientModifier(new AttributeModifier(foreign, 0.25,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			instance.addTransientModifier(new AttributeModifier(owned, 0.50,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

			assertTrue(instance.removeModifier(owned), ownedPath);
			assertFalse(instance.hasModifier(owned), ownedPath);
			assertTrue(instance.hasModifier(foreign), ownedPath + " removed a foreign modifier");
		}
	}

	@Test
	void productionNeverClearsAnEntireAttributeOrMutatesCreativeFlightAuthority() throws Exception {
		try (var files = Files.walk(Path.of("src/main/java"))) {
			for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String text = Files.readString(source);
				assertFalse(text.contains(".removeModifiers()"), source.toString());
			}
		}
		String flight = Files.readString(Path.of(
				"src/main/java/com/powers/power/abilities/FlightAbility.java"));
		assertFalse(flight.contains("getAbilities().mayfly"));
		assertFalse(flight.contains("flying ="));
	}
}
