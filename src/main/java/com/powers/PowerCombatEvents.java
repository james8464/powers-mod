package com.powers;

import com.powers.mind.BodyProxyManager;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.InvisibilityToggleAbility;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;

/** Registers damage policy, body-proxy mirroring, and cast interruption hooks. */
final class PowerCombatEvents {
	private PowerCombatEvents() {
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (BodyProxyManager.isProxy(entity)) return BodyProxyManager.allowsDamage(entity, source);
			if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return true;
			if (entity instanceof ServerPlayer player
					&& ForcefieldAbility.absorbDamage(player, source, amount)) return false;
			// Amethyst blocks power damage, not ordinary weapons or environmental harm.
			if (AmethystDampening.isDampened(entity) && PowerDamage.isPowerDamage(source)) return false;
			String dimension = entity.level().dimension().identifier().toString();
			return !dimension.equals("powers:dark_realm") && !dimension.equals("powers:light_realm");
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			BodyProxyManager.afterDamage(entity, source, damageTaken);
			if (damageTaken <= 0) return;
			SpellCastingManager.markDamaged(entity);
			EnergyDrainAbility.markDamaged(entity);
			if (source.getEntity() instanceof ServerPlayer attacker) {
				InvisibilityToggleAbility.breakOnAttack(attacker);
				SpellCastingManager.revealConcealment(attacker);
			}
		});
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
				BodyProxyManager.allowsDeath(entity));
	}
}
