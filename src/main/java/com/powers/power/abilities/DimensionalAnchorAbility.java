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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Dimensional Anchor: bind a player in place by dimension so a later recall
// power knows exactly where they're meant to be; the anchor fades after 2 min.
public class DimensionalAnchorAbility extends Ability {
	public static final Map<UUID, ResourceKey<Level>> ANCHORS = new HashMap<>();
	// counts how many times each player has been anchored, to tag expiry tasks
	private static final Map<UUID, Long> GENERATIONS = new HashMap<>();
	// 2 minutes before the anchor fades
	private static final int ANCHOR_TICKS = 2400;

	public DimensionalAnchorAbility() {
		super(PowersMod.id("dimensional_anchor"),
				Component.translatable("ability.powers.dimensional_anchor"),
				1200, false);
	}

	public static boolean isAnchored(ServerPlayer player) {
		return ANCHORS.containsKey(player.getUUID());
	}

	public static ResourceKey<Level> anchorDimension(ServerPlayer player) {
		return ANCHORS.get(player.getUUID());
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(player, 32.0);
		if (!(target instanceof ServerPlayer targetSP)) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}

		if (AmethystDampening.isDampened(targetSP)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		ResourceKey<Level> dim = targetSP.level().dimension();
		String dimName = dim.identifier().getPath();
		// bump the generation so older expiry tasks can tell they're stale
		long generation = GENERATIONS.merge(targetSP.getUUID(), 1L, Long::sum);
		ANCHORS.put(targetSP.getUUID(), dim);

		ServerLevel targetLevel = (ServerLevel) targetSP.level();
		PowerFx.rune(targetLevel, targetSP.position().add(0, 1.0, 0), 1.6, 0x8A2BE2, 20, 0.5);
		PowerFx.burst(targetLevel, targetSP.position().add(0, 1.5, 0),
				ParticleTypes.END_ROD, 12, 0.75, 0.05);
		PowerFx.sound(targetLevel, targetSP.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.1f);

		PowerMessages.send(targetSP, "ability.powers.anchored", 3, dimName);
		PowerMessages.send(player, "ability.powers.anchor_applied", 3,
				targetSP.getName().getString(), dimName);

		// every anchor schedules its own expiry, and the generation check keeps
		// a stale task from releasing a newer anchor on a re-anchored player
		PowersMod.scheduleDelayed(player.level().getServer(), ANCHOR_TICKS, () -> {
			if (GENERATIONS.getOrDefault(targetSP.getUUID(), 0L) == generation) {
				ANCHORS.remove(targetSP.getUUID());
				GENERATIONS.remove(targetSP.getUUID());
			}
		});
		return true;
	}

	public static void clear(UUID player) {
		ANCHORS.remove(player);
		GENERATIONS.remove(player);
	}

	public static void clearAll() {
		ANCHORS.clear();
		GENERATIONS.clear();
	}
}
