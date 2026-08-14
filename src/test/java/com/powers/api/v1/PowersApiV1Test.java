package com.powers.api.v1;

import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.protection.PowerProtectionAdapters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowersApiV1Test {
	private final PowersApiRuntime runtime = new PowersApiRuntime();

	@AfterEach
	void closeEpoch() {
		runtime.stopServer();
	}

	@Test
	void exposesStableSemanticVersionAndBinaryFacingInterfaceShape() throws Exception {
		assertEquals(1, PowersApiV1.VERSION.major());
		assertEquals(0, PowersApiV1.VERSION.minor());
		assertEquals("1.0", PowersApiV1.VERSION.toString());
		assertTrue(PowersExtension.class.isInterface());
		assertTrue(CastContext.class.isInterface());
		assertTrue(PowersApiV1.class.getMethod("registerAction", ActionRegistration.class)
				.getReturnType().equals(RegistrationResult.class));
		assertTrue(PowersApiV1.class.getMethod("registerPresence", CastContext.class, PhysicalPresence.class)
				.getReturnType().equals(PresenceHandle.class));
		assertTrue(PowersApiV1.class.getMethod("registerProtectionService", ProtectionService.class)
				.getReturnType().equals(RegistrationResult.class));
	}

	@Test
	void publicApiDoesNotLinkClientOnlyMinecraftClasses() throws Exception {
		for (Class<?> type : List.of(PowersApiV1.class, PowersExtension.class, ActionRegistration.class,
				CastContext.class, PhysicalPresence.class, ProtectionService.class, LifecycleHook.class)) {
			String resource = "/" + type.getName().replace('.', '/') + ".class";
			byte[] bytes = type.getResourceAsStream(resource).readAllBytes();
			assertFalse(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)
					.contains("net/minecraft/client/"), type.getName());
		}
	}

	@Test
	void registrationIsDeterministicDuplicateSafeAndClosedAfterStartup() {
		List<String> order = new ArrayList<>();
		PowersExtension zed = extension("z.example", order);
		PowersExtension alpha = extension("a.example", order);

		runtime.startServer(null, List.of(zed, alpha, alpha));

		assertEquals(List.of("a.example", "z.example"), order);
		assertEquals(List.of("a.example", "z.example"), runtime.extensionIds());
		assertEquals(RegistrationResult.LATE,
				runtime.api().registerAction(action("late_action")));
	}

	@Test
	void lifecycleStartedHookWaitsForActualStartedBoundaryAndStopRemainsOrdered() {
		List<ApiLifecycleEvent> events = new ArrayList<>();
		runtime.startServer(null, List.of(new PowersExtension() {
			@Override public String id() { return "lifecycle.example"; }
			@Override public void register(PowersApiV1 api) {
				assertEquals(RegistrationResult.ACCEPTED, api.registerLifecycleHook(events::add));
			}
		}));

		assertTrue(events.isEmpty());
		runtime.serverStarted(null);
		assertEquals(List.of(ApiLifecycleEvent.SERVER_STARTED), events);
		runtime.stopServer();
		assertEquals(List.of(ApiLifecycleEvent.SERVER_STARTED, ApiLifecycleEvent.SERVER_STOPPING), events);
	}

	@Test
	void discoveryStopsBeforeCallingIdentityBeyondHardEntrypointLimit() {
		AtomicInteger identityCalls = new AtomicInteger();
		List<PowersExtension> discovered = java.util.stream.IntStream.range(0, 257)
				.<PowersExtension>mapToObj(index -> new PowersExtension() {
					@Override public String id() {
						identityCalls.incrementAndGet();
						return "bounded." + index;
					}
					@Override public void register(PowersApiV1 api) { }
				}).toList();

		runtime.startServer(null, discovered);

		assertEquals(256, identityCalls.get());
		assertEquals(256, runtime.extensionIds().size());
	}

	@Test
	void exceedingPerExtensionRegistrationLimitsRollsBackEachExtension() {
		List<RegistrationResult> actionResults = new ArrayList<>();
		PowersExtension actionFlood = new PowersExtension() {
			@Override public String id() { return "a.action_flood"; }
			@Override public void register(PowersApiV1 api) {
				for (int index = 0; index < 65; index++) {
					actionResults.add(api.registerAction(action("flood_action_" + index)));
				}
			}
		};
		List<RegistrationResult> protectionResults = new ArrayList<>();
		PowersExtension protectionFlood = new PowersExtension() {
			@Override public String id() { return "b.protection_flood"; }
			@Override public void register(PowersApiV1 api) {
				for (int index = 0; index < 17; index++) {
					protectionResults.add(api.registerProtectionService(
							new ProtectionService("flood_guard_" + index, index, request -> true)));
				}
			}
		};
		List<RegistrationResult> hookResults = new ArrayList<>();
		PowersExtension hookFlood = new PowersExtension() {
			@Override public String id() { return "c.hook_flood"; }
			@Override public void register(PowersApiV1 api) {
				for (int index = 0; index < 17; index++) {
					hookResults.add(api.registerLifecycleHook(event -> { }));
				}
			}
		};

		runtime.startServer(null, List.of(actionFlood, protectionFlood, hookFlood));

		assertEquals(RegistrationResult.LIMIT, actionResults.getLast());
		assertEquals(RegistrationResult.LIMIT, protectionResults.getLast());
		assertEquals(RegistrationResult.LIMIT, hookResults.getLast());
		assertTrue(runtime.extensionIds().isEmpty());
		assertTrue(MagicRuntime.catalogue().definition(new MagicActionId("flood_action_0")) == null);
		assertFalse(PowerProtectionAdapters.registeredIds().contains("flood_guard_0"));
	}

	@Test
	void extensionAndProtectionFailuresFailClosedWithoutPreventingOtherExtensions() {
		List<String> order = new ArrayList<>();
		PowersExtension broken = new PowersExtension() {
			@Override public String id() { return "a.broken"; }
			@Override public void register(PowersApiV1 api) { throw new IllegalStateException("boom"); }
		};
		PowersExtension healthy = extension("b.healthy", order);
		PowersExtension guarded = new PowersExtension() {
			@Override public String id() { return "c.guarded"; }
			@Override public void register(PowersApiV1 api) {
				assertEquals(RegistrationResult.INVALID, api.registerProtectionService(
						new ProtectionService("INVALID ID", 10, query -> true)));
				assertEquals(RegistrationResult.ACCEPTED, api.registerProtectionService(
						new ProtectionService("broken_guard", 10,
								query -> { throw new LinkageError("old adapter"); })));
			}
		};

		runtime.startServer(null, List.of(healthy, broken, guarded));

		assertEquals(List.of("b.healthy"), order);
		assertFalse(runtime.extensionIds().contains("a.broken"));
		assertFalse(PowerProtectionAdapters.allows(new com.powers.protection.ProtectionQuery(
				com.powers.protection.ProtectionAction.RITUAL, null, null, null, null)));
	}

	@Test
	void stopClearsExternalActionsProtectionAndHooksAtTheServerBoundary() {
		List<ApiLifecycleEvent> events = new ArrayList<>();
		runtime.startServer(null, List.of(new PowersExtension() {
			@Override public String id() { return "cleanup.example"; }
			@Override public void register(PowersApiV1 api) {
				assertEquals(RegistrationResult.ACCEPTED, api.registerAction(action("cleanup_action")));
				assertEquals(RegistrationResult.ACCEPTED, api.registerProtectionService(
						new ProtectionService("cleanup_guard", 1, query -> true)));
				assertEquals(RegistrationResult.ACCEPTED, api.registerLifecycleHook(events::add));
			}
		}));
		runtime.serverStarted(null);
		assertTrue(MagicRuntime.catalogue().definition(new MagicActionId("cleanup_action")) != null);

		runtime.stopServer();

		assertEquals(List.of(ApiLifecycleEvent.SERVER_STARTED, ApiLifecycleEvent.SERVER_STOPPING), events);
		assertTrue(MagicRuntime.catalogue().definition(new MagicActionId("cleanup_action")) == null);
		assertFalse(PowerProtectionAdapters.registeredIds().contains("cleanup_guard"));
		assertTrue(runtime.extensionIds().isEmpty());
	}

	@Test
	void mutationMethodsRejectCallsOutsideTheBoundServerThread() throws Exception {
		runtime.startServer(null, List.of(extension("thread.example", new ArrayList<>())));
		var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
		Thread thread = Thread.ofPlatform().start(() -> {
			try {
				runtime.api().registerPresence(null,
						new PhysicalPresence(null, 0, 64, 0, 1, 20, PresenceKind.FIELD));
			} catch (Throwable thrown) {
				failure.set(thrown);
			}
		});
		thread.join();
		assertTrue(failure.get() instanceof IllegalStateException);
	}

	private static PowersExtension extension(String id, List<String> order) {
		return new PowersExtension() {
			@Override public String id() { return id; }
			@Override public void register(PowersApiV1 api) {
				order.add(id);
				assertEquals(RegistrationResult.ACCEPTED, api.registerAction(action(id.replace('.', '_'))));
			}
		};
	}

	private static ActionRegistration action(String id) {
		return new ActionRegistration(id, CastSource.EXTENSION, Set.of(ActionAspect.FORCE),
				ActionDelivery.FIELD, ActionIntent.SUPPORT, 4, 8, 40, 5, 20, 20, 10, 0x55AAFF);
	}
}
