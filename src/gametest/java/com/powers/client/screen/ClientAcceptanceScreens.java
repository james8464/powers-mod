package com.powers.client.screen;

import com.powers.item.artifact.ArtifactActionSnapshot;

import java.util.List;

/** Package bridge used only by the client acceptance harness. */
public final class ClientAcceptanceScreens {
    private ClientAcceptanceScreens() {
    }

    public static ArtifactCatalogueScreen artifactCatalogue(String alignment, String selectedKey,
            int rank, int sizeMorphOption, int energy, List<String> favourites,
            List<ArtifactActionSnapshot> snapshots) {
		return new ArtifactCatalogueScreen(ArtifactMenuState.fromPacket(0L, alignment, selectedKey,
                rank, sizeMorphOption, energy, favourites, snapshots));
    }
}
