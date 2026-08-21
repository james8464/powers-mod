package com.powers.gametest;

import com.mojang.authlib.GameProfile;
import com.powers.PowersEntities;
import com.powers.PowersWeapons;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.entity.EchoClone;
import com.powers.entity.FirstVessel;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.component.ResolvableProfile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stable server-side inventory for the exact renderer families audited by VFX-011. */
public final class VfxGalleryFixture {
	private static final List<String> ENTITIES = List.of(
			"dark_herald", "darkness_creature", "first_vessel", "light_herald",
			"power_test_actor", "radiant_sentinel", "shadow", "echo");
	private static final List<String> SPAWN_EGGS = List.of(
			"dark_herald_spawn_egg", "darkness_creature_spawn_egg", "first_vessel_spawn_egg",
			"light_herald_spawn_egg", "power_test_actor_spawn_egg", "radiant_sentinel_spawn_egg");

	private VfxGalleryFixture() {
	}

	public static List<String> entityFamilies() {
		return ENTITIES;
	}

	public static List<String> spawnEggIds() {
		return SPAWN_EGGS;
	}

	public static List<Integer> spawnRendererEntities(ServerPlayer viewer, UUID wideProfile, UUID slimProfile) {
		var level = viewer.level();
		List<Entity> entities = new ArrayList<>();
		entities.add(PowersEntities.DARK_HERALD.create(level, EntitySpawnReason.COMMAND));
		entities.add(PowersEntities.DARKNESS_CREATURE.create(level, EntitySpawnReason.COMMAND));
		entities.add(PowersEntities.FIRST_VESSEL.create(level, EntitySpawnReason.COMMAND));
		entities.add(PowersEntities.LIGHT_HERALD.create(level, EntitySpawnReason.COMMAND));
		entities.add(PowersEntities.POWER_TEST_ACTOR.create(level, EntitySpawnReason.COMMAND));
		entities.add(PowersEntities.RADIANT_SENTINEL.create(level, EntitySpawnReason.COMMAND));
		entities.add(profiledShadow(level, wideProfile, "GalleryWide"));
		entities.add(profiledShadow(level, slimProfile, "GallerySlim"));
		entities.add(profiledEcho(level, wideProfile, "GalleryWide"));
		entities.add(profiledEcho(level, slimProfile, "GallerySlim"));
		for (int index = 0; index < entities.size(); index++) {
			Entity entity = entities.get(index);
			if (entity == null) throw new AssertionError("Gallery entity factory returned null at " + index);
			entity.setPos(viewer.getX() + index % 5 - 2.0, viewer.getY(), viewer.getZ() + 8.0 + index / 5);
			entity.setInvulnerable(true);
			entity.addTag("powers_vfx_gallery");
			if (entity instanceof Mob mob) {
				mob.setNoAi(true);
				mob.setPersistenceRequired();
				mob.setItemSlot(EquipmentSlot.MAINHAND, PowersWeapons.weapon("solstice").getDefaultInstance());
				mob.setItemSlot(EquipmentSlot.OFFHAND, PowersWeapons.weapon("nocturne").getDefaultInstance());
				mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			}
			if (!level.addFreshEntity(entity)) {
				throw new AssertionError("Authoritative gallery rejected entity "
						+ entity.getType() + " id=" + entity.getId());
			}
		}
		return entities.stream().map(Entity::getId).toList();
	}

