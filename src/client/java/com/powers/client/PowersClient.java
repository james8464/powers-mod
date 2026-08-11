package com.powers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.powers.PowersMod;
import com.powers.client.screen.CelestialLocatorScreen;
import com.powers.client.screen.PowerSelectionScreen;
import com.powers.client.screen.TeleportInputScreen;
import com.powers.client.screen.RankMazeScreen;
import com.powers.client.screen.ShadowSwordScreen;
import com.powers.client.screen.GrimoireIndexScreen;
import com.powers.client.screen.ReservoirTransferScreen;
import com.powers.client.screen.RainbowConvergenceScreen;
import com.powers.client.fx.ClientMagicFx;
import com.powers.client.fx.ClientShapeFx;
import com.powers.client.fx.ClientBeamFx;
import com.powers.client.fx.ClientCelestialRuinFx;
import com.powers.client.body.ClientBodySnapshots;
import com.powers.client.fx.particle.ArcaneParticle;
import com.powers.PowersParticles;
import com.powers.PowersEntities;
import com.powers.PowersMenus;
import com.powers.client.screen.ArcaneCrucibleScreen;
import com.powers.network.PowerStatePayload;
import com.powers.network.PowersPackets;
import com.powers.network.MagicFxPackets;
import com.powers.network.CelestialRuinPackets;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.BodyProxyPackets;
import com.powers.network.CompanionPackets;
import com.powers.network.VesselControlPackets;
import com.powers.network.GrimoirePackets;
import com.powers.network.RelicPackets;
import com.powers.network.CrystalSelectorPackets;
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
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** client entry point: registers the v/x/c slot keys, wires up the huds and the teleport screen */
public class PowersClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(PowersMod.id("powers"));

	public static KeyMapping slotKey1;
	public static KeyMapping slotKey2;
	public static KeyMapping slotKey3;
	public static KeyMapping rankMazeKey;
	public static KeyMapping companionKey;
	public static KeyMapping releaseCastToggleKey;

	@Override
	public void onInitializeClient() {
		ClientProtocolHandshake.initialize();
		ClientHudPreferences.initialize();
		ClientInteractionPreferences.initialize();
		MenuScreens.register(PowersMenus.ARCANE_CRUCIBLE, ArcaneCrucibleScreen::new);
		CrucibleWeaponTooltip.register();
		RelicTooltip.register();
		slotKey1 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
		slotKey2 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
		slotKey3 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.slot3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
		rankMazeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.rank_maze", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
		companionKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.companion", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
		releaseCastToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.powers.release_cast_toggle", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
		ClientPlayNetworking.registerGlobalReceiver(PowerStatePayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientPowerState.update(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MagicFxPackets.MagicFxPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientMagicFx.handle(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MagicFxPackets.BeamFxPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientBeamFx.handle(payload)));
		ClientPlayNetworking.registerGlobalReceiver(MagicFxPackets.ShapeFxPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientShapeFx.handle(payload)));
		ClientPlayNetworking.registerGlobalReceiver(BodyProxyPackets.BodySnapshotPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientBodySnapshots.handle(payload)));
		ClientPlayNetworking.registerGlobalReceiver(CompanionPackets.StatePayload.TYPE,
				(payload, context) -> context.client().execute(() -> PrivateCompanionClient.handle(payload)));
		ClientPlayNetworking.registerGlobalReceiver(CompanionPackets.StatusPayload.TYPE,
				(payload, context) -> context.client().execute(() ->
						PrivateCompanionClient.handleStatus(payload)));
		ClientPlayNetworking.registerGlobalReceiver(VesselControlPackets.StatePayload.TYPE,
				(payload, context) -> context.client().execute(() ->
						VesselControlClient.setActive(payload.active())));
		// the celestial grimoire summons its target picker when the server vouches for the cast
		ClientPlayNetworking.registerGlobalReceiver(PowersPackets.OpenLocatorScreenPayload.TYPE,
				(payload, context) -> context.client().execute(() ->
						Minecraft.getInstance().gui.setScreen(
								new CelestialLocatorScreen(payload.mode(), payload.nonce()))));
		ClientPlayNetworking.registerGlobalReceiver(GrimoirePackets.OpenIndexPayload.TYPE,
				(payload, context) -> context.client().execute(() -> Minecraft.getInstance().gui.setScreen(
						new GrimoireIndexScreen(payload.grimoireKey(), payload.selected(), payload.entries()))));
		ClientPlayNetworking.registerGlobalReceiver(ShadowSwordPackets.OpenMenuPayload.TYPE,
				(payload, context) -> context.client().execute(() -> Minecraft.getInstance().gui.setScreen(
						new ShadowSwordScreen(payload.alignment(), payload.selectedKey(), payload.rank(),
								payload.sizeMorphOption(), payload.energy(),
								payload.favourites(),
								payload.actions()))));
		ClientPlayNetworking.registerGlobalReceiver(ShadowSwordPackets.OpenTeleportPayload.TYPE,
				(payload, context) -> context.client().execute(() -> Minecraft.getInstance().gui.setScreen(
						TeleportInputScreen.artifact(payload.alignment()))));
		ClientPlayNetworking.registerGlobalReceiver(RelicPackets.OpenReservoirPayload.TYPE,
				(payload, context) -> context.client().execute(() -> Minecraft.getInstance().gui.setScreen(
						new ReservoirTransferScreen(payload))));
		ClientPlayNetworking.registerGlobalReceiver(CrystalSelectorPackets.OpenPayload.TYPE,
				(payload, context) -> context.client().execute(() -> Minecraft.getInstance().gui.setScreen(
						new RainbowConvergenceScreen(payload.modes(), payload.selected()))));
		ClientPlayNetworking.registerGlobalReceiver(CelestialRuinPackets.Payload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientCelestialRuinFx.handle(payload)));
		// clear the cached state when you leave the server so the hud doesn't carry over old powers
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientPowerState.reset();
			ClientMagicFx.reset();
			ClientBodySnapshots.clear();
			PrivateCompanionClient.clear();
			VesselControlClient.setActive(false);
			ClientCelestialRuinFx.reset();
		});

		registerParticles();
		EntityRenderers.register(PowersEntities.DARKNESS_CREATURE,
				context -> new PlayerLikeMobRenderer(context, "darkness_player"));
		EntityRenderers.register(PowersEntities.POWER_TEST_ACTOR,
				context -> new PlayerLikeMobRenderer(context, "test_actor"));
		EntityRenderers.register(PowersEntities.RADIANT_SENTINEL,
				context -> new PlayerLikeMobRenderer(context, "radiant_sentinel"));
		EntityRenderers.register(PowersEntities.DARK_HERALD,
				context -> new PlayerLikeMobRenderer(context, "dark_herald", 0.8F));
		EntityRenderers.register(PowersEntities.LIGHT_HERALD,
				context -> new PlayerLikeMobRenderer(context, "light_herald", 0.8F));
		EntityRenderers.register(PowersEntities.FIRST_VESSEL,
				context -> new PlayerLikeMobRenderer(context, "first_vessel", 0.65F));
		EntityRenderers.register(PowersEntities.ECHO_CLONE, EchoCloneRenderer::new);
		EntityRenderers.register(PowersEntities.SHADOW_COMPANION, ShadowCompanionRenderer::new);

		// Join vanilla's survival-bar layer so extra heart/armour rows are known
		// before the adaptive energy vessel and icon rail are extracted.
		var energyHud = PowersMod.id("energy_hud");
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.AIR_BAR,
				energyHud,
				(graphics, tickCounter) -> EnergyHudRenderer.render(graphics));
		HudElementRegistry.attachElementAfter(
				energyHud,
				PowersMod.id("power_hud"),
				PowersClient::renderHud);
		HudElementRegistry.attachElementAfter(
				PowersMod.id("power_hud"), PowersMod.id("celestial_ruin_flash"),
				(graphics, tickCounter) -> ClientCelestialRuinFx.renderFlash(graphics));
		HudElementRegistry.attachElementAfter(
				PowersMod.id("power_hud"), PowersMod.id("shadow_status_hud"),
				(graphics, tickCounter) -> ShadowStatusHudRenderer.render(graphics));

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
		ClientCelestialRuinFx.tick();
		ClientPowerState.tickCooldowns();
		PrivateCompanionClient.tick();
		VesselControlClient.tick(client);
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
		while (companionKey.consumeClick()) {
			if (client.gui.screen() == null && client.player != null) {
				PrivateCompanionClient.interact();
			}
		}
		while (releaseCastToggleKey.consumeClick()) {
			boolean enabled = ClientInteractionPreferences.toggleReleaseToCast();
			Component message = Component.translatable(enabled
					? "screen.powers.artifact.release_cast.on"
					: "screen.powers.artifact.release_cast.off");
			if (client.player != null) client.player.sendOverlayMessage(message);
			client.getNarrator().saySystemNow(message);
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
		// Crouch-key opens an explicit selector; an ordinary press still casts
		// the primed element or toggles the selected body scale.
		if (ability.selectionOptionCount() > 0 && client.player != null
				&& client.player.isCrouching()) {
			client.gui.setScreen(new PowerSelectionScreen(slot, ability,
					ClientPowerState.sizeMorphOption()));
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
