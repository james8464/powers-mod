package com.powers.client.screen;

import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactCatalogueTab;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.MagicSignificance;

import java.util.List;
import java.util.stream.IntStream;

/** Package bridge used only by the client acceptance harness. */
public final class ClientAcceptanceScreens {
    private ClientAcceptanceScreens() {
    }

    public static ArtifactCatalogueScreen artifactCatalogue(long revision, String alignment, String selectedKey,
            int rank, int sizeMorphOption, int energy, List<String> favourites,
            List<ArtifactActionSnapshot> snapshots) {
		return new ArtifactCatalogueScreen(ArtifactMenuState.fromPacket(revision, alignment, selectedKey,
                rank, sizeMorphOption, energy, favourites, List.of(), snapshots));
    }

	public static CatalogueProbe catalogueProbe(ArtifactCatalogueScreen screen) {
		return new CatalogueProbe(screen.actionWidgetCount(), screen.actionWidgetAllocations(),
				screen.filteredCount(), screen.firstVisibleIndex(), screen.selectedKey(), screen.firstActionLabel(),
				screen.focusedActionKey(), screen.focusedNarrationText(),
				screen.hiddenActionHasFocus(), screen.favouriteKey(0), screen.lastBindNonOptimistic(),
				screen.noCategoryTabSelected(), screen.selectedCategoryTab());
	}

	public static void searchInnate(ArtifactCatalogueScreen screen, String query) {
		screen.verificationQuery(query, ArtifactCatalogueTab.INNATE);
	}

	public static void searchDefault(ArtifactCatalogueScreen screen, String query) {
		screen.verificationQuery(query, ArtifactCatalogueTab.FAVOURITES);
	}

	public static void clearSearch(ArtifactCatalogueScreen screen) {
		screen.verificationSearchValue("");
	}

	public static void moveDown(ArtifactCatalogueScreen screen) {
		screen.verificationKey(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN);
	}

	public static ArtifactCatalogueScreen syntheticCatalogue(int count) {
		List<ArtifactActionDefinition> actions = IntStream.range(0, count).mapToObj(index ->
				new ArtifactActionDefinition("innate/synthetic_" + index, "synthetic_" + index,
						ArtifactActionCategory.ROUTED_POWER, ArtifactAlignment.DARKNESS,
						1, 1, 0, MagicSignificance.MINIMAL)).toList();
		List<ArtifactActionSnapshot> snapshots = actions.stream().map(action ->
				new ArtifactActionSnapshot(action.key(), action.category(), 1, 0, 0, 0,
						false, false, -1)).toList();
		return new ArtifactCatalogueScreen(new ArtifactMenuState(100L, ArtifactAlignment.DARKNESS,
				actions.getFirst().key(), 10, 3, 1_000,
				List.of(actions.get(0).key(), actions.get(1).key(), actions.get(2).key(),
						actions.get(3).key(), actions.get(4).key(), actions.get(5).key(),
						actions.get(6).key(), actions.get(7).key()), List.of(), actions, snapshots));
	}

	public static void refreshSynthetic(ArtifactCatalogueScreen screen, long revision) {
		ArtifactMenuState current = screen.verificationState();
		screen.acceptRefresh(new ArtifactMenuState(revision, current.alignment(), current.selectedKey(),
				current.rank(), current.sizeMorphOption(), current.energy(), current.favourites(),
				current.recents(), current.actions(), current.snapshots()));
	}

	public record CatalogueProbe(int widgets, int allocations, int results, int firstVisibleIndex,
			String selectedKey, String firstActionLabel, String focusedActionKey,
			String focusedNarrationText, boolean hiddenActionHasFocus, String firstFavouriteKey,
			boolean lastBindNonOptimistic, boolean noCategoryTabSelected, String selectedCategoryTab) {
	}
}
