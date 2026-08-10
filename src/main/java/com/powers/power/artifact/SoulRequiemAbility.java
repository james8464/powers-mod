package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.ShadowSwordFx;
import com.powers.item.ShadowSwordPowerRules;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Rank-nine execution curse whose delayed impact can be broken by cover or amethyst. */
public final class SoulRequiemAbility extends Ability {
	private static final double RANGE = 96.0;

	public SoulRequiemAbility() {
		super(PowersMod.id("soul_requiem"),
				Component.translatable("ability.powers.soul_requiem"), 1200, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(player, RANGE);
		if (!eligible(player, target, RANGE * RANGE)) return false;
		ServerLevel level = (ServerLevel) player.level();
		target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 40, 0, false, true));
		ShadowSwordFx.soulRequiem(level, player.getEyePosition(), target.getEyePosition(), false);
		PowersMod.scheduleDelayed(level.getServer(), 40, () -> execute(player, target));
		return true;
	}

	private static void execute(ServerPlayer caster, LivingEntity target) {
		if (!eligible(caster, target, 128.0 * 128.0) || !caster.hasLineOfSight(target)) return;
		ServerLevel level = (ServerLevel) caster.level();
		float before = target.getHealth();
		target.hurtServer(level, PowerDamage.source(caster),
				ShadowSwordPowerRules.soulRequiemDamage(target.getMaxHealth()));
		float dealt = Math.max(0.0F, before - target.getHealth());
		caster.heal(Math.max(20.0F, dealt * 0.5F));
		PlayerPowers.get(caster).refundEnergy(Math.max(100, Math.round(dealt)));
		ShadowSwordFx.soulRequiem(level, caster.getEyePosition(), target.getEyePosition(), true);
	}

	private static boolean eligible(ServerPlayer caster, LivingEntity target, double maximumDistanceSquared) {
		return target != null && target != caster && target.isAlive() && caster.isAlive()
				&& caster.level() == target.level() && caster.distanceToSqr(target) <= maximumDistanceSquared
				&& !target.entityTags().contains(SkillSystem.DARKNESS_TAG)
				&& !AmethystDampening.isDampened(target)
				&& PowerProtection.mayHarm(caster, target)
				&& !SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target);
	}
}
