package com.powers.player;

import com.powers.item.ShadowSwordCatalogue;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.stream.Collectors;

/** Persistent, server-owned Shadow Sword selection with catalogue validation. */
public final class ShadowSwordState {
	private static final String DEFAULT = "innate/lightning_strike";
	private static final Set<String> VALID_KEYS = ShadowSwordCatalogue.definitions().stream()
			.map(ShadowSwordCatalogue.Definition::key).collect(Collectors.toUnmodifiableSet());

	private ShadowSwordState() {
	}

	public static String selected(ServerPlayer player) {
		String stored = player.getAttachedOrElse(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, DEFAULT);
		return VALID_KEYS.contains(stored) ? stored : DEFAULT;
	}

	public static boolean select(ServerPlayer player, String key) {
		if (key == null || key.length() > 96 || !VALID_KEYS.contains(key)) return false;
		player.setAttached(PlayerPowerAttachments.SHADOW_SWORD_SELECTION, key);
		return true;
	}
}
