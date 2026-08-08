package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.config.PowersConfigLoader;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityArithmetic;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Space-Time: the crystal that bends the moment. Sneak-right-click cycles
 * between slow, accelerate and freeze modes; a normal right-click applies
 * the chosen mode to the world around you
 */
public class SpaceTimeAbility extends Ability {
	// the freeze holds the world for 120 ticks = 6 seconds
	private static final int DURATION = 120;
	private static final Map<UUID, ActiveFreeze> ACTIVE = new HashMap<>();
	// per-player mode, 0 slow, 1 accelerate, 2 freeze
	private static final Map<UUID, Integer> MODES = new HashMap<>();
	private final boolean automaticModeCycle;

	private record ActiveFreeze(Set<UUID> entities, long endsAt) {}

	public SpaceTimeAbility() {
		this(false);
	}

	public SpaceTimeAbility(boolean automaticModeCycle) {
		super(PowersMod.id("space_time"), Component.translatable("ability.powers.space_time"), 1200, false);
		this.automaticModeCycle = automaticModeCycle;
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return !automaticModeCycle && player.isCrouching();
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!automaticModeCycle && player.isCrouching()) {
			// sneak-right-click steps 0 -> 1 -> 2 -> 0 to pick the next mode
			int mode = AbilityArithmetic.nextMode(MODES.getOrDefault(player.getUUID(), 0), 3);
			MODES.put(player.getUUID(), mode);
			PowerMessages.send(player, "ability.powers.space_time_mode", 3, modeNameFor(mode));
			return true;
		}
		ServerLevel level = (ServerLevel) player.level();
		int mode = MODES.getOrDefault(player.getUUID(), 0);
		int duration = scaledDuration(player, DURATION);
		if (mode == 0) {
			// slow: 120 ticks of slowness, the moment drags around you
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 2, false, false));
		} else if (mode == 1) {
			// accelerate: 120 ticks of speed, hunger the price of outrunning time
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, 1, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1, false, false));
		} else {
			if (ACTIVE.containsKey(player.getUUID())) return false;
			UUID freezeOwner = FreezeOwner.token("space_time", player.getUUID());
			double radius = scaledRange(player, PowersConfigLoader.get().spaceTimeRadius());
			Set<UUID> frozen = new LinkedHashSet<>();
			AABB area = AABB.ofSize(player.position().add(0, 1, 0), radius * 2, radius * 2, radius * 2);
			for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class), area,
					e -> e.isAlive() && e != player && e.distanceToSqr(player) <= radius * radius
						&& (!(e instanceof LivingEntity living) || !AmethystDampening.isDampened(living))
						&& !PowerProtection.isSafeZone(level, e.position())
						&& (!(e instanceof ServerPlayer target) || PowerProtection.mayForceMove(player, target)))) {
				UUID entityId = entity.getUUID();
				EntityFreezeController.claim(entity, freezeOwner);
				frozen.add(entityId);
			}
			if (frozen.isEmpty()) return false;
			ACTIVE.put(player.getUUID(), new ActiveFreeze(Set.copyOf(frozen),
					level.getGameTime() + duration));
		}
		com.powers.fx.PowerFx.ring(level, player.position(), 5.0, 0x00BCD4, 32, 0);
		com.powers.fx.PowerFx.spiral(level, player.position(), 3.0, 2.5, 0x00BCD4, 28, 0);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, 1.0f, mode == 2 ? 0.35f : 1.4f);
		if (automaticModeCycle) {
			int next = AbilityArithmetic.nextMode(mode, 3);
			MODES.put(player.getUUID(), next);
			PowerMessages.send(player, "ability.powers.space_time_mode", 3, modeNameFor(next));
		}
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			UUID ownerId = entry.getKey();
			ActiveFreeze active = entry.getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
			// 6 seconds up, or the caster logged off or died: release everyone and drop the state
			if (owner == null || !owner.isAlive() || owner.level().getGameTime() >= active.endsAt()) {
				EntityFreezeController.release(FreezeOwner.token("space_time", ownerId), active.entities());
				it.remove();
				continue;
			}
			// pulse the ring every 5 ticks so the freeze looks alive
			if (server.getTickCount() % 5 == 0 && owner.level() instanceof ServerLevel level) {
				com.powers.fx.PowerFx.ring(level, owner.position(), 5.0, 0x00BCD4, 32,
						server.getTickCount() * 0.04);
			}
		}
		EntityFreezeController.holdAll();
	}

	/** whether this player is currently held by someone's freeze */
	public static boolean isFrozen(ServerPlayer player) {
		return EntityFreezeController.isFrozen(player);
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

	/** undo one caster's freeze on disconnect, releasing their captives */
	public static void clear(UUID player) {
		ActiveFreeze active = ACTIVE.remove(player);
		if (active != null) {
			EntityFreezeController.release(FreezeOwner.token("space_time", player), active.entities());
		}
		MODES.remove(player);
	}

	/** release every frozen entity and wipe all modes on server stop */
	public static void clearAll() {
		for (var entry : ACTIVE.entrySet()) {
			EntityFreezeController.release(FreezeOwner.token("space_time", entry.getKey()), entry.getValue().entities());
		}
		ACTIVE.clear();
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
