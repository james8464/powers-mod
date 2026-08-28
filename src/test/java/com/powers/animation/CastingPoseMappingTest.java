package com.powers.animation;

import com.powers.boss.FirstVesselPowerAction;
import com.powers.companion.combat.ShadowPowerExecutor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

final class CastingPoseMappingTest {
	@Test
	void everyFirstVesselKindHasOneStablePoseFamily() throws Exception {
		Map<FirstVesselPowerAction.Kind, CastingPose> expected = Map.of(
				FirstVesselPowerAction.Kind.MOBILITY, CastingPose.INVOKE,
				FirstVesselPowerAction.Kind.PROJECTILE, CastingPose.PROJECT,
				FirstVesselPowerAction.Kind.BEAM, CastingPose.CHANNEL,
				FirstVesselPowerAction.Kind.AREA, CastingPose.INVOKE,
				FirstVesselPowerAction.Kind.CONTROL, CastingPose.INVOKE,
				FirstVesselPowerAction.Kind.DEFENSE, CastingPose.INVOKE,
				FirstVesselPowerAction.Kind.RECOVERY, CastingPose.CHANNEL);
		Method method = mappingMethod("forFirstVessel", FirstVesselPowerAction.Kind.class);
		for (var entry : expected.entrySet()) {
			assertEquals(entry.getValue(), method.invoke(null, entry.getKey()), entry.getKey().name());
		}
	}

	@Test
	void everySupportedShadowHandlerHasOneStablePoseAndDuration() throws Exception {
		Map<ShadowPowerExecutor.Handler, CastingPose> expected = Map.ofEntries(
				Map.entry(ShadowPowerExecutor.Handler.MOBILITY, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.PROJECTILE, CastingPose.PROJECT),
				Map.entry(ShadowPowerExecutor.Handler.BEAM, CastingPose.CHANNEL),
				Map.entry(ShadowPowerExecutor.Handler.AREA, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.CONTROL, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.DEFENSE, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.RECOVERY, CastingPose.CHANNEL),
				Map.entry(ShadowPowerExecutor.Handler.TOGGLE, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.MIND, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.SUMMON, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.TERRAIN, CastingPose.INVOKE),
				Map.entry(ShadowPowerExecutor.Handler.APOTHEOSIS, CastingPose.RELEASE));
		Method pose = mappingMethod("forShadow", ShadowPowerExecutor.Handler.class);
		Method duration = mappingMethod("duration", ShadowPowerExecutor.Handler.class);
		for (var entry : expected.entrySet()) {
			assertEquals(entry.getValue(), pose.invoke(null, entry.getKey()), entry.getKey().name());
			int ticks = (int) duration.invoke(null, entry.getKey());
			assertEquals(true, ticks >= 1 && ticks <= 120, entry.getKey().name());
		}
		InvocationTargetException unsupported = assertThrows(InvocationTargetException.class,
				() -> pose.invoke(null, ShadowPowerExecutor.Handler.UNSUPPORTED));
		assertEquals(IllegalArgumentException.class, unsupported.getCause().getClass());
	}

	@Test
	void actionHandsAreLiteralAndStable() throws Exception {
		Method hand = mappingMethod("hand", String.class);
		assertEquals(CastingHand.RIGHT, hand.invoke(null, "fireball"));
		assertEquals(CastingHand.RIGHT, hand.invoke(null, "void_beam"));
		assertEquals(CastingHand.LEFT, hand.invoke(null, "energy_drain"));
		assertEquals(CastingHand.BOTH, hand.invoke(null, "starfall"));
		assertEquals(CastingHand.BOTH, hand.invoke(null, "nightfall_dominion"));
	}

	private static Method mappingMethod(String name, Class<?> parameter) {
		try {
			return Class.forName("com.powers.animation.CastingPoseMapping").getMethod(name, parameter);
		} catch (ReflectiveOperationException missing) {
			fail("CastingPoseMapping is not implemented: " + missing.getMessage());
			throw new AssertionError(missing);
		}
	}
}
