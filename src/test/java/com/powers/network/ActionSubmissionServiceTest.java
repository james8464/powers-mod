package com.powers.network;

import com.powers.magic.MagicActionCatalogue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionSubmissionServiceTest {
	@ParameterizedTest(name = "{0} stale submission refreshes before limiter")
	@ValueSource(strings = {"artifact-select", "artifact-commit", "artifact-cycle",
			"artifact-bind", "artifact-teleport", "grimoire-select", "crystal-select"})
	void staleSubmissionsRefreshExactlyOnceBeforeEverySideEffect(String route) {
		var snapshot = MagicActionCatalogue.defaults().snapshot();
		List<String> order = new ArrayList<>();
		AtomicInteger refreshes = new AtomicInteger();

		ActionSubmissionService.Result result = ActionSubmissionService.submit(snapshot,
				new ActionSubmissionService.Request(snapshot.revision() + 1L, "fireball"),
				() -> { order.add("context"); return true; },
				() -> { order.add("refresh"); refreshes.incrementAndGet(); },
				() -> { order.add("limiter"); return true; }, () -> order.add("mutation"));

		assertEquals(ActionSubmissionService.Result.REFRESHED, result, route);
		assertEquals(1, refreshes.get(), route);
		assertEquals(List.of("refresh"), order, route);
	}

	@ParameterizedTest(name = "{0} context mismatch refreshes before limiter")
	@ValueSource(strings = {"artifact-select", "artifact-commit", "artifact-cycle",
			"artifact-bind", "artifact-teleport", "grimoire-select", "crystal-select"})
	void contextMismatchesRefreshExactlyOnceBeforeLimiterOrMutation(String route) {
		var snapshot = MagicActionCatalogue.defaults().snapshot();
		List<String> order = new ArrayList<>();

		ActionSubmissionService.Result result = ActionSubmissionService.submit(snapshot,
				new ActionSubmissionService.Request(snapshot.revision(), "fireball"),
				() -> { order.add("context"); return false; },
				() -> order.add("refresh"),
				() -> { order.add("limiter"); return true; }, () -> order.add("mutation"));

		assertEquals(ActionSubmissionService.Result.REFRESHED, result, route);
		assertEquals(List.of("context", "refresh"), order, route);
	}

	@ParameterizedTest(name = "{0} valid submission preserves validation order")
	@ValueSource(strings = {"artifact-select", "artifact-commit", "artifact-cycle",
			"artifact-bind", "artifact-teleport", "grimoire-select", "crystal-select"})
	void validSubmissionsReachLimiterThenMutation(String route) {
		var snapshot = MagicActionCatalogue.defaults().snapshot();
		List<String> order = new ArrayList<>();

		ActionSubmissionService.Result result = ActionSubmissionService.submit(snapshot,
				new ActionSubmissionService.Request(snapshot.revision(), "fireball"),
				() -> { order.add("context"); return true; },
				() -> order.add("refresh"),
				() -> { order.add("limiter"); return true; }, () -> order.add("mutation"));

		assertEquals(ActionSubmissionService.Result.ACCEPTED, result, route);
		assertEquals(List.of("context", "limiter", "mutation"), order, route);
	}
}
