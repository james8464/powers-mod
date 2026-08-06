package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DimensionalAnchorAbility extends Ability {
	public static final Map<UUID, ResourceKey<Level>> ANCHORS = new HashMap<>();
	private static final Map<UUID, Long> GENERATIONS = new HashMap<>();
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
		HitResult hit = player.pick(32.0, 0.0f, false);
		if (!(hit instanceof EntityHitResult eHit) || !(eHit.getEntity() instanceof Player target)) {
			player.sendSystemMessage(Component.translatable("ability.powers.no_player_target"));
			return false;
		}

		ServerPlayer targetSP = (ServerPlayer) target;
		if (AmethystDampening.isDampened(targetSP)) {
			player.sendSystemMessage(Component.translatable("amethyst.powers.target_protected"));
			return false;
		}
		ResourceKey<Level> dim = targetSP.level().dimension();
		String dimName = dim.identifier().getPath();
		long generation = GENERATIONS.merge(targetSP.getUUID(), 1L, Long::sum);
		ANCHORS.put(targetSP.getUUID(), dim);

		targetSP.sendSystemMessage(Component.translatable("ability.powers.anchored", dimName));
		player.sendSystemMessage(Component.translatable("ability.powers.anchor_applied",
				targetSP.getName().getString(), dimName));

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
