package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TimeFreezeToggleAbility extends ToggleAbility {
	private static final double RADIUS = 48.0;
	private static final Map<UUID, Set<Mob>> OWNER_MOBS = new HashMap<>();
	private static final Map<Mob, Boolean> ORIGINAL_AI = new HashMap<>();
	private static final Map<Mob, Boolean> ORIGINAL_GRAVITY = new HashMap<>();

	public TimeFreezeToggleAbility() {
		super(PowersMod.id("time_freeze"),
				Component.translatable("ability.powers.time_freeze"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		freezeNearby(player);
		PowerFx.rune(level, player.position().add(0, 0.1, 0), 2.2, 0x96F5FF, 28, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.WITHER_SPAWN, 0.9f, 0.75f);
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		releaseOwner(player.getUUID());
		PowerFx.sound(level, player.position(), SoundEvents.NOTE_BLOCK_BELL.value(), 0.85f, 1.3f);
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		freezeNearby(player);
		ServerLevel level = (ServerLevel) player.level();
		long tick = level.getServer().getTickCount();
		if (tick % 10 == 0) {
			PowerFx.ring(level, player.position().add(0, 0.15, 0), 1.8, 0x96F5FF, 20, tick * 0.08);
			PowerFx.burst(level, player.position().add(0, 1.0, 0), ParticleTypes.SOUL, 4, 0.25, 0.02);
		}
	}

	private void freezeNearby(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		AABB area = AABB.ofSize(player.position(), RADIUS * 2, RADIUS * 2, RADIUS * 2);
		Set<Mob> current = new HashSet<>();
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), area,
				e -> e.isAlive())) {
			current.add(mob);
			ORIGINAL_AI.putIfAbsent(mob, mob.isNoAi());
			ORIGINAL_GRAVITY.putIfAbsent(mob, mob.isNoGravity());
			mob.setNoAi(true);
			mob.setNoGravity(true);
			mob.setDeltaMovement(Vec3.ZERO);
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
				Boolean originalAi = ORIGINAL_AI.remove(mob);
				Boolean originalGravity = ORIGINAL_GRAVITY.remove(mob);
				if (originalAi != null && !mob.isRemoved()) mob.setNoAi(originalAi);
				if (originalGravity != null && !mob.isRemoved()) mob.setNoGravity(originalGravity);
			}
		}
	}
}
