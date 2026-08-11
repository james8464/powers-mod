package com.powers.item;

import com.powers.item.artifact.ArtifactAlignment;
import com.powers.power.artifact.ArtifactChainManager;
import com.powers.power.artifact.ArtifactCovenantManager;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.power.artifact.ArtifactFieldManager;
import com.powers.power.artifact.ArtifactGateManager;
import com.powers.power.artifact.ArtifactGroundWorkQueue;
import com.powers.power.artifact.ArtifactGuardianSummons;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** One fail-closed revocation boundary for every long-lived artifact-owned state. */
public final class ArtifactOwnedStateRevoker {
	private ArtifactOwnedStateRevoker() {
	}

	public static void revoke(MinecraftServer server, UUID ownerId, ArtifactAlignment alignment) {
		ArtifactFieldManager.forget(ownerId, alignment);
		ArtifactGateManager.forget(ownerId, alignment);
		ArtifactGroundWorkQueue.forget(ownerId, alignment);
		ArtifactGuardianSummons.revokeOwner(server, ownerId, alignment);
		ArtifactDeathWardManager.forget(ownerId, alignment);
		ArtifactCovenantManager.forget(ownerId, alignment);
		ArtifactChainManager.forget(ownerId, alignment);
	}
}
