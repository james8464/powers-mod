package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class SpaceTimeAbility extends Ability {
	private static final int DURATION = 120;
	private static final Map<ServerPlayer, ActiveFreeze> FROZEN = new HashMap<>();
	private static final Map<java.util.UUID, Integer> MODES = new HashMap<>();

	private record Frozen(Entity entity, Vec3 position, Vec3 velocity, boolean noGravity, boolean noAi) {}
	private record ActiveFreeze(List<Frozen> states, long endsAt) {}

	public SpaceTimeAbility() {
		super(PowersMod.id("space_time"), Component.translatable("ability.powers.space_time"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (player.isCrouching()) {
			int mode = (MODES.getOrDefault(player.getUUID(), 0) + 1) % 3;
			MODES.put(player.getUUID(), mode);
			player.sendSystemMessage(Component.translatable("ability.powers.space_time_mode", modeNameFor(mode)));
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
							entity instanceof Mob mob && mob.isNoAi()));
				}
			}
			frozen.add(new Frozen(player, player.position(), player.getDeltaMovement(), player.isNoGravity(), false));
			FROZEN.put(player, new ActiveFreeze(frozen,
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
			ActiveFreeze active = entry.getValue();
			List<Frozen> states = active.states();
			ServerPlayer owner = entry.getKey();
			if (server.getTickCount() >= active.endsAt()) {
				release(owner, states);
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
			if (server.getTickCount() % 5 == 0) {
				if (owner.level() instanceof ServerLevel level) {
					com.powers.fx.PowerFx.ring(level, owner.position(), 5.0, 0x00BCD4, 32,
							server.getTickCount() * 0.04);
				}
			}
			if (server.getPlayerList().getPlayer(owner.getUUID()) == null) {
				release(owner, states);
				it.remove();
			}
		}
	}

	public static boolean isFrozen(ServerPlayer player) {
		return FROZEN.values().stream().anyMatch(active ->
				active.states().stream().anyMatch(state -> state.entity() == player));
	}

	private static void release(ServerPlayer owner, List<Frozen> states) {
		for (Frozen frozen : states) {
			Entity entity = frozen.entity();
			if (entity.isRemoved()) continue;
			entity.setNoGravity(frozen.noGravity());
			entity.setDeltaMovement(frozen.velocity());
			if (entity instanceof Mob mob) mob.setNoAi(frozen.noAi());
		}
	}

	public static void clearAll() {
		for (var entry : FROZEN.entrySet()) release(entry.getKey(), entry.getValue().states());
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
