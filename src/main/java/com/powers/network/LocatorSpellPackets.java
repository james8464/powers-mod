package com.powers.network;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.item.GrimoireItem;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.progression.PowerScalingService;
import com.powers.progression.RankVariantRules;
import com.powers.protection.PowerProtection;
import com.powers.spell.GrimoireDefinition;
import com.powers.spell.SpellCastingManager;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import java.util.List;
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
	private record TargetRef(UUID id, ResourceKey<Level> dimension) { }

	private LocatorSpellPackets() {
	}

	static void open(ServerPlayer player) {
		UUID nonce = NONCES.issue(player.getUUID(), player.level().getServer().getTickCount());
		ServerPlayNetworking.send(player, new PowersPackets.OpenLocatorScreenPayload(nonce));
	}

	static void handleLocate(PowersPackets.LocateTargetPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			if (PacketRateLimiter.allow(context.player(), PacketRateLimiter.Lane.LOCATOR)) {
				locate(context.player(), payload, context.server().getTickCount());
			}
		});
	}

	private static void locate(ServerPlayer player, PowersPackets.LocateTargetPayload payload, long currentTick) {
		if (!NONCES.consume(player.getUUID(), payload.nonce(), currentTick) || !holdsCelestialGrimoire(player)) return;
		if (payload.targetName().isBlank() || payload.targetName().length() > 64) return;
		if (SpaceTimeAbility.isFrozen(player)) {
			SpaceTimeAbility.reject(player);
			return;
		}

		NamedTargetRules.Resolution<LivingEntity> resolution = findNamedTarget(
				player.level().getServer(), payload.targetName());
		if (resolution.status() == NamedTargetRules.Status.AMBIGUOUS) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.ambiguous"));
			return;
		}
		if (resolution.status() == NamedTargetRules.Status.SCAN_LIMIT) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.scan_limit"));
			return;
		}
		LivingEntity target = resolution.target();
		if (target == null) {
			PowerMessages.send(player, "grimoire.celestial.offline", 3);
			return;
		}
		if (target instanceof ServerPlayer targetPlayer
				&& !PowerProtection.mayLocate(player, targetPlayer)) {
			PowerMessages.sendImportant(player, "grimoire.celestial.consent_denied", 1);
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		Vec3 castPosition = player.position().add(0, 1, 0);
		AmethystDampening.update(player);
		if (AmethystDampening.isDampened(player)) {
			retaliate(player, level, castPosition, "grimoire.celestial.amethyst", 3);
			return;
		}
		boolean ordinaryVeilAccess = hasOrdinaryRealmAccess(player, target);
		boolean trueSight = PowerScalingService.hasVariant(player, "true_sight");
		if (!RankVariantRules.mayPierceRealmVeil(ordinaryVeilAccess, trueSight)) {
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
		PowerFx.burst(level, position, ParticleTypes.REVERSE_PORTAL, 14, 0.5, 0.05);
		PowerFx.sound(level, position, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.8f);
		player.addEffect(PowerStatusEffects.hidden(MobEffects.NAUSEA, 400, 0, false, true));
		PowerMessages.send(player, messageKey, messageVariants);
	}

	private static void cast(ServerPlayer player, ServerLevel level, Vec3 position, LivingEntity target,
			boolean trueSight) {
		PowerFx.sound(level, position, SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		PowerFx.rune(level, position, 2.2, CELESTIAL_COLOR, 26, 0.0);
		PowerFx.spiral(level, position.add(0, 0.1, 0), 0.7, 3.4, CELESTIAL_COLOR, 20, 0.0);
		PowerFx.burst(level, position, ParticleTypes.END_ROD, 24, 0.6, 0.04);
		if (trueSight) PowerFx.trueSightPiercing(level, position);

		MinecraftServer server = level.getServer();
		PowersMod.scheduleDelayed(server, 16, () -> swellRitual(player, level, position));
		PowersMod.scheduleDelayed(server, 32, () -> openHeavens(player, level, position));
		TargetRef targetRef = new TargetRef(target.getUUID(), target.level().dimension());
		PowersMod.scheduleDelayed(server, 48, () -> revealTarget(player, level, position,
				targetRef, trueSight));
	}

	private static void swellRitual(ServerPlayer player, ServerLevel level, Vec3 position) {
		if (player.isRemoved()) return;
		PowerFx.ring(level, position, 4.2, CELESTIAL_COLOR, 34, 0.4);
		PowerFx.ring(level, position, 2.8, 0xFFE8F4FF, 26, 1.1);
		PowerFx.sound(level, position, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.25f);
	}

	private static void openHeavens(ServerPlayer player, ServerLevel level, Vec3 position) {
		if (player.isRemoved()) return;
		PowerFx.beam(level, position, position.add(0, 36, 0),
				PowerFx.dust(CELESTIAL_COLOR, 1.25F), 18);
		PowerFx.burst(level, position.add(0, 0.2, 0), ParticleTypes.END_ROD, 18, 0.4, 0.05);
		PowerFx.sound(level, position, SoundEvents.CONDUIT_ACTIVATE, 1.0f, 1.15f);
	}

	private static void revealTarget(ServerPlayer player, ServerLevel level, Vec3 position, TargetRef targetRef,
			boolean trueSight) {
		if (player.isRemoved()) return;
		ServerLevel targetLevel = level.getServer().getLevel(targetRef.dimension());
		LivingEntity target = targetLevel == null ? null
				: targetLevel.getEntity(targetRef.id()) instanceof LivingEntity living ? living : null;
		if (target == null || !target.isAlive()) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.view_lost"));
			return;
		}
		PowerFx.rune(level, position, 3.0, GOLD_COLOR, 34, Math.PI);
		PowerFx.coloredBurst(level, position.add(0, 1.2, 0), GOLD_COLOR, 40, 1.6);
		PowerFx.burst(level, position.add(0, 1.2, 0), ParticleTypes.END_ROD, 26, 1.2, 0.06);
		PowerFx.sound(level, position, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.4f);

		if (target.isAlive() && !target.isRemoved()) {
			PowerFx.coloredBurst(targetLevel, target.position().add(0, 1, 0), 0xFFFFFFFF, 10, 0.6);
			PowerFx.burst(targetLevel, target.position().add(0, 1, 0), ParticleTypes.END_ROD, 8, 0.4, 0.04);
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
		if (DreamwalkingAbility.beginRemoteView(player, target, 20 * 60)) {
			PowerMessages.overlay(player, Component.translatable("grimoire.celestial.view_started",
					target.getName()));
		}
	}

	private static NamedTargetRules.Resolution<LivingEntity> findNamedTarget(
			MinecraftServer server, String requestedName) {
		// Only matching candidates are retained and the scan stops at two: the
		// second match is enough to refuse ambiguity without building a global list.
		List<NamedTargetRules.Candidate<LivingEntity>> matches = new ArrayList<>(2);
		for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
			addMatch(matches, requestedName, candidate.getName().getString(), candidate);
			if (matches.size() == 2) return NamedTargetRules.resolve(requestedName, matches);
		}
		NamedLivingTargetIndex.appendMatches(server, requestedName, matches);
		return NamedTargetRules.resolve(requestedName, matches);
	}

	private static void addMatch(List<NamedTargetRules.Candidate<LivingEntity>> matches,
			String requestedName, String candidateName, LivingEntity target) {
		if (NamedTargetRules.matches(requestedName, candidateName)) {
			matches.add(new NamedTargetRules.Candidate<>(target, candidateName));
		}
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
