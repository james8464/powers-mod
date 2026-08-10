package com.powers.forge;

import com.powers.PowersDataComponents;
import com.powers.PowersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Forgeability policy plus a small public compatibility hook for modded weapons. */
public final class CrucibleEligibility {
	public static final TagKey<Item> BASE_WEAPONS = TagKey.create(Registries.ITEM,
			PowersMod.id("arcane_crucible_base_weapons"));
	private static final List<Predicate<ItemStack>> EXTENSIONS = new CopyOnWriteArrayList<>();
	private static final List<Predicate<ItemStack>> EXCLUSIONS = new CopyOnWriteArrayList<>();

	private CrucibleEligibility() {
	}

	public static boolean isBaseWeapon(ItemStack stack) {
		if (stack == null || stack.isEmpty() || isMythic(stack)
				|| stack.has(PowersDataComponents.CRUCIBLE_WEAPON)) return false;
		if (EXCLUSIONS.stream().anyMatch(rule -> rule.test(stack))) return false;
		return stack.is(BASE_WEAPONS) || EXTENSIONS.stream().anyMatch(rule -> rule.test(stack));
	}

	public static boolean isConvertedWeapon(ItemStack stack) {
		return stack != null && !stack.isEmpty() && !isMythic(stack)
				&& stack.has(PowersDataComponents.CRUCIBLE_WEAPON);
	}

	public static boolean isMythic(ItemStack stack) {
		return stack != null && stack.has(PowersDataComponents.ARTIFACT_IDENTITY);
	}

	public static boolean isAnimatedStar(ItemStack stack) {
		return hasPath(stack, "imported_artifact_star_animated");
	}

	public static int runeXp(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return 0;
		Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id.getNamespace().equals(PowersMod.MOD_ID) ? CrucibleRuneRules.xpFor(id.getPath()) : 0;
	}

	public static boolean isCatalyst(ItemStack stack) {
		return stack != null && !stack.isEmpty() && (stack.is(com.powers.PowersBlocks.DARKNESS.asItem())
				|| stack.is(com.powers.PowersBlocks.PURE_LIGHT.asItem())
				|| isAnimatedStar(stack) || runeXp(stack) > 0);
	}

	public static void registerBaseWeapon(Predicate<ItemStack> rule) {
		if (rule != null) EXTENSIONS.add(rule);
	}

	public static void registerExclusion(Predicate<ItemStack> rule) {
		if (rule != null) EXCLUSIONS.add(rule);
	}

	private static boolean hasPath(ItemStack stack, String path) {
		if (stack == null || stack.isEmpty()) return false;
		Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id.getNamespace().equals(PowersMod.MOD_ID) && id.getPath().equals(path);
	}
}
