package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityArithmetic;
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

/**
 * Space-Time: the crystal that bends the moment. Sneak-right-click cycles
 * between slow, accelerate and freeze modes; a normal right-click applies
 * the chosen mode to the world around you
 */
public class SpaceTimeAbility extends Ability {
	// the freeze holds the world for 120 ticks = 6 seconds
	private static final int DURATION = 120;
	// every entity's saved state while a freeze is live, per caster
	private static final Map<UUID, ActiveFreeze> FROZEN = new HashMap<>();
	// per-player mode, 0 slow, 1 accelerate, 2 freeze
	private static final Map<UUID, Integer> MODES = new HashMap<>();

	private record Frozen(Entity entity, Vec3 position, Vec3 velocity, boolean noGravity,
			boolean noAi, double fallDistance) {}
	private record ActiveFreeze(List<Frozen> states, long endsAt) {}

	public SpaceTimeAbility() {
		super(PowersMod.id("space_time"), Component.translatable("ability.powers.space_time"), 1200, false);
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return player.isCrouching();
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (player.isCrouching()) {
			// sneak-right-click steps 0 -> 1 -> 2 -> 0 to pick the next mode
			int mode = AbilityArithmetic.nextMode(MODES.getOrDefault(player.getUUID(), 0), 3);
			MODES.put(player.getUUID(), mode);
			PowerMessages.send(player, "ability.powers.space_time_mode", 3, modeNameFor(mode));
			return true;
		}
		ServerLevel level = (ServerLevel) player.level();
		int mode = MODES.getOrDefault(player.getUUID(), 0);
		if (mode == 0) {
			// slow: 120 ticks of slowness, the moment drags around you
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION, 2, false, false));
		} else if (mode == 1) {
			// accelerate: 120 ticks of speed, hunger the price of outrunning time
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, DURATION, 1, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, 1, false, false));
		} else {
			// snapshot position, motion, gravity, ai and fall distance so release can restore it all
			List<Frozen> frozen = new ArrayList<>();
			for (ServerLevel world : level.getServer().getAllLevels()) {
				for (Entity entity : world.getEntities(EntityTypeTest.forClass(Entity.class),
						e -> e.isAlive() && e != player)) {
					frozen.add(new Frozen(entity, entity.position(), entity.getDeltaMovement(), entity.isNoGravity(),
							entity instanceof Mob mob && mob.isNoAi(), entity.fallDistance));
				}
			}
			// the caster is never frozen, or they couldn't move to end the freeze
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
			// 6 seconds up, or the caster logged off or died: release everyone and drop the state
			if (server.getTickCount() >= active.endsAt() || owner == null || !owner.isAlive()) {
				release(states);
				it.remove();
				continue;
			}
			for (Frozen frozen : states) {
				Entity entity = frozen.entity();
				// already gone, nothing left to hold
				if (entity.isRemoved()) continue;
				entity.setDeltaMovement(Vec3.ZERO);
				entity.setNoGravity(true);
				entity.setPos(frozen.position().x, frozen.position().y, frozen.position().z);
				if (entity instanceof Mob mob) mob.setNoAi(true);
			}
			// pulse the ring every 5 ticks so the freeze looks alive
			if (server.getTickCount() % 5 == 0 && owner.level() instanceof ServerLevel level) {
				com.powers.fx.PowerFx.ring(level, owner.position(), 5.0, 0x00BCD4, 32,
						server.getTickCount() * 0.04);
			}
		}
	}

	/** whether this player is currently held by someone's freeze */
	public static boolean isFrozen(ServerPlayer player) {
		return FROZEN.values().stream().anyMatch(active ->
				active.states().stream().anyMatch(state -> state.entity() == player));
	}

	/**
	 * feedback when a frozen player tries to act: the frozen moment pushes
	 * back with a cold chime and frost sparks, plus a reminder that time
	 * itself holds them still
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
		// hand everything back the way the freeze found it
		for (Frozen frozen : states) {
			Entity entity = frozen.entity();
			if (entity.isRemoved()) continue;
			entity.setNoGravity(frozen.noGravity());
			entity.setDeltaMovement(frozen.velocity());
			entity.fallDistance = frozen.fallDistance();
			if (entity instanceof Mob mob) mob.setNoAi(frozen.noAi());
		}
	}

	/** undo one caster's freeze on disconnect, releasing their captives */
	public static void clear(UUID player) {
		ActiveFreeze active = FROZEN.remove(player);
		if (active != null) {
			release(active.states());
		}
		MODES.remove(player);
	}

	/** release every frozen entity and wipe all modes on server stop */
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
