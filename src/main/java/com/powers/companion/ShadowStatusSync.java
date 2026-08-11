package com.powers.companion;

import com.powers.network.CompanionPackets;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Builds authoritative owner-only HUD snapshots at lifecycle boundaries. */
final class ShadowStatusSync {
	private static final Map<UUID, ShadowStatusSyncRules.Snapshot> LAST_SENT = new HashMap<>();

	private ShadowStatusSync() {
	}

	static boolean mayProceed(ServerPlayer owner, ShadowCompanionData data) {
		if (ShadowManifestationRules.mayRecall(data, owner.level().getGameTime())) return true;
		long remaining = Math.max(0L, data.recallReadyAt() - owner.level().getGameTime());
		publish(owner, new ShadowStatusSyncRules.Snapshot(false, data.energy(),
				data.stance().serializedName(), data.revealed(), false,
				(int) Math.min(Integer.MAX_VALUE, remaining)), false);
		return false;
	}

	static void active(ServerPlayer owner, ShadowCompanionEntity body) {
		publish(owner, new ShadowStatusSyncRules.Snapshot(true, body.energy(),
				ShadowCompanionStore.get(owner).stance().serializedName(), body.revealed(),
				ShadowMagicState.actionsSuppressed(body), 0), false);
	}

	static void inactive(ServerPlayer owner) {
		ShadowCompanionData data = ShadowCompanionStore.get(owner);
		publish(owner, new ShadowStatusSyncRules.Snapshot(false, data.energy(),
				data.stance().serializedName(), false, false, 0), true);
		LAST_SENT.remove(owner.getUUID());
	}

	static void clear() {
		LAST_SENT.clear();
	}

	private static void publish(ServerPlayer owner, ShadowStatusSyncRules.Snapshot snapshot,
			boolean immediate) {
		long tick = owner.level().getServer().getTickCount();
		if (!ShadowStatusSyncRules.shouldSend(
				LAST_SENT.get(owner.getUUID()), snapshot, tick, immediate)) return;
		CompanionPackets.sendStatus(owner, snapshot.active(), snapshot.energy(), snapshot.stance(),
				snapshot.revealed(), snapshot.suppressed(), snapshot.recallTicks());
		LAST_SENT.put(owner.getUUID(), snapshot);
	}
}
