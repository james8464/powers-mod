package com.powers.client;

import com.powers.network.VesselControlPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;

/** Captures only bounded movement/hotbar/attack intent while the server owns a possession. */
public final class VesselControlClient {
	private static boolean active;
	private static boolean attackHeld;
	private static int ticks;

	private VesselControlClient() {
	}

	public static void setActive(boolean value) {
		active = value;
		attackHeld = false;
		ticks = 0;
	}

	public static void tick(Minecraft client) {
		if (!active || client.player == null || client.getCameraEntity() == null) return;
		boolean attack = client.options.keyAttack.isDown();
		int attackId = attack && !attackHeld && client.hitResult instanceof EntityHitResult entityHit
				? entityHit.getEntity().getId() : -1;
		attackHeld = attack;
		if (++ticks % 2 != 0 && attackId < 0) return;

		float forward = (client.options.keyUp.isDown() ? 1.0F : 0.0F)
				- (client.options.keyDown.isDown() ? 1.0F : 0.0F);
		float strafe = (client.options.keyLeft.isDown() ? 1.0F : 0.0F)
				- (client.options.keyRight.isDown() ? 1.0F : 0.0F);
		var camera = client.getCameraEntity();
		ClientPlayNetworking.send(new VesselControlPackets.InputPayload(
				forward, strafe, client.options.keyJump.isDown(), client.options.keyShift.isDown(),
				camera.getYRot(), camera.getXRot(), client.player.getInventory().getSelectedSlot(), attackId));
	}
}
