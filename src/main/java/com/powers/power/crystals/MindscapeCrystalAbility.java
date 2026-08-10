package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.AsyncAbilityTransaction;
import com.powers.power.PowerTargeting;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
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

/** Shared, server-authoritative journey logic for the opposed mindscape crystals. */
abstract class MindscapeCrystalAbility extends Ability {
	private static final double BASE_REACH = 48.0;
	private static final int BASE_STORM_TICKS = 80;
	private static final int BASE_TELEPORT_DELAY = 30;

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
		if (CrystalTargeting.journeyTarget(caster.isCrouching(), aimedPlayer)
				== CrystalTargeting.JourneyTarget.CASTER) {
			return beginJourney(caster, caster, destinationLevel, data);
		}
		if (target instanceof ServerPlayer subject) {
			if (!PowerProtection.mayForceMove(caster, subject)) {
				PowerMessages.sendImportant(caster, "powers.packet.consent_denied", 1,
						subject.getName().getString());
				return false;
			}
			if (AmethystDampening.isDampened(subject)) {
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			PowerFx.beam(level, origin, subject.getEyePosition(), particle(), 18);
			PowerFx.rune(level, subject.position(), 1.35, color, 24, 0.0);
			PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, pitch);
			return beginJourney(caster, subject, destinationLevel, data);
		}

		return false;
	}

	private boolean beginJourney(ServerPlayer caster, ServerPlayer subject, ServerLevel destinationLevel,
			PlayerPowers.PlayerPowersData data) {
		ServerLevel sourceLevel = (ServerLevel) subject.level();
		Vec3 destinationPosition = new Vec3(8.5, destinationLevel.getMinY() + 1, 8.5);
		SafeDestinationResolver.Result preflight = SafeDestinationResolver.validatePreload(
				subject, destinationLevel, destinationPosition, TravelKind.CRYSTAL);
		if (!preflight.allowed()) {
			PowerMessages.overlay(caster, Component.translatable(
					"ability.powers.realm_route_blocked",
					preflight.failure().name().toLowerCase(java.util.Locale.ROOT)));
			return false;
		}
		double permittedSeparation = scaledRange(caster, BASE_REACH) + 4.0;
		AsyncAbilityTransaction transaction = new AsyncAbilityTransaction(caster, data, this);
		PowerMessages.overlay(caster, Component.translatable("ability.powers.realm_focusing",
				destination.identifier().toString()));
		return TravelChunkLoader.request(caster.getUUID(), destinationLevel, BlockPos.containing(destinationPosition),
				() -> startJourney(caster, subject, sourceLevel, destinationLevel,
						destinationPosition, permittedSeparation, transaction),
				() -> {
					transaction.fail();
					PowerMessages.overlay(caster, Component.translatable(
							"ability.powers.realm_load_timeout"));
				});
	}

	private void startJourney(ServerPlayer caster, ServerPlayer subject, ServerLevel sourceLevel,
			ServerLevel destinationLevel, Vec3 requestedPosition, double permittedSeparation,
			AsyncAbilityTransaction transaction) {
		Vec3 destinationPosition = findArrival(subject, destinationLevel, requestedPosition);
		if (destinationPosition == null) {
			transaction.fail();
			PowerMessages.send(caster, "ability.powers.no_room", 3);
			return;
		}
		if (!stillEligible(caster, subject, sourceLevel, destinationLevel, destinationPosition,
				permittedSeparation)) {
			transaction.fail();
			PowerMessages.overlay(caster, Component.translatable(
					"ability.powers.realm_journey_interrupted"));
			return;
		}
		Vec3 sourcePosition = subject.position();
		int stormTicks = scaledDuration(caster, BASE_STORM_TICKS);
		int delay = Math.max(12, (int) Math.round(BASE_TELEPORT_DELAY
				/ Math.max(1.0, scaling(caster).durationMultiplier())));
		PowersMod.startStorm(sourceLevel, sourcePosition, stormTicks, departureTheme);
		PowersMod.startStorm(destinationLevel, destinationPosition, stormTicks);
		PowerFx.rune(sourceLevel, sourcePosition, 2.2, color, 36, 0.0);
		PowerFx.spiral(sourceLevel, sourcePosition, 1.6, 2.8, color, 30, Math.PI / 8);

		PowersMod.scheduleDelayed(sourceLevel.getServer(), delay, () -> {
			if (!stillEligible(caster, subject, sourceLevel, destinationLevel, destinationPosition,
					permittedSeparation)) {
				transaction.fail();
				PowerMessages.overlay(caster, Component.translatable(
						"ability.powers.realm_journey_interrupted"));
				return;
			}
			if (!BodyProxyManager.start(subject, BodyProxyKind.REALM)) {
				transaction.fail();
				PowerMessages.overlay(caster, Component.translatable(
						"ability.powers.realm_body_occupied"));
				return;
			}
			subject.teleport(new TeleportTransition(destinationLevel, destinationPosition, Vec3.ZERO,
					subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			transaction.succeed();
			PowerFx.rune(destinationLevel, destinationPosition, 2.2, color, 36, Math.PI);
			PowerFx.spiral(destinationLevel, destinationPosition, 1.6, 2.8, color, 30, Math.PI);
		});
	}

	private boolean stillEligible(ServerPlayer caster, ServerPlayer subject, ServerLevel sourceLevel,
			ServerLevel destinationLevel, Vec3 destinationPosition, double permittedSeparation) {
		if (subject.isRemoved() || caster.isRemoved() || !subject.isAlive() || !caster.isAlive()
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

	private static Vec3 findArrival(ServerPlayer subject, ServerLevel level, Vec3 requested) {
		BlockPos origin = BlockPos.containing(requested);
		for (MindscapeArrivalRules.Offset offset : MindscapeArrivalRules.horizontalOffsets()) {
			int x = origin.getX() + offset.x();
			int z = origin.getZ() + offset.z();
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			for (int dy = 0; dy <= 3; dy++) {
				Vec3 candidate = Vec3.atBottomCenterOf(new BlockPos(x, surface + dy, z));
				if (destinationAllowed(subject, level, candidate)) return candidate;
			}
		}
		return null;
	}

	private DustParticleOptions particle() {
		return PowerFx.dust(color, 1.2F);
	}
}
