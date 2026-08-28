package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import com.powers.audio.LayeredAudioService;
import com.powers.entity.TestActorPowerState;
import com.powers.network.LayeredAudioPackets;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
		var actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 2));
		actor.setHealth(13.0F);
		TestActorPowerState.setEnergy(actor.getUUID(), 713);
		BlockPos worldProbe = new BlockPos(1, 1, 1);
		helper.setBlock(worldProbe, Blocks.GOLD_BLOCK);
		UUID playerId = actor.getUUID();
		Fixture runtime = new Fixture(List.of(new LayeredAudioService.Observer(playerId,
				OVERWORLD, 0, 0, 0)), null);
		GameplaySnapshot before = new GameplaySnapshot(actor.getHealth(), actor.position(),
				TestActorPowerState.energy(playerId), helper.getBlockState(worldProbe));

		LayeredAudioService.deliver(runtime, Vec3.ZERO, LayeredAudioCue.RUNE_HUM, 1.0F, 1.0F);

		helper.assertTrue(runtime.sent.size() == 1, "Presentation payload was not delivered once");
		GameplaySnapshot after = new GameplaySnapshot(actor.getHealth(), actor.position(),
				TestActorPowerState.energy(playerId), helper.getBlockState(worldProbe));
		helper.assertTrue(before.equals(after),
				"Layered audio mutated actor health, energy, position, or world state");
		helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(playerId) == null,
				"Isolated layered-audio fixture polluted the global player list");
		TestActorPowerState.clear(playerId);
		helper.succeed();
	}

	private static LayeredAudioService.Observer observer(UUID id, Identifier dimension, double x) {
		return new LayeredAudioService.Observer(id, dimension, x, 0, 0);
	}

	private record Send(UUID observer, LayeredAudioPackets.Payload payload) { }
	private record GameplaySnapshot(float health, Vec3 position, int energy, BlockState worldState) { }

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
