package com.powers.network;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.item.GrimoireItem;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.magic.runtime.CastSource;
import com.powers.progression.PowerScalingService;
import com.powers.progression.RankVariantRules;
import com.powers.protection.PowerProtection;
import com.powers.spell.GrimoireDefinition;
import com.powers.spell.CartographerQuery;
import com.powers.spell.CartographerSearch;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellEffect;
import com.powers.spell.SpellCastingManager;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validates celestial-locator requests and stages the associated server-side
 * ritual. A short-lived nonce binds each selection to a screen opened by the
 * server, preventing replayed or fabricated locate packets.
 */
final class LocatorSpellPackets {
	private static final int NONCE_LIFETIME_TICKS = 20 * 30;
	private static final int CELESTIAL_COLOR = 0xFFD9E9FF;
	private static final int GOLD_COLOR = 0xFFFFE08A;
	private static final CastNonceTracker NONCES = new CastNonceTracker(NONCE_LIFETIME_TICKS);
	private static final Map<UUID, CelestialSearchMode> MODES = new HashMap<>();
	private record TargetRef(UUID id, ResourceKey<Level> dimension) { }

	private LocatorSpellPackets() {
	}

	static void open(ServerPlayer player, CelestialSearchMode mode) {
		UUID nonce = NONCES.issue(player.getUUID(), player.level().getServer().getTickCount());
		MODES.put(player.getUUID(), mode);
		PowersPlayNetworking.send(player, new PowersPackets.OpenLocatorScreenPayload(mode, nonce));
	}

	static void forget(UUID owner) {
		NONCES.clear(owner);
		MODES.remove(owner);
	}

	static void clearAll() {
		NONCES.clearAll();
		MODES.clear();
	}

	static boolean hasPendingNonce(UUID owner) {
		return NONCES.contains(owner);
	}

