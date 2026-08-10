package com.powers.forge;

/** Server-thread re-entry guard for one Crucible block entity. */
public final class CrucibleMutationGuard {
	private boolean locked;

	public boolean tryLock() {
		if (locked) return false;
		locked = true;
		return true;
	}

	public void unlock() {
		locked = false;
	}

	public boolean isLocked() {
		return locked;
	}
}
