package com.powers.client;

import com.powers.PowersEntities;
import com.powers.entity.PrivateCompanionGhost;
import com.powers.network.CompanionPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/** Owns the single apparition that exists only inside its owner's client. */
public final class PrivateCompanionClient {
	private static final int LOCAL_ENTITY_ID = -1_930_062_001;
	private static PrivateCompanionGhost ghost;
	private static long sessionId = -1L;
	private static Vec3 target = Vec3.ZERO;
	private static float targetYaw;
	private static String dialogue = "";
	private static int dialogueTicks;

	private PrivateCompanionClient() {
	}

	public static void handle(CompanionPackets.StatePayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (!payload.active()) {
			clear();
			return;
		}
		if (client.level == null || client.player == null) return;
		if (ghost == null || sessionId != payload.sessionId() || ghost.isRemoved()) {
			clear();
			ghost = PowersEntities.PRIVATE_COMPANION_GHOST.create(
					client.level, EntitySpawnReason.TRIGGERED);
			if (ghost == null) return;
			ghost.setId(LOCAL_ENTITY_ID);
			ghost.setPos(payload.x(), payload.y(), payload.z());
			ghost.setYRot(payload.yaw());
			client.level.addEntity(ghost);
			sessionId = payload.sessionId();
		}
		target = new Vec3(payload.x(), payload.y(), payload.z());
		targetYaw = payload.yaw();
		if (payload.teleport()) ghost.setPos(target);
		if (!payload.dialogue().isBlank()) {
			dialogue = payload.dialogue();
			dialogueTicks = 140;
		}
	}

	public static void tick() {
		if (ghost == null || ghost.isRemoved()) return;
		Vec3 next = ghost.position().lerp(target, 0.34);
		ghost.setPos(next);
		ghost.setYRot(rotateToward(ghost.getYRot(), targetYaw, 0.28F));
		ghost.setYHeadRot(ghost.getYRot());
		if (dialogueTicks > 0) dialogueTicks--;
		else dialogue = "";
	}

	public static void interact() {
		if (sessionId >= 0L && ghost != null && !ghost.isRemoved()) {
			net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new CompanionPackets.InteractPayload(sessionId));
		}
	}

	/** Renders dialogue as a compact title-style panel, never as chat spam. */
	public static void renderDialogue(GuiGraphicsExtractor graphics) {
		if (dialogue.isBlank() || dialogueTicks <= 0) return;
		Minecraft client = Minecraft.getInstance();
		String visible = client.font.plainSubstrByWidth(dialogue,
				Math.min(300, client.getWindow().getGuiScaledWidth() - 40));
		int width = client.font.width(visible);
		int center = client.getWindow().getGuiScaledWidth() / 2;
		int y = client.getWindow().getGuiScaledHeight() - 82;
		int alpha = dialogueTicks < 20 ? Math.max(0x22, dialogueTicks * 0x0C) : 0xCC;
		graphics.fill(center - width / 2 - 7, y - 5,
				center + width / 2 + 7, y + 14, alpha << 24 | 0x120E18);
		graphics.fill(center - width / 2 - 7, y - 5,
				center + width / 2 + 7, y - 3, alpha << 24 | 0x6F3B88);
		graphics.text(client.font, Component.literal(visible), center - width / 2, y,
				alpha << 24 | 0xE8D8F4, true);
	}

	public static void clear() {
		Minecraft client = Minecraft.getInstance();
		if (ghost != null && client.level != null && !ghost.isRemoved()) {
			client.level.removeEntity(ghost.getId(), Entity.RemovalReason.DISCARDED);
		}
		ghost = null;
		sessionId = -1L;
		target = Vec3.ZERO;
		dialogue = "";
		dialogueTicks = 0;
	}

	private static float rotateToward(float from, float to, float fraction) {
		float difference = net.minecraft.util.Mth.wrapDegrees(to - from);
		return from + difference * fraction;
	}
}
