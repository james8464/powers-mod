package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.power.MagicUseGate;
import com.powers.power.travel.MindscapeMobReturnTracker;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelCohort;
import com.powers.power.travel.TravelCohortRules;
import com.powers.power.travel.TravelKind;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared, consent-free group journey for all fixed-destination realm crystals. */
public abstract class MindscapeCrystalAbility extends Ability {
	private static final int STORM_TICKS = 80;
	private static final int TELEPORT_DELAY = 30;

	private final ResourceKey<Level> destination;
	private final PowersMod.StormTheme departureTheme;
	private final int color;
	private final float pitch;
	private record Journey(LivingEntity subject, Vec3 arrival) { }

	protected MindscapeCrystalAbility(String actionId, String realmId, PowersMod.StormTheme departureTheme,
			int color, float pitch) {
		this(actionId, ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
				PowersMod.id(realmId)), departureTheme, color, pitch);
	}

	/** Test/compatibility constructor for a server-advertised destination key. */
	protected MindscapeCrystalAbility(String actionId, ResourceKey<Level> destination,
			PowersMod.StormTheme departureTheme, int color, float pitch) {
		super(PowersMod.id(actionId), Component.translatable("ability.powers." + actionId),
				2400, false, false);
		this.destination = destination;
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
		if (BodyProxyManager.hasSession(caster, BodyProxyKind.REALM)) return returnNearby(caster);
		ServerLevel destinationLevel = ((ServerLevel) caster.level()).getServer().getLevel(destination);
		if (destinationLevel == null) {
			PowerMessages.sendImportant(caster, "ability.powers.realm_unavailable", 3,
					destination.identifier().toString());
			return false;
		}

		ServerLevel sourceLevel = (ServerLevel) caster.level();
		TravelCohort.Snapshot cohort = TravelCohort.capture(sourceLevel, caster, caster);
		List<LivingEntity> subjects = travellers(cohort);
		for (LivingEntity subject : subjects) {
			PowerFx.beam(sourceLevel, caster.getEyePosition(), subject.getEyePosition(), particle(), 12);
		}
		return beginJourney(caster, subjects, sourceLevel, destinationLevel, data,
				CastScalingContext.currentSource());
	}

	private boolean returnNearby(ServerPlayer caster) {
		TravelCohort.Snapshot cohort = TravelCohort.capture((ServerLevel) caster.level(), caster, caster);
		for (TravelCohort.Member member : cohort.companions()) {
			LivingEntity subject = member.entity();
			if (subject instanceof ServerPlayer player
					&& BodyProxyManager.hasSession(player, BodyProxyKind.REALM)) {
				BodyProxyManager.returnToBody(player);
			} else if (!(subject instanceof ShadowCompanionEntity)
					&& MindscapeMobReturnTracker.tracked(subject)) {
				MindscapeMobReturnTracker.returnToOrigin(subject);
			}
		}
		return BodyProxyManager.returnToBody(caster);
	}

	private boolean beginJourney(ServerPlayer caster, List<LivingEntity> subjects,
			ServerLevel sourceLevel, ServerLevel destinationLevel,
			PlayerPowers.PlayerPowersData data, CastSource castSource) {
		if (subjects.isEmpty()) return false;
		Vec3 requested = new Vec3(8.5,
				com.powers.realm.RealmTerrain.provisionalArrivalY(destinationLevel), 8.5);
		for (LivingEntity subject : subjects) {
			SafeDestinationResolver.Result preflight = SafeDestinationResolver.validatePreload(
					subject, destinationLevel, requested, TravelKind.CRYSTAL);
			if (!preflight.allowed()) {
				PowerMessages.overlay(caster, Component.translatable(
						"ability.powers.realm_route_blocked",
						preflight.failure().name().toLowerCase(java.util.Locale.ROOT)));
				return false;
			}
		}
		AsyncAbilityTransaction transaction = new AsyncAbilityTransaction(caster, data, this);
		PowerMessages.overlay(caster, Component.translatable("ability.powers.realm_focusing",
				destination.identifier().toString()));
		return TravelChunkLoader.request(caster.getUUID(), destinationLevel, BlockPos.containing(requested),
				"mindscape_crystal",
				() -> startJourney(caster, subjects, sourceLevel, destinationLevel, requested,
						castSource, transaction),
				() -> {
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_load_timeout"));
				});
	}

	private void startJourney(ServerPlayer caster, List<LivingEntity> subjects,
			ServerLevel sourceLevel, ServerLevel destinationLevel, Vec3 requested,
			CastSource castSource, AsyncAbilityTransaction transaction) {
		List<Vec3> arrivals = findArrivals(subjects, destinationLevel, requested);
		if (arrivals.size() != subjects.size()) {
			transaction.fail();
			PowerMessages.send(caster, "ability.powers.no_room", 3);
			return;
		}
		Vec3 sourceOrigin = caster.position();
		if (eligibilityFailure(caster, caster, sourceOrigin, sourceLevel, destinationLevel,
				arrivals.getFirst(), castSource) != null) {
			transaction.fail();
			PowerMessages.overlay(caster, Component.translatable(
					"ability.powers.realm_journey_interrupted"));
			return;
		}

		PowersMod.startStorm(sourceLevel, sourceOrigin, STORM_TICKS, departureTheme);
		PowersMod.startStorm(destinationLevel, arrivals.getFirst(), STORM_TICKS);
		PowerFx.rune(sourceLevel, sourceOrigin, 2.2, color, 36, 0.0);
		PowerFx.spiral(sourceLevel, sourceOrigin, 1.6, 2.8, color, 30, Math.PI / 8);
		PowersMod.scheduleDelayed(sourceLevel.getServer(), TELEPORT_DELAY, () -> {
			String casterFailure = eligibilityFailure(caster, caster, sourceOrigin, sourceLevel,
					destinationLevel, arrivals.getFirst(), castSource);
			if (casterFailure != null) {
				PowersMod.LOGGER.warn("Mindscape group journey interrupted: caster={}, reason={}",
						caster.getUUID(), casterFailure);
				transaction.fail();
				PowerMessages.overlay(caster, Component.translatable(
						"ability.powers.realm_journey_interrupted"));
				return;
			}
			List<Journey> journeys = eligibleJourneys(caster, subjects, arrivals, sourceOrigin,
					sourceLevel, destinationLevel, castSource);
			List<ServerPlayer> startedPlayers = new ArrayList<>();
			List<LivingEntity> trackedMobs = new ArrayList<>();
			for (Journey journey : journeys) {
				LivingEntity subject = journey.subject();
				if (subject instanceof ServerPlayer player) {
					if (!BodyProxyManager.start(player, BodyProxyKind.REALM)) {
						rollback(startedPlayers, trackedMobs);
						transaction.fail();
						PowerMessages.overlay(caster, Component.translatable(
								"ability.powers.realm_body_occupied"));
						return;
					}
					startedPlayers.add(player);
				} else if (MindscapeMobReturnTracker.track(subject)) {
					trackedMobs.add(subject);
				}
			}

			for (Journey journey : journeys) {
				LivingEntity subject = journey.subject();
				Vec3 arrival = journey.arrival();
				Entity moved = move(subject, destinationLevel, arrival);
				if (!(moved instanceof LivingEntity living) || living.level() != destinationLevel) {
					rollback(startedPlayers, trackedMobs);
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_journey_interrupted"));
					return;
				}
				if (living instanceof ServerPlayer player) PowersPackets.syncTo(player);
				PowerFx.rune(destinationLevel, arrival, 2.2, color, 36, Math.PI);
				PowerFx.spiral(destinationLevel, arrival, 1.6, 2.8, color, 30, Math.PI);
			}
			transaction.succeed();
		});
	}

	private Entity move(LivingEntity subject, ServerLevel destinationLevel, Vec3 arrival) {
		if (subject instanceof ShadowCompanionEntity shadow) {
			return PrivateCompanionManager.travelBody(shadow, destinationLevel, arrival)
					? PrivateCompanionManager.body(shadow.ownerId()).orElse(null) : null;
		}
		return subject.teleport(new TeleportTransition(destinationLevel, arrival, Vec3.ZERO,
				subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
	}

	private static void rollback(List<ServerPlayer> players, List<LivingEntity> mobs) {
		for (ServerPlayer player : players) {
			if (!BodyProxyManager.recoverToBody(player)) BodyProxyManager.finish(player);
		}
		for (LivingEntity mob : mobs) MindscapeMobReturnTracker.returnToOrigin(mob);
	}

	private List<Journey> eligibleJourneys(ServerPlayer caster, List<LivingEntity> subjects,
			List<Vec3> arrivals, Vec3 sourceOrigin,
			ServerLevel sourceLevel, ServerLevel destinationLevel,
			CastSource castSource) {
		List<Journey> eligible = new ArrayList<>(subjects.size());
		for (int index = 0; index < subjects.size(); index++) {
			String failure = eligibilityFailure(caster, subjects.get(index), sourceOrigin, sourceLevel,
					destinationLevel, arrivals.get(index), castSource);
			if (failure != null) {
				PowersMod.LOGGER.info("Mindscape companion skipped: caster={}, subject={}, reason={}",
						caster.getUUID(), subjects.get(index).getUUID(), failure);
				continue;
			}
			eligible.add(new Journey(subjects.get(index), arrivals.get(index)));
		}
		return List.copyOf(eligible);
	}

	private String eligibilityFailure(ServerPlayer caster, LivingEntity subject, Vec3 sourceOrigin,
			ServerLevel sourceLevel, ServerLevel destinationLevel, Vec3 destinationPosition,
			CastSource castSource) {
		if (subject.isRemoved() || caster.isRemoved() || !subject.isAlive() || !caster.isAlive()
				|| !MagicUseGate.ongoingAllowed(caster)
				|| !ServerCastLifecycle.mayContinue(caster, castSource, false)
				|| subject.level() != sourceLevel || caster.level() != sourceLevel
				|| AmethystDampening.isDampened(caster)) return "caster_or_source_invalid";
		if (subject instanceof ServerPlayer player
				&& (!MagicUseGate.ongoingAllowed(player)
				|| sourceLevel.getServer().getPlayerList().getPlayer(player.getUUID()) != player)) {
			return "player_unavailable";
		}
		if (!(subject instanceof ShadowCompanionEntity) && AmethystDampening.isDampened(subject)) {
			return "amethyst";
		}
		if (subject != caster && !(subject instanceof ShadowCompanionEntity)
				&& !TravelCohortRules.mayCommit(true, true,
				subject.position().distanceToSqr(sourceOrigin))) return "left_cohort";
		SafeDestinationResolver.Result destination = SafeDestinationResolver.validate(
				subject, destinationLevel, destinationPosition, TravelKind.CRYSTAL);
		return destination.allowed() ? null : "destination_" + destination.failure().name().toLowerCase();
	}

	private static List<LivingEntity> travellers(TravelCohort.Snapshot cohort) {
		List<LivingEntity> travellers = new ArrayList<>(cohort.companions().size() + 1);
		travellers.add(cohort.principal());
		for (TravelCohort.Member member : cohort.companions()) travellers.add(member.entity());
		return List.copyOf(travellers);
	}

	private static List<Vec3> findArrivals(List<LivingEntity> subjects, ServerLevel level,
			Vec3 requested) {
		List<Vec3> arrivals = new ArrayList<>();
		Set<BlockPos> reserved = new HashSet<>();
		for (LivingEntity subject : subjects) {
			Vec3 arrival = findArrival(subject, level, requested, reserved);
			if (arrival == null) return List.of();
			arrivals.add(arrival);
			reserved.add(BlockPos.containing(arrival));
		}
		return List.copyOf(arrivals);
	}

	private static Vec3 findArrival(LivingEntity subject, ServerLevel level, Vec3 requested,
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
				if (SafeDestinationResolver.validate(subject, level, candidate,
						TravelKind.CRYSTAL).allowed()) return candidate;
			}
		}
		return null;
	}

	private DustParticleOptions particle() {
		return PowerFx.dust(color, 1.2F);
	}
}
