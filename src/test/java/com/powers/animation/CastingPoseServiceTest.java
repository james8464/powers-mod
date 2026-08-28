package com.powers.animation;

import com.powers.companion.ShadowCompanionEntity;
import com.powers.entity.DarknessCreature;
import com.powers.entity.EchoClone;
import com.powers.entity.FirstVessel;
import com.powers.entity.PowerTestActor;
import com.powers.entity.RadiantSentinel;
import com.powers.entity.RealmHerald;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class CastingPoseServiceTest {
	private static final UUID ENTITY = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID OBSERVER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID OBSERVER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Test
	void exactProductionScopeExcludesCloneAndTestActor() throws Exception {
		try {
			Class<?> type = Class.forName("com.powers.animation.CastingPoseService");
			var scopeType = type.getMethod("scopeType", Class.class);
			assertTrue((boolean) scopeType.invoke(null, ShadowCompanionEntity.class));
			assertTrue((boolean) scopeType.invoke(null, RadiantSentinel.class));
			assertTrue((boolean) scopeType.invoke(null, DarknessCreature.class));
			assertTrue((boolean) scopeType.invoke(null, RealmHerald.class));
			assertTrue((boolean) scopeType.invoke(null, FirstVessel.class));
			assertFalse((boolean) scopeType.invoke(null, EchoClone.class));
			assertFalse((boolean) scopeType.invoke(null, PowerTestActor.class));
		} catch (ReflectiveOperationException missing) {
			fail("CastingPoseService is not implemented");
		}
	}

	@Test
	void deliveryUsesOnlyTrackingCompatibleObservers() throws Exception {
		Class<?> runtimeType = requireRuntimeType();
		List<UUID> sent = new ArrayList<>();
		Object runtime = Proxy.newProxyInstance(runtimeType.getClassLoader(),
				new Class<?>[] {runtimeType}, (proxy, method, arguments) -> switch (method.getName()) {
					case "entityId" -> 33;
					case "entityUuid" -> ENTITY;
					case "dimension" -> "overworld";
					case "gameTime" -> 100L;
					case "eligible" -> true;
					case "trackingObservers" -> List.of(OBSERVER_A, OBSERVER_B);
					case "canSend" -> arguments[0].equals(OBSERVER_A);
					case "sendGuarded" -> { sent.add((UUID) arguments[0]); yield null; }
					default -> throw new UnsupportedOperationException(method.getName());
				});
		var deliver = CastingPoseService.class.getMethod("deliver", CastingPoseLedger.class,
				runtimeType, CastingPose.class, CastingStyle.class, CastingHand.class, int.class);
		@SuppressWarnings("unchecked")
		Optional<CastingPoseEvent> result = (Optional<CastingPoseEvent>) deliver.invoke(null,
				new CastingPoseLedger(), runtime, CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.RIGHT, 20);
		assertTrue(result.isPresent());
		assertEquals(List.of(OBSERVER_A), sent);
	}

	@Test
	void duplicateStartInSameTickDoesNotSendSecondPacket() throws Exception {
		Class<?> runtimeType = requireRuntimeType();
		List<UUID> sent = new ArrayList<>();
		Object runtime = Proxy.newProxyInstance(runtimeType.getClassLoader(),
				new Class<?>[] {runtimeType}, (proxy, method, arguments) -> switch (method.getName()) {
					case "entityId" -> 33;
					case "entityUuid" -> ENTITY;
					case "dimension" -> "overworld";
					case "gameTime" -> 100L;
					case "eligible" -> true;
					case "trackingObservers" -> List.of(OBSERVER_A);
					case "canSend" -> true;
					case "sendGuarded" -> { sent.add((UUID) arguments[0]); yield null; }
					default -> throw new UnsupportedOperationException(method.getName());
				});
		var ledger = new CastingPoseLedger();
		CastingPoseService.deliver(ledger, (CastingPoseService.RuntimeAccess) runtime,
				CastingPose.PROJECT, CastingStyle.RADIANT, CastingHand.RIGHT, 20);
		CastingPoseService.deliver(ledger, (CastingPoseService.RuntimeAccess) runtime,
				CastingPose.CHANNEL, CastingStyle.RADIANT, CastingHand.BOTH, 20);
		assertEquals(List.of(OBSERVER_A), sent);
	}

	@Test
	void ineligibleRuntimeCannotCreateOrDeliverPose() throws Exception {
		Class<?> runtimeType = requireRuntimeType();
		Object runtime = Proxy.newProxyInstance(runtimeType.getClassLoader(),
				new Class<?>[] {runtimeType}, (proxy, method, arguments) -> switch (method.getName()) {
					case "entityId" -> 33;
					case "entityUuid" -> ENTITY;
					case "dimension" -> "overworld";
					case "gameTime" -> 100L;
					case "eligible" -> false;
					case "trackingObservers" -> fail("ineligible runtime scanned tracking observers");
					case "canSend", "sendGuarded" -> fail("ineligible runtime attempted delivery");
					default -> throw new UnsupportedOperationException(method.getName());
				});
		var deliver = CastingPoseService.class.getMethod("deliver", CastingPoseLedger.class,
				runtimeType, CastingPose.class, CastingStyle.class, CastingHand.class, int.class);
		assertEquals(Optional.empty(), deliver.invoke(null, new CastingPoseLedger(), runtime,
				CastingPose.PROJECT, CastingStyle.RADIANT, CastingHand.RIGHT, 20));
	}

	private static Class<?> requireRuntimeType() {
		try {
			return Class.forName("com.powers.animation.CastingPoseService$RuntimeAccess");
		} catch (ClassNotFoundException missing) {
			fail("CastingPoseService.RuntimeAccess is not implemented");
			throw new AssertionError(missing);
		}
	}
}
