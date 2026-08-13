package com.powers.player;

/** Registers the persistent player attachment schema during content bootstrap. */
public final class PlayerPowerSchema {
	private PlayerPowerSchema() {
	}

	/** Forces attachment types to exist before Fabric decodes any player save. */
	public static void initialize() {
		PlayerPowerAttachments.initialize();
	}
}
