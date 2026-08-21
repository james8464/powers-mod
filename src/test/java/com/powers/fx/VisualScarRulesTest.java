package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisualScarRulesTest {
	@Test
	void supportAndFaceOriginFactsAreIndependentlyDeniedIncludingCrossChunkCases() {
		var valid = support(true, true, true, false, false, true, true,
				true, false, false);
		assertEquals(VisualScarRules.Admission.ALLOW, VisualScarRules.admit(valid));
		assertEquals(VisualScarRules.Admission.DENY_SUPPORT_UNLOADED,
				VisualScarRules.admit(support(false, true, true, false, false, true, true,
						true, false, false)));
		assertEquals(VisualScarRules.Admission.DENY_ORIGIN_UNLOADED,
				VisualScarRules.admit(support(true, false, true, false, false, true, true,
						true, false, false)));
		assertEquals(VisualScarRules.Admission.DENY_SUPPORT_PROTECTED,
				VisualScarRules.admit(with(valid, 2, false)));
		assertEquals(VisualScarRules.Admission.DENY_SUPPORT_BLOCK_ENTITY,
				VisualScarRules.admit(with(valid, 3, true)));
		assertEquals(VisualScarRules.Admission.DENY_SUPPORT_FLUID,
				VisualScarRules.admit(with(valid, 4, true)));
		assertEquals(VisualScarRules.Admission.DENY_SUPPORT_FACE,
				VisualScarRules.admit(with(valid, 5, false)));
		assertEquals(VisualScarRules.Admission.DENY_SUPPORT_MATERIAL,
				VisualScarRules.admit(with(valid, 6, false)));
		assertEquals(VisualScarRules.Admission.DENY_ORIGIN_OCCLUDED,
				VisualScarRules.admit(with(valid, 7, false)));
		assertEquals(VisualScarRules.Admission.DENY_ORIGIN_BLOCK_ENTITY,
				VisualScarRules.admit(with(valid, 8, true)));
		assertEquals(VisualScarRules.Admission.DENY_ORIGIN_FLUID,
				VisualScarRules.admit(with(valid, 9, true)));
	}

	@Test
	void exactSupportClassificationIsClosedAndAmbiguityFails() {
		for (int index = 0; index < 6; index++) {
			boolean[] facts = new boolean[6];
			facts[index] = true;
			assertEquals(Optional.of(VisualScarRules.Material.values()[index]),
					VisualScarRules.classify(new VisualScarRules.MaterialFacts(
							facts[0], facts[1], facts[2], facts[3], facts[4], facts[5])));
		}
		assertEquals(Optional.empty(), VisualScarRules.classify(
				new VisualScarRules.MaterialFacts(false, false, false, false, false, false)));
		assertEquals(Optional.empty(), VisualScarRules.classify(
				new VisualScarRules.MaterialFacts(true, true, false, false, false, false)));
		assertEquals(5, VisualScarRules.Impact.values().length);
		assertEquals(6, VisualScarRules.Face.values().length);
	}

	@Test
	void hardCapsCannotBeRaisedAndContractCannotMutateOrPersist() {
		var limits = VisualScarRules.Limits.hardCeilings();
		assertEquals(128, limits.activePerOwner());
		assertEquals(2_048, limits.activeGlobal());
		assertEquals(128, limits.queuedPerOwner());
		assertEquals(2_048, limits.queuedGlobal());
		assertEquals(64, limits.requestsPerTick());
		assertEquals(64, limits.revalidationsPerTick());
		assertEquals(256, limits.sendsPerTick());
		assertEquals(2_048, limits.pendingPerObserver());
		assertEquals(32_768, limits.pendingGlobal());
		assertEquals(1_200, limits.maximumLease());
		assertThrows(IllegalArgumentException.class, () -> limits.withQueuedPerOwner(129));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Limits(
				129, 2_048, 128, 2_048, 64, 64, 256, 2_048, 32_768, 1_200));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Limits(
				128, 2_048, 0, 2_048, 64, 64, 256, 2_048, 32_768, 1_200));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Limits(
				128, 2_048, 128, 2_048, 65, 64, 256, 2_048, 32_768, 1_200));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Limits(
				128, 2_048, 128, 2_048, 64, 65, 256, 2_048, 32_768, 1_200));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Limits(
				128, 2_048, 128, 2_048, 64, 64, 257, 2_048, 32_768, 1_200));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Limits(
				128, 2_048, 128, 2_048, 64, 64, 256, 2_048, 32_768, 39));
		var contract = VisualScarRules.presentationOnly();
		assertFalse(contract.mutatesTerrain());
		assertFalse(contract.loadsChunks());
		assertFalse(contract.persists());
		assertFalse(contract.registersWorldObjects());
	}

	@Test
	void sameKeyRequestsCoalesceLatestWithoutOwnerCapLaundering() {
		UUID owner = new UUID(0, 1);
		var old = new VisualScarRules.Request("minecraft:overworld", 42,
				VisualScarRules.Face.UP, owner, VisualScarRules.Impact.BEAM, 1, 100);
		var latest = new VisualScarRules.Request("minecraft:overworld", 42,
				VisualScarRules.Face.UP, owner, VisualScarRules.Impact.FIRE, 2, 110);
		assertEquals(latest, VisualScarRules.coalesce(old, latest).orElseThrow());
		assertEquals(Optional.empty(), VisualScarRules.coalesce(old,
				new VisualScarRules.Request("minecraft:overworld", 42, VisualScarRules.Face.UP,
						new UUID(0, 2), VisualScarRules.Impact.ICE, 3, 120)));
		assertThrows(IllegalArgumentException.class, () -> new VisualScarRules.Request(
				"minecraft:overworld", 42, VisualScarRules.Face.UP, owner,
				VisualScarRules.Impact.BEAM, 1, -1));
	}

	private static VisualScarRules.SupportFacts support(boolean supportLoaded, boolean originLoaded,
			boolean supportPolicy, boolean supportBlockEntity, boolean supportFluid,
			boolean sturdyFace, boolean classifiable, boolean originOpen,
			boolean originBlockEntity, boolean originFluid) {
		return new VisualScarRules.SupportFacts(supportLoaded, originLoaded, supportPolicy,
				supportBlockEntity, supportFluid, sturdyFace, classifiable, originOpen,
				originBlockEntity, originFluid);
	}

	private static VisualScarRules.SupportFacts with(VisualScarRules.SupportFacts facts,
			int field, boolean value) {
		boolean[] values = {facts.supportLoaded(), facts.originLoaded(), facts.supportPolicy(),
				facts.supportBlockEntity(), facts.supportFluid(), facts.sturdyFace(),
				facts.classifiable(), facts.originOpen(), facts.originBlockEntity(), facts.originFluid()};
		values[field] = value;
		return support(values[0], values[1], values[2], values[3], values[4],
				values[5], values[6], values[7], values[8], values[9]);
	}
}
