package com.powers.magic.runtime;

import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.server.level.ServerPlayer;

/** Minecraft adapter that revalidates the physical owner of a delayed cast. */
public final class ServerCastLifecycle {
	private ServerCastLifecycle() {
	}

	public static boolean mayContinue(ServerPlayer player, CastSource source, boolean innateOwned) {
		boolean darknessCarried = ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS);
		boolean lightCarried = ArtifactWeaponManager.carries(player, ArtifactAlignment.LIGHT);
		boolean artifactOwned = darknessCarried || lightCarried;
		boolean artifactAuthorized = darknessCarried
				&& ArtifactWeaponManager.authorized(player, ArtifactAlignment.DARKNESS)
				|| lightCarried && ArtifactWeaponManager.authorized(player, ArtifactAlignment.LIGHT);
		return CastLifecycleOwnership.mayContinue(
				source, innateOwned, artifactOwned, artifactAuthorized);
	}
}
