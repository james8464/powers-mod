package com.powers.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards stateful HUD fields carried by the normal deduplicated sync packet. */
class PowerStatePayloadTest {
	@Test
	void elementalPhaseSurvivesPayloadConstruction() {
		PowerStatePayload payload = new PowerStatePayload(
				List.of("powers:elemental_blast"), List.of(), List.of(0), List.of(120), List.of(0),
				200, 250, false, false, false, 3, List.of(), "", 0);

		assertEquals(3, payload.elementalPhase());
	}

	@Test
	void stateSurvivesNetworkRoundTrip() {
		PowerStatePayload expected = new PowerStatePayload(
				List.of("powers:elemental_blast"), List.of("powers:flight"),
				List.of(17), List.of(120), List.of(41), 200, 250, true, false, true, 2,
				List.of("initiate", "conduit"), "conduit", 4);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			PowerStatePayload.STREAM_CODEC.encode(buffer, expected);

			PowerStatePayload actual = PowerStatePayload.STREAM_CODEC.decode(buffer);

			assertEquals(expected, actual);
		} finally {
			bytes.release();
		}
	}

	@Test
	void stateSnapshotDoesNotAliasMutableSources() {
		List<String> powers = new ArrayList<>(List.of("powers:elemental_blast"));
		List<Integer> reactivations = new ArrayList<>(List.of(41));
		List<String> rankNodes = new ArrayList<>(List.of("initiate"));
		PowerStatePayload payload = new PowerStatePayload(
				powers, List.of(), List.of(0), List.of(120), reactivations,
				200, 250, false, false, false, 3, rankNodes, "initiate", 1);

		powers.clear();
		reactivations.clear();
		rankNodes.clear();

		assertEquals(List.of("powers:elemental_blast"), payload.powerIds());
		assertEquals(List.of(41), payload.reactivationTicks());
		assertEquals(List.of("initiate"), payload.rankNodes());
	}

	@Test
	void reactivationTimersAreNonNegativeAndAlignedToPowerSlots() {
		PowerStatePayload payload = new PowerStatePayload(
				List.of("powers:speed_burst", "powers:flight"), List.of(),
				List.of(80, 0), List.of(140, 0), List.of(-5),
				200, 250, false, false, false, 0, List.of(), "", 0);

		assertEquals(List.of(0, 0), payload.reactivationTicks());
	}
}
