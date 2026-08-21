package com.powers.companion;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Package-owned mutable state shared by the Shadow body, command, and diagnostics owners. */
final class PrivateCompanionSession {
	final long id;
	ShadowCompanionEntity body;
	final Set<UUID> apparitionViewers = new HashSet<>();
	final ShadowTaskController tasks = new ShadowTaskController();
	int dimensionTransferFailures;

	PrivateCompanionSession(long id, ShadowCompanionEntity body) {
		this.id = id;
		this.body = body;
	}
}
