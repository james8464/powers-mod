package com.powers.forge;

import com.powers.PowersBlocks;
import com.powers.PowersMod;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Deterministic built-in peers; datapacks control base eligibility through the item tag. */
public final class CrucibleTransformationCatalogue {
	private static final List<String> DARK_TARGETS = List.of(
			"nocturne", "calamity_blade", "revenants_gravecleaver");
	private static final List<String> LIGHT_TARGETS = List.of(
			"solstice", "valhakyra", "zenith");

	private CrucibleTransformationCatalogue() {
	}

	public static List<CrucibleChoice> choices(ItemStack weapon, ItemStack catalyst) {
		if (weapon == null || catalyst == null || weapon.isEmpty() || catalyst.isEmpty()) return List.of();
		if (CrucibleEligibility.isBaseWeapon(weapon)) {
			if (catalyst.is(PowersBlocks.DARKNESS.asItem())) {
				return conversions(ArtifactAlignment.DARKNESS, DARK_TARGETS);
			}
			if (catalyst.is(PowersBlocks.PURE_LIGHT.asItem())) {
				return conversions(ArtifactAlignment.LIGHT, LIGHT_TARGETS);
			}
			return List.of();
		}
		CrucibleWeaponData data = weapon.get(com.powers.PowersDataComponents.CRUCIBLE_WEAPON);
		if (data == null) return List.of();
		if (CrucibleEligibility.isAnimatedStar(catalyst) && !data.starBound()) {
			return List.of(new CrucibleChoice("bind_animated_star", CrucibleOperation.BIND_STAR,
					data.alignment(), null));
		}
		if (data.starBound() && CrucibleEligibility.runeXp(catalyst) > 0) {
			return List.of(new CrucibleChoice("infuse_runestone", CrucibleOperation.INFUSE_RUNE,
					data.alignment(), null));
		}
		return List.of();
	}

	private static List<CrucibleChoice> conversions(ArtifactAlignment alignment, List<String> targets) {
		List<CrucibleChoice> result = new ArrayList<>(targets.size());
		for (String path : targets) {
			Identifier id = Identifier.fromNamespaceAndPath(PowersMod.MOD_ID, path);
			result.add(new CrucibleChoice("convert_" + alignment.serializedName() + "_" + path,
					CrucibleOperation.CONVERT, alignment, id));
		}
		return List.copyOf(result);
	}
}
