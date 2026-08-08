package com.powers.magic.runtime;

/** Presentation boundary called only for deduplicated interaction events. */
@FunctionalInterface
public interface MagicReactionSink {
	/** Emits one semantic reaction without changing mechanical state. */
	void emit(MagicReactionEvent event);
}
