package com.powers.resource;

/** Pure namespace/missing-state decision used by the client item-model fallback hook. */
public final class OptionalItemModelRules {
	private OptionalItemModelRules() { }

	public static boolean useBarrier(String namespace, boolean missing) {
		return missing && "powers".equals(namespace);
	}
}
