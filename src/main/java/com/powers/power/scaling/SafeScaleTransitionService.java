package com.powers.power.scaling;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Shared no-mutation collision preview and exact player-scale transition service. */
public final class SafeScaleTransitionService {
	private SafeScaleTransitionService() { }

	public record Transition(double scale, Pose pose) { }

	public static Transition preview(ServerLevel level, ServerPlayer player, List<Double> candidates,
			boolean rejectBlockedFirst, boolean preferStanding, double lookaheadBlocks) {
		List<Double> scales = SafeScaleTransitionRules.candidates(candidates);
		double currentScale = Math.max(0.001, player.getScale());
		double lookahead = SafeScaleTransitionRules.sanitizeLookahead(lookaheadBlocks);
		for (int index = 0; index < scales.size(); index++) {
			double candidateScale = scales.get(index);
			boolean currentPositionClear = false;
			for (Pose pose : candidatePoses(player, candidateScale, preferStanding)) {
				AABB body = dimensionsAt(player, pose, candidateScale, currentScale)
						.makeBoundingBox(player.position()).deflate(1.0E-7);
				if (!level.noCollision(player, body)) continue;
				currentPositionClear = true;
				if (level.noCollision(player, body.expandTowards(previewVector(player, lookahead)))) {
					return new Transition(candidateScale, pose);
				}
			}
			if (index == 0 && !currentPositionClear && rejectBlockedFirst) return null;
		}
		return null;
	}

	public static Transition previewNormal(ServerLevel level, ServerPlayer player) {
		double currentScale = Math.max(0.001, player.getScale());
		for (Pose pose : List.of(Pose.STANDING, player.getPose(), Pose.CROUCHING, Pose.SWIMMING)
				.stream().distinct().toList()) {
			AABB body = dimensionsAt(player, pose, 1.0, currentScale)
					.makeBoundingBox(player.position()).deflate(1.0E-7);
			if (level.noCollision(player, body)) return new Transition(1.0, pose);
		}
		return null;
	}

	public static boolean apply(ServerLevel level, ServerPlayer player,
			Identifier modifierId, Transition transition) {
		var scale = player.getAttribute(Attributes.SCALE);
		if (scale == null || transition == null) return false;
		scale.removeModifier(modifierId);
		if (transition.scale() != 1.0) scale.addTransientModifier(new AttributeModifier(
				modifierId, transition.scale() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		player.setPose(transition.pose());
		player.refreshDimensions();
		return level.noCollision(player, player.getBoundingBox());
	}

	private static List<Pose> candidatePoses(ServerPlayer player, double scale,
			boolean preferStanding) {
		List<Pose> poses = new ArrayList<>();
		if (preferStanding) poses.add(Pose.STANDING);
		poses.add(player.getPose());
		if (scale <= 1.0) {
			poses.add(Pose.CROUCHING);
			poses.add(Pose.SWIMMING);
		}
		return poses.stream().distinct().toList();
	}

	private static EntityDimensions dimensionsAt(ServerPlayer player, Pose pose,
			double candidateScale, double currentScale) {
		return player.getDimensions(pose).scale((float) (candidateScale / currentScale));
	}

	private static Vec3 previewVector(ServerPlayer player, double lookahead) {
		Vec3 look = player.getLookAngle();
		double horizontal = Math.hypot(look.x, look.z);
		return horizontal < 1.0E-6 ? Vec3.ZERO
				: new Vec3(look.x / horizontal * lookahead, 0.0, look.z / horizontal * lookahead);
	}
}
