package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.power.MagicUseGate;
import com.powers.power.PowerTargeting;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
import com.powers.network.PowersPackets;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared, server-authoritative journey logic for the opposed mindscape crystals. */
abstract class MindscapeCrystalAbility extends Ability {
	private static final double BASE_REACH = 48.0;
	private static final int BASE_STORM_TICKS = 80;
	private static final int BASE_TELEPORT_DELAY = 30;
	private static final double GROUP_RADIUS = 2.0;
	private static final int MAX_GROUP_SIZE = 16;

	private final ResourceKey<Level> destination;
	private final PowersMod.StormTheme departureTheme;
	private final int color;
	private final float pitch;

	MindscapeCrystalAbility(String actionId, String realmId, PowersMod.StormTheme departureTheme,
			int color, float pitch) {
		super(PowersMod.id(actionId), Component.translatable("ability.powers." + actionId),
				2400, false, false);
		this.destination = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
				PowersMod.id(realmId));
		this.departureTheme = departureTheme;
		this.color = color;
		this.pitch = pitch;
	}

	@Override
	public final boolean isSelectionAction(ServerPlayer player) {
		return BodyProxyManager.hasSession(player, BodyProxyKind.REALM);
	}

	@Override
	public final boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		if (BodyProxyManager.hasSession(caster, BodyProxyKind.REALM)) {
			return BodyProxyManager.returnToBody(caster);
		}
		ServerLevel destinationLevel = ((ServerLevel) caster.level()).getServer().getLevel(destination);
		if (destinationLevel == null) {
			PowerMessages.sendImportant(caster, "ability.powers.realm_unavailable", 3,
					destination.identifier().toString());
			return false;
		}

		ServerLevel level = (ServerLevel) caster.level();
		Vec3 origin = caster.getEyePosition();
		double reach = scaledRange(caster, BASE_REACH);
		LivingEntity target = caster.isCrouching() ? null : PowerTargeting.findLivingTarget(caster, reach);
		boolean aimedPlayer = target instanceof ServerPlayer;
		if (caster.isCrouching()) {
			List<ServerPlayer> group = nearbyGroup(caster);
			for (ServerPlayer subject : group) {
				PowerFx.beam(level, origin, subject.getEyePosition(), particle(), 12);
			}
			return beginJourney(caster, group, destinationLevel, data,
					CastScalingContext.currentSource());
		}
		if (CrystalTargeting.journeyTarget(caster.isCrouching(), aimedPlayer)
				== CrystalTargeting.JourneyTarget.CASTER) {
			return beginJourney(caster, List.of(caster), destinationLevel, data,
					CastScalingContext.currentSource());
		}
		if (target instanceof ServerPlayer subject) {
			if (!PowerProtection.mayForceMove(caster, subject)) {
				com.powers.knowledge.MagicAttemptReporter.failure(caster, id().getPath(),
						com.powers.knowledge.MagicFailureReason.CONSENT);
				PowerMessages.sendImportant(caster, "powers.packet.consent_denied", 1,
						subject.getName().getString());
				return false;
			}
			if (AmethystDampening.isDampened(subject)) {
				com.powers.knowledge.MagicAttemptReporter.failure(caster, id().getPath(),
						com.powers.knowledge.MagicFailureReason.AMETHYST);
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			PowerFx.beam(level, origin, subject.getEyePosition(), particle(), 18);
			PowerFx.rune(level, subject.position(), 1.35, color, 24, 0.0);
			PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, pitch);
			return beginJourney(caster, List.of(subject), destinationLevel, data,
					CastScalingContext.currentSource());
		}

		return false;
	}

	private boolean beginJourney(ServerPlayer caster, List<ServerPlayer> subjects,
			ServerLevel destinationLevel,
			PlayerPowers.PlayerPowersData data, CastSource castSource) {
		if (subjects.isEmpty()) return false;
		Vec3 destinationPosition = new Vec3(8.5,
				com.powers.realm.RealmTerrain.provisionalArrivalY(destinationLevel), 8.5);
		ServerLevel sourceLevel = (ServerLevel) caster.level();
		for (ServerPlayer subject : subjects) {
			SafeDestinationResolver.Result preflight = SafeDestinationResolver.validatePreload(
					subject, destinationLevel, destinationPosition, TravelKind.CRYSTAL);
			if (!preflight.allowed()) {
				PowerMessages.overlay(caster, Component.translatable(
						"ability.powers.realm_route_blocked",
						preflight.failure().name().toLowerCase(java.util.Locale.ROOT)));
				return false;
			}
		}
		double permittedSeparation = scaledRange(caster, BASE_REACH) + 4.0;
		int stormTicks = scaledDuration(caster, BASE_STORM_TICKS);
		int delay = Math.max(12, (int) Math.round(BASE_TELEPORT_DELAY
				/ Math.max(1.0, scaling(caster).durationMultiplier())));
		AsyncAbilityTransaction transaction = new AsyncAbilityTransaction(caster, data, this);
		PowerMessages.overlay(caster, Component.translatable("ability.powers.realm_focusing",
				destination.identifier().toString()));
		return TravelChunkLoader.request(caster.getUUID(), destinationLevel, BlockPos.containing(destinationPosition),
				"mindscape_crystal",
				() -> startJourney(caster, subjects, sourceLevel, destinationLevel,
						destinationPosition, permittedSeparation, castSource,
						stormTicks, delay, transaction),
				() -> {
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_load_timeout"));
				});
	}

	private void startJourney(ServerPlayer caster, List<ServerPlayer> subjects, ServerLevel sourceLevel,
			ServerLevel destinationLevel, Vec3 requestedPosition, double permittedSeparation,
			CastSource castSource, int stormTicks, int delay,
			AsyncAbilityTransaction transaction) {
		List<Vec3> destinationPositions = findArrivals(subjects, destinationLevel, requestedPosition);
		if (destinationPositions.size() != subjects.size()) {
			transaction.fail();
			PowerMessages.send(caster, "ability.powers.no_room", 3);
			return;
		}
		for (int index = 0; index < subjects.size(); index++) {
			if (!stillEligible(caster, subjects.get(index), sourceLevel, destinationLevel,
					destinationPositions.get(index), permittedSeparation, castSource)) {
				transaction.fail();
				PowerMessages.overlay(caster, Component.translatable(
						"ability.powers.realm_journey_interrupted"));
				return;
			}
		}
		Vec3 sourcePosition = caster.position();
		PowersMod.startStorm(sourceLevel, sourcePosition, stormTicks, departureTheme);
		PowersMod.startStorm(destinationLevel, destinationPositions.getFirst(), stormTicks);
		PowerFx.rune(sourceLevel, sourcePosition, 2.2, color, 36, 0.0);
		PowerFx.spiral(sourceLevel, sourcePosition, 1.6, 2.8, color, 30, Math.PI / 8);

		PowersMod.scheduleDelayed(sourceLevel.getServer(), delay, () -> {
			for (int index = 0; index < subjects.size(); index++) {
				if (!stillEligible(caster, subjects.get(index), sourceLevel, destinationLevel,
						destinationPositions.get(index), permittedSeparation, castSource)) {
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_journey_interrupted"));
					return;
				}
			}
			List<ServerPlayer> started = new ArrayList<>();
			for (ServerPlayer subject : subjects) {
				if (!BodyProxyManager.start(subject, BodyProxyKind.REALM)) {
					for (ServerPlayer rollback : started) BodyProxyManager.finish(rollback);
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_body_occupied"));
					return;
				}
				started.add(subject);
			}
			for (int index = 0; index < subjects.size(); index++) {
				ServerPlayer subject = subjects.get(index);
				Vec3 destinationPosition = destinationPositions.get(index);
				subject.teleport(new TeleportTransition(destinationLevel, destinationPosition, Vec3.ZERO,
						subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
				if (subject.level() != destinationLevel) {
					for (ServerPlayer rollback : started) {
						if (rollback.level() == destinationLevel) BodyProxyManager.recoverToBody(rollback);
						else BodyProxyManager.finish(rollback);
					}
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_journey_interrupted"));
					return;
				}
				PowersPackets.syncTo(subject);
				PowerFx.rune(destinationLevel, destinationPosition, 2.2, color, 36, Math.PI);
				PowerFx.spiral(destinationLevel, destinationPosition, 1.6, 2.8, color, 30, Math.PI);
			}
			transaction.succeed();
		});
	}

	private List<ServerPlayer> nearbyGroup(ServerPlayer caster) {
		List<ServerPlayer> group = new ArrayList<>();
		group.add(caster);
		for (ServerPlayer subject : caster.level().getServer().getPlayerList().getPlayers()) {
			if (group.size() >= MAX_GROUP_SIZE) break;
			if (subject == caster || subject.level() != caster.level() || !subject.isAlive()
					|| !CrystalTargeting.withinRadius(subject.distanceToSqr(caster), GROUP_RADIUS)
					|| AmethystDampening.isDampened(subject)
					|| !PowerProtection.mayForceMove(caster, subject)) continue;
			group.add(subject);
		}
		return List.copyOf(group);
	}

	private boolean stillEligible(ServerPlayer caster, ServerPlayer subject, ServerLevel sourceLevel,
			ServerLevel destinationLevel, Vec3 destinationPosition, double permittedSeparation,
			CastSource castSource) {
		if (subject.isRemoved() || caster.isRemoved() || !subject.isAlive() || !caster.isAlive()
				|| !MagicUseGate.ongoingAllowed(caster) || !MagicUseGate.ongoingAllowed(subject)
				|| !ServerCastLifecycle.mayContinue(caster, castSource, false)
				|| subject.level() != sourceLevel || caster.level() != sourceLevel
				|| sourceLevel.getServer().getPlayerList().getPlayer(subject.getUUID()) != subject
				|| AmethystDampening.isDampened(caster) || AmethystDampening.isDampened(subject)
				|| !PowerProtection.mayForceMove(caster, subject)) return false;
		if (caster != subject && caster.distanceToSqr(subject) > permittedSeparation * permittedSeparation) {
			return false;
		}
		return destinationAllowed(subject, destinationLevel, destinationPosition);
	}

	private static boolean destinationAllowed(ServerPlayer subject, ServerLevel level, Vec3 position) {
		return SafeDestinationResolver.validate(subject, level, position, TravelKind.CRYSTAL).allowed();
	}

	private static List<Vec3> findArrivals(List<ServerPlayer> subjects, ServerLevel level, Vec3 requested) {
		List<Vec3> arrivals = new ArrayList<>();
		Set<BlockPos> reserved = new HashSet<>();
		for (ServerPlayer subject : subjects) {
			Vec3 arrival = findArrival(subject, level, requested, reserved);
			if (arrival == null) return List.of();
			arrivals.add(arrival);
			reserved.add(BlockPos.containing(arrival));
		}
		return List.copyOf(arrivals);
	}

	private static Vec3 findArrival(ServerPlayer subject, ServerLevel level, Vec3 requested,
			Set<BlockPos> reserved) {
		BlockPos origin = BlockPos.containing(requested);
		for (MindscapeArrivalRules.Offset offset : MindscapeArrivalRules.horizontalOffsets()) {
			int x = origin.getX() + offset.x();
			int z = origin.getZ() + offset.z();
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			for (int dy = 0; dy <= 3; dy++) {
				BlockPos feet = new BlockPos(x, surface + dy, z);
				if (reserved.contains(feet)) continue;
				Vec3 candidate = Vec3.atBottomCenterOf(feet);
				if (destinationAllowed(subject, level, candidate)) return candidate;
			}
		}
		return null;
	}

	private DustParticleOptions particle() {
		return PowerFx.dust(color, 1.2F);
	}
}
