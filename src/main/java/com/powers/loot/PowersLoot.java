package com.powers.loot;

import com.powers.ImportedPackItems;
import com.powers.PowersMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** Injects imported food additively so datapacks and future vanilla loot remain intact. */
public final class PowersLoot {
	private PowersLoot() {
	}

	public static void initialize() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			String id = key.identifier().toString();
			for (LootDropGroup group : LootInjectionCatalog.groups()) {
				if (!group.tableId().equals(id)) continue;
				LootPool.Builder pool = LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.when(LootItemRandomChanceCondition.randomChance(group.chance()));
				for (String itemId : group.itemIds()) {
					Item item = ImportedPackItems.ITEMS.get(itemId);
					if (item == null) {
						PowersMod.LOGGER.error("Loot injection references missing item powers:{}", itemId);
						continue;
					}
					pool.add(LootItem.lootTableItem(item).setWeight(1));
				}
				table.withPool(pool);
			}
		});
	}
}
