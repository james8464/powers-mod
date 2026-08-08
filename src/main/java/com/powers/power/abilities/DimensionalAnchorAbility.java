package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
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

	public static boolean isAnchored(ServerPlayer player) {
		return anchorDimension(player) != null;
	}

	public static ResourceKey<Level> anchorDimension(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		PlayerPowers.AnchorState anchor = data.dimensionalAnchor();
		if (anchor == null) return null;
		if (anchor.expiresAt() <= player.level().getGameTime()) {
			data.clearDimensionalAnchor();
			return null;
		}
		Identifier id = Identifier.tryParse(anchor.dimensionId());
		if (id == null) {
			data.clearDimensionalAnchor();
			return null;
		}
		return ResourceKey.create(Registries.DIMENSION, id);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(player, 32.0);
		if (!(target instanceof ServerPlayer targetSP)) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}

		return apply(player, targetSP);
	}

	/** Shared by the Deep Grimoire; the former random power now delegates here. */
	public static boolean apply(ServerPlayer player, ServerPlayer targetSP) {
		if (AmethystDampening.isDampened(targetSP)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		ResourceKey<Level> dim = targetSP.level().dimension();
		String dimName = dim.identifier().getPath();
		PlayerPowers.get(targetSP).setDimensionalAnchor(dim.identifier().toString(),
				targetSP.level().getGameTime() + ANCHOR_TICKS);

		ServerLevel targetLevel = (ServerLevel) targetSP.level();
		PowerFx.rune(targetLevel, targetSP.position().add(0, 1.0, 0), 1.6, 0x8A2BE2, 20, 0.5);
		PowerFx.burst(targetLevel, targetSP.position().add(0, 1.5, 0),
				ParticleTypes.END_ROD, 12, 0.75, 0.05);
		PowerFx.sound(targetLevel, targetSP.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.1f);

		PowerMessages.send(targetSP, "ability.powers.anchored", 3, dimName);
		PowerMessages.send(player, "ability.powers.anchor_applied", 3,
				targetSP.getName().getString(), dimName);

		return true;
	}
}
