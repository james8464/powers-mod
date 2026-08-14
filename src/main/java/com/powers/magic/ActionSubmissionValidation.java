package com.powers.magic;

/** Pure pre-mutation gate for revisioned serverbound action and menu submissions. */
public enum ActionSubmissionValidation {
	ACCEPT,
	REFRESH;

	public static ActionSubmissionValidation validate(ActionRegistrySnapshot snapshot,
			long submittedRevision, String canonicalKey) {
		if (snapshot == null || submittedRevision != snapshot.revision()) return REFRESH;
		String resolved = snapshot.resolveKey(canonicalKey);
		return resolved != null && resolved.equals(canonicalKey) ? ACCEPT : REFRESH;
	}
}
