package com.powers.audio;

import com.powers.network.LayeredAudioPackets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class LayeredAudioServiceTest {
	private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath("minecraft", "overworld");
	private static final Identifier NETHER = Identifier.fromNamespaceAndPath("minecraft", "the_nether");
	private static final UUID NEAR = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID EDGE = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID OUTSIDE = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID CROSS_DIMENSION = UUID.fromString("00000000-0000-0000-0000-000000000004");
	private static final UUID UNSUPPORTED = UUID.fromString("00000000-0000-0000-0000-000000000005");

	@Test
	void sendsExactlyOnceToEachSameDimensionCapableListenerInsideFarRadius() {
		FakeRuntime runtime = new FakeRuntime(120L, List.of(
				new LayeredAudioService.Observer(NEAR, OVERWORLD, 0, 64, 0),
				new LayeredAudioService.Observer(EDGE, OVERWORLD, 128, 64, 0),
				new LayeredAudioService.Observer(OUTSIDE, OVERWORLD, 128.01, 64, 0),
				new LayeredAudioService.Observer(CROSS_DIMENSION, NETHER, 0, 64, 0),
				new LayeredAudioService.Observer(UNSUPPORTED, OVERWORLD, 1, 64, 0)));

		int sent = LayeredAudioService.deliver(runtime, new Vec3(0, 64, 0),
				LayeredAudioCue.RIFT_OPEN, 0.75F, 1.0F);

		assertEquals(2, sent);
		assertEquals(List.of(NEAR, EDGE), runtime.sent.stream().map(Send::observer).toList());
		assertEquals(1L, runtime.sent.stream().filter(send -> send.observer().equals(NEAR)).count());
		assertEquals(1L, runtime.sent.stream().filter(send -> send.observer().equals(EDGE)).count());
		assertEquals(OVERWORLD, runtime.sent.getFirst().payload().dimension());
		assertEquals(120L, runtime.sent.getFirst().payload().emittedGameTime());
	}

	@Test
	void eventIdsAdvanceWithinTickAndResetSequenceOnNextTick() {
		FakeRuntime runtime = new FakeRuntime(7L,
				List.of(new LayeredAudioService.Observer(NEAR, OVERWORLD, 0, 64, 0)));
		LayeredAudioService.deliver(runtime, new Vec3(0, 64, 0), LayeredAudioCue.RUNE_HUM, 1, 1);
		LayeredAudioService.deliver(runtime, new Vec3(0, 64, 0), LayeredAudioCue.RUNE_HUM, 1, 1);
		long first = runtime.sent.get(0).payload().eventId();
		long second = runtime.sent.get(1).payload().eventId();
		assertNotEquals(first, second);

		runtime.gameTime = 8L;
		LayeredAudioService.deliver(runtime, new Vec3(0, 64, 0), LayeredAudioCue.RUNE_HUM, 1, 1);
		assertEquals(8L << LayeredAudioService.EVENT_SEQUENCE_BITS,
				runtime.sent.get(2).payload().eventId());
	}

	private record Send(UUID observer, LayeredAudioPackets.Payload payload) { }

	private static final class FakeRuntime implements LayeredAudioService.RuntimeAccess {
		private long gameTime;
		private final List<LayeredAudioService.Observer> observers;
		private final List<Send> sent = new ArrayList<>();

		private FakeRuntime(long gameTime, List<LayeredAudioService.Observer> observers) {
			this.gameTime = gameTime;
			this.observers = observers;
		}

		@Override public long gameTime() { return gameTime; }
		@Override public Identifier dimension() { return OVERWORLD; }
		@Override public List<LayeredAudioService.Observer> observers() { return observers; }
		@Override public boolean canSend(UUID observer) { return !observer.equals(UNSUPPORTED); }
		@Override public void send(UUID observer, LayeredAudioPackets.Payload payload) {
			sent.add(new Send(observer, payload));
		}
	}
}
