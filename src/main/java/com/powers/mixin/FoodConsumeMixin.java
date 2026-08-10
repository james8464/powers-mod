package com.powers.mixin;

import com.powers.PowerStatusEffects;
import com.powers.player.FoodAffinity;
import com.powers.player.SkillSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** swaps eating behaviour for the darkness touched: normal food turns foul, strange food turns sweet */
@Mixin(FoodProperties.class)
public abstract class FoodConsumeMixin {
	@Redirect(method = "onConsume", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"))
	private void powers$darknessEating(FoodData foodData, FoodProperties food, Level level,
			LivingEntity entity, ItemStack stack, Consumable consumable) {
		if (!(entity instanceof ServerPlayer player) || !SkillSystem.hasDarknessTag(player)) {
			foodData.eat(food);
			return;
		}
		String affinity = FoodAffinity.of(stack);
		if (affinity == FoodAffinity.NEUTRAL) {
			foodData.eat(food);
			return;
		}
		if (affinity == FoodAffinity.NORMAL) {
			foodData.eat(new FoodProperties(4, 0.8F, food.canAlwaysEat()));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.HUNGER, 600, 0, false, true));
		} else {
			int nutrition = Math.max(6, (int) Math.ceil(food.nutrition() * 1.5));
			float saturation = Math.max(0.9F, food.saturation() * 3.0F);
			foodData.eat(new FoodProperties(nutrition, saturation, food.canAlwaysEat()));
		}
	}
}
