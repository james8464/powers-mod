package com.powers.power.abilities;

import com.powers.power.AmethystDampening;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Resolves body-scoped protection evidence before lightning may deal damage. */
final class LightningStrikeBodyResolver {
	/** Stable inputs make the protection priority independently reviewable. */
	record Protections(boolean harmAllowed, boolean amethyst, boolean sanctuary,
			boolean kineticWard, boolean forcefield) {
	}

	private LightningStrikeBodyResolver() {
	}

	static LightningStrikeRules.Counterplay resolve(ServerLevel level,
			ServerPlayer caster, LivingEntity target, Vec3 rayOrigin, long now,
			boolean includeForcefield) {
		SpellFieldManager.RayWardHit ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, caster.getUUID(), rayOrigin, bodyCenter(target)).orElse(null);
		boolean crossedSanctuary = ward != null
				&& ward.counterplay() == VoidBeamRules.Counterplay.SANCTUARY;
		boolean sanctuary = SpellFieldManager.isSanctuaryProtected(level, target)
				|| crossedSanctuary;
		boolean kineticWard = ward != null
				&& ward.counterplay() == VoidBeamRules.Counterplay.KINETIC_WARD;
		boolean forcefield = includeForcefield
				&& MagicShieldManager.global().active(target.getUUID(), now);
		return decide(new Protections(PowerProtection.mayHarm(caster, target),
				bodyAmethyst(level, target), sanctuary, kineticWard, forcefield));
	}

	static LightningStrikeRules.Counterplay decide(Protections protections) {
		return LightningStrikeRules.bodyDecision(protections.harmAllowed(),
				protections.amethyst(), protections.sanctuary(),
				protections.kineticWard(), protections.forcefield());
	}

	private static boolean bodyAmethyst(ServerLevel level, LivingEntity target) {
		BlockPos feet = target.blockPosition();
		return AmethystDampening.isDampened(target)
				|| level.getBlockState(feet).is(AmethystDampening.AMETHYST_BLOCKS)
				|| level.getBlockState(feet.below()).is(AmethystDampening.AMETHYST_BLOCKS)
				|| AmethystDampening.findPoweredWard(level, feet).isPresent();
	}

	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}
}
