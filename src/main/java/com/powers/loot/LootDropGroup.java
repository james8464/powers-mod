package com.powers.loot;

import java.util.List;

/** Validated immutable description of one injected loot-table roll. */
public record LootDropGroup(String tableId, float chance, int minCount, int maxCount, List<String> itemIds) {
	public LootDropGroup {
		itemIds = List.copyOf(itemIds);
		if (tableId == null || tableId.isBlank() || itemIds.isEmpty() || chance <= 0 || chance > 1
				|| minCount < 1 || maxCount < minCount) throw new IllegalArgumentException("Invalid loot group");
	}
}
