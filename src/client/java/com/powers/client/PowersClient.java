package com.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.powers.PowersMod;
import com.powers.PowersItems;
import com.powers.client.screen.PowerSelectionScreen;
import com.powers.client.screen.TeleportInputScreen;
import com.powers.network.PowersPackets;
import com.powers.power.Ability;
import com.powers.power.Power;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class PowersClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(PowersMod.id("powers"));

	public static KeyMapping slotKey1;
	public static KeyMapping slotKey2;
	public static KeyMapping slotKey3;
	public static KeyMapping powerMenuKey;

	@Override
	public void onInitializeClient() {
		slotKey1 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
		slotKey2 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
		slotKey3 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
		powerMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY));

		ClientPlayNetworking.registerGlobalReceiver(PowersPackets.PowerStatePayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientPowerState.update(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientPowerState.reset());

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				PowersMod.id("power_hud"),
				PowersClient::renderHud);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				PowersMod.id("energy_hud"),
				(graphics, tickCounter) -> EnergyHudRenderer.render(graphics));

		ClientTickEvents.END_CLIENT_TICK.register(PowersClient::tick);

		ItemEvents.USE.register((level, user, hand) -> {
			if (!level.isClientSide()) {
				return InteractionResult.PASS;
			}
			if (user.getItemInHand(hand).is(PowersItems.RAINBOW_CRYSTAL)) {
				Minecraft.getInstance().gui.setScreen(new PowerSelectionScreen());
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static void tick(Minecraft client) {
		if (ClientPowerState.markingSlot >= 0) {
			if (--ClientPowerState.markingTicks <= 0) {
				ClientPowerState.markingSlot = -1;
			}
		}
		while (slotKey1.consumeClick()) {
			handleSlotKey(client, 0);
		}
		while (slotKey2.consumeClick()) {
			handleSlotKey(client, 1);
		}
		while (slotKey3.consumeClick()) {
			handleSlotKey(client, 2);
		}
		while (powerMenuKey.consumeClick()) {
			if (client.gui.screen() == null) {
				client.gui.setScreen(new PowerSelectionScreen());
			}
		}
	}

	private static void handleSlotKey(Minecraft client, int slot) {
		if (ClientPowerState.isMarking() && ClientPowerState.markingSlot == slot) {
			var player = client.player;
			if (player != null) {
				var pos = player.position();
				ClientPlayNetworking.send(new PowersPackets.TeleportMarkPayload(slot, pos.x, pos.y, pos.z));
				ClientPowerState.markingSlot = -1;
			}
			return;
		}
		if (client.gui.screen() != null) return;
		Power power = ClientPowerState.getPower(slot);
		if (power == null) {
			return;
		}
		Ability ability = power.ability();
		if (ability == null) {
			return;
		}

		if (ability.requiresInput()) {
			client.gui.setScreen(new TeleportInputScreen(slot, 0));
		} else {
			ClientPlayNetworking.send(new PowersPackets.ActivateAbilityPayload(slot, 0));
		}
	}

	private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		PowerHudRenderer.render(graphics, tickCounter);
	}
}
