package com.powers.mixin;

import com.powers.power.AmethystDampening;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the natural-amethyst section index coherent without scanning players' 13-cubes. */
@Mixin(Level.class)
abstract class LevelBlockChangeMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At("RETURN"))
	private void powers$indexAmethyst(BlockPos position, BlockState state, int flags,
			int recursionLeft, CallbackInfoReturnable<Boolean> result) {
		if (result.getReturnValueZ() && (Object) this instanceof ServerLevel level) {
			AmethystDampening.blockChanged(level, position, state);
		}
	}
}
