package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.ShadowSwordFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;

/** Rank-ten apotheosis aura: extreme self-buffs and a hostile darkness pressure field. */
public final class NightfallDominionAbility extends Ability {
	public NightfallDominionAbility() {
		super(PowersMod.id("nightfall_dominion"),
				Component.translatable("ability.powers.nightfall_dominion"), 0, false, false);
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ShadowSwordFx.dominion((ServerLevel) player.level(), player.position(),
				player.level().getServer().getTickCount(), true);
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ShadowSwordFx.dominion((ServerLevel) player.level(), player.position(),
				player.level().getServer().getTickCount(), false);
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.addEffect(PowerStatusEffects.hidden(MobEffects.STRENGTH, 12, 9, false, true));
		player.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 12, 3, false, true));
		player.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION, 12, 4, false, true));
		player.addEffect(PowerStatusEffects.hidden(MobEffects.FIRE_RESISTANCE, 12, 0, false, true));
		player.addEffect(PowerStatusEffects.hidden(MobEffects.SPEED, 12, 3, false, true));
		ServerLevel level = (ServerLevel) player.level();
		long tick = level.getServer().getTickCount();
		if (tick % 10 == 0) ShadowSwordFx.dominion(level, player.position(), tick, false);
		if (tick % 20 != 0) return;

		AABB bounds = AABB.ofSize(player.position(), 48.0, 24.0, 48.0);
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds, 128,
				candidate -> candidate != player && candidate.isAlive()
						&& !candidate.entityTags().contains(SkillSystem.DARKNESS_TAG)
						&& candidate.distanceToSqr(player) <= 24.0 * 24.0
						&& !AmethystDampening.isDampened(candidate)
						&& PowerProtection.mayHarm(player, candidate)
						&& !SpellFieldManager.isSanctuaryProtected(level, candidate),
				Comparator.comparingDouble(player::distanceToSqr))) {
			target.hurtServer(level, PowerDamage.source(player), 24.0F);
			target.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 60, 3, false, true));
		}
	}
}
