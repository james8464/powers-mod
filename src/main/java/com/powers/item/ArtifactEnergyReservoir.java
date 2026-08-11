package com.powers.item;

import com.powers.PowersDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Persistent, deterministic auxiliary energy storage carried in Soulstones and the Soul Matrix. */
public final class ArtifactEnergyReservoir {
	public record Debit(boolean paid, List<Integer> balances) {
		public Debit {
			balances = List.copyOf(balances);
		}
	}

	private ArtifactEnergyReservoir() {
	}

	public static int capacity(String texture) {
		if (texture == null) return 0;
		if (texture.contains("soulmatrix")) return 1_600;
		if (texture.contains("large")) return 800;
		if (texture.contains("medium")) return 400;
		return texture.contains("soulstone") ? 200 : 0;
	}

	public static int clamp(String texture, int energy) {
		return Math.clamp(energy, 0, capacity(texture));
	}

	/** Computes a stable first-slot-first debit without mutating caller state. */
	public static Debit debit(List<Integer> balances, int requested) {
		List<Integer> original = balances.stream().map(value -> Math.max(0, value)).toList();
		int needed = Math.max(0, requested);
		long total = original.stream().mapToLong(Integer::longValue).sum();
		if (total < needed) return new Debit(false, original);
		List<Integer> updated = new ArrayList<>(original);
		for (int index = 0; index < updated.size() && needed > 0; index++) {
			int taken = Math.min(needed, updated.get(index));
			updated.set(index, updated.get(index) - taken);
			needed -= taken;
		}
		return new Debit(true, updated);
	}

	public static int stored(ItemStack stack) {
		if (!(stack.getItem() instanceof ImportedArtifactItem relic)
				|| relic.role() != ArtifactRole.ENERGY_RESERVOIR) return 0;
		return clamp(relic.texture(), stack.getOrDefault(PowersDataComponents.STORED_ENERGY, 0));
	}

	public static void setStored(ItemStack stack, int energy) {
		if (!(stack.getItem() instanceof ImportedArtifactItem relic)
				|| relic.role() != ArtifactRole.ENERGY_RESERVOIR) return;
		stack.set(PowersDataComponents.STORED_ENERGY, clamp(relic.texture(), energy));
	}

	/** Atomically pays one shortfall from carried reservoirs in inventory order. */
	public static boolean payShortfall(ServerPlayer player, int shortfall) {
		if (shortfall <= 0) return true;
		List<ItemStack> reservoirs = new ArrayList<>();
		List<Integer> balances = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!(stack.getItem() instanceof ImportedArtifactItem relic)
					|| relic.role() != ArtifactRole.ENERGY_RESERVOIR) continue;
			reservoirs.add(stack);
			balances.add(stored(stack));
		}
		Debit debit = debit(balances, shortfall);
		if (!debit.paid()) return false;
		for (int index = 0; index < reservoirs.size(); index++) {
			setStored(reservoirs.get(index), debit.balances().get(index));
		}
		return true;
	}

	/** Total valid auxiliary energy visible to one cast, saturated for diagnostics. */
	public static int totalStored(ServerPlayer player) {
		long total = 0L;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			total += stored(player.getInventory().getItem(slot));
			if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
		}
		return (int) total;
	}
}
