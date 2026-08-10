package com.powers.player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import java.util.ArrayList;
import java.util.List;

import static com.powers.player.PlayerPowerAttachments.REALM_MEMORIES;

/** Copy-on-write persistence adapter for discovered mindscape memories. */
final class RealmMemoryStore {
	private RealmMemoryStore() {
	}

	static boolean discover(AttachmentTarget target, String memoryId) {
		List<String> current = target.getAttachedOrElse(REALM_MEMORIES, List.of());
		if (current.contains(memoryId)) {
			return false;
		}
		List<String> updated = new ArrayList<>(current);
		updated.add(memoryId);
		target.setAttached(REALM_MEMORIES, updated);
		return true;
	}

	static List<String> read(AttachmentTarget target) {
		return List.copyOf(target.getAttachedOrElse(REALM_MEMORIES, List.of()));
	}
}
