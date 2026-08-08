package com.powers.magic;

import java.util.Objects;

/**
 * Canonically ordered unordered action pair. Same-action pairs are valid
 * because two players can cast the same force into one space.
 *
 * @param first lexicographically earlier action
 * @param second lexicographically later action, or the same action
 */
public record ActionPair(MagicActionId first, MagicActionId second) implements Comparable<ActionPair> {
	/** Orders IDs so callers cannot create duplicate reverse pairs. */
	public ActionPair {
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(second, "second");
		if (first.compareTo(second) > 0) {
			MagicActionId swap = first;
			first = second;
			second = swap;
		}
	}

	/** Creates a canonical pair from two IDs. */
	public static ActionPair of(MagicActionId first, MagicActionId second) {
		return new ActionPair(first, second);
	}

	@Override
	public int compareTo(ActionPair other) {
		int firstOrder = first.compareTo(other.first);
		return firstOrder != 0 ? firstOrder : second.compareTo(other.second);
	}

	@Override
	public String toString() {
		return first + "+" + second;
	}
}
