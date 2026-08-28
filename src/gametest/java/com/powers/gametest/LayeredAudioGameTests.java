package com.powers.gametest;

import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import com.powers.audio.LayeredAudioService;
import com.powers.network.LayeredAudioPackets;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Live-server acceptance for semantic audio distance, capability, and mutation boundaries. */
@SuppressWarnings("removal")
public final class LayeredAudioGameTests {
	private static final Identifier OVERWORLD = Identifier.parse("minecraft:overworld");
	private static final Identifier NETHER = Identifier.parse("minecraft:the_nether");

	@GameTest(maxTicks = 20)
	public void exactDistanceAndRecipientBoundariesDeliverOnePayloadPerListener(GameTestHelper helper) {
		UUID near = new UUID(0, 1);
		UUID mid = new UUID(0, 2);
		UUID far = new UUID(0, 3);
		UUID outside = new UUID(0, 4);
		UUID otherDimension = new UUID(0, 5);
		UUID unsupported = new UUID(0, 6);
		Fixture runtime = new Fixture(List.of(
				observer(near, OVERWORLD, 20.0), observer(mid, OVERWORLD, 96.0),
				observer(far, OVERWORLD, 256.0), observer(outside, OVERWORLD, 256.001),
				observer(otherDimension, NETHER, 0.0), observer(unsupported, OVERWORLD, 1.0)),
				unsupported);

		int sent = LayeredAudioService.deliver(runtime, Vec3.ZERO,
				LayeredAudioCue.LIGHT_CHORUS, 1.0F, 1.0F);

		helper.assertTrue(sent == 3, "Expected exact near/mid/far recipients: " + runtime.sent);
		helper.assertTrue(runtime.sent.stream().map(Send::observer).toList()
				.equals(List.of(near, mid, far)), "Recipient boundary/order drifted: " + runtime.sent);
		helper.assertTrue(runtime.sent.stream().map(Send::observer).distinct().count() == sent,
				"A listener received more than one payload");
		helper.assertTrue(LayeredAudioCue.LIGHT_CHORUS.profile().layer(20.0).orElseThrow()
				== LayeredAudioLayer.NEAR, "Near boundary drifted");
		helper.assertTrue(LayeredAudioCue.LIGHT_CHORUS.profile().layer(20.001).orElseThrow()
				== LayeredAudioLayer.MID, "Mid lower boundary drifted");
		helper.assertTrue(LayeredAudioCue.LIGHT_CHORUS.profile().layer(96.001).orElseThrow()
				== LayeredAudioLayer.FAR, "Far lower boundary drifted");
		helper.assertTrue(LayeredAudioCue.LIGHT_CHORUS.profile().layer(256.001).isEmpty(),
				"Outside-final-radius listener remained eligible");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void audioDeliveryHasZeroGameplayMutation(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		float health = player.getHealth();
		Vec3 position = player.position();
		int inventoryCount = player.getInventory().countItem(net.minecraft.world.item.Items.STONE);
		Fixture runtime = new Fixture(List.of(new LayeredAudioService.Observer(player.getUUID(),
				OVERWORLD, 0, 0, 0)), null);

		LayeredAudioService.deliver(runtime, Vec3.ZERO, LayeredAudioCue.RUNE_HUM, 1.0F, 1.0F);

		helper.assertTrue(runtime.sent.size() == 1, "Presentation payload was not delivered once");
		helper.assertTrue(player.getHealth() == health && player.position().equals(position)
				&& player.getInventory().countItem(net.minecraft.world.item.Items.STONE) == inventoryCount,
				"Layered audio mutated health, position, or inventory");
		helper.succeed();
	}

	private static LayeredAudioService.Observer observer(UUID id, Identifier dimension, double x) {
		return new LayeredAudioService.Observer(id, dimension, x, 0, 0);
	}

	private record Send(UUID observer, LayeredAudioPackets.Payload payload) { }

	private static final class Fixture implements LayeredAudioService.RuntimeAccess {
		private final List<LayeredAudioService.Observer> observers;
		private final UUID unsupported;
		private final List<Send> sent = new ArrayList<>();

		private Fixture(List<LayeredAudioService.Observer> observers, UUID unsupported) {
			this.observers = observers;
			this.unsupported = unsupported;
		}

		@Override public long gameTime() { return 40L; }
		@Override public Identifier dimension() { return OVERWORLD; }
		@Override public List<LayeredAudioService.Observer> observers() { return observers; }
		@Override public boolean canSend(UUID observer) { return !observer.equals(unsupported); }
		@Override public void send(UUID observer, LayeredAudioPackets.Payload payload) {
			sent.add(new Send(observer, payload));
		}
	}
}