	/** Spawns one tracked production boss and configures a deterministic boss-bar phase. */
	public static void showBoss(ServerPlayer viewer, String id, float healthRatio) {
		clearBosses(viewer);
		Entity boss = switch (id) {
			case "light_herald" -> PowersEntities.LIGHT_HERALD.create(viewer.level(), EntitySpawnReason.COMMAND);
			case "dark_herald" -> PowersEntities.DARK_HERALD.create(viewer.level(), EntitySpawnReason.COMMAND);
			case "first_vessel/opening", "first_vessel/unbound", "first_vessel/last_covenant" ->
					PowersEntities.FIRST_VESSEL.create(viewer.level(), EntitySpawnReason.COMMAND);
			default -> throw new AssertionError("Unknown VFX boss fixture " + id);
		};
		if (!(boss instanceof Mob mob)) throw new AssertionError("VFX boss factory returned null for " + id);
		mob.setPos(viewer.getX(), viewer.getY(), viewer.getZ() + 12.0);
		mob.setNoAi(false);
		mob.setInvulnerable(true);
		mob.setPersistenceRequired();
		mob.addTag("powers_vfx_ui_boss");
		if (mob instanceof FirstVessel vessel) {
			set(vessel, FirstVessel.class, "effectiveMaximumHealth", 2_400.0F);
			set(vessel, FirstVessel.class, "effectiveHealth", 2_400.0F * healthRatio);
		} else {
			mob.setHealth(mob.getMaxHealth() * healthRatio);
		}
		if (!viewer.level().addFreshEntity(mob)) {
			throw new AssertionError("Could not add production VFX boss " + id);
		}
	}

	public static void clearBosses(ServerPlayer viewer) {
		for (Entity entity : viewer.level().getAllEntities()) {
			if (entity.entityTags().contains("powers_vfx_ui_boss")) entity.discard();
		}
		viewer.setGameMode(GameType.SURVIVAL);
	}

	public static void clearRendererEntities(ServerPlayer viewer) {
		for (Entity entity : viewer.level().getAllEntities()) {
			if (entity.entityTags().contains("powers_vfx_gallery")) entity.discard();
		}
	}

	/** Moves the connected fixture player through the same vanilla dimension command path. */
	public static void teleportDimension(ServerPlayer viewer, String dimension) {
		try {
			var server = viewer.level().getServer();
			int result = server.getCommands().getDispatcher().execute(
					"execute in " + dimension + " run tp @s 0 100 0",
					server.createCommandSourceStack().withEntity(viewer).withPosition(viewer.position()));
			if (result <= 0) throw new AssertionError("Dimension command rejected " + dimension);
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
			throw new AssertionError("Could not enter VFX HUD dimension " + dimension, error);
		}
	}

	/** Switches the fixture player's production-visible advancement tree. */
	public static void configureAdvancementPath(ServerPlayer viewer, boolean darkness) {
		if (darkness) viewer.addTag(SkillSystem.DARKNESS_TAG);
		else viewer.removeTag(SkillSystem.DARKNESS_TAG);
		if (darkness) PlayerPowers.get(viewer).setDarknessLevel(viewer, 1);
		else PlayerPowers.get(viewer).setSkillLevel(viewer, 1);
		SkillSystem.syncPathVisibility(viewer);
		if (darkness) SkillSystem.awardDarknessRite(viewer, 1);
		else SkillSystem.awardSkillRite(viewer, 1);
	}

