package com.powers.forge;

import com.powers.PowersDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Builds a side-effect-free transaction plan from copied stacks. */
public final class CrucibleTransactionEngine {
	private CrucibleTransactionEngine() {
	}

	public static CrucibleTransactionResult prepare(ItemStack weaponInput,
			ItemStack catalystInput, CrucibleChoice requested) {
		if (weaponInput == null || catalystInput == null || requested == null
				|| weaponInput.isEmpty() || catalystInput.isEmpty()) {
			return CrucibleTransactionResult.fail("inputs");
		}
		ItemStack weapon = weaponInput.copy();
		ItemStack catalyst = catalystInput.copy();
		List<CrucibleChoice> legal = CrucibleTransformationCatalogue.choices(weapon, catalyst);
		if (!legal.contains(requested)) return CrucibleTransactionResult.fail("stale_choice");
		return switch (requested.operation()) {
			case CONVERT -> convert(weapon, catalyst, requested);
			case BIND_STAR -> bind(weapon, catalyst);
			case INFUSE_RUNE -> infuse(weapon, catalyst);
		};
	}

	private static CrucibleTransactionResult convert(ItemStack weapon, ItemStack catalyst,
			CrucibleChoice choice) {
		Item target = BuiltInRegistries.ITEM.getValue(choice.targetItem());
		if (target == null || target == net.minecraft.world.item.Items.AIR) {
			return CrucibleTransactionResult.fail("missing_target");
		}
		Identifier lineage = BuiltInRegistries.ITEM.getKey(weapon.getItem());
		double damageRatio = weapon.isDamageableItem() && weapon.getMaxDamage() > 0
				? (double) weapon.getDamageValue() / weapon.getMaxDamage() : 0.0;
		ItemStack result = weapon.transmuteCopy(target, 1);
		if (result.isDamageableItem()) {
			result.setDamageValue((int) Math.round(result.getMaxDamage() * damageRatio));
		}
		result.set(PowersDataComponents.CRUCIBLE_WEAPON,
				CrucibleWeaponData.create(lineage, choice.alignment(), false, 0));
		return success(result, catalyst, false);
	}

	private static CrucibleTransactionResult bind(ItemStack weapon, ItemStack catalyst) {
		CrucibleWeaponData data = weapon.get(PowersDataComponents.CRUCIBLE_WEAPON);
		if (data == null || data.starBound() || !CrucibleEligibility.isAnimatedStar(catalyst)) {
			return CrucibleTransactionResult.fail("binding");
		}
		weapon.set(PowersDataComponents.CRUCIBLE_WEAPON, data.bindStar());
		return success(weapon, catalyst, false);
	}

	private static CrucibleTransactionResult infuse(ItemStack weapon, ItemStack catalyst) {
		CrucibleWeaponData data = weapon.get(PowersDataComponents.CRUCIBLE_WEAPON);
		int award = CrucibleEligibility.runeXp(catalyst);
		if (data == null || !data.starBound() || award <= 0) {
			return CrucibleTransactionResult.fail("infusion");
		}
		CrucibleWeaponData updated = data.awardXp(award);
		weapon.set(PowersDataComponents.CRUCIBLE_WEAPON, updated);
		return success(weapon, catalyst, updated.level() > data.level());
	}

	private static CrucibleTransactionResult success(ItemStack result,
			ItemStack catalyst, boolean levelUp) {
		catalyst.shrink(1);
		return new CrucibleTransactionResult(true, "ok", result,
				ItemStack.EMPTY, catalyst, levelUp);
	}
}
