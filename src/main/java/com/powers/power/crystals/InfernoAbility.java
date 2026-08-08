package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * inferno - the red crystal's power: for 8 seconds (160 ticks) the world
 * around you becomes a firestorm - blazing meteors rain down, everything
 * within 12 blocks is set ablaze, and no one inside escapes the flames
 */
public class InfernoAbility extends Ability {
	// 8 seconds of firestorm
	private static final int DURATION_TICKS = 160;
	// 90 seconds between uses
	private static final int COOLDOWN_TICKS = 1800;
	// everything within 12 blocks gets caught in the flames
	private static final int RADIUS = 12;

	// one inferno per owner uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

	public InfernoAbility() {
		super(PowersMod.id("inferno"),
				Component.translatable("ability.powers.inferno"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// no stacking - a second cast while already burning is refused
		if (ACTIVE.containsKey(player.getUUID())) {
			return false;
		}
		ACTIVE.put(player.getUUID(), DURATION_TICKS);
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFF3D00, 30, 1.5);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.FLAME, 40, 1.2, 0.4);
		PowerFx.sound(level, player.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
		return true;
	}

	/** Called every server tick while any inferno is active. */
	public static void tickAll(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			int left = entry.getValue();

			// the owner logged off or died - drop the inferno instead of leaving it burning forever
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			// every 8 ticks (0.4s) the barrage fires
			if (left % 8 == 0) {
				Vec3 origin = player.position().add(0, 1.2, 0);
				for (int i = 0; i < 6; i++) {
					Vec3 impact = origin.add((level.getRandom().nextDouble() - 0.5) * 2 * RADIUS,
							-1.0, (level.getRandom().nextDouble() - 0.5) * 2 * RADIUS);
					PowerFx.beam(level, impact.add(0, 8, 0), impact, ParticleTypes.FLAME, 10);
					PowerFx.burst(level, impact, ParticleTypes.LARGE_SMOKE, 4, 0.4, 0.04);
				}
				// set everything within 12 blocks alight for 8 seconds
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
						AABB.ofSize(origin, RADIUS * 2, 8, RADIUS * 2),
						e -> e.isAlive() && e != player && e.distanceToSqr(player) <= RADIUS * RADIUS
								&& !AmethystDampening.isDampened(e)
								&& PowerProtection.mayHarm(player, e))) {
					target.hurtServer(level, PowerDamage.source(player), 2.0f);
					target.igniteForSeconds(8);
				}
				PowerFx.burst(level, origin, ParticleTypes.FLAME, 20, 2.5, 0.2);
			}

			if (--left <= 0) {
				// time's up - a smoke burst marks the end of the firestorm
				it.remove();
				PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.SMOKE, 24, 1.2, 0.1);
			} else {
				entry.setValue(left);
			}
		}
	}

	// disconnect - stop the firestorm of the player who left
	public static void clear(UUID player) {
		ACTIVE.remove(player);
	}

	// server stop - no inferno should outlive the world
	public static void clearAll() {
		ACTIVE.clear();
	}
}
