package com.powers.mixin;

import com.powers.PowersItems;
import com.powers.client.ClientPowerState;
import com.powers.item.RainbowCrystalVisualRules;
import com.powers.player.SkillSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Selects the corrupted Rainbow model from holder state without mutating item data. */
@Mixin(ItemModelResolver.class)
abstract class RainbowCrystalVisualMixin {
	@Redirect(method = "updateForLiving", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem("
					+ "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
					+ "Lnet/minecraft/world/item/ItemStack;"
					+ "Lnet/minecraft/world/item/ItemDisplayContext;"
					+ "Lnet/minecraft/world/level/Level;"
					+ "Lnet/minecraft/world/entity/ItemOwner;I)V"))
	private void powers$holderSensitiveRainbow(ItemModelResolver resolver,
			ItemStackRenderState state, ItemStack stack, ItemDisplayContext context,
			Level level, ItemOwner owner, int seed) {
		boolean darkness = false;
		if (owner instanceof LivingEntity holder) {
			var local = Minecraft.getInstance().player;
			darkness = local != null && holder.getUUID().equals(local.getUUID())
					? ClientPowerState.darkness()
					: holder.entityTags().contains(SkillSystem.DARKNESS_TAG);
		}
		ItemStack visual = RainbowCrystalVisualRules.corrupted(
				stack.is(PowersItems.RAINBOW_CRYSTAL), darkness)
				? PowersItems.LEGACY_INFECTED_RAINBOW_CRYSTAL.getDefaultInstance() : stack;
		resolver.updateForTopItem(state, visual, context, level, owner, seed);
	}
}
