package com.powers.client.screen;

import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactCatalogueTab;

import java.util.List;

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
				screen.filteredCount(), screen.selectedKey(), screen.firstActionLabel());
	}

	public static void searchInnate(ArtifactCatalogueScreen screen, String query) {
		screen.verificationQuery(query, ArtifactCatalogueTab.INNATE);
	}

	public record CatalogueProbe(int widgets, int allocations, int results,
			String selectedKey, String firstActionLabel) {
	}
}
