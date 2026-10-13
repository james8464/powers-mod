package com.powers.item;

import com.powers.PowerStatusEffects;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Applies one bounded per-second inventory pass for relic attunements. */
public final class ImportedArtifactRuntime {
	private ImportedArtifactRuntime() {
	}

	public static void tickPlayer(ServerPlayer player, int tick) {
		if (tick % 20 != 0) return;
		List<String> attunementTextures = new ArrayList<>();
		ImportedArtifactRules.HeartSpecialization strongestHeart =
				ImportedArtifactRules.HeartSpecialization.NONE;
		boolean celestialFocus = false;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!(stack.getItem() instanceof ImportedArtifactItem relic)) continue;
			switch (relic.kind()) {
				case ATTUNEMENT -> attunementTextures.add(relic.texture());
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
		Map<ImportedArtifactRules.AttunementSchool, ImportedArtifactRules.AttunementProfile> attunements =
				ImportedArtifactRules.strongestAttunements(attunementTextures);
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		int attunementEnergy = ImportedArtifactRules.attunementEnergy(attunements,
				ImportedArtifactRules.lowEnergy(powers.energy(), powers.energyCapacity()), player.isUsingItem());
		int energy = ImportedArtifactRules.cappedPassiveEnergy(
				attunementEnergy, celestialFocus,
				strongestHeart == ImportedArtifactRules.HeartSpecialization.GHOUL);
		if (energy > 0 && powers.regenerateEnergy(energy)) PowersPackets.syncTo(player);
		boolean wounded = ImportedArtifactRules.wounded(player.getHealth(), player.getMaxHealth());
		ImportedArtifactRules.AttunementProfile warding = attunements.get(
				ImportedArtifactRules.AttunementSchool.WARDING);
		if (wounded && warding != null) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.RESISTANCE, 40, warding.magnitude() - 1, true, true));
		ImportedArtifactRules.AttunementProfile verdancy = attunements.get(
				ImportedArtifactRules.AttunementSchool.VERDANCY);
		if (wounded && verdancy != null) player.addEffect(PowerStatusEffects.hidden(
				MobEffects.REGENERATION, 40, verdancy.magnitude() - 1, true, true));
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
