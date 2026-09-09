package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import com.powers.power.scaling.SafeScaleTransitionService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Pose;

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
		if (removeModifier(player)) showChange(player, 1.0);
	}

	@Override
	public boolean canDeactivateToggle(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return player.level() instanceof ServerLevel level
				&& findNormalTransition(level, player) != null;
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (player.isPassenger()) {
			boolean normalized = normalizeActiveScale(player, data);
			if (normalized) data.setToggleActive(player, id().toString(), false);
			else player.stopRiding();
			com.powers.util.PowerMessages.overlay(player,
					Component.translatable("ability.powers.size_morph.mounted"));
			com.powers.network.PowersPackets.syncTo(player);
			return;
		}
		if (!applySelectedScale(player, data, true)) {
			normalizeActiveScale(player, data);
		}
		// The sync cache makes unchanged five-tick checks free while fallback and
		// restoration reach the HUD in the same authoritative transition.
		com.powers.network.PowersPackets.syncTo(player);
	}

	private static boolean applySelectedScale(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			boolean allowOverlapFallback) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale == null || !(player.level() instanceof ServerLevel level)) return false;
		int rank = com.powers.player.SkillSystem.effectiveLevel(player);
		List<Double> candidates = SizeMorphRules.transitionCandidates(data.getSizeMorphOption(), rank)
				.stream().map(SizeMorphRules::scale).toList();
		SafeScaleTransitionService.Transition transition = SafeScaleTransitionService.preview(
				level, player, candidates, !allowOverlapFallback,
				FORCED_CRAWLERS.contains(player.getUUID()), LOOKAHEAD_BLOCKS);
		if (transition == null) return false;
		if (!SafeScaleTransitionService.apply(level, player, MODIFIER_ID, transition)) return false;
		applyPhysicalProfile(player, SizeMorphRules.profile(transition.scale()));
		if (transition.pose() == Pose.SWIMMING) FORCED_CRAWLERS.add(player.getUUID());
		else FORCED_CRAWLERS.remove(player.getUUID());
		return true;
	}

	private static void applyPhysicalProfile(ServerPlayer player, SizeMorphRules.ScaleProfile profile) {
		setContribution(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_REACH_ID,
				profile.blockReachBonus());
		setContribution(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_REACH_ID,
				profile.entityReachBonus());
		setContribution(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MASS_KNOCKBACK_ID,
				profile.knockbackResistance());
	}

	private static void setContribution(AttributeInstance attribute,
			net.minecraft.resources.Identifier id, double value) {
		if (attribute == null) return;
		attribute.removeModifier(id);
		if (Math.abs(value) > 1.0E-9) attribute.addTransientModifier(
				new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
	}

	private static boolean normalizeActiveScale(ServerPlayer player,
			PlayerPowers.PlayerPowersData data) {
		if (!removeModifier(player)) return false;
		data.setSizeMorphOption(SizeMorphRules.normalOption());
		return true;
	}

	private static boolean removeModifier(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return false;
		SafeScaleTransitionService.Transition normal = findNormalTransition(level, player);
		if (normal == null) return false;
		player.setPose(normal.pose());
		player.refreshDimensions();
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.removeModifier(MODIFIER_ID);
			player.refreshDimensions();
		}
		FORCED_CRAWLERS.remove(player.getUUID());
		removeOwned(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_REACH_ID);
		removeOwned(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_REACH_ID);
		removeOwned(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MASS_KNOCKBACK_ID);
		return level.noCollision(player, player.getBoundingBox());
	}

	private static SafeScaleTransitionService.Transition findNormalTransition(
			ServerLevel level, ServerPlayer player) {
		return SafeScaleTransitionService.previewNormal(level, player);
	}

	private static void removeOwned(AttributeInstance attribute,
			net.minecraft.resources.Identifier id) {
		if (attribute != null) attribute.removeModifier(id);
	}

	/** Drops pose preference owned by a departing player entity. */
	public static void forget(UUID player) {
		FORCED_CRAWLERS.remove(player);
	}

	/** Drops all UUID-keyed pose preference at server shutdown. */
	public static void clearAll() {
		FORCED_CRAWLERS.clear();
	}

	private static void showChange(ServerPlayer player, double scale) {
		if (!(player.level() instanceof ServerLevel level)) return;
		int color = scale < 1.0 ? 0x00E5FF : scale > 1.0 ? 0xFFD600 : 0xFFFFFF;
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.POOF, 14, 0.6, 0.08);
		PowerFx.rune(level, player.position(), Math.max(0.8, scale), color, 20, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.PLAYER_HURT_FREEZE, 0.8f,
				(float) Math.max(0.55, Math.min(1.6, 1.25 / scale)));
	}

}
