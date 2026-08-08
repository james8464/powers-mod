package com.powers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Creates stable item resource keys and registers items in the POWERS namespace. */
public final class ModItemIds {
	private ModItemIds() {
	}

	public static ResourceKey<Item> create(String name) {
		return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PowersMod.MOD_ID, name));
	}

	public static Item register(ResourceKey<Item> itemKey, java.util.function.Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
		// setId must run before registration so the item's id is fully known
		Item item = itemFactory.apply(settings.setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return item;
	}
}
