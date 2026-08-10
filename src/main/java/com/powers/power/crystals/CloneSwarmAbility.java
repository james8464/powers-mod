package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.state.PowerEntityState;
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
 * cloning - the orange crystal's power of creation turned inward: tear three
 * living wolf copies of yourself out of the air, loyal fighters that double
 * your claws in every battle
 */
public class CloneSwarmAbility extends Ability {
	// 90 seconds between uses
	private static final int COOLDOWN_TICKS = 1800;
	private static final int CLONE_COUNT = 3;
	// clones last 60 seconds (1200 ticks) before poofing away
	private static final int CLONE_LIFE_TICKS = 1200;

	public CloneSwarmAbility() {
		super(PowersMod.id("clone_swarm"),
				Component.translatable("ability.powers.clone_swarm"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		double potency = scaling(player).potencyMultiplier();
		int cloneCount = Math.min(5, CLONE_COUNT + (int) Math.floor((potency - 1.0) * 5.0));
		int spawned = 0;
		for (int i = 0; i < cloneCount; i++) {
			Wolf clone = EntityTypes.WOLF.create(level, EntitySpawnReason.TRIGGERED);
			if (clone == null) {
				continue;
			}
			// spread the clones evenly around the player, 1.5 blocks out
			double angle = Math.PI * 2 * i / cloneCount;
			clone.setPos(player.getX() + Math.cos(angle) * 1.5, player.getY() + 0.2,
					player.getZ() + Math.sin(angle) * 1.5);
			clone.tame(player);
			// standing wolves follow and fight instead of sitting around
			clone.setOrderedToSit(false);
			clone.setCustomName(Component.literal(player.getGameProfile().name() + "'s Clone"));
			clone.setCustomNameVisible(true);
			PowerEntityState.markBanishableSummon(clone);
			// stop them despawning on their own - they go poof on our timer instead
			clone.setPersistenceRequired();
			// tough fighters: 80 health, 18 attack damage, brisk speed
			clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80.0 * potency);
			clone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18.0 * potency);
			clone.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
			clone.setHealth(clone.getMaxHealth());
			if (!level.noBlockCollision(clone, clone.getBoundingBox()) || !level.addFreshEntity(clone)) {
				clone.discard();
				continue;
			}
			spawned++;
			PowerFx.coloredBurst(level, clone.position().add(0, 1, 0), 0xFF6D00, 12, 0.6);

			Wolf endClone = clone;
			// dismiss each clone with a poof after its 60 seconds are up
			PowersMod.scheduleDelayed(level.getServer(), scaledDuration(player, CLONE_LIFE_TICKS), () -> {
				if (endClone.isAlive() && !endClone.isRemoved()) {
					PowerFx.burst((ServerLevel) endClone.level(), endClone.position().add(0, 1, 0),
							ParticleTypes.POOF, 14, 0.7, 0.2);
					endClone.discard();
				}
			});
		}
		if (spawned == 0) {
			// nothing could be summoned - don't charge the player
			return false;
		}
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFF6D00, 30, 1.5);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.ENCHANTED_HIT, 26, 1.0, 0.3);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.2f);
		return true;
	}
}
