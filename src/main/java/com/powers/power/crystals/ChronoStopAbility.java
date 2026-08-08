package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.FreezeOwner;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * chrono stop - the crystal-tier time stop: freeze nearby unprotected entities
 * for 30 seconds (600 ticks) while you move freely, and
 * everything resumes exactly where it was when time flows again - a power
 * that wins fights outright, never given out by the rainbow
 */
public class ChronoStopAbility extends Ability {
	// 30 seconds of frozen time
	public static final int DURATION_TICKS = 600;

	// 3 minutes between stops
	private static final int COOLDOWN_TICKS = 3600;
	// one stop per owner uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, ActiveStop> ACTIVE = new HashMap<>();

	private record ActiveStop(long endsAt, Set<UUID> frozen) {
	}

	public ChronoStopAbility() {
		super(PowersMod.id("chrono_stop"),
				Component.translatable("ability.powers.chrono_stop"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// only one stop at a time - a second freeze while the world is already
		// frozen would corrupt the restore states of the first
		if (!ACTIVE.isEmpty()) {
			PowerMessages.send(player, "crystal.powers.chrono_blocked", 3);
			return false;
		}

		ServerLevel level = (ServerLevel) player.level();
		double radius = scaledRange(player, PowersConfigLoader.get().chronoStopRadius());
		AABB area = AABB.ofSize(player.position().add(0, 1, 0), radius * 2, radius * 2, radius * 2);
		Set<UUID> frozen = new LinkedHashSet<>();
		UUID freezeOwner = FreezeOwner.token("chrono_stop", player.getUUID());
		for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class), area,
				e -> e.isAlive() && e != player && e.distanceToSqr(player) <= radius * radius
						&& e != player.getVehicle() && !player.getPassengers().contains(e)
						&& (!(e instanceof LivingEntity living) || !AmethystDampening.isDampened(living))
						&& !PowerProtection.isSafeZone(level, e.position())
						&& (!(e instanceof ServerPlayer target) || PowerProtection.mayForceMove(player, target)))) {
			EntityFreezeController.claim(entity, freezeOwner);
			frozen.add(entity.getUUID());
		}
		if (frozen.isEmpty()) return false;

		ACTIVE.put(player.getUUID(), new ActiveStop(
				level.getGameTime() + scaledDuration(player, DURATION_TICKS), Set.copyOf(frozen)));
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x2962FF, 28, 1.2);
		PowerFx.ring(level, player.position().add(0, 0.1, 0), 3.5, 0x2962FF, 32, 0);
		PowerFx.ring(level, player.position().add(0, 2.0, 0), 3.5, 0x2962FF, 32, Math.PI);
		PowerFx.spiral(level, player.position(), 2.5, 2.2, 0x2962FF, 24, 0);
		PowerFx.burst(level, player.position().add(0, 1, 0),
				ParticleTypes.TOTEM_OF_UNDYING, 14, 0.9, 0.25);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.5f);
		PowerMessages.send(player, "crystal.powers.chrono_start", 3);
		return true;
	}

	/** Called every server tick - pins every frozen entity in place and counts down the stop. */
	public static void tickStops(MinecraftServer server) {
		var it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveStop> entry = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			if (owner == null || !owner.isAlive()) {
				EntityFreezeController.release(FreezeOwner.token("chrono_stop", entry.getKey()),
						entry.getValue().frozen());
				it.remove();
				continue;
			}
			ActiveStop stop = entry.getValue();
			ServerLevel ownerLevel = (ServerLevel) owner.level();
			long left = Math.max(0L, stop.endsAt() - ownerLevel.getGameTime());
			// pulse a ring every 5 ticks while the stop holds
			if (left % 5 == 0) {
				double phase = left * 0.035;
				PowerFx.ring(ownerLevel, owner.position().add(0, 0.1, 0), 4.5, 0x2962FF, 32, phase);
				PowerFx.ring(ownerLevel, owner.position().add(0, 2.1, 0), 4.5, 0x2962FF, 32, -phase);
				PowerFx.burst(ownerLevel, owner.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 5, 1.8, 0.01);
			}

			if (left <= 0) {
				// time's up - restore everything and let the world move again
				EntityFreezeController.release(FreezeOwner.token("chrono_stop", entry.getKey()), stop.frozen());
				it.remove();
				PowerFx.coloredBurst(ownerLevel, owner.position().add(0, 1, 0), 0x2962FF, 16, 0.8);
				PowerFx.sound(ownerLevel, owner.position(), SoundEvents.TOTEM_USE, 0.8f, 1.4f);
				PowerMessages.send(owner, "crystal.powers.chrono_end", 3);
			}
		}
		EntityFreezeController.holdAll();
	}

	// disconnect - free the frozen entities before the owner leaves
	public static void clear(UUID player) {
		ActiveStop stop = ACTIVE.remove(player);
		if (stop != null) {
			EntityFreezeController.release(FreezeOwner.token("chrono_stop", player), stop.frozen());
		}
	}

	// server stop - never leave entities frozen in a dying world
	public static void clearAll() {
		for (var entry : ACTIVE.entrySet()) {
			EntityFreezeController.release(FreezeOwner.token("chrono_stop", entry.getKey()), entry.getValue().frozen());
		}
		ACTIVE.clear();
	}
}
