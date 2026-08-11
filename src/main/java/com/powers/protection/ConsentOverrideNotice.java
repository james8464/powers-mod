package com.powers.protection;

import com.powers.util.PowerMessages;

import java.util.Locale;

/** Pure target-facing description of a paid Empyrean consent override. */
public record ConsentOverrideNotice(PowerMessages.Delivery delivery, String translationKey,
		String kind, int energySurcharge) {
	public static ConsentOverrideNotice forTarget(ConsentKind kind) {
		return new ConsentOverrideNotice(PowerMessages.Delivery.CHAT,
				"artifact.powers.empyrean.overridden", kind.name().toLowerCase(Locale.ROOT),
				ConsentOverrideRules.OVERRIDE_ENERGY_SURCHARGE);
	}
}
