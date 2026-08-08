package com.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.powers.PowersMod;
import com.powers.client.screen.CelestialLocatorScreen;
import com.powers.client.screen.TeleportInputScreen;
import com.powers.client.screen.RankMazeScreen;
import com.powers.client.fx.ClientMagicFx;
import com.powers.client.fx.particle.ArcaneParticle;
import com.powers.PowersParticles;
import com.powers.network.PowerStatePayload;
import com.powers.network.PowersPackets;
import com.powers.network.MagicFxPackets;
import com.powers.power.Ability;
import com.powers.power.Power;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

/** client entry point: registers the v/x/c slot keys, wires up the huds and the teleport screen */
public class PowersClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(PowersMod.id("powers"));

	public static KeyMapping slotKey1;
	public static KeyMapping slotKey2;
	public static KeyMapping slotKey3;
	public static KeyMapping rankMazeKey;

	@Override
	public void onInitializeClient() {
		slotKey1 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
		slotKey2 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
		slotKey3 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
		rankMazeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.rank_maze", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));

		ClientPlayNetworking.registerGlobalReceiver(PowerStatePayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientPowerState.update(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MagicFxPackets.MagicFxPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientMagicFx.handle(payload)));
		// the celestial grimoire summons its target picker when the server vouches for the cast
		ClientPlayNetworking.registerGlobalReceiver(PowersPackets.OpenLocatorScreenPayload.TYPE,
				(payload, context) -> context.client().execute(() ->
						Minecraft.getInstance().gui.setScreen(new CelestialLocatorScreen(payload.nonce()))));
		// clear the cached state when you leave the server so the hud doesn't carry over old powers
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientPowerState.reset();
			ClientMagicFx.reset();
		});

		registerParticles();

		// both huds join the same render pass as vanilla chat so they layer correctly
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				PowersMod.id("power_hud"),
				PowersClient::renderHud);
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				PowersMod.id("energy_hud"),
				(graphics, tickCounter) -> EnergyHudRenderer.render(graphics));

		ClientTickEvents.END_CLIENT_TICK.register(PowersClient::tick);
	}

	/** Registers atlas-backed providers without loading client classes on a server. */
	private static void registerParticles() {
		ParticleProviderRegistry registry = ParticleProviderRegistry.getInstance();
		registry.register(PowersParticles.MOTE, sprites -> new ArcaneParticle.Provider(sprites, 0.75f));
		registry.register(PowersParticles.SHARD, sprites -> new ArcaneParticle.Provider(sprites, 1.05f));
		registry.register(PowersParticles.GLYPH, sprites -> new ArcaneParticle.Provider(sprites, 1.15f));
		registry.register(PowersParticles.RIBBON, sprites -> new ArcaneParticle.Provider(sprites, 0.95f));
		registry.register(PowersParticles.SPARK, sprites -> new ArcaneParticle.Provider(sprites, 0.85f));
		registry.register(PowersParticles.ECLIPSE, sprites -> new ArcaneParticle.Provider(sprites, 1.25f));
		registry.register(PowersParticles.ROOT, sprites -> new ArcaneParticle.Provider(sprites, 1.0f));
		registry.register(PowersParticles.FRACTURE, sprites -> new ArcaneParticle.Provider(sprites, 1.1f));
	}

	private static void tick(Minecraft client) {
		ClientMagicFx.tick();
		ClientPowerState.tickCooldowns();
		// the marking window for a player teleport counts down here and closes itself
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
		while (rankMazeKey.consumeClick()) {
			if (client.gui.screen() == null && client.player != null) {
				client.gui.setScreen(new RankMazeScreen());
			}
		}
	}

	private static void handleSlotKey(Minecraft client, int slot) {
		// pressing the same key again while marking confirms the teleport to the named player
		if (ClientPowerState.isMarking() && ClientPowerState.markingSlot == slot) {
			var player = client.player;
			if (player != null) {
				var pos = player.position();
				ClientPlayNetworking.send(new PowersPackets.TeleportMarkPayload(slot, pos.x, pos.y, pos.z));
				ClientPowerState.markingSlot = -1;
			}
			return;
		}
		// don't trigger powers while another screen is open
		if (client.gui.screen() != null) return;
		Power power = ClientPowerState.getPower(slot);
		if (power == null) {
			return;
		}
		Ability ability = power.ability();
		if (ability == null) {
			return;
		}

		// powers that need a destination open the input screen, everything else activates directly
		if (ability.requiresInput()) {
			client.gui.setScreen(new TeleportInputScreen(slot));
		} else {
			ClientPlayNetworking.send(new PowersPackets.ActivateAbilityPayload(slot));
		}
	}

	private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		PowerHudRenderer.render(graphics, tickCounter);
	}
}
