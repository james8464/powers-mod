package com.powers.magic.runtime;

/** Authoritative route that invoked an action, independent of its registry identity. */
public enum CastSource {
	INNATE,
	ARTIFACT,
	CRYSTAL,
	SPELL;

	/** Only a directly invoked innate ability may consume player-rank scaling. */
	public boolean appliesPlayerRank(boolean abilityOptedIn) {
		return this == INNATE && abilityOptedIn;
	}
}
