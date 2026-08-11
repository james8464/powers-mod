package com.powers.companion.combat;

import com.powers.PowersSounds;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.fx.PowerFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Corrupted semantic presentation shared by manual and tactical Shadow casts. */
public final class ShadowPowerFx {
	private ShadowPowerFx() {
	}

	public static void cast(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, ShadowPowerAction action) {
		int color = switch (action.intent()) {
			case DEFENSE, RECOVERY -> 0x8A4B9F;
			case MOBILITY -> 0x4A2264;
			case CONTROL -> 0x2E0A47;
			case SUMMON, TERRAIN -> 0x180321;
			case OFFENSE -> 0x681C72;
		};
		PowerFx.rune(level, shadow.position().add(0.0, 0.08, 0.0),
				action.workClass() == ShadowPowerAction.WorkClass.GLOBAL ? 5.0 : 1.5,
				color, action.workClass() == ShadowPowerAction.WorkClass.GLOBAL ? 48 : 20,
				level.getGameTime() * 0.06);
		PowerFx.burst(level, shadow.getEyePosition(), ParticleTypes.REVERSE_PORTAL,
				action.workClass() == ShadowPowerAction.WorkClass.GLOBAL ? 28 : 8, 0.45, 0.02);
		if (target != null) PowerFx.coloredBurst(level, target.getEyePosition(), color, 8, 0.35);
		PowerFx.sound(level, shadow.position(), PowersSounds.DARK_WHISPER,
				action.workClass() == ShadowPowerAction.WorkClass.GLOBAL ? 1.5F : 0.55F,
				action.intent() == ShadowPowerAction.Intent.OFFENSE ? 0.62F : 0.78F);
	}
}
