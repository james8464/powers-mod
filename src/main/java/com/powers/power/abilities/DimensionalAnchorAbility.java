package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.entity.PlayerLikeTarget;
import com.powers.entity.TestActorPowerState;
import com.powers.fx.PowerFx;
import com.powers.mind.PersistentDimensionDiagnostics;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.progression.PowerScalingService;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Applies a temporary dimensional binding used to counter forced travel. */
public class DimensionalAnchorAbility extends Ability {
	// 2 minutes before the anchor fades
	private static final int ANCHOR_TICKS = 2400;

	public DimensionalAnchorAbility() {
		super(PowersMod.id("dimensional_anchor"),
				Component.translatable("ability.powers.dimensional_anchor"),
				1200, false);
	}

	public static boolean isAnchored(LivingEntity target) {
		return anchorDimension(target) != null;
	}

	public static ResourceKey<Level> anchorDimension(LivingEntity target) {
		String dimensionId;
		if (target instanceof ServerPlayer player) {
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			PlayerPowers.AnchorState anchor = data.dimensionalAnchor();
			if (anchor == null) return null;
			if (anchor.expiresAt() <= player.level().getGameTime()) {
				data.clearDimensionalAnchor();
				return null;
			}
			dimensionId = anchor.dimensionId();
		} else if (target instanceof PlayerLikeTarget) {
			dimensionId = TestActorPowerState.anchorDimensionId(
					target.getUUID(), target.level().getGameTime());
			if (dimensionId == null) return null;
		} else {
			return null;
		}
		Identifier id = Identifier.tryParse(dimensionId);
		if (id != null) {
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
			if (target.level().getServer().getLevel(key) != null) return key;
			PersistentDimensionDiagnostics.record("anchor", dimensionId);
		}
		if (target instanceof ServerPlayer player) PlayerPowers.get(player).clearDimensionalAnchor();
		else TestActorPowerState.clearAnchor(target.getUUID());
		return null;
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(player,
				PowerScalingService.range(player, "dimensional_anchor", 32.0));
		if (!PlayerLikeTarget.isCompatible(target)) {
			com.powers.knowledge.MagicAttemptReporter.failure(player, "dimensional_anchor",
					com.powers.knowledge.MagicFailureReason.NO_TARGET);
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}

		return apply(player, target);
	}

	/** Shared by the Deep Grimoire; the former random power now delegates here. */
	public static boolean apply(ServerPlayer player, LivingEntity target) {
		if (!PlayerLikeTarget.isCompatible(target) || AmethystDampening.isDampened(target)
				|| !com.powers.protection.PowerProtection.mayForceMove(player, target)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		ControlResistance.Outcome control = ControlResistance.outcome(target);
		ResourceKey<Level> dim = target.level().dimension();
		String dimName = dim.identifier().getPath();
		int duration = ControlResistance.adjustDuration(
				PowerScalingService.duration(player, "dimensional_anchor", ANCHOR_TICKS), control);
		if (duration <= 0) return false;
		long expiresAt = target.level().getGameTime() + duration;
		if (target instanceof ServerPlayer targetPlayer) {
			PlayerPowers.get(targetPlayer).setDimensionalAnchor(dim.identifier().toString(), expiresAt);
		} else {
			TestActorPowerState.anchor(target.getUUID(), dim.identifier().toString(), expiresAt);
		}

		ServerLevel targetLevel = (ServerLevel) target.level();
		PowerFx.rune(targetLevel, target.position().add(0, 1.0, 0), 1.6, 0x8A2BE2, 20, 0.5);
		PowerFx.burst(targetLevel, target.position().add(0, 1.5, 0),
				com.powers.PowersParticles.GLYPH, 12, 0.75, 0.05);
		PowerFx.sound(targetLevel, target.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.1f);

		if (target instanceof ServerPlayer targetPlayer) {
			PowerMessages.sendImportant(targetPlayer, "ability.powers.anchored", 3, dimName);
		}
		PowerMessages.sendImportant(player, "ability.powers.anchor_applied", 3,
				PlayerLikeTarget.username(target), dimName);

		return true;
	}
}
