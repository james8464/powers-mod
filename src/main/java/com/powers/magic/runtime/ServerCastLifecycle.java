package com.powers.magic.runtime;

import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.server.level.ServerPlayer;

/** Minecraft adapter that revalidates the physical owner of a delayed cast. */
public final class ServerCastLifecycle {
	private ServerCastLifecycle() {
	}

	public static boolean mayContinue(ServerPlayer player, CastSource source, boolean innateOwned) {
		boolean darknessHeld = ArtifactWeaponManager.holds(player, ArtifactAlignment.DARKNESS);
		boolean lightHeld = ArtifactWeaponManager.holds(player, ArtifactAlignment.LIGHT);
		boolean artifactHeld = darknessHeld || lightHeld;
		boolean artifactAuthorized = darknessHeld
				&& ArtifactWeaponManager.authorized(player, ArtifactAlignment.DARKNESS)
				|| lightHeld && ArtifactWeaponManager.authorized(player, ArtifactAlignment.LIGHT);
		return CastLifecycleOwnership.mayContinue(
				source, innateOwned, artifactHeld, artifactAuthorized);
	}
}
