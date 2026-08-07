package com.powers.mixin;

import com.powers.player.FoodAffinity;
import com.powers.player.SkillSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** swaps eating behaviour for the darkness touched: normal food turns foul, strange food turns sweet */
@Mixin(Consumable.class)
public abstract class FoodConsumeMixin {
	@Inject(method = "onConsume", at = @At("HEAD"), cancellable = true)
	private void powers$darknessEating(Level level, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		if (!(entity instanceof ServerPlayer player) || !SkillSystem.hasDarknessTag(player) || !stack.has(DataComponents.FOOD)) {
			return;
		}
		String affinity = FoodAffinity.of(stack);
		if (affinity == FoodAffinity.NEUTRAL) {
			return;
		}
		Consumable consumable = (Consumable) (Object) this;
		consumable.emitParticlesAndSounds(player.getRandom(), player, stack, 16);
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		if (affinity == FoodAffinity.NORMAL) {
			player.getFoodData().eat(4, 0.1F);
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
		} else {
			var food = stack.get(DataComponents.FOOD);
			int nutrition = Math.max(6, (int) Math.ceil(food.nutrition() * 1.5));
			float saturation = Math.max(0.9F, food.saturation() * 3.0F);
			player.getFoodData().eat(nutrition, saturation);
		}
		cir.setReturnValue(stack);
	}
}
