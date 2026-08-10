package com.powers;

import com.powers.entity.AbstractPlayerLikeMob;
import com.powers.entity.DarknessCreature;
import com.powers.entity.PowerTestActor;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/** Registers the two server-authoritative player-shaped testing/combat mobs. */
public final class PowersEntities {
	public static final EntityType<DarknessCreature> DARKNESS_CREATURE = register(
			"darkness_creature", DarknessCreature::new, MobCategory.MONSTER);
	public static final EntityType<PowerTestActor> POWER_TEST_ACTOR = register(
			"power_test_actor", PowerTestActor::new, MobCategory.CREATURE);

	private PowersEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(DARKNESS_CREATURE,
				AbstractPlayerLikeMob.createAttributes());
		FabricDefaultAttributeRegistry.register(POWER_TEST_ACTOR,
				AbstractPlayerLikeMob.createAttributes());
		SpawnPlacements.register(DARKNESS_CREATURE, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
	}

	private static <T extends AbstractPlayerLikeMob> EntityType<T> register(String path,
			EntityType.EntityFactory<T> factory, MobCategory category) {
		Identifier id = PowersMod.id(path);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<T> type = EntityType.Builder.of(factory, category).sized(0.6F, 1.8F)
				.eyeHeight(1.62F).clientTrackingRange(10).build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
	}
}