	/** Configures actual vanilla player/vehicle state so HUD ordering is rendered by production code. */
	public static void configureVanillaHud(ServerPlayer viewer, String state) {
		viewer.stopRiding();
		for (Entity entity : viewer.level().getAllEntities()) {
			if (entity.entityTags().contains("powers_vfx_ui_mount")) entity.discard();
		}
		viewer.setGameMode(GameType.SURVIVAL);
		viewer.setInvulnerable(true);
		viewer.setNoGravity(true);
		viewer.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		viewer.setHealth(viewer.getMaxHealth());
		viewer.setAirSupply(viewer.getMaxAirSupply());
		viewer.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET)) viewer.setItemSlot(slot, ItemStack.EMPTY);
		switch (state) {
			case "default" -> { }
			case "low_health" -> viewer.setHealth(5.0F);
			case "armor" -> viewer.getAttribute(Attributes.ARMOR).setBaseValue(16.0);
			case "air" -> viewer.setAirSupply(80);
			case "mount" -> {
				Horse horse = net.minecraft.world.entity.EntityTypes.HORSE.create(
						viewer.level(), EntitySpawnReason.COMMAND);
				if (horse == null) throw new AssertionError("Could not create VFX HUD mount");
				horse.setPos(viewer.position());
				horse.setNoAi(true);
				horse.setHealth(horse.getMaxHealth() * 0.55F);
				horse.addTag("powers_vfx_ui_mount");
				viewer.level().addFreshEntity(horse);
				viewer.startRiding(horse, true, false);
			}
			case "spectator" -> viewer.setGameMode(GameType.SPECTATOR);
			default -> throw new AssertionError("Unknown vanilla HUD state " + state);
		}
	}

	/** Builds a stable roomy arena for literal hand-camera and local-player captures. */
	public static void configureGameplay(ServerPlayer viewer) {
		configureVanillaHud(viewer, "default");
		if (viewer.getVehicle() != null) throw new AssertionError("Gameplay fixture retained a mount");
		var level = viewer.level();
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 14; z++) {
				level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x, 99, z),
						Blocks.SMOOTH_STONE.defaultBlockState());
			}
		}
		for (int x = -10; x <= 10; x++) {
			for (int y = 96; y <= 108; y++) {
				level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x, y, 14),
						Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			}
		}
		viewer.snapTo(0.5, 100.0, 0.5, 0.0F, 8.0F);
		viewer.setNoGravity(true);
		viewer.setInvulnerable(true);
		viewer.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
	}

	/** Makes renderer captures independent of the save's ambient clock and weather. */
	public static void stabilize(ServerPlayer viewer) {
		var level = viewer.level();
		var server = level.getServer();
		try {
			server.getCommands().getDispatcher().execute("time set noon", server.createCommandSourceStack());
			server.getCommands().getDispatcher().execute("gamerule advance_time false",
					server.createCommandSourceStack());
			server.getCommands().getDispatcher().execute("gamerule advance_weather false",
					server.createCommandSourceStack());
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
			throw new AssertionError("Could not stabilize VFX gallery time", error);
		}
		var weather = level.getWeatherData();
		weather.setClearWeatherTime(1_000_000);
		weather.setRainTime(1_000_000);
		weather.setThunderTime(1_000_000);
		weather.setRaining(false);
		weather.setThundering(false);
		level.setRainLevel(0.0F);
		server.forceGameTimeSynchronization();
	}

	private static ShadowCompanionEntity profiledShadow(net.minecraft.server.level.ServerLevel level,
			UUID ownerId, String name) {
		ShadowCompanionEntity shadow = PowersEntities.SHADOW_COMPANION.create(level, EntitySpawnReason.COMMAND);
		set(shadow, ShadowCompanionEntity.class, "ownerId", ownerId);
		setProfile(shadow, ShadowCompanionEntity.class, ownerId, name);
		shadow.setRevealed(true);
		return shadow;
	}

	private static EchoClone profiledEcho(net.minecraft.server.level.ServerLevel level,
			UUID ownerId, String name) {
		EchoClone echo = PowersEntities.ECHO_CLONE.create(level, EntitySpawnReason.COMMAND);
		set(echo, EchoClone.class, "ownerId", ownerId);
		set(echo, EchoClone.class, "remainingTicks", 20_000);
		setProfile(echo, EchoClone.class, ownerId, name);
		return echo;
	}

	@SuppressWarnings("unchecked")
	private static void setProfile(Entity entity, Class<?> type, UUID id, String name) {
		try {
			Field field = type.getDeclaredField("PROFILE");
			field.setAccessible(true);
			EntityDataAccessor<ResolvableProfile> accessor =
					(EntityDataAccessor<ResolvableProfile>) field.get(null);
			entity.getEntityData().set(accessor,
					ResolvableProfile.createResolved(new GameProfile(id, name)));
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Could not configure gallery profile for " + type.getSimpleName(), error);
		}
	}

	private static void set(Object instance, Class<?> type, String fieldName, Object value) {
		try {
			Field field = type.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(instance, value);
		} catch (ReflectiveOperationException error) {
			throw new AssertionError("Could not configure gallery field " + type.getSimpleName()
					+ "." + fieldName, error);
		}
	}
}
