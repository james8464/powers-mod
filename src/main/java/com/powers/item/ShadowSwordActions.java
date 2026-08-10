package com.powers.item;

import net.minecraft.server.level.ServerPlayer;

/** Public narrow adapter used by artifact abilities without exposing world internals. */
public final class ShadowSwordActions {
	private ShadowSwordActions() {
	}

	public static int summon(ServerPlayer player, int requested) {
		int nearby = ShadowSwordWorldActions.nearbyGuardians(player, 64.0);
		int allowed = ShadowSwordRules.commandedGuardiansToSummon(requested, nearby);
		return allowed == 0 ? 0 : ShadowSwordWorldActions.summonGuardians(player, null, allowed);
	}

	public static int spread(ServerPlayer player) {
		return ShadowSwordWorldActions.spreadDarkness(player);
	}
}
