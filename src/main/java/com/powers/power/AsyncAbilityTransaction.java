package com.powers.power;

import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.atomic.AtomicBoolean;

/** Rolls back a paid cast when its accepted asynchronous world action later fails. */
public final class AsyncAbilityTransaction {
	private final ServerPlayer player;
	private final PlayerPowers.PlayerPowersData data;
	private final Ability energyAbility;
	private final long expectedCooldownDeadline;
	private final AtomicBoolean settled = new AtomicBoolean();

	public AsyncAbilityTransaction(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			Ability energyAbility) {
		this.player = player;
		this.data = data;
		this.energyAbility = energyAbility;
		int cooldown = energyAbility.cooldownTicksFor(player, data);
		this.expectedCooldownDeadline = cooldown <= 0 ? 0L
				: player.level().getGameTime() + cooldown;
	}

	/** Marks the real world action complete; later timeout callbacks become harmless. */
	public void succeed() {
		settled.compareAndSet(false, true);
	}

	/** Refunds exactly once and clears only the cooldown started by this cast. */
	public void fail() {
		if (!settled.compareAndSet(false, true)) return;
		data.refundEnergy(energyAbility);
		if (expectedCooldownDeadline > 0L
				&& data.cooldownReadyAt(energyAbility.id().toString()) == expectedCooldownDeadline) {
			data.clearCooldown(energyAbility.id().toString());
		}
		if (player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player) {
			PowersPackets.syncTo(player);
		}
	}
}
