package com.powers.mixin;

import com.powers.item.MythicArtifactItem;
import com.powers.item.artifact.ArtifactScrollRules;
import com.powers.network.ShadowSwordPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces hotbar scrolling only for an intentional crouch-held artifact gesture. */
@Mixin(MouseHandler.class)
public abstract class ArtifactScrollMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void powers$cycleHeldArtifact(long window, double horizontal, double vertical,
			CallbackInfo callback) {
		Minecraft client = Minecraft.getInstance();
		if (window != client.getWindow().handle() || client.player == null) return;
		MythicArtifactItem artifact = heldArtifact(client.player.getMainHandItem());
		if (artifact == null) artifact = heldArtifact(client.player.getOffhandItem());
		boolean screenOpen = client.gui.screen() != null || client.gui.overlay() != null;
		if (!ArtifactScrollRules.shouldIntercept(screenOpen, client.player.isCrouching(),
				artifact != null, vertical)) return;
		int direction = ArtifactScrollRules.direction(vertical);
		if (direction == 0 || !ClientPlayNetworking.canSend(ShadowSwordPackets.CyclePayload.TYPE)) return;
		ClientPlayNetworking.send(new ShadowSwordPackets.CyclePayload(
				artifact.alignment().serializedName(), direction));
		callback.cancel();
	}

	private static MythicArtifactItem heldArtifact(ItemStack stack) {
		return stack.getItem() instanceof MythicArtifactItem artifact ? artifact : null;
	}
}
