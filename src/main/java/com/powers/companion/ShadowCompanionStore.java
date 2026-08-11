package com.powers.companion;

import com.powers.PowersMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** Persistent owner state survives body death while transient world handles do not. */
public final class ShadowCompanionStore {
	private static final AttachmentType<ShadowCompanionData> DATA = AttachmentRegistry.create(
			PowersMod.id("shadow_companion"), builder -> builder
					.initializer(ShadowCompanionData::defaults)
					.persistent(ShadowCompanionData.CODEC).copyOnDeath());

	private ShadowCompanionStore() {
	}

	public static void initialize() {
		// Loading this class registers the attachment before any world save is decoded.
	}

	public static ShadowCompanionData get(ServerPlayer owner) {
		return owner.getAttachedOrElse(DATA, ShadowCompanionData.defaults());
	}

	public static void set(ServerPlayer owner, ShadowCompanionData data) {
		owner.setAttached(DATA, Objects.requireNonNull(data, "data"));
	}

	public static ShadowCompanionData update(ServerPlayer owner,
			UnaryOperator<ShadowCompanionData> operation) {
		ShadowCompanionData updated = Objects.requireNonNull(operation, "operation").apply(get(owner));
		set(owner, Objects.requireNonNull(updated, "updated"));
		return updated;
	}

	public static void clearBody(ServerPlayer owner) {
		update(owner, ShadowCompanionData::withoutBody);
	}
}
