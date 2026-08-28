package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

final class CastingPoseContractTest {
	@Test
	void invalidDurationCannotConstructPoseEvent() throws Exception {
		Class<?> type = requireType("CastingPoseEvent");
		Constructor<?> constructor = type.getConstructors()[0];
		Object[] valid = eventArguments();
		assertRejected(constructor, replacing(valid, 7, 0));
		assertRejected(constructor, replacing(valid, 7, 121));
	}

	@Test
	void invalidIdentitySequenceAndOverflowAreRejected() throws Exception {
		Constructor<?> constructor = requireType("CastingPoseEvent").getConstructors()[0];
		Object[] valid = eventArguments();
		assertRejected(constructor, replacing(valid, 0, -1));
		assertRejected(constructor, replacing(valid, 1, new UUID(0L, 0L)));
		assertRejected(constructor, replacing(valid, 2, 0L));
		assertRejected(constructor, replacing(valid, 6, -1L));
		Object[] overflowing = replacing(valid, 6, Long.MAX_VALUE);
		assertRejected(constructor, replacing(overflowing, 7, 120));
	}

	@Test
	void closedNetworkIdsRoundTripAndRejectUnknownValues() throws Exception {
		assertEnumContract("CastingPose", List.of("INVOKE", "PROJECT", "CHANNEL", "RELEASE"));
		assertEnumContract("CastingStyle", List.of("SHADOW", "RADIANT", "DARKNESS",
				"HERALD_LIGHT", "HERALD_DARK", "FIRST_VESSEL"));
		assertEnumContract("CastingHand", List.of("NONE", "LEFT", "RIGHT", "BOTH"));
	}

	@Test
	void authoritativeTimeDeterminesProgressAndExpiry() throws Exception {
		Class<?> eventType = requireType("CastingPoseEvent");
		Object event = eventType.getConstructors()[0].newInstance(7,
				UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L,
				enumValue("com.powers.animation.CastingPose", "PROJECT"),
				enumValue("com.powers.animation.CastingStyle", "RADIANT"),
				enumValue("com.powers.animation.CastingHand", "RIGHT"), 20L, 20);
		Class<?> rules = requireType("CastingPoseRules");
		Method progress = rules.getMethod("progress", long.class, eventType);
		Method active = rules.getMethod("active", long.class, eventType);
		assertEquals(0.0, (double) progress.invoke(null, 20L, event));
		assertEquals(0.5, (double) progress.invoke(null, 30L, event));
		assertTrue((boolean) active.invoke(null, 39L, event));
		assertFalse((boolean) active.invoke(null, 40L, event));
	}

	private static void assertEnumContract(String simpleName, List<String> names) throws Exception {
		Class<?> type = requireType(simpleName);
		Method networkId = type.getMethod("networkId");
		Method fromNetworkId = type.getMethod("fromNetworkId", int.class);
		for (int id = 0; id < names.size(); id++) {
			Object value = enumValue(type.getName(), names.get(id));
			assertEquals(id, networkId.invoke(value));
			assertEquals(Optional.of(value), fromNetworkId.invoke(null, id));
		}
		assertEquals(Optional.empty(), fromNetworkId.invoke(null, names.size()));
		assertEquals(Optional.empty(), fromNetworkId.invoke(null, -1));
	}

	private static Object[] eventArguments() throws Exception {
		return new Object[] {7, UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L,
				enumValue("com.powers.animation.CastingPose", "PROJECT"),
				enumValue("com.powers.animation.CastingStyle", "RADIANT"),
				enumValue("com.powers.animation.CastingHand", "RIGHT"), 100L, 20};
	}

	private static Object[] replacing(Object[] values, int index, Object replacement) {
		Object[] copy = values.clone();
		copy[index] = replacement;
		return copy;
	}

	private static void assertRejected(Constructor<?> constructor, Object[] arguments) {
		assertThrows(InvocationTargetException.class, () -> constructor.newInstance(arguments));
	}

	private static Class<?> requireType(String simpleName) {
		try {
			return Class.forName("com.powers.animation." + simpleName);
		} catch (ClassNotFoundException missing) {
			fail(simpleName + " is not implemented");
			throw new AssertionError(missing);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object enumValue(String type, String name) throws ClassNotFoundException {
		return Enum.valueOf((Class<? extends Enum>) Class.forName(type), name);
	}
}
