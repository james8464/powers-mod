package com.powers.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Immutable, bounded visual frame captured when a player's consciousness departs. */
public record BodySnapshot(Profile profile, PoseState pose, AnimationState animation) {
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
	public static final Codec<BodySnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Profile.CODEC.fieldOf("profile").forGetter(BodySnapshot::profile),
			PoseState.CODEC.fieldOf("pose").forGetter(BodySnapshot::pose),
			AnimationState.CODEC.fieldOf("animation").forGetter(BodySnapshot::animation)
	).apply(instance, BodySnapshot::new));

	private static final EquipmentSlot[] VISIBLE_EQUIPMENT = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
	};

	public BodySnapshot {
		Objects.requireNonNull(profile, "profile");
		Objects.requireNonNull(pose, "pose");
		Objects.requireNonNull(animation, "animation");
	}

	/** Captures one exact frame without retaining the player or mutable item stacks. */
	public static BodySnapshot capture(ServerPlayer player) {
		Objects.requireNonNull(player, "player");
		int modelParts = 0;
		for (PlayerModelPart part : PlayerModelPart.values()) {
			if (player.isModelPartShown(part)) modelParts |= part.getMask();
		}
		List<String> equipment = new ArrayList<>(VISIBLE_EQUIPMENT.length);
		for (EquipmentSlot slot : VISIBLE_EQUIPMENT) {
			ItemStack stack = player.getItemBySlot(slot);
			equipment.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#"
					+ stack.getCount() + "#" + stack.getDamageValue());
		}
		String bed = player.getBedOrientation() == null ? ""
				: player.getBedOrientation().name().toLowerCase(Locale.ROOT);
		String swingingHand = player.swingingArm == null ? "main_hand"
				: handName(player.swingingArm);
		String usedHand = player.isUsingItem() ? handName(player.getUsedItemHand()) : "main_hand";
		return new BodySnapshot(
				new Profile(player.getUUID(), player.getGameProfile().name(), modelParts, equipment),
				new PoseState(player.getPose().name().toLowerCase(Locale.ROOT),
						player.getMainArm().name().toLowerCase(Locale.ROOT), bed,
						player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot,
						player.getScale(), player.getDeltaMovement().x, player.getDeltaMovement().y,
						player.getDeltaMovement().z),
				new AnimationState(player.swinging, swingingHand, player.swingTime, player.attackAnim,
						player.walkAnimation.position(), player.walkAnimation.speed(), player.isUsingItem(),
						usedHand, player.getTicksUsingItem()));
	}

	private static String handName(InteractionHand hand) {
		return hand.name().toLowerCase(Locale.ROOT);
	}

	/** Profile identity, vanilla model-part mask, and immutable equipment evidence. */
	public record Profile(UUID id, String name, int modelParts, List<String> equipment) {
		public static final Codec<Profile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUID_CODEC.fieldOf("id").forGetter(Profile::id),
				Codec.STRING.fieldOf("name").forGetter(Profile::name),
				Codec.intRange(0, 255).fieldOf("model_parts").forGetter(Profile::modelParts),
				Codec.STRING.listOf().fieldOf("equipment").forGetter(Profile::equipment)
		).apply(instance, Profile::new));

		public Profile {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(name, "name");
			if (name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Invalid profile name");
			if (modelParts < 0 || modelParts > 255) throw new IllegalArgumentException("Invalid model-part mask");
			equipment = List.copyOf(Objects.requireNonNull(equipment, "equipment"));
			if (equipment.size() > VISIBLE_EQUIPMENT.length
					|| equipment.stream().anyMatch(value -> value.length() > 160)) {
				throw new IllegalArgumentException("Invalid frozen equipment descriptors");
			}
		}
	}

	/** Exact body transform, pose, handedness, scale, and departure movement. */
	public record PoseState(String pose, String mainArm, String bedOrientation,
			float yRot, float xRot, float headRot, float bodyRot, float scale,
			double velocityX, double velocityY, double velocityZ) {
		public static final Codec<PoseState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("pose").forGetter(PoseState::pose),
				Codec.STRING.fieldOf("main_arm").forGetter(PoseState::mainArm),
				Codec.STRING.fieldOf("bed_orientation").forGetter(PoseState::bedOrientation),
				Codec.FLOAT.fieldOf("y_rot").forGetter(PoseState::yRot),
				Codec.FLOAT.fieldOf("x_rot").forGetter(PoseState::xRot),
				Codec.FLOAT.fieldOf("head_rot").forGetter(PoseState::headRot),
				Codec.FLOAT.fieldOf("body_rot").forGetter(PoseState::bodyRot),
				Codec.FLOAT.fieldOf("scale").forGetter(PoseState::scale),
				Codec.DOUBLE.fieldOf("velocity_x").forGetter(PoseState::velocityX),
				Codec.DOUBLE.fieldOf("velocity_y").forGetter(PoseState::velocityY),
				Codec.DOUBLE.fieldOf("velocity_z").forGetter(PoseState::velocityZ)
		).apply(instance, PoseState::new));

		public PoseState {
			Objects.requireNonNull(pose, "pose");
			Objects.requireNonNull(mainArm, "mainArm");
			Objects.requireNonNull(bedOrientation, "bedOrientation");
			if (!finite(yRot, xRot, headRot, bodyRot, scale)
					|| !Double.isFinite(velocityX) || !Double.isFinite(velocityY)
					|| !Double.isFinite(velocityZ) || scale < 0.0625F || scale > 16.0F) {
				throw new IllegalArgumentException("Invalid frozen pose transform");
			}
		}
	}

	/** Exact departure swing, limb phase, and item-use frame. */
	public record AnimationState(boolean swinging, String swingingHand, int swingTime,
			float attackAnimation, float walkPosition, float walkSpeed,
			boolean usingItem, String usedHand, int useTicks) {
		public static final Codec<AnimationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.BOOL.fieldOf("swinging").forGetter(AnimationState::swinging),
				Codec.STRING.fieldOf("swinging_hand").forGetter(AnimationState::swingingHand),
				Codec.INT.fieldOf("swing_time").forGetter(AnimationState::swingTime),
				Codec.FLOAT.fieldOf("attack_animation").forGetter(AnimationState::attackAnimation),
				Codec.FLOAT.fieldOf("walk_position").forGetter(AnimationState::walkPosition),
				Codec.FLOAT.fieldOf("walk_speed").forGetter(AnimationState::walkSpeed),
				Codec.BOOL.fieldOf("using_item").forGetter(AnimationState::usingItem),
				Codec.STRING.fieldOf("used_hand").forGetter(AnimationState::usedHand),
				Codec.INT.fieldOf("use_ticks").forGetter(AnimationState::useTicks)
		).apply(instance, AnimationState::new));

		public AnimationState {
			Objects.requireNonNull(swingingHand, "swingingHand");
			Objects.requireNonNull(usedHand, "usedHand");
			if (swingTime < 0 || useTicks < 0
					|| !finite(attackAnimation, walkPosition, walkSpeed)) {
				throw new IllegalArgumentException("Invalid frozen animation frame");
			}
		}
	}

	private static boolean finite(float... values) {
		for (float value : values) if (!Float.isFinite(value)) return false;
		return true;
	}
}
