package com.powers.testing.network;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.powers.fx.FxLodTier;
import com.powers.network.BodyProxyPackets;
import com.powers.network.ActionSubmissionService;
import com.powers.network.CelestialRuinPackets;
import com.powers.network.CompanionPackets;
import com.powers.network.MagicFxPackets;
import com.powers.network.RankPackets;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.VesselControlPackets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PacketFaultStreamsTest {
	@Test
	void independentStateOwnersNeverShareALatestWinsStream() {
		assertNotEquals(PacketFaultStreams.key(new BodyProxyPackets.BodySnapshotPayload(4, "")),
				PacketFaultStreams.key(new BodyProxyPackets.BodySnapshotPayload(5, "")));
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
		assertNotEquals(PacketFaultStreams.key(new CompanionPackets.StatePayload(
				first, 7L, true, false, "minecraft:overworld", 0, 0, 0, 0)),
				PacketFaultStreams.key(new CompanionPackets.StatePayload(
				second, 7L, true, false, "minecraft:overworld", 0, 0, 0, 0)));
		assertNotEquals(PacketFaultStreams.key(new CompanionPackets.StatusPayload(
				first, true, 1, 100, "follow", false, false, 0)),
				PacketFaultStreams.key(new VesselControlPackets.StatePayload(true)));
		assertNotEquals(PacketFaultStreams.key(new CelestialRuinPackets.Payload(
				CelestialRuinPackets.Phase.BEGIN, 0, 64, 0, 0, FxLodTier.NEAR)),
				PacketFaultStreams.key(new CelestialRuinPackets.Payload(
				CelestialRuinPackets.Phase.BEGIN, 100, 64, 100, 0, FxLodTier.NEAR)));
	}

	@Test
	void favouriteSlotsAndRankNodesRemainIndependentCommands() {
		assertNotEquals(PacketFaultStreams.key(new ShadowSwordPackets.BindFavouritePayload(
				7L, "darkness", 0, "innate/fireball")),
				PacketFaultStreams.key(new ShadowSwordPackets.BindFavouritePayload(
				7L, "darkness", 1, "innate/fireball")));
		assertNotEquals(PacketFaultStreams.key(new RankPackets.RankActionPayload("one", true)),
				PacketFaultStreams.key(new RankPackets.RankActionPayload("two", true)));
	}

	@Test
	void menuInvalidationsForDifferentSurfacesDoNotSuppressEachOther() {
		assertNotEquals(PacketFaultStreams.key(new ActionSubmissionService.RefreshPayload(7L, "artifact")),
				PacketFaultStreams.key(new ActionSubmissionService.RefreshPayload(7L, "crystal")));
	}

	@Test
	void independentScarKeysNeverSuppressEachOther() {
		var first = new MagicFxPackets.ScarFxPayload(1, 42, 1, 0, 0, 7, 1, 40);
		var second = new MagicFxPackets.ScarFxPayload(1, 43, 1, 0, 0, 8, 1, 40);
		var otherFace = new MagicFxPackets.ScarFxPayload(1, 42, 2, 0, 0, 9, 1, 40);
		var origin = new MagicFxPackets.ScarFxPayload(0, 0, 0, 0, 0, 9, 1, 40);
		var reset = new MagicFxPackets.ScarFxPayload(2, 0, 0, 0, 0, 0, 1, 1);
		assertNotEquals(PacketFaultStreams.key(first), PacketFaultStreams.key(second));
		assertNotEquals(PacketFaultStreams.key(first), PacketFaultStreams.key(otherFace));
		assertNotEquals(PacketFaultStreams.key(origin), PacketFaultStreams.key(reset));
	}
}
