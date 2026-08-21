package com.powers.item.artifact;

import com.powers.magic.MagicSignificance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactCatalogueViewModelTest {
	@Test
	void tenThousandActionsReuseOneFixedVisiblePoolWhileScrollingAndSearching() {
		List<ArtifactActionDefinition> actions = actions(10_000);
		ArtifactCatalogueViewModel model = new ArtifactCatalogueViewModel(7L, actions,
				List.of(), List.of(), actions.getFirst().key(), action -> "Action " + suffix(action), 2, 12);
		model.setFilter(ArtifactCatalogueTab.INNATE, "");

		assertEquals(24, model.poolSize());
		assertEquals(24, model.visible().size());
		model.scrollRows(2_000);
		assertEquals(2_000, suffix(model.visible().getFirst()));
		model.setFilter(ArtifactCatalogueTab.INNATE, "Action 9999");
		assertEquals(List.of(9_999), model.visible().stream().map(ArtifactCatalogueViewModelTest::suffix).toList());
		assertEquals(24, model.poolSize());
		assertEquals(0, model.firstVisibleIndex());
	}

	@Test
	void selectionSurvivesRevisionRefreshByCanonicalKeyAndFallsBackWhenRemoved() {
		List<ArtifactActionDefinition> actions = actions(30);
		ArtifactCatalogueViewModel model = new ArtifactCatalogueViewModel(4L, actions,
				List.of(), List.of(), actions.get(17).key(), action -> "Action " + suffix(action), 2, 5);
		assertEquals(actions.get(17).key(), model.selectedKey());
		model.refresh(5L, actions, List.of(), List.of(), actions.getFirst().key(),
				action -> "Renamed " + suffix(action));
		assertEquals(5L, model.revision());
		assertEquals(actions.get(17).key(), model.selectedKey());

		List<ArtifactActionDefinition> withoutSelected = actions.stream()
				.filter(action -> !action.key().equals(actions.get(17).key())).toList();
		model.refresh(6L, withoutSelected, List.of(), List.of(), actions.get(3).key(),
				action -> "Action " + suffix(action));
		assertEquals(actions.get(3).key(), model.selectedKey());
	}

	@Test
	void revisionRefreshPreservesAndClampsTheScrolledWindowInPlace() {
		List<ArtifactActionDefinition> actions = actions(100);
		ArtifactCatalogueViewModel model = new ArtifactCatalogueViewModel(4L, actions,
				List.of(), List.of(), actions.get(67).key(), ArtifactActionDefinition::abilityId, 2, 5);
		model.setFilter(ArtifactCatalogueTab.INNATE, "");
		model.scrollRows(30);
		assertEquals(30, model.firstVisibleIndex());

		model.refresh(5L, actions, List.of(), List.of(), actions.getFirst().key(),
				ArtifactActionDefinition::abilityId);
		assertEquals(30, model.firstVisibleIndex(), "A revision refresh jumped the virtual window to the start");
		assertEquals(actions.get(67).key(), model.selectedKey());

		model.refresh(6L, actions.subList(0, 18), List.of(), List.of(), actions.get(17).key(),
				ArtifactActionDefinition::abilityId);
		assertEquals(8, model.firstVisibleIndex(), "A shorter revision did not clamp to its last full window");
	}

	@Test
	void searchResultBindsInSelectThenSlotInteractionAndRecentFilterIsBounded() {
		List<ArtifactActionDefinition> actions = actions(40);
		List<String> recent = IntStream.iterate(39, index -> index - 1).limit(10)
				.mapToObj(index -> actions.get(index).key()).toList();
		ArtifactCatalogueViewModel model = new ArtifactCatalogueViewModel(9L, actions,
				List.of(), recent, actions.getFirst().key(), action -> "Action " + suffix(action), 1, 8);
		model.setFilter(ArtifactCatalogueTab.INNATE, "Action 37");
		model.selectVisible(0);
		assertEquals(new ArtifactCatalogueViewModel.BindingIntent(6, actions.get(37).key()), model.bind(6));

		model.setFilter(ArtifactCatalogueTab.RECENT, "");
		assertEquals(ArtifactRecentRules.LIMIT, model.filteredCount());
		assertEquals(actions.get(39).key(), model.visible().getFirst().key());
	}

	@Test
	void nonBlankSearchFromDefaultFavouritesSurfaceFindsAnUnboundActionGlobally() {
		List<ArtifactActionDefinition> actions = actions(40);
		ArtifactCatalogueViewModel model = new ArtifactCatalogueViewModel(9L, actions,
				List.of(actions.getFirst().key()), List.of(), actions.getFirst().key(),
				action -> "Action " + suffix(action), 1, 8);

		model.setFilter(ArtifactCatalogueTab.FAVOURITES, "Action 37");

		assertEquals(List.of(actions.get(37).key()),
				model.visible().stream().map(ArtifactActionDefinition::key).toList());
	}

	@Test
	void keyboardSelectionMovesWithinFilteredResultsAndScrollsItIntoView() {
		List<ArtifactActionDefinition> actions = actions(40);
		ArtifactCatalogueViewModel model = new ArtifactCatalogueViewModel(1L, actions,
				List.of(), List.of(), actions.getFirst().key(), ArtifactActionDefinition::abilityId, 2, 3);
		model.setFilter(ArtifactCatalogueTab.INNATE, "");
		for (int step = 0; step < 9; step++) model.moveSelection(1);
		assertEquals(actions.get(9).key(), model.selectedKey());
		assertTrue(model.firstVisibleIndex() > 0);
		assertTrue(model.visible().stream().anyMatch(action -> action.key().equals(model.selectedKey())));
	}

	private static List<ArtifactActionDefinition> actions(int count) {
		return IntStream.range(0, count).mapToObj(index -> new ArtifactActionDefinition(
				"innate/action_" + index, "action_" + index,
				ArtifactActionCategory.ROUTED_POWER, ArtifactAlignment.DARKNESS,
				1, 1, 0, MagicSignificance.MINIMAL)).toList();
	}

	private static int suffix(ArtifactActionDefinition action) {
		return Integer.parseInt(action.abilityId().substring("action_".length()));
	}
}
