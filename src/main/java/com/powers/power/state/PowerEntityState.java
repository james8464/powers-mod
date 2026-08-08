package com.powers.power.state;

import com.powers.PowersMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Runtime-only ownership flags for temporary entities spawned by powers. */
public final class PowerEntityState {
	private static final AttachmentType<Boolean> EPHEMERAL = AttachmentRegistry.create(
			PowersMod.id("ephemeral_summon"), builder -> builder.initializer(() -> Boolean.FALSE));

	private PowerEntityState() {
	}

	public static void initialize() {
		// Forces registration during mod initialization, before any world saves.
	}

	public static void markEphemeral(AttachmentTarget entity) {
		entity.setAttached(EPHEMERAL, Boolean.TRUE);
	}

	public static boolean isEphemeral(AttachmentTarget entity) {
		return entity.getAttachedOrElse(EPHEMERAL, Boolean.FALSE);
	}
}
