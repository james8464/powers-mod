package com.powers.spell;

import com.powers.player.SkillSystem;
import com.powers.realm.RealmKind;

/** Progression-only policy for revealing Cartographer destinations. */
public final class CartographerVisibilityRules {
	private CartographerVisibilityRules() {
	}

	public static boolean mayReveal(boolean landmark, RealmKind realm, boolean darkness,
			int lightLevel, int darknessLevel) {
		if (!landmark) return true;
		if (realm == RealmKind.LIGHT) {
			return !darkness && lightLevel >= SkillSystem.MAX_LEVEL;
		}
		if (realm == RealmKind.DARK) {
			return darkness && darknessLevel >= SkillSystem.DARKNESS_MAX_LEVEL;
		}
		return false;
	}
}
