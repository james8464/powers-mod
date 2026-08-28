package com.powers.network;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

final class CastingPosePacketsTest {
	@Test
	void payloadConvertsClosedWireIdsToSemanticEvent() throws Exception {
		Class<?> payloadType = requirePayloadType();
		Object payload = payloadType.getConstructors()[0].newInstance(17,
				UUID.fromString("22222222-2222-2222-2222-222222222222"), 9L,
				1, 3, 2, 400L, 20, false);
		Object event = payloadType.getMethod("event").invoke(payload);
		assertEquals("PROJECT", event.getClass().getMethod("pose").invoke(event).toString());
		assertEquals("HERALD_LIGHT", event.getClass().getMethod("style").invoke(event).toString());
		assertEquals("RIGHT", event.getClass().getMethod("hand").invoke(event).toString());
		assertEquals(400L, event.getClass().getMethod("startGameTime").invoke(event));
		assertEquals(false, event.getClass().getMethod("terminal").invoke(event));
	}

	@Test
	void payloadRejectsUnknownWireIdsBeforeClientMutation() throws Exception {
		var constructor = requirePayloadType().getConstructors()[0];
		Object[] valid = {17, UUID.fromString("22222222-2222-2222-2222-222222222222"),
				9L, 1, 3, 2, 400L, 20, false};
		for (int index : new int[] {3, 4, 5}) {
			Object[] invalid = valid.clone();
			invalid[index] = 99;
			assertThrows(InvocationTargetException.class, () -> constructor.newInstance(invalid));
		}
	}

	private static Class<?> requirePayloadType() {
		try {
			return Class.forName("com.powers.network.CastingPosePackets$Payload");
		} catch (ClassNotFoundException missing) {
			fail("CastingPosePackets.Payload is not implemented");
			throw new AssertionError(missing);
		}
	}
}
