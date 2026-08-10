package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.TimeStopFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.FreezeOwner;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	private record ActiveStop(long endsAt, Set<UUID> frozen, double presentationRadius) {
	}

	public ChronoStopAbility() {
		super(PowersMod.id("chrono_stop"),
				Component.translatable("ability.powers.chrono_stop"),
				COOLDOWN_TICKS, false, false);
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
		for (Entity entity : BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Entity.class), area, 1_024,
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
				level.getGameTime() + scaledDuration(player, DURATION_TICKS),
				Set.copyOf(frozen), radius));
		TimeStopFx.begin(level, player.position(), radius, true);
		PowerMessages.sendImportant(player, "crystal.powers.chrono_start", 3);
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
			// Ten-tick pulse is legible without filling the owner's first-person view.
			if (left % 10 == 0) {
				TimeStopFx.sustain(ownerLevel, owner.position(),
						stop.presentationRadius(), left, true);
			}

			if (left <= 0) {
				// time's up - restore everything and let the world move again
				EntityFreezeController.release(FreezeOwner.token("chrono_stop", entry.getKey()), stop.frozen());
				it.remove();
				TimeStopFx.release(ownerLevel, owner.position(), stop.presentationRadius(), true);
				PowerMessages.sendImportant(owner, "crystal.powers.chrono_end", 3);
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
