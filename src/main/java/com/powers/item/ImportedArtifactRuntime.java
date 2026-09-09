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
		ImportedArtifactRules.HeartSpecialization strongestHeart =
				ImportedArtifactRules.HeartSpecialization.NONE;
		boolean celestialFocus = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!(stack.getItem() instanceof ImportedArtifactItem relic)) continue;
			switch (relic.kind()) {
				case ATTUNEMENT -> {
					attunements++;
					attunementEnergy += ImportedArtifactRules.attunementEnergy(relic.texture());
				}
				case HEART_RELIC -> {
					ImportedArtifactRules.HeartSpecialization candidate =
							ImportedArtifactRules.heartSpecialization(relic.texture());
					if (ImportedArtifactRules.heartTier(candidate)
							> ImportedArtifactRules.heartTier(strongestHeart)) {
						strongestHeart = candidate;
					}
				}
				default -> { }
			}
			celestialFocus |= relic.role() == ArtifactRole.CELESTIAL_FOCUS;
		}
		int energy = Math.min(6, attunementEnergy) + (celestialFocus ? 1 : 0)
				+ (strongestHeart == ImportedArtifactRules.HeartSpecialization.GHOUL ? 1 : 0);
		if (energy > 0 && PlayerPowers.get(player).regenerateEnergy(energy)) PowersPackets.syncTo(player);
		if (attunements > 0) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.RESISTANCE, 40, Math.min(1, attunements - 1), true, true));
		switch (strongestHeart) {
			case LIVING -> player.addEffect(PowerStatusEffects.hidden(
					MobEffects.REGENERATION, 40, 0, true, true));
			case WILDWOOD -> player.addEffect(PowerStatusEffects.hidden(
					MobEffects.REGENERATION, 40, 1, true, true));
			case CLOCKWORK -> player.addEffect(PowerStatusEffects.hidden(
					MobEffects.ABSORPTION, 40, 0, true, true));
			case BLOOD_WARD -> player.addEffect(PowerStatusEffects.hidden(
					MobEffects.RESISTANCE, 40, 0, true, true));
			case GHOUL, NONE -> { }
		}
	}
}
