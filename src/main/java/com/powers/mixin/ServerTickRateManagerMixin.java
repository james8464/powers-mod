package com.powers.mixin;

import com.powers.power.state.GlobalTimeStopManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects external clock writes so a player power never undoes administrator state. */
@Mixin(ServerTickRateManager.class)
abstract class ServerTickRateManagerMixin {
	@Shadow @Final private MinecraftServer server;

	@Inject(method = "setFrozen", at = @At("HEAD"))
	private void powers$observeClockWrite(boolean frozen, CallbackInfo callback) {
		GlobalTimeStopManager.observeClockWrite(server);
	}
}
