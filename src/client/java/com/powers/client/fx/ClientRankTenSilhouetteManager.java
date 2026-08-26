package com.powers.client.fx;

import com.powers.fx.ClientRankTenSilhouetteState;
import com.powers.network.RankTenSilhouettePackets;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Objects;

/** Owns the capped connection-scoped rank-ten silhouette state independently of render resources. */
public final class ClientRankTenSilhouetteManager {
	private static final int CAPACITY = 64;
	private static volatile long connectionEpoch = 1;
	private static volatile String dimension = "minecraft:overworld";
	private static ClientRankTenSilhouetteState state = ClientRankTenSilhouetteState.empty(CAPACITY,
			connectionEpoch, dimension);

	private ClientRankTenSilhouetteManager() {
	}

	/** Captures the handler owner before the network callback is queued onto the client thread. */
	public static HandlerStamp captureHandlerStamp(Minecraft client) {
		return new HandlerStamp(connectionEpoch, dimension(client));
	}

	/** Applies one validated wire only while its captured connection and dimension still own it. */
	public static void handle(RankTenSilhouettePackets.Payload payload, HandlerStamp captured) {
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(captured, "captured");
		Minecraft client = Minecraft.getInstance();
		String currentDimension = dimension(client);
		resetForDimensionChange(currentDimension);
		HandlerStamp current = new HandlerStamp(connectionEpoch, currentDimension);
		if (!captured.equals(current)) return;
		state = state.receive(payload.wire(), state.lifecycleTick(), captured.connectionEpoch(),
				captured.dimension());
	}

	/** Advances receipt-local lifetime and clears all entries at an observed dimension boundary. */
	public static void tick(Minecraft client) {
		resetForDimensionChange(dimension(client));
		state = state.tick();
	}

	/** Starts a new connection epoch so delayed callbacks from the prior session fail closed. */
	public static void resetConnectionEpoch() {
		connectionEpoch = connectionEpoch == Long.MAX_VALUE ? 1 : connectionEpoch + 1;
		dimension = "minecraft:overworld";
		state = state.reset(connectionEpoch, dimension);
	}

	/** Returns the immutable deterministic render snapshot owned by the accepted shared state. */
	public static List<ClientRankTenSilhouetteState.Entry> entries() {
		return state.entries();
	}

	/** Derives the normal-motion phase solely from lifecycle time and the server visual seed. */
	public static double animatedPhase(ClientRankTenSilhouetteState.Entry entry) {
		return (state.lifecycleTick() + Integer.toUnsignedLong(entry.wire().visualSeed())) * 0.12D;
	}

	/** Renderer resources are reloadable; logical semantic entries deliberately survive. */
	public static void rendererResourcesClosed() {
		state = state.rendererResourcesClosed();
	}

	/** Renderer resources are reloadable; logical semantic entries deliberately survive. */
	public static void rendererResourcesRecreated() {
		state = state.rendererResourcesRecreated();
	}

	private static void resetForDimensionChange(String currentDimension) {
		if (currentDimension.equals(dimension)) return;
		dimension = currentDimension;
		state = state.reset(connectionEpoch, currentDimension);
	}

	private static String dimension(Minecraft client) {
		return client.level == null ? dimension : client.level.dimension().identifier().toString();
	}

	/** Immutable network-handler ownership stamp. */
	public record HandlerStamp(long connectionEpoch, String dimension) {
		public HandlerStamp {
			if (connectionEpoch < 0 || dimension == null || dimension.isBlank()) {
				throw new IllegalArgumentException("invalid silhouette handler stamp");
			}
		}
	}
}
