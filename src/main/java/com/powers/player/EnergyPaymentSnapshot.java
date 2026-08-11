package com.powers.player;

import com.powers.item.ArtifactEnergyReservoir;
import com.powers.item.ArtifactRole;
import com.powers.item.ImportedArtifactItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Exact same-tick rollback frame for the player pool and carried reservoirs. */
public final class EnergyPaymentSnapshot {
	private record Reservoir(ItemStack stack, int energy) {
	}

	private final int playerEnergy;
	private final List<Reservoir> reservoirs;

	private EnergyPaymentSnapshot(int playerEnergy, List<Reservoir> reservoirs) {
		this.playerEnergy = playerEnergy;
		this.reservoirs = List.copyOf(reservoirs);
	}

	public static EnergyPaymentSnapshot capture(ServerPlayer player) {
		List<Reservoir> reservoirs = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.getItem() instanceof ImportedArtifactItem relic
					&& relic.role() == ArtifactRole.ENERGY_RESERVOIR) {
				reservoirs.add(new Reservoir(stack, ArtifactEnergyReservoir.stored(stack)));
			}
		}
		return new EnergyPaymentSnapshot(PlayerEnergyStorage.energy(player), reservoirs);
	}

	/** Restores exact pre-payment distribution, not merely the same total. */
	public void restore(ServerPlayer player) {
		PlayerEnergyStorage.store(player, playerEnergy);
		for (Reservoir reservoir : reservoirs) {
			ArtifactEnergyReservoir.setStored(reservoir.stack(), reservoir.energy());
		}
	}
}
