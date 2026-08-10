package com.powers;

import com.powers.mind.BodyProxyManager;
import com.powers.companion.PrivateCompanionManager;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.InvisibilityToggleAbility;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.power.artifact.ArtifactCovenantManager;
import com.powers.player.DarknessQuestTracker;
import com.powers.player.SkillQuestTracker;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;

/** Registers damage policy, body-proxy mirroring, and cast interruption hooks. */
final class PowerCombatEvents {
	private PowerCombatEvents() {
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return true;
			if (GlobalTimeStopManager.isStopped(((ServerLevel) entity.level()).getServer())) {
				if (!(source.getEntity() instanceof ServerPlayer actor)
						|| !GlobalTimeStopManager.mayAct(actor)) return false;
			}
			if (BodyProxyManager.isProxy(entity)) return BodyProxyManager.allowsDamage(entity, source);
			if (entity instanceof ServerPlayer player
					&& ForcefieldAbility.absorbDamage(player, source, amount)) return false;
			// Amethyst blocks power damage, not ordinary weapons or environmental harm.
			if (AmethystDampening.isDampened(entity) && PowerDamage.isPowerDamage(source)) return false;
			// Mindscape avatars keep their ordinary health while their proxy body is
			// independently vulnerable; blanket realm immunity would neutralize realm mobs.
			return true;
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			BodyProxyManager.afterDamage(entity, source, damageTaken);
			if (damageTaken <= 0) return;
			ArtifactCovenantManager.shareDamage(entity, source, damageTaken);
			SpellCastingManager.markDamaged(entity);
			EnergyDrainAbility.markDamaged(entity);
			if (source.getEntity() instanceof ServerPlayer attacker) {
				InvisibilityToggleAbility.breakOnAttack(attacker);
				SpellCastingManager.revealConcealment(attacker);
			}
		});
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!BodyProxyManager.allowsDeath(entity)) return false;
			return !(entity instanceof ServerPlayer player)
					|| !ArtifactDeathWardManager.preventDeath(player, source);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) PrivateCompanionManager.recordDeath(player);
			DarknessQuestTracker.recordKill(entity, source);
			SkillQuestTracker.recordKill(entity, source);
		});
	}
}
