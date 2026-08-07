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

/**
 * time freeze - a toggle that stops every mob within 48 blocks cold: no ai,
 * no gravity, no motion, while you move freely and pick them off
 */
public class TimeFreezeToggleAbility extends ToggleAbility {
	// freeze reach, 48 blocks in every direction
	private static final double RADIUS = 48.0;
	// per-owner set of mobs currently frozen, plus each mob's original ai and gravity so they can be restored exactly
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
		// toggle off - hand back every mob this player froze
		releaseOwner(player.getUUID());
		PowerFx.sound(level, player.position(), SoundEvents.NOTE_BLOCK_BELL.value(), 0.85f, 1.3f);
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// keep re-freezing as mobs wander into range, and unfreeze any that walked out
		freezeNearby(player);
		ServerLevel level = (ServerLevel) player.level();
		long tick = level.getServer().getTickCount();
		// pulse a ring every 10 ticks so it's obvious the freeze is still up
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
			// remember the original state only once - restoring over a later overwrite would lose it
			ORIGINAL_AI.putIfAbsent(mob, mob.isNoAi());
			ORIGINAL_GRAVITY.putIfAbsent(mob, mob.isNoGravity());
			mob.setNoAi(true);
			mob.setNoGravity(true);
			// stop any motion in progress so they hang frozen mid-air
			mob.setDeltaMovement(Vec3.ZERO);
		}
		Set<Mob> previous = OWNER_MOBS.put(player.getUUID(), current);
		if (previous != null) {
			// mobs that left the freeze box are released unless another owner still freezes them
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

	// called on disconnect so one player's freeze can't outlive them
	public static void clear(UUID owner) {
		releaseOwner(owner);
	}

	// called on server stop so no mob is left frozen forever
	public static void clearAll() {
		for (UUID owner : new HashSet<>(OWNER_MOBS.keySet())) {
			releaseOwner(owner);
		}
	}

	private static void restoreUnowned(Set<Mob> mobs) {
		for (Mob mob : mobs) {
			boolean owned = OWNER_MOBS.values().stream().anyMatch(set -> set.contains(mob));
			// only restore a mob when no other owner still has it in their freeze set
			if (!owned) {
				Boolean originalAi = ORIGINAL_AI.remove(mob);
				Boolean originalGravity = ORIGINAL_GRAVITY.remove(mob);
				// the mob may have been killed while frozen - nothing left to restore
				if (originalAi != null && !mob.isRemoved()) mob.setNoAi(originalAi);
				if (originalGravity != null && !mob.isRemoved()) mob.setNoGravity(originalGravity);
			}
		}
	}
}
