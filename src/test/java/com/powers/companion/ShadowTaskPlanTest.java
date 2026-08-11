package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowTaskPlanTest {
	@Test
	void everyExecutableIntentHasBoundedPrerequisiteCostAndRollbackSteps() {
		for (ShadowRequest.Kind kind : ShadowTaskPlan.executableKinds()) {
			ShadowTaskPlan plan = ShadowTaskPlan.forTask(ShadowTask.create(
					kind, "minecraft:stone", 2, 10, 100, 0));
			assertFalse(plan.steps().isEmpty(), kind.name());
			assertTrue(plan.steps().size() <= ShadowTaskPlan.MAX_STEPS, kind.name());
			for (ShadowTaskPlan.Step step : plan.steps()) {
				assertFalse(step.id().isBlank(), kind.name());
				assertTrue(step.prerequisite() != null, kind.name());
				assertTrue(step.cost() != null, kind.name());
				assertTrue(step.rollback() != null, kind.name());
			}
		}
	}

	@Test
	void controllerExposesCurrentStepAndCancellationRollback() {
		ShadowTaskController controller = new ShadowTaskController();
		controller.submit(ShadowTask.create(ShadowRequest.Kind.GET_ITEM,
				"minecraft:torch", 4, 10, 100, 0));
		assertEquals("validate_item", controller.progress().stepId());
		assertTrue(controller.advance().state() == ShadowTask.State.RUNNING);
		assertEquals("locate_drop", controller.progress().stepId());
		var cancelled = controller.cancel("owner_stop");
		assertEquals(ShadowTaskPlan.Rollback.RELEASE_ITEM_CLAIM, cancelled.rollback());
		assertEquals("locate_drop", cancelled.stepId());
		assertTrue(controller.active().isEmpty());
	}
}
