package com.powers.player;

import com.powers.power.Power;
import com.powers.power.PowerAffinity;
import com.powers.power.PowerRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Centralizes allegiance mapping and loadout migration for innate player slots. */
public final class PlayerPowerAffinity {
	private PlayerPowerAffinity() {
	}

	public static PowerAffinity allegiance(ServerPlayer player) {
		return SkillSystem.hasDarknessTag(player) ? PowerAffinity.DARKNESS : PowerAffinity.RADIANT;
	}

	public static boolean permits(ServerPlayer player, Power power) {
		return power != null && power.affinity().permits(allegiance(player));
	}

	public static List<String> reconcile(ServerPlayer player, List<String> current) {
		return PowerRegistry.reconcile(current, allegiance(player));
	}
}
