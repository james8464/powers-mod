package com.powers.item;

import com.powers.PowerStatusEffects;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** Applies one bounded per-second inventory pass for relic attunements. */
public final class ImportedArtifactRuntime {
	private ImportedArtifactRuntime() {
	}

	public static void tickPlayer(ServerPlayer player, int tick) {
		if (tick % 20 != 0) return;
		int attunements = 0;
		boolean vitality = false;
		boolean soulMatrix = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!(stack.getItem() instanceof ImportedArtifactItem relic)) continue;
			switch (relic.kind()) {
				case ATTUNEMENT -> attunements++;
				case HEART_RELIC -> vitality = true;
				case SOUL_VESSEL -> soulMatrix |= relic.texture().contains("soulmatrix");
				default -> { }
			}
		}
		int energy = ImportedArtifactRules.attunementEnergy(attunements) + (soulMatrix ? 3 : 0);
		if (energy > 0 && PlayerPowers.get(player).regenerateEnergy(energy)) PowersPackets.syncTo(player);
		if (attunements > 0) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.RESISTANCE, 40, Math.min(1, attunements - 1), true, true));
		if (vitality) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.REGENERATION, 40, 0, true, true));
	}
}
