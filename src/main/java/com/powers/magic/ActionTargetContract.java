package com.powers.magic;

/** Entity support declared by every registered magic action. */
public enum ActionTargetContract {
	NONE,
	ANY_LIVING,
	PLAYER_PARTICIPANT,
	PLAYER_OR_MOB_FALLBACK
}
