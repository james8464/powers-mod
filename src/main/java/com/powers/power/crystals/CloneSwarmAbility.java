package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

/**
 * Cloning: the Orange Crystal's power of creation turned inward. You tear
 * three living copies of yourself out of the air - loyal beasts that fight
 * alongside you, doubling your claws with every battle.
 */
public class CloneSwarmAbility extends Ability {
	private static final int COOLDOWN_TICKS = 1800;
	private static final int CLONE_COUNT = 3;
	private static final int CLONE_LIFE_TICKS = 1200;

	public CloneSwarmAbility() {
		super(PowersMod.id("clone_swarm"),
				Component.translatable("ability.powers.clone_swarm"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		int spawned = 0;
		for (int i = 0; i < CLONE_COUNT; i++) {
			Wolf clone = EntityTypes.WOLF.create(level, EntitySpawnReason.TRIGGERED);
			if (clone == null) {
				continue;
			}
			double angle = Math.PI * 2 * i / CLONE_COUNT;
			clone.setPos(player.getX() + Math.cos(angle) * 1.5, player.getY() + 0.2,
					player.getZ() + Math.sin(angle) * 1.5);
			clone.tame(player);
			clone.setOrderedToSit(false);
			clone.setCustomName(Component.literal(player.getGameProfile().name() + "'s Clone"));
			clone.setCustomNameVisible(true);
			clone.setPersistenceRequired();
			clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80.0);
			clone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18.0);
			clone.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
			clone.setHealth(clone.getMaxHealth());
			level.addFreshEntity(clone);
			spawned++;
			PowerFx.coloredBurst(level, clone.position().add(0, 1, 0), 0xFF6D00, 12, 0.6);

			Wolf endClone = clone;
			PowersMod.scheduleDelayed(level.getServer(), CLONE_LIFE_TICKS, () -> {
				if (endClone.isAlive() && !endClone.isRemoved()) {
					PowerFx.burst((ServerLevel) endClone.level(), endClone.position().add(0, 1, 0),
							ParticleTypes.POOF, 14, 0.7, 0.2);
					endClone.discard();
				}
			});
		}
		if (spawned == 0) {
			return false;
		}
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFF6D00, 30, 1.5);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.ENCHANTED_HIT, 26, 1.0, 0.3);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.2f);
		return true;
	}
}
