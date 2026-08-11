package com.powers.mixin;

import com.powers.power.abilities.VesselPossessionAbility;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops a possessed player's local client from fighting the authenticated controller input. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PossessedPlayerInputMixin {
	private boolean powers$controlled() {
		return VesselPossessionAbility.isControlledPlayer(
				((ServerGamePacketListenerImpl) (Object) this).player.getUUID());
	}

	@Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
	private void powers$blockMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
	private void powers$blockInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
	private void powers$blockHotbar(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handleAttack", at = @At("HEAD"), cancellable = true)
	private void powers$blockAttack(ServerboundAttackPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
	private void powers$blockInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
	private void powers$blockUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
	private void powers$blockUseOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}

	@Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
	private void powers$blockSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
		if (powers$controlled()) ci.cancel();
	}
}
