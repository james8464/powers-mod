package com.powers.entity;

import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.artifact.ArtifactGuardianSummons;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

/** Central lifecycle boundary for indexed and transient state owned by loaded entities. */
public final class EntityRuntimeLifecycle {
	private EntityRuntimeLifecycle() {
	}

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			NamedLivingTargetIndex.track(entity);
			if (entity instanceof AbstractPlayerLikeMob guardian) {
				ArtifactGuardianSummons.trackLoaded(guardian);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			NamedLivingTargetIndex.untrack(entity);
			PhysicalMagicPresences.unload(entity);
			if (entity instanceof PlayerLikeTarget) {
				TestActorPowerState.clear(entity.getUUID());
				ForcefieldAbility.clear(entity.getUUID());
			}
			if (entity instanceof AbstractPlayerLikeMob guardian) {
				ArtifactGuardianSummons.untrackLoaded(guardian);
			}
		});
	}
}
