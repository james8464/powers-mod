package com.powers.companion;

import com.powers.mind.MindSessionManager;
import net.minecraft.server.level.ServerLevel;

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
	UUID realmMindSessionId;
	UUID realmParticipantId;

	PrivateCompanionSession(long id, ShadowCompanionEntity body) {
		this.id = id;
		this.body = body;
	}

	void completedRealmTravel(ShadowCompanionEntity previousBody, ServerLevel targetLevel) {
		if (isMindscape(targetLevel)) {
			realmMindSessionId = MindSessionManager.activeFor(previousBody.getUUID())
					.filter(active -> active.kind() == MindSessionManager.Kind.REALM)
					.map(MindSessionManager.Session::id).orElse(null);
			realmParticipantId = realmMindSessionId == null ? null : previousBody.getUUID();
		} else if (isMindscape((ServerLevel) previousBody.level())) {
			releaseRealmParticipation();
		}
	}

	void releaseRealmParticipation() {
		UUID sessionId = realmMindSessionId;
		UUID participantId = realmParticipantId;
		realmMindSessionId = null;
		realmParticipantId = null;
		if (sessionId != null && participantId != null) {
			MindSessionManager.realmParticipantReturned(sessionId, participantId);
		}
	}

	private static boolean isMindscape(ServerLevel level) {
		return level.dimension().identifier().equals(com.powers.PowersMod.id("light_realm"))
				|| level.dimension().identifier().equals(com.powers.PowersMod.id("dark_realm"));
	}
}
