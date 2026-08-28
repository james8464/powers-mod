package com.powers.animation;

import com.powers.boss.FirstVesselPowerAction;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.combat.ShadowPowerExecutor;
import com.powers.entity.DarknessCreature;
import com.powers.entity.FirstVessel;
import com.powers.entity.RadiantSentinel;
import com.powers.entity.RealmHerald;
import com.powers.realm.RealmKind;
import net.minecraft.world.entity.LivingEntity;

/** Literal production-action to semantic-pose mapping for VFX-006 scope. */
public final class CastingPoseMapping {
	private CastingPoseMapping() {
	}

	public static CastingPose forFirstVessel(FirstVesselPowerAction.Kind kind) {
		return switch (kind) {
			case PROJECTILE -> CastingPose.PROJECT;
			case BEAM, RECOVERY -> CastingPose.CHANNEL;
			case MOBILITY, AREA, CONTROL, DEFENSE -> CastingPose.INVOKE;
		};
	}

	public static CastingPose forShadow(ShadowPowerExecutor.Handler handler) {
		return switch (handler) {
			case PROJECTILE -> CastingPose.PROJECT;
			case BEAM, RECOVERY -> CastingPose.CHANNEL;
			case APOTHEOSIS -> CastingPose.RELEASE;
			case MOBILITY, AREA, CONTROL, DEFENSE, TOGGLE, MIND, SUMMON, TERRAIN ->
					CastingPose.INVOKE;
			case UNSUPPORTED -> throw new IllegalArgumentException("Unsupported Shadow handler");
		};
	}

	public static CastingStyle style(LivingEntity entity) {
		if (entity.getClass() == ShadowCompanionEntity.class) return CastingStyle.SHADOW;
		if (entity.getClass() == RadiantSentinel.class) return CastingStyle.RADIANT;
		if (entity.getClass() == DarknessCreature.class) return CastingStyle.DARKNESS;
		if (entity.getClass() == FirstVessel.class) return CastingStyle.FIRST_VESSEL;
		if (entity.getClass() == RealmHerald.class) {
			return ((RealmHerald) entity).realmKind() == RealmKind.LIGHT
					? CastingStyle.HERALD_LIGHT : CastingStyle.HERALD_DARK;
		}
		throw new IllegalArgumentException("Entity is outside casting-pose scope");
	}

	public static CastingHand hand(String actionId) {
		return switch (actionId) {
			case "fireball", "lightning_strike", "void_beam", "energy_beam",
					"ice_manipulation" -> CastingHand.RIGHT;
			case "energy_drain", "plant_healing_acceleration" -> CastingHand.LEFT;
			default -> CastingHand.BOTH;
		};
	}

	public static int duration(ShadowPowerExecutor.Handler handler) {
		return switch (handler) {
			case PROJECTILE -> 14;
			case BEAM, RECOVERY -> 30;
			case APOTHEOSIS -> 20;
			case MOBILITY, AREA, CONTROL, DEFENSE, TOGGLE, MIND, SUMMON, TERRAIN -> 16;
			case UNSUPPORTED -> throw new IllegalArgumentException("Unsupported Shadow handler");
		};
	}

	public static int duration(FirstVesselPowerAction.Kind kind) {
		return switch (kind) {
			case PROJECTILE -> 14;
			case BEAM, RECOVERY -> 30;
			case MOBILITY, AREA, CONTROL, DEFENSE -> 16;
		};
	}
}
