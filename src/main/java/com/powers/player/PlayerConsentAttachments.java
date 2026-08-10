package com.powers.player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import static com.powers.player.PlayerPowerAttachments.COMPANION_CONSENT;
import static com.powers.player.PlayerPowerAttachments.DREAMWALK_CONSENT;
import static com.powers.player.PlayerPowerAttachments.LOCATOR_CONSENT;
import static com.powers.player.PlayerPowerAttachments.POSSESSION_CONSENT;
import static com.powers.player.PlayerPowerAttachments.TELEPORT_CONSENT;

/** Maps public consent categories to their persistent attachment without duplicating switches. */
final class PlayerConsentAttachments {
	private PlayerConsentAttachments() {
	}

	static AttachmentType<Boolean> type(PlayerPowers.ConsentKind kind) {
		return switch (kind) {
			case TELEPORT -> TELEPORT_CONSENT;
			case LOCATOR -> LOCATOR_CONSENT;
			case COMPANION -> COMPANION_CONSENT;
			case DREAMWALK -> DREAMWALK_CONSENT;
			case POSSESSION -> POSSESSION_CONSENT;
		};
	}
}
