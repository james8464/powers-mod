package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chrono Stop: the crystal-tier time stop. Freezes every entity in every
 * loaded dimension - no movement, no gravity, no AI - while the caster moves
 * freely. Everything resumes exactly where it was when time flows again.
 * A power that wins fights outright, never given out by the rainbow.
 */
public class ChronoStopAbility extends Ability {
	public static final int DURATION_TICKS = 600;

	private static final int COOLDOWN_TICKS = 3600;
	private static final Map<UUID, ActiveStop> ACTIVE = new HashMap<>();

	private record FrozenState(Entity entity, Vec3 pos, Vec3 delta, float yRot, float xRot,
			boolean noGravity, boolean noAi, double fallDistance) {
	}

	private record ActiveStop(int ticksLeft, List<FrozenState> frozen) {
	}

	public ChronoStopAbility() {
		super(PowersMod.id("chrono_stop"),
				Component.translatable("ability.powers.chrono_stop"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// Only one stop at a time: a second freeze while the world is already
		// frozen would corrupt the restore states of the first.
		if (!ACTIVE.isEmpty()) {
			PowerMessages.send(player, "crystal.powers.chrono_blocked", 3);
			return false;
		}

		List<FrozenState> frozen = new ArrayList<>();
		MinecraftServer server = player.level().getServer();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class),
					e -> e.isAlive() && e != player)) {
				if (entity == player.getVehicle() || player.getPassengers().contains(entity)) {
					continue;
				}
				frozen.add(new FrozenState(entity, entity.position(), entity.getDeltaMovement(),
						entity.getYRot(), entity.getXRot(), entity.isNoGravity(),
						entity instanceof Mob mob && mob.isNoAi(), entity.fallDistance));
			}
		}

		ACTIVE.put(player.getUUID(), new ActiveStop(DURATION_TICKS, frozen));
		ServerLevel level = (ServerLevel) player.level();
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

	/** Called every server tick; freezes everything and counts down. */
	public static void tickStops(MinecraftServer server) {
		var it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveStop> entry = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			if (owner == null || !owner.isAlive()) {
				release(null, entry.getValue());
				it.remove();
				continue;
			}
			ActiveStop stop = entry.getValue();

			for (FrozenState f : stop.frozen()) {
				Entity entity = f.entity();
				if (entity.isRemoved()) {
					continue;
				}
				entity.setDeltaMovement(Vec3.ZERO);
				entity.setNoGravity(true);
				entity.setPos(f.pos().x, f.pos().y, f.pos().z);
				if (entity instanceof Mob mob) {
					mob.setNoAi(true);
				}
				if (entity instanceof ServerPlayer other) {
					other.connection.teleport(f.pos().x, f.pos().y, f.pos().z, f.yRot(), f.xRot());
				}
			}
			ServerLevel ownerLevel = (ServerLevel) owner.level();
			if (stop.ticksLeft() % 5 == 0) {
				double phase = stop.ticksLeft() * 0.035;
				PowerFx.ring(ownerLevel, owner.position().add(0, 0.1, 0), 4.5, 0x2962FF, 32, phase);
				PowerFx.ring(ownerLevel, owner.position().add(0, 2.1, 0), 4.5, 0x2962FF, 32, -phase);
				PowerFx.burst(ownerLevel, owner.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 5, 1.8, 0.01);
			}

			int left = stop.ticksLeft() - 1;
			if (left <= 0) {
				release(owner, stop);
				it.remove();
			} else {
				entry.setValue(new ActiveStop(left, stop.frozen()));
			}
		}
	}

	private static void release(ServerPlayer owner, ActiveStop stop) {
		for (FrozenState f : stop.frozen()) {
			Entity entity = f.entity();
			if (entity.isRemoved()) {
				continue;
			}
			entity.setNoGravity(f.noGravity());
			entity.setDeltaMovement(f.delta());
			entity.fallDistance = f.fallDistance();
			if (entity instanceof Mob mob) {
				mob.setNoAi(f.noAi());
			}
		}
		if (owner != null && owner.level() instanceof ServerLevel level) {
			PowerFx.coloredBurst(level, owner.position().add(0, 1, 0), 0x2962FF, 16, 0.8);
			PowerFx.sound(level, owner.position(), SoundEvents.TOTEM_USE, 0.8f, 1.4f);
			PowerMessages.send(owner, "crystal.powers.chrono_end", 3);
		}
	}

	public static void clear(UUID player) {
		ActiveStop stop = ACTIVE.remove(player);
		if (stop != null) {
			release(null, stop);
		}
	}

	public static void clearAll() {
		for (ActiveStop stop : ACTIVE.values()) {
			release(null, stop);
		}
		ACTIVE.clear();
	}
}
