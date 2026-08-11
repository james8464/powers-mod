package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Blue Crystal toggle that owns Minecraft's true global tick freeze for at most one minute. */
public final class ChronoStopAbility extends Ability {
	private static final int COOLDOWN_TICKS = 3_600;

	public ChronoStopAbility() {
		super(PowersMod.id("chrono_stop"), Component.translatable("ability.powers.chrono_stop"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return ChronoStopRules.isSelectionAction(GlobalTimeStopManager.isCrystalOwnedBy(player));
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (GlobalTimeStopManager.isCrystalOwnedBy(player)) {
			GlobalTimeStopManager.stopCrystal(player);
			PowerMessages.sendImportant(player, "crystal.powers.chrono_end", 3);
			return true;
		}
		if (!GlobalTimeStopManager.startCrystal(player, ChronoStopRules.MAX_DURATION_TICKS)) {
			PowerMessages.send(player, "crystal.powers.chrono_blocked", 3);
			return false;
		}
		PowerMessages.sendImportant(player, "crystal.powers.chrono_start", 3);
		return true;
	}
}
