package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Telekinesis: psychically seize everything around you, yank it into the air
 * and fling it over your head - the way telekinetic heroes (Scarlet Witch)
 * throw enemies in superhero mods.
 */
public class TelekinesisAbility extends Ability {
	public TelekinesisAbility() {
		super(PowersMod.id("telekinesis"),
				Component.translatable("ability.powers.telekinesis"),
				240, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		AABB area = AABB.ofSize(player.position(), 16.0, 12.0, 16.0);
		Vec3 center = player.position();
		for (LivingEntity target : level.getEntities(
				EntityTypeTest.forClass(LivingEntity.class), area,
				e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e))) {
			Vec3 toward = center.subtract(target.position());
			double horizontal = toward.horizontalDistance();
			if (horizontal < 0.01) {
				continue;
			}
			Vec3 fling = toward.multiply(1, 0, 1).normalize().scale(2.2).add(0, 0.7, 0);
			target.setDeltaMovement(target.getDeltaMovement().add(fling));
			target.hurtMarked = true;
			com.powers.fx.PowerFx.beam(level, center.add(0, 1.2, 0), target.position().add(0, 1, 0),
					net.minecraft.core.particles.ParticleTypes.ENCHANT, 8);
			com.powers.fx.PowerFx.coloredBurst(level, target.position().add(0, 1, 0), 0x9C27B0, 6, 0.4);
		}
		com.powers.fx.PowerFx.coloredBurst(level, center.add(0, 1.2, 0), 0x9C27B0, 20, 0.8);
		com.powers.fx.PowerFx.sound(level, center, net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		return true;
	}
}
