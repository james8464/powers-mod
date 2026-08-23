package com.powers.client.fx;

import com.powers.fx.ClientVisualScarState;
import com.powers.fx.VisualScarDeliveryModel;
import com.powers.network.MagicFxPackets;
import com.powers.network.VisualScarResyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Owns the capped semantic client state independently from reloadable renderer resources. */
public final class ClientVisualScarManager {
	private static final int CAPACITY = 2_048;
	private static volatile long connectionEpoch = 1;
	private static volatile String dimension = "minecraft:overworld";
	private static ClientVisualScarState state = ClientVisualScarState.empty(CAPACITY, connectionEpoch);
	private static long localTick;
	private static boolean resyncRequested;
	private static long lastResyncRequestTick;

	private ClientVisualScarManager() {
	}

	/** Applies one payload only when its captured connection and dimension still own the handler. */
	public static void handle(MagicFxPackets.ScarFxPayload payload,
			ClientVisualScarState.HandlerStamp captured) {
		Minecraft client = Minecraft.getInstance();
		String currentDimension = dimension(client);
		if (!currentDimension.equals(dimension)) {
			dimension = currentDimension;
			state = state.reset(ClientVisualScarState.Reset.DIMENSION_CHANGE, connectionEpoch);
			localTick = 0;
			resyncRequested = false;
		}
		ClientVisualScarState.HandlerStamp current = new ClientVisualScarState.HandlerStamp(
				connectionEpoch, currentDimension);
		if (!Objects.equals(captured, current)) return;
		ClientVisualScarState.ReceiveResult received = state.receiveObserved(
				payload.wire(), localTick, connectionEpoch);
		state = received.state();
		if (received.outcome() == ClientVisualScarState.ReceiveOutcome.APPLIED_RESET) {
			resyncRequested = false;
		}
		if (received.needsAuthoritativeResync() && !resyncRequested
				&& ClientPlayNetworking.canSend(VisualScarResyncPayload.TYPE)) {
			sendResyncRequest();
		}
	}

	/** Captures the connection/dimension owner before a network callback is queued to the client. */
	public static ClientVisualScarState.HandlerStamp captureHandlerStamp(Minecraft client) {
		return new ClientVisualScarState.HandlerStamp(connectionEpoch, dimension(client));
	}

	/** Advances receipt-local leases and clears semantic state on a dimension boundary. */
	public static void tick(Minecraft client) {
		String current = dimension(client);
		if (!current.equals(dimension)) {
			dimension = current;
			state = state.reset(ClientVisualScarState.Reset.DIMENSION_CHANGE, connectionEpoch);
			localTick = 0;
			resyncRequested = false;
		}
		state = state.tickLifecycle(false);
		if (localTick < Long.MAX_VALUE) localTick++;
		if (resyncRequested && localTick - lastResyncRequestTick >= 40
				&& ClientPlayNetworking.canSend(VisualScarResyncPayload.TYPE)) {
			sendResyncRequest();
		}
	}

	/** Starts a fresh connection epoch; delayed handlers from the old connection become stale. */
	public static void resetConnectionEpoch() {
		if (connectionEpoch == Long.MAX_VALUE) connectionEpoch = 1;
		else connectionEpoch++;
		dimension = "minecraft:overworld";
		state = state.reset(ClientVisualScarState.Reset.CONNECTION_EPOCH, connectionEpoch);
		localTick = 0;
		resyncRequested = false;
	}

	/** Returns an immutable render snapshot without exposing the mutable lifecycle owner. */
	public static List<ClientVisualScarState.Entry> entries() {
		List<ClientVisualScarState.Entry> result = new ArrayList<>(state.size());
		for (VisualScarDeliveryModel.ScarKey key : state.generations().keySet()) {
			state.get(key.position(), key.face()).ifPresent(result::add);
		}
		return List.copyOf(result);
	}

	public static void rendererResourcesClosed() {
		state = state.rendererResourcesClosed();
	}

	public static void rendererResourcesRecreated() {
		state = state.rendererResourcesRecreated();
	}

	private static void sendResyncRequest() {
		ClientPlayNetworking.send(new VisualScarResyncPayload());
		resyncRequested = true;
		lastResyncRequestTick = localTick;
	}

	private static String dimension(Minecraft client) {
		return client.level == null ? dimension
				: client.level.dimension().identifier().toString();
	}
}
