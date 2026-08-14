package com.powers.spell;

import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.ActionRegistrySnapshot;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.player.EnergyPaymentSnapshot;
import com.powers.player.PlayerPowers;
import com.powers.testing.TestingOverrides;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Owns payment, cooldown, execution, presence, and compensation for one ritual. */
final class SpellCastTransaction {
	private final ServerPlayer player;
	private final SpellDefinition spell;
	private final PreparedMagicCast magic;
	private final int energyCost;
	private final long previousCooldown;
	private final EnergyPaymentSnapshot energySnapshot;
	private boolean begun;
	private boolean terminal;
	private MagicPresenceId presence;

	SpellCastTransaction(ServerPlayer player, SpellDefinition spell,
			PreparedMagicCast magic, int energyCost) {
		this.player = Objects.requireNonNull(player, "player");
		this.spell = Objects.requireNonNull(spell, "spell");
		this.magic = Objects.requireNonNull(magic, "magic");
		this.energyCost = Math.max(0, energyCost);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		this.previousCooldown = data.cooldownReadyAt(cooldownId());
		this.energySnapshot = EnergyPaymentSnapshot.capture(player);
	}

	boolean begin() {
		if (begun || terminal) return false;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (!data.consumeEnergy(energyCost)) return false;
		try {
			if (!TestingOverrides.cooldownsDisabled(player.getUUID())) {
				data.setCooldown(cooldownId(), player.level().getGameTime() + spell.cooldownTicks());
			}
			begun = true;
			return true;
		} catch (RuntimeException failure) {
			energySnapshot.restore(player);
			throw failure;
		}
	}

	boolean complete(BooleanSupplier effect) {
		if (!begun || terminal) return false;
		try {
			if (!ServerMagicCasts.execute(magic, effect::getAsBoolean)) {
				rollbackFull();
				return false;
			}
			presence = ServerMagicCasts.commit(magic, player);
			terminal = true;
			return true;
		} catch (RuntimeException failure) {
			rollbackFull();
			return false;
		}
	}

	/** Normal interruption keeps its authored half-energy penalty and cooldown. */
	void interrupt() {
		if (!begun || terminal) return;
		energySnapshot.restore(player);
		PlayerPowers.get(player).consumeEnergy((energyCost + 1) / 2);
		terminal = true;
	}

	void rollbackFull() {
		if (terminal) return;
		if (presence != null) MagicRuntime.global().removePresence(presence);
		energySnapshot.restore(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (previousCooldown <= 0L) data.clearCooldown(cooldownId());
		else data.setCooldown(cooldownId(), previousCooldown);
		terminal = true;
	}

	int energyCost() {
		return energyCost;
	}

	ActionRegistrySnapshot registrySnapshot() {
		return magic.registrySnapshot();
	}

	private String cooldownId() {
		return "spell:" + spell.id();
	}
}
