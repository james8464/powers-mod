package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TimeFreezeToggleAbility extends ToggleAbility {
	private static final double RADIUS = 48.0;
	private static final Map<UUID, Set<Mob>> OWNER_MOBS = new HashMap<>();
	private static final Map<Mob, Boolean> ORIGINAL_AI = new HashMap<>();

	public TimeFreezeToggleAbility() {
		super(PowersMod.id("time_freeze"),
				Component.translatable("ability.powers.time_freeze"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		freezeNearby(player);
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		releaseOwner(player.getUUID());
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		freezeNearby(player);
	}

	private void freezeNearby(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		AABB area = AABB.ofSize(player.position(), RADIUS * 2, RADIUS * 2, RADIUS * 2);
		Set<Mob> current = new HashSet<>();
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), area,
				e -> e.isAlive())) {
			current.add(mob);
			ORIGINAL_AI.putIfAbsent(mob, mob.isNoAi());
			mob.setNoAi(true);
		}
		Set<Mob> previous = OWNER_MOBS.put(player.getUUID(), current);
		if (previous != null) {
			previous.removeAll(current);
			restoreUnowned(previous);
		}
	}

	private static void releaseOwner(UUID owner) {
		Set<Mob> mobs = OWNER_MOBS.remove(owner);
		if (mobs != null) {
			restoreUnowned(mobs);
		}
	}

	public static void clear(UUID owner) {
		releaseOwner(owner);
	}

	public static void clearAll() {
		for (UUID owner : new HashSet<>(OWNER_MOBS.keySet())) {
			releaseOwner(owner);
		}
	}

	private static void restoreUnowned(Set<Mob> mobs) {
		for (Mob mob : mobs) {
			boolean owned = OWNER_MOBS.values().stream().anyMatch(set -> set.contains(mob));
			if (!owned) {
				Boolean original = ORIGINAL_AI.remove(mob);
				if (original != null && !mob.isRemoved()) mob.setNoAi(original);
			}
		}
	}
}
