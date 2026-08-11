package com.powers.mixin;

import com.powers.resource.OptionalItemModelRules;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces a missing POWERS item model with vanilla's conspicuous barrier model. */
@Mixin(ItemModelResolver.class)
abstract class OptionalItemModelFallbackMixin {
	private static final Identifier BARRIER_MODEL = Identifier.withDefaultNamespace("barrier");

	@Shadow @Final private ModelManager modelManager;

	@Inject(method = "getItemModel", at = @At("RETURN"), cancellable = true)
	private void powers$visibleMissingModel(Identifier id,
			CallbackInfoReturnable<ItemModel> result) {
		if (!OptionalItemModelRules.useBarrier(id.getNamespace(),
				result.getReturnValue() instanceof MissingItemModel)) return;
		ItemModel barrier = modelManager.getItemModel(BARRIER_MODEL);
		if (!(barrier instanceof MissingItemModel)) result.setReturnValue(barrier);
	}
}
