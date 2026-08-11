package com.powers.companion;

import com.powers.PowersDataComponents;
import com.powers.PowersItems;
import com.powers.PowersMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/** Converts live registry/tag/component state into the small pure policy input. */
public final class ShadowConjurationPolicy {
	public static final TagKey<Item> FORBIDDEN = tag("shadow_conjuration_forbidden");
	public static final TagKey<Item> UNCOMMON = tag("shadow_conjuration_uncommon");
	public static final TagKey<Item> RARE = tag("shadow_conjuration_rare");
	public static final TagKey<Item> MYTHIC = tag("shadow_conjuration_mythic");
	public static final TagKey<Item> ALLOWED_EXTERNAL = tag("shadow_conjuration_allowed_external");

	private ShadowConjurationPolicy() {
	}

	public static ShadowConjurationFacts facts(Item item, int count, int energy,
			boolean testingBypass) {
		ItemStack plain = new ItemStack(item);
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		boolean trusted = id != null && (id.getNamespace().equals("minecraft")
				|| id.getNamespace().equals(PowersMod.MOD_ID));
		ShadowConjurationTier tier = plain.is(MYTHIC) ? ShadowConjurationTier.MYTHIC
				: plain.is(RARE) ? ShadowConjurationTier.RARE
				: plain.is(UNCOMMON) ? ShadowConjurationTier.UNCOMMON
				: ShadowConjurationTier.COMMON;
		return new ShadowConjurationFacts(count, plain.getMaxStackSize(), tier, trusted,
				plain.is(ALLOWED_EXTERNAL), plain.has(PowersDataComponents.ARTIFACT_IDENTITY),
				plain.is(FORBIDDEN), item instanceof SpawnEggItem, PowersItems.isCrystal(plain),
				item == PowersItems.DARK_CRYSTAL, testingBypass, energy);
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, PowersMod.id(path));
	}
}
