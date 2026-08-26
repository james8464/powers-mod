package com.powers.client.fx;

import com.powers.fx.ClientRankTenSilhouetteState;
import com.powers.fx.RankTenSilhouetteClientOwnership;
import com.powers.network.RankTenSilhouettePackets;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Objects;

/** Owns the capped connection-scoped rank-ten silhouette state independently of render resources. */
public final class ClientRankTenSilhouetteManager {
	private static final int CAPACITY = 64;
	private static final String INITIAL_DIMENSION = "minecraft:overworld";
	private static RankTenSilhouetteClientOwnership ownership =
			RankTenSilhouetteClientOwnership.empty(1, INITIAL_DIMENSION);
	private static ClientRankTenSilhouetteState state = ClientRankTenSilhouetteState.empty(CAPACITY,
			ownership.connectionEpoch(), ownership.dimension());
	private static Object observedWorld;
	private static boolean pendingWorldReset;

	private ClientRankTenSilhouetteManager() {
	}

	/** Captures the handler owner before the network callback is queued onto the client thread. */
	public static synchronized RankTenSilhouetteClientOwnership.HandlerStamp captureHandlerStamp(
			Minecraft client) {
		observeWorld(client);
		return ownership.stamp();
	}

	/** Applies one validated wire only while its captured connection and dimension still own it. */
	public static synchronized void handle(RankTenSilhouettePackets.Payload payload,
			RankTenSilhouetteClientOwnership.HandlerStamp captured) {
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(captured, "captured");
		Minecraft client = Minecraft.getInstance();
		observeWorld(client);
		String currentDimension = dimension(client);
		resetStateForWorldChange(currentDimension);
		if (!ownership.canAccept(captured, payload.eventId())) return;
		ClientRankTenSilhouetteState received = state.receive(payload.wire(), state.lifecycleTick(),
				captured.connectionEpoch(), captured.dimension());
		if (received == state) return;
		state = received;
		ownership = ownership.accept(captured, payload.eventId());
	}

	/** Advances receipt-local lifetime and clears all entries at an observed dimension boundary. */
	public static synchronized void tick(Minecraft client) {
		observeWorld(client);
		String currentDimension = dimension(client);
		resetStateForWorldChange(currentDimension);
		state = state.tick();
	}

	/** Starts a new connection epoch so delayed callbacks from the prior session fail closed. */
	public static synchronized void resetConnectionEpoch() {
		long nextEpoch = ownership.connectionEpoch() == Long.MAX_VALUE
				? 1 : ownership.connectionEpoch() + 1;
		ownership = ownership.resetConnection(nextEpoch, INITIAL_DIMENSION);
		state = state.reset(ownership.connectionEpoch(), ownership.dimension());
		observedWorld = null;
		pendingWorldReset = false;
	}

	/** Returns the immutable deterministic render snapshot owned by the accepted shared state. */
	public static synchronized List<ClientRankTenSilhouetteState.Entry> entries() {
		return state.entries();
	}

	/** Returns receipt-local time for pure lifecycle-and-seed phase derivation. */
	public static synchronized long lifecycleTick() {
		return state.lifecycleTick();
	}

	/** Renderer resources are reloadable; logical semantic entries deliberately survive. */
	public static synchronized void rendererResourcesClosed() {
		state = state.rendererResourcesClosed();
	}

	/** Renderer resources are reloadable; logical semantic entries deliberately survive. */
	public static synchronized void rendererResourcesRecreated() {
		state = state.rendererResourcesRecreated();
	}

	private static void observeWorld(Minecraft client) {
		Object currentWorld = client.level;
		String currentDimension = dimension(client);
		if (currentWorld != observedWorld) {
			observedWorld = currentWorld;
			ownership = ownership.advanceWorld(currentDimension);
			pendingWorldReset = true;
		} else {
			ownership = ownership.observeDimension(currentDimension);
		}
	}

	private static void resetStateForWorldChange(String currentDimension) {
		if (!pendingWorldReset && currentDimension.equals(state.dimension())) return;
		state = state.reset(ownership.connectionEpoch(), currentDimension);
		pendingWorldReset = false;
	}

	private static String dimension(Minecraft client) {
		return client.level == null ? ownership.dimension()
				: client.level.dimension().identifier().toString();
	}
}
