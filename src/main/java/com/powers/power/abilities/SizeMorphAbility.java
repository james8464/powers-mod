package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persistent player Size Morphing with explicit scale selection and safe reset. */
public final class SizeMorphAbility extends ToggleAbility {
	private static final net.minecraft.resources.Identifier MODIFIER_ID = PowersMod.id("size_morph");
	private static final net.minecraft.resources.Identifier BLOCK_REACH_ID =
			PowersMod.id("size_morph_block_reach");
	private static final net.minecraft.resources.Identifier ENTITY_REACH_ID =
			PowersMod.id("size_morph_entity_reach");
	private static final net.minecraft.resources.Identifier MASS_KNOCKBACK_ID =
			PowersMod.id("size_morph_mass");
	private static final double LOOKAHEAD_BLOCKS = 1.5;
	private static final Set<UUID> FORCED_CRAWLERS = new HashSet<>();

	public SizeMorphAbility() {
		super(PowersMod.id("size_shift"), Component.translatable("ability.powers.size_morph"));
	}

	@Override
	public String magicActionId(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return "size_morph";
	}

	@Override
	public int selectionOptionCount() {
		return SizeMorphRules.scales().size();
	}

	@Override
	public Component selectionOptionName(int option) {
		return Component.literal(SizeMorphRules.scale(option) + "×");
	}

	@Override
	public boolean selectOption(ServerPlayer player, PlayerPowers.PlayerPowersData data, int option) {
		if (!SizeMorphRules.isValidOption(option)
				|| com.powers.player.SkillSystem.effectiveLevel(player)
				< SizeMorphRules.minimumRank(option)) return false;
		int previousOption = data.getSizeMorphOption();
		data.setSizeMorphOption(option);
		if (data.isToggleActive(id().toString()) && !applySelectedScale(player, data, false)) {
			data.setSizeMorphOption(previousOption);
			if (!applySelectedScale(player, data, false)) normalizeActiveScale(player, data);
			return false;
		}
		return true;
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (player.isPassenger()) {
			com.powers.util.PowerMessages.overlay(player,
					Component.translatable("ability.powers.size_morph.mounted"));
			return false;
		}
		if (!applySelectedScale(player, data, false)) return false;
		showChange(player, SizeMorphRules.scale(data.getSizeMorphOption()));
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		removeModifier(player);
		showChange(player, 1.0);
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (player.isPassenger()) {
			normalizeActiveScale(player, data);
			data.setToggleActive(player, id().toString(), false);
			com.powers.util.PowerMessages.overlay(player,
					Component.translatable("ability.powers.size_morph.mounted"));
			com.powers.network.PowersPackets.syncTo(player);
			return;
		}
		if (!applySelectedScale(player, data, true)) {
			normalizeActiveScale(player, data);
		}
	}

	private static boolean applySelectedScale(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			boolean allowOverlapFallback) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale == null || !(player.level() instanceof ServerLevel level)) return false;
		Transition transition = findTransition(level, player, data.getSizeMorphOption(),
				allowOverlapFallback);
		if (transition == null) return false;
		applyScaleModifier(scale, transition.scale());
		player.setPose(transition.pose());
		player.refreshDimensions();
		applyPhysicalProfile(player, SizeMorphRules.profile(transition.scale()));
		if (transition.pose() == Pose.SWIMMING) FORCED_CRAWLERS.add(player.getUUID());
		else FORCED_CRAWLERS.remove(player.getUUID());
		return level.noCollision(player, player.getBoundingBox());
	}

	private static Transition findTransition(ServerLevel level, ServerPlayer player, int requestedOption,
			boolean allowOverlapFallback) {
		double currentScale = Math.max(0.001, player.getScale());
		boolean requested = true;
		for (int option : SizeMorphRules.transitionCandidates(requestedOption)) {
			double candidateScale = SizeMorphRules.scale(option);
			boolean currentPositionClear = false;
			for (Pose pose : candidatePoses(player, candidateScale)) {
				AABB body = dimensionsAt(player, pose, candidateScale, currentScale)
						.makeBoundingBox(player.position()).deflate(1.0E-7);
				if (!level.noCollision(player, body)) continue;
				currentPositionClear = true;
				Vec3 look = player.getLookAngle();
				double horizontal = Math.hypot(look.x, look.z);
				Vec3 preview = horizontal < 1.0E-6 ? Vec3.ZERO
						: new Vec3(look.x / horizontal * LOOKAHEAD_BLOCKS, 0.0,
								look.z / horizontal * LOOKAHEAD_BLOCKS);
				if (level.noCollision(player, body.expandTowards(preview))) {
					return new Transition(candidateScale, pose);
				}
			}
			if (requested && !currentPositionClear && !allowOverlapFallback) return null;
			requested = false;
		}
		return null;
	}

	private static List<Pose> candidatePoses(ServerPlayer player, double scale) {
		List<Pose> poses = new ArrayList<>();
		if (FORCED_CRAWLERS.contains(player.getUUID())) poses.add(Pose.STANDING);
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

	private static void applyScaleModifier(AttributeInstance scale, double selected) {
		scale.removeModifier(MODIFIER_ID);
		if (selected != 1.0) scale.addTransientModifier(new AttributeModifier(
				MODIFIER_ID, selected - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void applyPhysicalProfile(ServerPlayer player, SizeMorphRules.ScaleProfile profile) {
		setAbsolute(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_REACH_ID,
				profile.blockReach());
		setAbsolute(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_REACH_ID,
				profile.entityReach());
		setAbsolute(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MASS_KNOCKBACK_ID,
				profile.knockbackResistance());
	}

	private static void setAbsolute(AttributeInstance attribute,
			net.minecraft.resources.Identifier id, double value) {
		if (attribute == null) return;
		attribute.removeModifier(id);
		double delta = value - attribute.getValue();
		if (Math.abs(delta) > 1.0E-9) attribute.addTransientModifier(
				new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
	}

	private static void normalizeActiveScale(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		data.setSizeMorphOption(SizeMorphRules.normalOption());
		removeModifier(player);
	}

	private static void removeModifier(ServerPlayer player) {
		boolean forcedCrawler = FORCED_CRAWLERS.remove(player.getUUID());
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.removeModifier(MODIFIER_ID);
			player.refreshDimensions();
		}
		if (forcedCrawler && player.level() instanceof ServerLevel level) {
			AABB standing = player.getDimensions(Pose.STANDING)
					.makeBoundingBox(player.position()).deflate(1.0E-7);
			if (level.noCollision(player, standing)) {
				player.setPose(Pose.STANDING);
				player.refreshDimensions();
			}
		}
		removeOwned(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_REACH_ID);
		removeOwned(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_REACH_ID);
		removeOwned(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MASS_KNOCKBACK_ID);
	}

	private static void removeOwned(AttributeInstance attribute,
			net.minecraft.resources.Identifier id) {
		if (attribute != null) attribute.removeModifier(id);
	}

	private static void showChange(ServerPlayer player, double scale) {
		if (!(player.level() instanceof ServerLevel level)) return;
		int color = scale < 1.0 ? 0x00E5FF : scale > 1.0 ? 0xFFD600 : 0xFFFFFF;
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.POOF, 14, 0.6, 0.08);
		PowerFx.rune(level, player.position(), Math.max(0.8, scale), color, 20, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.PLAYER_HURT_FREEZE, 0.8f,
				(float) Math.max(0.55, Math.min(1.6, 1.25 / scale)));
	}

	private record Transition(double scale, Pose pose) { }
}
