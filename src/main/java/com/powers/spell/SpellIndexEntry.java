package com.powers.spell;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionId;

/** Server-authored compact grimoire row used by the in-game contents screen. */
public record SpellIndexEntry(String id, int energy, int cooldownTicks, int channelTicks,
		double range, String purposeKey, String targetKey, String counterKey) {
	private static final MagicActionCatalogue ACTIONS = MagicActionCatalogue.defaults();

	public SpellIndexEntry {
		if (id == null || id.isBlank() || energy <= 0 || cooldownTicks <= 0 || channelTicks < 0
				|| !Double.isFinite(range) || range < 0 || purposeKey == null || targetKey == null
				|| counterKey == null) throw new IllegalArgumentException("Invalid spell index entry");
	}

	/** Builds display metadata exclusively from the canonical spell/action registries. */
	public static SpellIndexEntry from(SpellDefinition spell) {
		var action = ACTIONS.definition(new MagicActionId(spell.id()));
		if (action == null) throw new IllegalArgumentException("Missing magic action: " + spell.id());
		String base = "spell.powers.index.";
		return new SpellIndexEntry(spell.id(), spell.energyCost(), spell.cooldownTicks(),
				spell.channelTicks(), action.baseRange(), base + "purpose." + spell.id(),
				base + "target." + spell.id(), base + "counter." + spell.id());
	}
}