	static void handleLocate(PowersPackets.LocateTargetPayload payload, ServerPlayer player) {
		if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.LOCATOR)) {
			locate(player, payload, player.level().getServer().getTickCount());
		}
	}

	private static void locate(ServerPlayer player, PowersPackets.LocateTargetPayload payload, long currentTick) {
		if (!NONCES.consume(player.getUUID(), payload.nonce(), currentTick) || !holdsCelestialGrimoire(player)) return;
		CelestialSearchMode mode = MODES.remove(player.getUUID());
		if (mode == null) return;
		if (payload.targetName().isBlank() || payload.targetName().length() > 64) return;
		if (EntityFreezeController.isFrozen(player)) {
			EntityFreezeController.reject(player);
			return;
		}

		if (mode == CelestialSearchMode.WORLD) {
			locateWorld(player, payload.targetName());
			return;
		}

		NamedTargetRules.Resolution<LivingEntity> resolution = findNamedTarget(
				player.level().getServer(), payload.targetName());
		if (resolution.status() == NamedTargetRules.Status.AMBIGUOUS) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "soul_compass",
					com.powers.knowledge.MagicFailureReason.NO_TARGET);
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.ambiguous"));
			return;
		}
		if (resolution.status() == NamedTargetRules.Status.SCAN_LIMIT) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "soul_compass",
					com.powers.knowledge.MagicFailureReason.SERVER_BUDGET);
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.scan_limit"));
			return;
		}
		LivingEntity target = resolution.target();
		if (target == null) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "soul_compass",
					com.powers.knowledge.MagicFailureReason.NO_TARGET);
			PowerMessages.send(player, "grimoire.celestial.offline", 3);
			return;
		}
		if (!PowerProtection.mayLocate(player, target)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "soul_compass",
					com.powers.knowledge.MagicFailureReason.CONSENT);
			PowerMessages.sendImportant(player, "grimoire.celestial.consent_denied", 1);
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		Vec3 castPosition = player.position().add(0, 1, 0);
		AmethystDampening.update(player);
		if (AmethystDampening.isDampened(player)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "soul_compass",
					com.powers.knowledge.MagicFailureReason.AMETHYST);
			retaliate(player, level, castPosition, "grimoire.celestial.amethyst", 3);
			return;
		}
		boolean ordinaryVeilAccess = hasOrdinaryRealmAccess(player, target);
		boolean trueSight = PowerScalingService.hasVariant(player, "true_sight");
		if (!RankVariantRules.mayPierceRealmVeil(ordinaryVeilAccess, trueSight)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "soul_compass",
					com.powers.knowledge.MagicFailureReason.ALIGNMENT_LOCK);
			String key = isLightRealm(target.level().dimension())
					? "grimoire.celestial.light_gate" : "grimoire.celestial.dark_gate";
			retaliate(player, level, castPosition, key, 3);
			return;
		}
		boolean piercedWithTrueSight = !ordinaryVeilAccess && trueSight;

		// The selection packet names only a target. Payment and the selected
		// celestial spell are revalidated immediately before the ritual commits.
		if (SpellCastingManager.commitSoulCompass(player)) {
			cast(player, level, castPosition, target, piercedWithTrueSight);
		}
	}

	private static void locateWorld(ServerPlayer player, String rawQuery) {
		CartographerQuery query = CartographerQuery.parse(rawQuery).orElse(null);
		if (query == null) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.world.syntax"));
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		if (!CartographerSearch.isKnownTarget(level, query)) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.world.unknown",
					query.target()));
			return;
		}
		if (!SpellCastingManager.commitLocator(player, SpellEffect.CARTOGRAPHERS_STAR)) return;
		CartographerSearch.Result result = CartographerSearch.find(level, player.blockPosition(), query)
				.orElse(null);
		if (result == null) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.world.not_found",
					query.target()));
			return;
		}
		revealWorldResult(player, level, result);
	}

	private static void revealWorldResult(ServerPlayer player, ServerLevel level,
			CartographerSearch.Result result) {
		BlockPos origin = player.blockPosition();
		BlockPos target = result.position();
		long dx = (long) target.getX() - origin.getX();
		long dz = (long) target.getZ() - origin.getZ();
		long distance = Math.round(Math.sqrt(dx * dx + dz * dz));
		String direction = compassDirection(dx, dz);
		player.sendSystemMessage(Component.translatable("grimoire.celestial.world.result",
				result.registryId(), direction, distance));
		player.sendSystemMessage(Component.translatable("grimoire.celestial.world.coordinates",
				level.dimension().identifier().toString(), target.getX(), target.getY(), target.getZ()));
		Vec3 castPosition = player.position().add(0.0, 1.0, 0.0);
		PowerFx.rune(level, castPosition, 2.8, GOLD_COLOR, 30, level.getGameTime() * 0.03);
		PowerFx.beam(level, castPosition, castPosition.add(dx == 0 ? 0 : Math.signum(dx) * 8.0,
				3.0, dz == 0 ? 0 : Math.signum(dz) * 8.0), PowerFx.dust(CELESTIAL_COLOR, 1.0F), 16);
		PowerFx.sound(level, castPosition, SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F, 1.25F);
	}

	static String compassDirection(long dx, long dz) {
		if (dx == 0 && dz == 0) return "here";
		double angle = Math.atan2(dx, -dz);
		String[] names = {"north", "north-east", "east", "south-east",
				"south", "south-west", "west", "north-west"};
		int index = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0)), names.length);
		return names[index];
	}

	private static boolean holdsCelestialGrimoire(ServerPlayer player) {
		return isCelestial(player.getMainHandItem()) || isCelestial(player.getOffhandItem());
	}

	private static boolean isCelestial(ItemStack stack) {
		if (!(stack.getItem() instanceof GrimoireItem grimoire)) return false;
		GrimoireDefinition definition = SpellCastingManager.registry().forTexture(grimoire.key());
		return definition != null && definition.key().equals("book_grimoire_celestial");
	}

	private static boolean hasOrdinaryRealmAccess(ServerPlayer player, LivingEntity target) {
		ResourceKey<Level> targetDimension = target.level().dimension();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (isLightRealm(targetDimension)) {
			return !SkillSystem.hasDarknessTag(player) && data.skillLevel() >= SkillSystem.MAX_LEVEL;
		}
		if (SkillSystem.isDarkRealm(targetDimension)) {
			return SkillSystem.hasDarknessTag(player)
					&& data.darknessLevel() >= SkillSystem.DARKNESS_MAX_LEVEL;
		}
		return true;
	}

	private static void retaliate(ServerPlayer player, ServerLevel level, Vec3 position, String messageKey,
			int messageVariants) {
		PowerFx.cancelled(level, position, 0xFF8C6FD8);
		PowerFx.coloredBurst(level, position, 0xFF4B2E50, 26, 1.0);
		PowerFx.burst(level, position, com.powers.PowersParticles.ECLIPSE, 14, 0.5, 0.05);
		PowerFx.sound(level, position, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.8f);
		player.addEffect(PowerStatusEffects.hidden(MobEffects.NAUSEA, 400, 0, false, true));
		PowerMessages.send(player, messageKey, messageVariants);
	}

	private static void cast(ServerPlayer player, ServerLevel level, Vec3 position, LivingEntity target,
			boolean trueSight) {
		PowerFx.sound(level, position, SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		PowerFx.rune(level, position, 2.2, CELESTIAL_COLOR, 26, 0.0);
		PowerFx.spiral(level, position.add(0, 0.1, 0), 0.7, 3.4, CELESTIAL_COLOR, 20, 0.0);
		PowerFx.burst(level, position, com.powers.PowersParticles.GLYPH, 24, 0.6, 0.04);
		if (trueSight) PowerFx.trueSightPiercing(level, position);

		MinecraftServer server = level.getServer();
		UUID ownerId = player.getUUID();
		ResourceKey<Level> ritualDimension = level.dimension();
		PowersMod.scheduleDelayed(server, 16, ownerId, ritualDimension, ownerId,
				"locator_swell", (current, task) -> swellRitual(current, task.subjectId(),
						ritualDimension, position));
		PowersMod.scheduleDelayed(server, 32, ownerId, ritualDimension, ownerId,
				"locator_heavens", (current, task) -> openHeavens(current, task.subjectId(),
						ritualDimension, position));
		TargetRef targetRef = new TargetRef(target.getUUID(), target.level().dimension());
		PowersMod.scheduleDelayed(server, 48, ownerId, ritualDimension, ownerId,
				"locator_reveal", (current, task) -> revealTarget(current, task.subjectId(),
						ritualDimension, position, targetRef, trueSight));
	}

	private static void swellRitual(MinecraftServer server, UUID ownerId,
			ResourceKey<Level> dimension, Vec3 position) {
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		ServerLevel level = server.getLevel(dimension);
		if (!ritualOwnerValid(player, level)) return;
		PowerFx.ring(level, position, 4.2, CELESTIAL_COLOR, 34, 0.4);
		PowerFx.ring(level, position, 2.8, 0xFFE8F4FF, 26, 1.1);
		PowerFx.sound(level, position, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.25f);
	}

	private static void openHeavens(MinecraftServer server, UUID ownerId,
			ResourceKey<Level> dimension, Vec3 position) {
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		ServerLevel level = server.getLevel(dimension);
		if (!ritualOwnerValid(player, level)) return;
		PowerFx.beam(level, position, position.add(0, 36, 0),
				PowerFx.dust(CELESTIAL_COLOR, 1.25F), 18);
		PowerFx.burst(level, position.add(0, 0.2, 0), com.powers.PowersParticles.GLYPH, 18, 0.4, 0.05);
		PowerFx.sound(level, position, SoundEvents.CONDUIT_ACTIVATE, 1.0f, 1.15f);
	}

	private static void revealTarget(MinecraftServer server, UUID ownerId,
			ResourceKey<Level> dimension, Vec3 position, TargetRef targetRef, boolean trueSight) {
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		ServerLevel level = server.getLevel(dimension);
		if (!ritualOwnerValid(player, level)) return;
		ServerLevel targetLevel = server.getLevel(targetRef.dimension());
		LivingEntity target = targetLevel == null ? null
				: targetLevel.getEntity(targetRef.id()) instanceof LivingEntity living ? living : null;
		if (target == null || !target.isAlive()) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.view_lost"));
			return;
		}
		PowerFx.rune(level, position, 3.0, GOLD_COLOR, 34, Math.PI);
		PowerFx.coloredBurst(level, position.add(0, 1.2, 0), GOLD_COLOR, 40, 1.6);
		PowerFx.burst(level, position.add(0, 1.2, 0), com.powers.PowersParticles.GLYPH, 26, 1.2, 0.06);
		PowerFx.sound(level, position, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.4f);

		if (target.isAlive() && !target.isRemoved()) {
			PowerFx.coloredBurst(targetLevel, target.position().add(0, 1, 0), 0xFFFFFFFF, 10, 0.6);
			PowerFx.burst(targetLevel, target.position().add(0, 1, 0), com.powers.PowersParticles.GLYPH, 8, 0.4, 0.04);
			if (trueSight) PowerFx.trueSightPiercing(targetLevel, target.position().add(0, 1, 0));
		}

		Vec3 targetPosition = target.position();
		PowerMessages.sendImportant(player, "grimoire.celestial.reveal", 3,
				target.getName().getString());
		if (trueSight) PowerMessages.sendImportant(player, "grimoire.celestial.true_sight", 3);
		player.sendSystemMessage(Component.literal("Dimension: ")
				.append(Component.literal(dimensionName(target.level().dimension()))
						.withStyle(style -> style.withColor(GOLD_COLOR))));
		player.sendSystemMessage(Component.literal("Coordinates: ")
				.append(Component.literal((int) Math.floor(targetPosition.x) + " "
						+ (int) Math.floor(targetPosition.y) + " " + (int) Math.floor(targetPosition.z))
						.withStyle(style -> style.withColor(GOLD_COLOR).withBold(true))));
		if (DreamwalkingAbility.beginRemoteView(player, target, 20 * 60, CastSource.SPELL)) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.view_started",
					target.getName()));
		}
	}

	private static boolean ritualOwnerValid(ServerPlayer player, ServerLevel level) {
		return player != null && player.isAlive() && !player.isRemoved()
				&& player.level() == level && MagicUseGate.ongoingAllowed(player);
	}

	private static NamedTargetRules.Resolution<LivingEntity> findNamedTarget(
			MinecraftServer server, String requestedName) {
		return NamedLivingTargetIndex.resolve(server, requestedName);
	}

	private static boolean isLightRealm(ResourceKey<Level> dimension) {
		return dimension.identifier().equals(PowersMod.id("light_realm"));
	}

	private static String dimensionName(ResourceKey<Level> dimension) {
		return switch (dimension.identifier().getPath()) {
			case "overworld" -> "The Overworld";
			case "the_nether" -> "The Nether";
			case "the_end" -> "The End";
			case "dark_realm" -> "The Dark Realm";
			case "light_realm" -> "The Light Realm";
			case "middleworld" -> "The Middleworld";
			default -> dimension.identifier().toString();
		};
	}
}
