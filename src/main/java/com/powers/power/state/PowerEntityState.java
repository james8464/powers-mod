package com.powers.power.state;

import com.powers.PowersMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Runtime-only ownership flags for temporary entities spawned by powers. */
public final class PowerEntityState {
	private static final AttachmentType<Boolean> EPHEMERAL = AttachmentRegistry.create(
			PowersMod.id("ephemeral_summon"), builder -> builder.initializer(() -> Boolean.FALSE));
	private static final AttachmentType<Boolean> POWER_PROJECTILE = AttachmentRegistry.create(
			PowersMod.id("power_projectile"), builder -> builder.initializer(() -> Boolean.FALSE));
	private static final AttachmentType<Boolean> BANISHABLE_SUMMON = AttachmentRegistry.create(
			PowersMod.id("banishable_summon"), builder -> builder.initializer(() -> Boolean.FALSE));
	private static final AttachmentType<Integer> REFLECTION_COUNT = AttachmentRegistry.create(
			PowersMod.id("reflection_count"), builder -> builder.initializer(() -> 0));

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

	public static void markPowerProjectile(AttachmentTarget entity) {
		markEphemeral(entity);
		entity.setAttached(POWER_PROJECTILE, Boolean.TRUE);
	}

	public static boolean isPowerProjectile(AttachmentTarget entity) {
		return entity.getAttachedOrElse(POWER_PROJECTILE, Boolean.FALSE);
	}

	/** Marks a temporary construct that banishment rituals may safely discard. */
	public static void markBanishableSummon(AttachmentTarget entity) {
		markEphemeral(entity);
		entity.setAttached(BANISHABLE_SUMMON, Boolean.TRUE);
	}

	/** Distinguishes summons from vulnerable body proxies and other temporary entities. */
	public static boolean isBanishableSummon(AttachmentTarget entity) {
		return entity.getAttachedOrElse(BANISHABLE_SUMMON, Boolean.FALSE);
	}

	/** Claims one finite reflection slot, returning false after the cap. */
	public static boolean tryReflect(AttachmentTarget entity, int maximumReflections) {
		if (maximumReflections <= 0) return false;
		int count = entity.getAttachedOrElse(REFLECTION_COUNT, 0);
		if (count >= maximumReflections) return false;
		entity.setAttached(REFLECTION_COUNT, count + 1);
		return true;
	}
}
