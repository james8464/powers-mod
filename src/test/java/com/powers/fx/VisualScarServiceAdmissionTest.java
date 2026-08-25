package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualScarServiceAdmissionTest {
	private static final Path SERVICE = Path.of(
			"src/main/java/com/powers/fx/VisualScarService.java");

	@Test
	void requestEdgeQueuesIdentityWithoutInspectingWorldOrProtection() throws IOException {
		String service = Files.readString(SERVICE);
		String request = method(service,
				"public static boolean request(ServerLevel level");
		for (String inspection : List.of("LoadedChunks.contains", "getBlockState(",
				"getBlockEntity(", "PowerProtection.blockDecision", "classify(")) {
			assertFalse(request.contains(inspection), inspection + " escaped the bounded tick path");
		}
		assertTrue(request.contains("state.offer(new PendingRequest("));
		assertFalse(request.contains("support.relative(face)"));
		String pending = declaration(service, "private record PendingRequest(", ") {");
		assertFalse(pending.contains("Material"));
		assertFalse(pending.contains("fingerprint"));
	}

	@Test
	void hardCappedSelectionDispatchesAtMost64FreshInspections() throws IOException {
		VisualScarRequestQueue queue = new VisualScarRequestQueue(2_048, 128);
		UUID owner = new UUID(0, 1);
		for (int index = 0; index < 128; index++) {
			assertTrue(queue.offer(new VisualScarLedgerRules.Request("minecraft:overworld", 7,
					owner, VisualScarRules.Impact.BEAM)));
		}
		AtomicInteger inspections = new AtomicInteger();
		queue.poll(VisualScarRules.Limits.hardCeilings().requestsPerTick())
				.forEach(ignored -> inspections.incrementAndGet());
		assertEquals(64, inspections.get());

		String service = Files.readString(SERVICE);
		String tick = method(service, "private void tick(MinecraftServer server)");
		assertTrue(tick.contains("requests.poll(LIMITS.requestsPerTick())"));
		assertTrue(tick.contains("inspectAndActivate(server, pending, now)"));
		assertTrue(tick.indexOf("requests.poll(LIMITS.requestsPerTick())")
				< tick.indexOf("inspectAndActivate(server, pending, now)"));
	}

	@Test
	void selectedInspectionReadsFreshFactsOnlyAfterBothLoadedChecksAndBeforeActivation()
			throws IOException {
		String inspection = method(Files.readString(SERVICE),
				"private void inspectAndActivate(MinecraftServer server, PendingRequest pending, long now)");
		int supportLoaded = inspection.indexOf("LoadedChunks.contains(level, support)");
		int originLoaded = inspection.indexOf("LoadedChunks.contains(level, origin)");
		int protection = inspection.indexOf("PowerProtection.blockDecision");
		int classification = inspection.indexOf("classify(supportState)");
		for (String read : List.of("level.getBlockState(support)", "level.getBlockEntity(support)",
				"level.getBlockState(origin)", "level.getBlockEntity(origin)")) {
			int position = inspection.indexOf(read);
			assertTrue(position > supportLoaded && position > originLoaded, read);
		}
		int admission = inspection.indexOf("VisualScarRules.admit(facts)");
		int activation = inspection.indexOf("activate(server, pending, material,");
		assertTrue(protection > originLoaded);
		assertTrue(classification > protection);
		assertTrue(admission > protection);
		assertTrue(activation > admission);
		assertTrue(inspection.indexOf("supportState.hashCode()") > admission);
	}

	private static String method(String source, String signature) {
		int start = source.indexOf(signature);
		assertTrue(start >= 0, "missing method: " + signature);
		int open = source.indexOf('{', start);
		int depth = 0;
		for (int index = open; index < source.length(); index++) {
			char character = source.charAt(index);
			if (character == '{') depth++;
			if (character == '}' && --depth == 0) return source.substring(start, index + 1);
		}
		throw new AssertionError("unterminated method: " + signature);
	}

	private static String declaration(String source, String startText, String endText) {
		int start = source.indexOf(startText);
		assertTrue(start >= 0, "missing declaration: " + startText);
		int end = source.indexOf(endText, start);
		assertTrue(end > start, "unterminated declaration: " + startText);
		return source.substring(start, end);
	}
}
