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
		int attunementEnergy = 0;
		boolean livingHeart = false;
		boolean ghoulHeart = false;
		boolean clockworkHeart = false;
		boolean celestialFocus = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!(stack.getItem() instanceof ImportedArtifactItem relic)) continue;
			switch (relic.kind()) {
				case ATTUNEMENT -> {
					attunements++;
					attunementEnergy += ImportedArtifactRules.attunementEnergy(relic.texture());
				}
				case HEART_RELIC -> {
					switch (ImportedArtifactRules.heartSpecialization(relic.texture())) {
						case LIVING, WILDWOOD -> livingHeart = true;
						case GHOUL -> ghoulHeart = true;
						case CLOCKWORK -> clockworkHeart = true;
						default -> { }
					}
				}
				default -> { }
			}
			celestialFocus |= relic.role() == ArtifactRole.CELESTIAL_FOCUS;
		}
		int energy = Math.min(6, attunementEnergy) + (celestialFocus ? 1 : 0)
				+ (ghoulHeart ? 1 : 0);
		if (energy > 0 && PlayerPowers.get(player).regenerateEnergy(energy)) PowersPackets.syncTo(player);
		if (attunements > 0) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.RESISTANCE, 40, Math.min(1, attunements - 1), true, true));
		if (livingHeart) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.REGENERATION, 40, 0, true, true));
		if (clockworkHeart) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.ABSORPTION, 40, 0, true, true));
	}
}
