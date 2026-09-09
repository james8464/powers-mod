package com.powers.spell;

import com.powers.PowersMod;
import com.powers.PowersParticles;
import com.powers.fx.PowerFx;
import com.powers.player.CartographerSite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Schedules a finite player-local route cue without reading or loading route chunks. */
public final class CartographerBreadcrumbs {
	private static final int COLOR = 0xFFFFE08A;

	private CartographerBreadcrumbs() {
	}

	public static int schedule(ServerPlayer player, CartographerSite site) {
		int accepted = 0;
		for (int pulse = 1; pulse <= CartographerBreadcrumbRules.pulseCount(); pulse++) {
			var token = PowersMod.scheduleDelayed(player.level().getServer(),
					pulse * CartographerBreadcrumbRules.pulseIntervalTicks(), player.getUUID(),
					player.level().dimension(), player.getUUID(), "cartographer_breadcrumb",
					(server, task) -> emit(server.getPlayerList().getPlayer(task.subjectId()), site));
			if (token.accepted()) accepted++;
		}
		return accepted;
	}

	private static void emit(ServerPlayer player, CartographerSite site) {
		if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel level)
				|| !level.dimension().identifier().toString().equals(site.dimensionId())) return;
		long dx = (long) site.x() - player.blockPosition().getX();
		long dz = (long) site.z() - player.blockPosition().getZ();
		for (CartographerBreadcrumbRules.Offset offset
				: CartographerBreadcrumbRules.localTrail(dx, dz)) {
			var position = player.position().add(offset.x(), 0.15, offset.z());
			PowerFx.coloredBurst(level, position, COLOR, 2, 0.08);
			PowerFx.burst(level, position, PowersParticles.GLYPH, 1, 0.04, 0.0);
		}
	}
}
