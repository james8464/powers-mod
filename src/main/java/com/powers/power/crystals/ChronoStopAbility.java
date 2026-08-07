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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chrono Stop: the crystal-tier time stop. Freezes every entity in every
 * loaded dimension - no movement, no gravity, no AI - while the caster moves
 * freely. Everything resumes exactly where it was when time flows again.
 * A power that wins fights outright, never given out by the rainbow.
 */
public class ChronoStopAbility extends Ability {
	public static final int DURATION_TICKS = 600;

	private static final int COOLDOWN_TICKS = 3600;
	private static final Map<ServerPlayer, ActiveStop> ACTIVE = new HashMap<>();
	private static final Set<Entity> FROZEN = new HashSet<>();

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
		if (ACTIVE.containsKey(player)) {
			return false;
		}

		List<FrozenState> frozen = new ArrayList<>();
		MinecraftServer server = player.level().getServer();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class),
					e -> e.isAlive() && e != player && !FROZEN.contains(e))) {
				if (entity == player.getVehicle() || player.getPassengers().contains(entity)) {
					continue;
				}
				FROZEN.add(entity);
				frozen.add(new FrozenState(entity, entity.position(), entity.getDeltaMovement(),
						entity.getYRot(), entity.getXRot(), entity.isNoGravity(),
						entity instanceof Mob mob && mob.isNoAi(), entity.fallDistance));
			}
		}

		ACTIVE.put(player, new ActiveStop(DURATION_TICKS, frozen));
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
	public static void tickStops() {
		var it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ServerPlayer, ActiveStop> entry = it.next();
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
			if (stop.ticksLeft() % 5 == 0 && entry.getKey().level() instanceof ServerLevel level) {
				double phase = stop.ticksLeft() * 0.035;
				PowerFx.ring(level, entry.getKey().position().add(0, 0.1, 0), 4.5, 0x2962FF, 32, phase);
				PowerFx.ring(level, entry.getKey().position().add(0, 2.1, 0), 4.5, 0x2962FF, 32, -phase);
				PowerFx.burst(level, entry.getKey().position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 5, 1.8, 0.01);
			}

			int left = stop.ticksLeft() - 1;
			if (left <= 0) {
				release(entry.getKey(), stop);
				it.remove();
			} else {
				entry.setValue(new ActiveStop(left, stop.frozen()));
			}
		}
	}

	private static void release(ServerPlayer owner, ActiveStop stop) {
		for (FrozenState f : stop.frozen()) {
			Entity entity = f.entity();
			FROZEN.remove(entity);
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
		if (owner.level() instanceof ServerLevel level) {
			PowerFx.coloredBurst(level, owner.position().add(0, 1, 0), 0x2962FF, 16, 0.8);
			PowerFx.sound(level, owner.position(), SoundEvents.TOTEM_USE, 0.8f, 1.4f);
		}
		PowerMessages.send(owner, "crystal.powers.chrono_end", 3);
	}
}
