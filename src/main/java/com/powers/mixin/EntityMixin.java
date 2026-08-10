package com.powers.mixin;

import com.powers.PowersItems;
import com.powers.power.state.PowerEntityState;
import com.powers.power.state.SummonPolicy;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.magic.runtime.PhysicalMagicPresences;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * keeps dropped crystals alive through /kill. kill is declared on Entity,
 * so the check has to live in an entity mixin instead of an item entity one
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "setCustomName", at = @At("TAIL"))
	private void powers$refreshNamedTargetIndex(Component name, CallbackInfo callback) {
		NamedLivingTargetIndex.track((Entity) (Object) this);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void powers$movePhysicalMagicPresence(CallbackInfo callback) {
		PhysicalMagicPresences.move((Entity) (Object) this);
	}

	@Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
	private void powers$skipEphemeralSummonSave(CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (!SummonPolicy.shouldPersist(PowerEntityState.isEphemeral(self))) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "kill", at = @At("HEAD"), cancellable = true)
	private void powers$protectCrystalFromKill(ServerLevel level, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		// a dropped crystal shrugs off /kill entirely
		if (self instanceof ItemEntity itemEntity && PowersItems.isCrystal(itemEntity.getItem())) {
			ci.cancel();
		}
	}
}
