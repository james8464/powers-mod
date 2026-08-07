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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpaceTimeAbility extends Ability {
	private static final int DURATION = 120;
	private static final Map<UUID, ActiveFreeze> FROZEN = new HashMap<>();
	private static final Map<UUID, Integer> MODES = new HashMap<>();

	private record Frozen(Entity entity, Vec3 position, Vec3 velocity, boolean noGravity,
			boolean noAi, double fallDistance) {}
	private record ActiveFreeze(List<Frozen> states, long endsAt) {}

	public SpaceTimeAbility() {
		super(PowersMod.id("space_time"), Component.translatable("ability.powers.space_time"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (player.isCrouching()) {
			int mode = (MODES.getOrDefault(player.getUUID(), 0) + 1) % 3;
			MODES.put(player.getUUID(), mode);
			PowerMessages.send(player, "ability.powers.space_time_mode", 3, modeNameFor(mode));
			return true;
		}
		ServerLevel level = (ServerLevel) player.level();
		int mode = MODES.getOrDefault(player.getUUID(), 0);
		if (mode == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION, 2, false, false));
		} else if (mode == 1) {
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, DURATION, 1, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, 1, false, false));
		} else {
			List<Frozen> frozen = new ArrayList<>();
			for (ServerLevel world : level.getServer().getAllLevels()) {
				for (Entity entity : world.getEntities(EntityTypeTest.forClass(Entity.class),
						e -> e.isAlive() && e != player)) {
					frozen.add(new Frozen(entity, entity.position(), entity.getDeltaMovement(), entity.isNoGravity(),
							entity instanceof Mob mob && mob.isNoAi(), entity.fallDistance));
				}
			}
			// The caster is never frozen: they must be able to move and
			// deactivate the freeze, or the ability would trap them.
			FROZEN.put(player.getUUID(), new ActiveFreeze(frozen,
					level.getServer().getTickCount() + DURATION));
		}
		com.powers.fx.PowerFx.ring(level, player.position(), 5.0, 0x00BCD4, 32, 0);
		com.powers.fx.PowerFx.spiral(level, player.position(), 3.0, 2.5, 0x00BCD4, 28, 0);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, 1.0f, mode == 2 ? 0.35f : 1.4f);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		for (var it = FROZEN.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			UUID ownerId = entry.getKey();
			ActiveFreeze active = entry.getValue();
			List<Frozen> states = active.states();
			ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
			if (server.getTickCount() >= active.endsAt() || owner == null || !owner.isAlive()) {
				release(states);
				it.remove();
				continue;
			}
			for (Frozen frozen : states) {
				Entity entity = frozen.entity();
				if (entity.isRemoved()) continue;
				entity.setDeltaMovement(Vec3.ZERO);
				entity.setNoGravity(true);
				entity.setPos(frozen.position().x, frozen.position().y, frozen.position().z);
				if (entity instanceof Mob mob) mob.setNoAi(true);
			}
			if (server.getTickCount() % 5 == 0 && owner.level() instanceof ServerLevel level) {
				com.powers.fx.PowerFx.ring(level, owner.position(), 5.0, 0x00BCD4, 32,
						server.getTickCount() * 0.04);
			}
		}
	}

	public static boolean isFrozen(ServerPlayer player) {
		return FROZEN.values().stream().anyMatch(active ->
				active.states().stream().anyMatch(state -> state.entity() == player));
	}

	/**
	 * Feedback when a player frozen by space-time tries to act: the frozen
	 * moment pushes back with a cold chime and frost sparks, and a reminder
	 * that time itself is holding them still.
	 */
	public static void reject(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.burst(level, pos, ParticleTypes.END_ROD, 12, 0.5, 0.2);
		PowerFx.coloredBurst(level, pos, 0xBFEFFF, 16, 0.6);
		PowerFx.sound(level, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.6f);
		PowerMessages.send(player, "ability.powers.frozen", 4);
	}

	private static void release(List<Frozen> states) {
		for (Frozen frozen : states) {
			Entity entity = frozen.entity();
			if (entity.isRemoved()) continue;
			entity.setNoGravity(frozen.noGravity());
			entity.setDeltaMovement(frozen.velocity());
			entity.fallDistance = frozen.fallDistance();
			if (entity instanceof Mob mob) mob.setNoAi(frozen.noAi());
		}
	}

	public static void clear(UUID player) {
		ActiveFreeze active = FROZEN.remove(player);
		if (active != null) {
			release(active.states());
		}
		MODES.remove(player);
	}

	public static void clearAll() {
		for (ActiveFreeze active : FROZEN.values()) {
			release(active.states());
		}
		FROZEN.clear();
		MODES.clear();
	}

	private static String modeNameFor(int mode) {
		return switch (mode) {
			case 0 -> "Slow";
			case 1 -> "Accelerate";
			default -> "Freeze";
		};
	}
}
