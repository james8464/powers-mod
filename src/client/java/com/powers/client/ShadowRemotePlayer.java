package com.powers.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** A non-interactive player renderer shell whose skin always resolves from its owner. */
final class ShadowRemotePlayer extends RemotePlayer {
	private final UUID ownerId;

	ShadowRemotePlayer(ClientLevel level, UUID ownerId) {
		super(level, new GameProfile(shadowUuid(ownerId), "Shadow"));
		this.ownerId = ownerId;
		setNoGravity(true);
		noPhysics = true;
		setSilent(true);
		setInvulnerable(true);
		getInventory().clearContent();
	}

	@Override
	public PlayerSkin getSkin() {
		var connection = Minecraft.getInstance().getConnection();
		PlayerInfo owner = connection == null ? null : connection.getPlayerInfo(ownerId);
		return owner == null ? DefaultPlayerSkin.get(ownerId) : owner.getSkin();
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean shouldShowName() {
		return false;
	}

	private static UUID shadowUuid(UUID ownerId) {
		return UUID.nameUUIDFromBytes(("powers:shadow/" + ownerId)
				.getBytes(StandardCharsets.UTF_8));
	}
}
