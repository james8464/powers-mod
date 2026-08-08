package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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
		super(PowersMod.id(actionId), Component.translatable("ability.powers." + actionId), 2400, false);
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
		if (destinationLevel == null) return false;

		if (caster.isCrouching()) return beginJourney(caster, caster, destinationLevel);

		ServerLevel level = (ServerLevel) caster.level();
		Vec3 origin = caster.getEyePosition();
		double reach = scaledRange(caster, BASE_REACH);
		LivingEntity target = PowerTargeting.findLivingTarget(caster, reach);
		if (target instanceof ServerPlayer subject) {
			if (!PowerProtection.mayForceMove(caster, subject)) {
				PowerMessages.send(caster, "powers.packet.consent_denied", 1, subject.getName().getString());
				return false;
			}
			if (AmethystDampening.isDampened(subject)) {
				PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
				return false;
			}
			PowerFx.beam(level, origin, subject.getEyePosition(), particle(), 18);
			PowerFx.rune(level, subject.position(), 1.35, color, 24, 0.0);
			PowerFx.sound(level, origin, SoundEvents.PORTAL_TRAVEL, 1.0f, pitch);
			return beginJourney(caster, subject, destinationLevel);
		}

		// A miss is a deliberate warning cast and never transports an unseen player.
		Vec3 end = origin.add(caster.getLookAngle().normalize().scale(reach));
		PowerFx.beam(level, origin, end, particle(), 18);
		PowerFx.rune(level, origin.add(caster.getLookAngle().scale(2.0)), 0.7, color, 16, Math.PI / 4);
		PowerFx.sound(level, origin, SoundEvents.PORTAL_TRIGGER, 0.6f, pitch);
		return true;
	}

	private boolean beginJourney(ServerPlayer caster, ServerPlayer subject, ServerLevel destinationLevel) {
		ServerLevel sourceLevel = (ServerLevel) subject.level();
		Vec3 sourcePosition = subject.position();
		Vec3 destinationPosition = new Vec3(8.5, destinationLevel.getMinY() + 1, 8.5);
		// This fixed, trusted realm entrance intentionally loads its single spawn chunk.
		destinationLevel.getChunk(0, 0);
		if (!destinationAllowed(subject, destinationLevel, destinationPosition)) {
			PowerMessages.send(caster, "ability.powers.no_room", 3);
			return false;
		}

		int stormTicks = scaledDuration(caster, BASE_STORM_TICKS);
		int delay = Math.max(12, (int) Math.round(BASE_TELEPORT_DELAY
				/ Math.max(1.0, scaling(caster).durationMultiplier())));
		double permittedSeparation = scaledRange(caster, BASE_REACH) + 4.0;
		PowersMod.startStorm(sourceLevel, sourcePosition, stormTicks, departureTheme);
		PowersMod.startStorm(destinationLevel, destinationPosition, stormTicks);
		PowerFx.rune(sourceLevel, sourcePosition, 2.2, color, 36, 0.0);
		PowerFx.spiral(sourceLevel, sourcePosition, 1.6, 2.8, color, 30, Math.PI / 8);

		PowersMod.scheduleDelayed(sourceLevel.getServer(), delay, () -> {
			if (!stillEligible(caster, subject, sourceLevel, destinationLevel, destinationPosition,
					permittedSeparation)) return;
			if (!BodyProxyManager.start(subject, BodyProxyKind.REALM)) return;
			subject.teleport(new TeleportTransition(destinationLevel, destinationPosition, Vec3.ZERO,
					subject.getYRot(), subject.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			PowerFx.rune(destinationLevel, destinationPosition, 2.2, color, 36, Math.PI);
			PowerFx.spiral(destinationLevel, destinationPosition, 1.6, 2.8, color, 30, Math.PI);
		});
		return true;
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

	private ColorParticleOption particle() {
		return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | color);
	}
}
