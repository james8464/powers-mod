package com.powers.mixin;

import com.powers.client.body.ClientBodySnapshots;
import com.powers.mind.BodySnapshot;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** Replaces mannequin idle-derived values with the owner's immutable departure frame. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
			+ "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void powers$freezeBodyFrame(Avatar entity, AvatarRenderState state,
			float partialTick, CallbackInfo callback) {
		if (!(entity instanceof ClientMannequin)) return;
		BodySnapshot snapshot = ClientBodySnapshots.get(entity.getId());
		if (snapshot == null) return;
		BodySnapshot.PoseState pose = snapshot.pose();
		BodySnapshot.AnimationState animation = snapshot.animation();
		Pose resolvedPose = enumValue(Pose.class, pose.pose(), Pose.STANDING);
		HumanoidArm mainArm = enumValue(HumanoidArm.class, pose.mainArm(), HumanoidArm.RIGHT);
		InteractionHand usedHand = enumValue(
				InteractionHand.class, animation.usedHand(), InteractionHand.MAIN_HAND);
		InteractionHand swingingHand = enumValue(
				InteractionHand.class, animation.swingingHand(), InteractionHand.MAIN_HAND);

		state.ageInTicks = 0.0F;
		state.bodyRot = pose.bodyRot();
		state.yRot = pose.headRot() - pose.bodyRot();
		state.xRot = pose.xRot();
		state.pose = resolvedPose;
		state.bedOrientation = pose.bedOrientation().isEmpty() ? null
				: enumValue(Direction.class, pose.bedOrientation(), null);
		state.walkAnimationPos = animation.walkPosition();
		state.walkAnimationSpeed = animation.walkSpeed();
		state.scale = pose.scale();
		state.mainArm = mainArm;
		state.attackArm = swingingHand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
		state.attackTime = animation.attackAnimation();
		state.isUsingItem = animation.usingItem();
		state.useItemHand = usedHand;
		state.ticksUsingItem = animation.useTicks();
		state.isCrouching = resolvedPose == Pose.CROUCHING;
		state.isFallFlying = resolvedPose == Pose.FALL_FLYING;
		state.isVisuallySwimming = resolvedPose == Pose.SWIMMING;
		state.swimAmount = resolvedPose == Pose.SWIMMING ? 1.0F : 0.0F;
		int parts = snapshot.profile().modelParts();
		state.showHat = shown(parts, PlayerModelPart.HAT);
		state.showJacket = shown(parts, PlayerModelPart.JACKET);
		state.showLeftPants = shown(parts, PlayerModelPart.LEFT_PANTS_LEG);
		state.showRightPants = shown(parts, PlayerModelPart.RIGHT_PANTS_LEG);
		state.showLeftSleeve = shown(parts, PlayerModelPart.LEFT_SLEEVE);
		state.showRightSleeve = shown(parts, PlayerModelPart.RIGHT_SLEEVE);
		state.showCape = shown(parts, PlayerModelPart.CAPE);
		state.capeFlap = 0.0F;
		state.capeLean = 0.0F;
		state.capeLean2 = 0.0F;
	}

	private static boolean shown(int mask, PlayerModelPart part) {
		return (mask & part.getMask()) != 0;
	}

	private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
		try {
			return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}
}
