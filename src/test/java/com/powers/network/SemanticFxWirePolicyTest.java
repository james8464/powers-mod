package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticFxWirePolicyTest {
	@Test
	void choosesOnlyAnActuallySmallerCompressedBatch() {
		byte[] repetitiveBatch = new byte[512];
		java.util.Arrays.fill(repetitiveBatch, (byte) 0x35);
		var profitable = SemanticFxWirePolicy.decide(
				List.of(new byte[128], new byte[128], new byte[128], new byte[128]),
				repetitiveBatch, 256);
		assertTrue(profitable.batch());
		assertTrue(profitable.batchWireBytes() < profitable.individualWireBytes());

		byte[] incompressible = new byte[512];
		new java.util.Random(15L).nextBytes(incompressible);
		var regression = SemanticFxWirePolicy.decide(
				List.of(java.util.Arrays.copyOfRange(incompressible, 0, 128),
						java.util.Arrays.copyOfRange(incompressible, 128, 256),
						java.util.Arrays.copyOfRange(incompressible, 256, 384),
						java.util.Arrays.copyOfRange(incompressible, 384, 512)),
				incompressible, 256);
		assertFalse(regression.batch());
	}
}
