package com.powers.power;

import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicBoolean;

/** Rolls back a paid cast when its accepted asynchronous world action later fails. */
public final class AsyncAbilityTransaction {
	private final MinecraftServer server;
	private final java.util.UUID playerId;
	private final int refundAmount;
	private final String abilityId;
	private final long expectedCooldownDeadline;
	private final AtomicBoolean settled = new AtomicBoolean();

	public AsyncAbilityTransaction(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			Ability energyAbility) {
		this.server = player.level().getServer();
		this.playerId = player.getUUID();
		this.refundAmount = PowerEnergy.cost(player, energyAbility);
		this.abilityId = energyAbility.id().toString();
		Integer override = AbilityActivationContext.cooldownOverride();
		int cooldown = override == null ? energyAbility.cooldownTicksFor(player, data) : override;
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
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player == null) return;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.refundEnergy(refundAmount);
		if (expectedCooldownDeadline > 0L
				&& data.cooldownReadyAt(abilityId) == expectedCooldownDeadline) {
			data.clearCooldown(abilityId);
		}
		PowersPackets.syncTo(player);
	}
}
