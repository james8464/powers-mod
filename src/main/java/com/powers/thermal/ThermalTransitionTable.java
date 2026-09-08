package com.powers.thermal;

import java.util.EnumMap;
import java.util.Map;

/** Total, deterministic thermal state table shared by power implementations. */
public final class ThermalTransitionTable {
	private static final Map<ThermalInfluence, EnumMap<ThermalMaterial, ThermalTransition>> TABLE = build();

	private ThermalTransitionTable() {
	}

	public static ThermalTransition resolve(ThermalInfluence influence, ThermalMaterial material) {
		return TABLE.get(influence).get(material);
	}

	public static int entryCount() {
		return TABLE.values().stream().mapToInt(Map::size).sum();
	}

	private static Map<ThermalInfluence, EnumMap<ThermalMaterial, ThermalTransition>> build() {
		EnumMap<ThermalInfluence, EnumMap<ThermalMaterial, ThermalTransition>> table =
				new EnumMap<>(ThermalInfluence.class);
		for (ThermalInfluence influence : ThermalInfluence.values()) {
			EnumMap<ThermalMaterial, ThermalTransition> row = new EnumMap<>(ThermalMaterial.class);
			for (ThermalMaterial material : ThermalMaterial.values()) {
				row.put(material, new ThermalTransition(influence, material, ThermalOutcome.KEEP));
			}
			table.put(influence, row);
		}

		set(table, ThermalInfluence.FIREBALL, ThermalMaterial.AIR, ThermalOutcome.IGNITE);
		set(table, ThermalInfluence.FIREBALL, ThermalMaterial.ICE, ThermalOutcome.MELT);
		set(table, ThermalInfluence.FIREBALL, ThermalMaterial.SNOW, ThermalOutcome.EVAPORATE);
		set(table, ThermalInfluence.FIREBALL, ThermalMaterial.PLANT, ThermalOutcome.SCORCH);
		set(table, ThermalInfluence.INFERNO, ThermalMaterial.AIR, ThermalOutcome.IGNITE);
		set(table, ThermalInfluence.INFERNO, ThermalMaterial.ICE, ThermalOutcome.MELT);
		set(table, ThermalInfluence.INFERNO, ThermalMaterial.SNOW, ThermalOutcome.EVAPORATE);
		set(table, ThermalInfluence.INFERNO, ThermalMaterial.PLANT, ThermalOutcome.SCORCH);
		set(table, ThermalInfluence.ICE, ThermalMaterial.FIRE, ThermalOutcome.EXTINGUISH);
		set(table, ThermalInfluence.ICE, ThermalMaterial.AIR, ThermalOutcome.COAT);
		set(table, ThermalInfluence.ICE, ThermalMaterial.WATER, ThermalOutcome.FREEZE);
		set(table, ThermalInfluence.ICE, ThermalMaterial.LAVA, ThermalOutcome.SOLIDIFY);
		set(table, ThermalInfluence.GROWTH, ThermalMaterial.PLANT, ThermalOutcome.GROW);
		return Map.copyOf(table);
	}

	private static void set(EnumMap<ThermalInfluence, EnumMap<ThermalMaterial, ThermalTransition>> table,
			ThermalInfluence influence, ThermalMaterial material, ThermalOutcome outcome) {
		table.get(influence).put(material, new ThermalTransition(influence, material, outcome));
	}
}
