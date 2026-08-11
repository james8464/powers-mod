package com.powers.companion;

import com.powers.PowersEffects;
import com.powers.force.LivingForceKind;
import com.powers.force.LivingForceManager;
import com.powers.fx.PowerFx;
import com.powers.power.AmethystDampening;
import com.powers.testing.TestingOverrides;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Applies environment and anti-magic state once per existing five-tick Shadow pulse. */
public final class ShadowMagicState {
	private ShadowMagicState() {
	}

	public static ShadowEnergyRules.TickResult tick(ServerPlayer owner,
			ShadowCompanionEntity shadow) {
		ServerLevel level = (ServerLevel) shadow.level();
		boolean amethyst = AmethystDampening.update(shadow);
		boolean channeling = ShadowConjurationManager.active(owner.getUUID());
		boolean linked = !channeling && owner.level() == level
				&& owner.distanceToSqr(shadow) <= 24.0 * 24.0;
		boolean darkness = LivingForceManager.isNearForce(level, shadow.blockPosition(), 6,
				LivingForceKind.DARKNESS) && !channeling;
		boolean pureLight = LivingForceManager.isNearForce(level, shadow.blockPosition(), 6,
				LivingForceKind.PURE_LIGHT);
		var result = ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(shadow.energy(),
				linked, darkness, pureLight, amethyst,
				TestingOverrides.energyDisabled(owner.getUUID())));
		shadow.setEnergy(result.energy());
		if (result.pureLightHarm() && shadow.tickCount % 20 == 0 && shadow.isAlive()) {
			shadow.hurtServer(level, shadow.damageSources().magic(), 3.0F);
			PowerFx.coloredBurst(level, shadow.getEyePosition(), 0xFFF5C7, 10, 0.42);
		}
		if (amethyst && shadow.tickCount % 20 == 0) {
			PowerFx.burst(level, shadow.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 8, 0.35, 0.03);
		}
		return result;
	}

	public static boolean actionsSuppressed(ShadowCompanionEntity shadow) {
		return shadow.hasEffect(PowersEffects.AMETHYST_POISONING);
	}
}
