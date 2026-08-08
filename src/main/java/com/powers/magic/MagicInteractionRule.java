package com.powers.magic;

/** Pure strategy used for exact action-pair overrides. */
@FunctionalInterface
public interface MagicInteractionRule {
	/** Resolves the caller-ordered action pair in the supplied server context. */
	InteractionResolution resolve(MagicActionDefinition first, MagicActionDefinition second,
			InteractionContext context);
}
