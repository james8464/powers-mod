package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.state.PowerEntityState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.powers.entity.EchoClone;
import net.minecraft.world.phys.Vec3;

/**
 * cloning - the orange crystal's power of creation turned inward: tear three
 * unarmed player-shaped echoes out of the air. Each copy owns its finite
 * lifetime, avoiding delayed tasks or persistent summon maps.
 */
public class CloneSwarmAbility extends Ability {
	// The authored ninety-second cooldown bounds repeated entity creation.
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
		int cloneCount = CLONE_COUNT;
		int spawned = 0;
		for (int i = 0; i < cloneCount; i++) {
			EchoClone clone = com.powers.PowersEntities.ECHO_CLONE.create(
					level, EntitySpawnReason.TRIGGERED);
			if (clone == null) {
				continue;
			}
			clone.configure(player, scaledDuration(player, CLONE_LIFE_TICKS));
			PowerEntityState.markBanishableSummon(clone);
			// Fixed combat baselines keep each temporary clone equivalent before cast scaling.
			clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80.0 * potency);
			clone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18.0 * potency);
			clone.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
			clone.setHealth(clone.getMaxHealth());
			if (!placeAroundCaster(level, player, clone, i, cloneCount)
					|| !level.addFreshEntity(clone)) {
				clone.discard();
				continue;
			}
			spawned++;
			PowerFx.coloredBurst(level, clone.position().add(0, 1, 0), 0xFF6D00, 12, 0.6);
		}
		if (spawned == 0) {
			// Refuse payment when collision prevents every clone from entering the world.
			return false;
		}
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFF6D00, 30, 1.5);
		PowerFx.burst(level, player.position().add(0, 1, 0),
				PowerFx.dust(0xFF8A3D, 1.1F), 22, 1.0, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.2f);
		return true;
	}

	/** Finds one of a small fixed set of collision-free points without scanning terrain. */
	private static boolean placeAroundCaster(ServerLevel level, ServerPlayer player,
			EchoClone clone, int index, int cloneCount) {
		double baseAngle = Math.PI * 2 * index / cloneCount;
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = baseAngle + Math.PI * 2 * attempt / 8.0;
			double radius = 1.5 + 0.5 * (attempt / 4);
			clone.setPos(player.getX() + Math.cos(angle) * radius, player.getY() + 0.2,
					player.getZ() + Math.sin(angle) * radius);
			if (level.noBlockCollision(clone, clone.getBoundingBox())) return true;
		}
		return false;
	}
}
