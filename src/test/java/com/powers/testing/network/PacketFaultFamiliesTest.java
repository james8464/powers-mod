package com.powers.testing.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.powers.network.ActionSubmissionService;
import com.powers.network.BodyProxyPackets;
import com.powers.network.CelestialRuinPackets;
import com.powers.network.CompanionPackets;
import com.powers.network.CrystalSelectorPackets;
import com.powers.network.GrimoirePackets;
import com.powers.network.LayeredAudioPackets;
import com.powers.network.MagicFxPackets;
import com.powers.network.PowerStatePayload;
import com.powers.network.PowersPackets;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.VesselControlPackets;
import com.powers.network.VisualScarResyncPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class PacketFaultFamiliesTest {
	@Test
	void everyAcceptancePayloadMapsToItsSemanticFamily() {
		assertEquals(PacketFaultFamily.ARTIFACT_SELECTION,
				PacketFaultFamilies.classify(ShadowSwordPackets.SelectPayload.TYPE));
		assertEquals(PacketFaultFamily.ARTIFACT_CYCLE,
				PacketFaultFamilies.classify(ShadowSwordPackets.CyclePayload.TYPE));
		assertEquals(PacketFaultFamily.ARTIFACT_BINDING,
				PacketFaultFamilies.classify(ShadowSwordPackets.BindFavouritePayload.TYPE));
		assertEquals(PacketFaultFamily.GRIMOIRE_SELECTION,
				PacketFaultFamilies.classify(GrimoirePackets.SelectSpellPayload.TYPE));
		assertEquals(PacketFaultFamily.CRYSTAL_SELECTION,
				PacketFaultFamilies.classify(CrystalSelectorPackets.SelectPayload.TYPE));
		assertEquals(PacketFaultFamily.VESSEL_INPUT,
				PacketFaultFamilies.classify(VesselControlPackets.InputPayload.TYPE));
		assertEquals(PacketFaultFamily.VESSEL_RELEASE,
				PacketFaultFamilies.classify(VesselControlPackets.ReleasePayload.TYPE));
		assertEquals(PacketFaultFamily.LOCATOR_REQUEST,
				PacketFaultFamilies.classify(PowersPackets.LocateTargetPayload.TYPE));
		assertEquals(PacketFaultFamily.MAGIC_FX,
				PacketFaultFamilies.classify(MagicFxPackets.MagicFxPayload.TYPE));
		assertEquals(PacketFaultFamily.SCAR_FX,
				PacketFaultFamilies.classify(MagicFxPackets.ScarFxPayload.TYPE));
		assertEquals(PacketFaultFamily.SCAR_FX,
				PacketFaultFamilies.classify(VisualScarResyncPayload.TYPE));
		assertEquals(PacketFaultFamily.BEAM_FX,
				PacketFaultFamilies.classify(MagicFxPackets.BeamFxPayload.TYPE));
		assertEquals(PacketFaultFamily.SHAPE_FX,
				PacketFaultFamilies.classify(MagicFxPackets.ShapeFxPayload.TYPE));
		assertEquals(PacketFaultFamily.SEMANTIC_BATCH,
				PacketFaultFamilies.classify(MagicFxPackets.SemanticFxBatchPayload.TYPE));
		assertEquals(PacketFaultFamily.CELESTIAL_RUIN,
				PacketFaultFamilies.classify(CelestialRuinPackets.Payload.TYPE));
		assertEquals(PacketFaultFamily.EVENT_AUDIO,
				PacketFaultFamilies.classify(LayeredAudioPackets.Payload.TYPE));
		assertEquals(PacketFaultFamily.COMPANION_SNAPSHOT,
				PacketFaultFamilies.classify(CompanionPackets.StatePayload.TYPE));
		assertEquals(PacketFaultFamily.BODY_SNAPSHOT,
				PacketFaultFamilies.classify(BodyProxyPackets.BodySnapshotPayload.TYPE));
		assertEquals(PacketFaultFamily.POWER_STATE,
				PacketFaultFamilies.classify(PowerStatePayload.TYPE));
		assertEquals(PacketFaultFamily.MENU_SNAPSHOT,
				PacketFaultFamilies.classify(ActionSubmissionService.RefreshPayload.TYPE));
	}

	@Test
	void foreignPayloadIdentifiersAreExplicitlyOutsideTheFaultBoundary() {
		CustomPacketPayload.Type<?> foreign = new CustomPacketPayload.Type<>(
				Identifier.fromNamespaceAndPath("example", "state"));

		assertEquals(false, PacketFaultFamilies.isProjectOwned(foreign));
		assertEquals(PacketFaultFamily.OTHER, PacketFaultFamilies.classify(foreign));
	}
}
