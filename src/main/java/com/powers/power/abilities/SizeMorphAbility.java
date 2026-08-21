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

/** Persistent player Size Morphing with explicit scale selection and safe reset. */
public final class SizeMorphAbility extends ToggleAbility {
	private static final net.minecraft.resources.Identifier MODIFIER_ID = PowersMod.id("size_morph");

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
		if (data.isToggleActive(id().toString()) && !applySelectedScale(player, data)) {
			data.setSizeMorphOption(previousOption);
			if (!applySelectedScale(player, data)) normalizeActiveScale(player, data);
			return false;
		}
		return true;
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!applySelectedScale(player, data)) return false;
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
		if (!applySelectedScale(player, data)) normalizeActiveScale(player, data);
	}

	private static boolean applySelectedScale(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale == null) return false;
		double selected = SizeMorphRules.scale(data.getSizeMorphOption());
		scale.removeModifier(MODIFIER_ID);
		if (selected != 1.0) {
			scale.addTransientModifier(new AttributeModifier(
					MODIFIER_ID, selected - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		player.refreshDimensions();
		if (selected > 1.0 && player.level() instanceof ServerLevel level
				&& !level.noCollision(player, player.getBoundingBox())) {
			scale.removeModifier(MODIFIER_ID);
			player.refreshDimensions();
			return false;
		}
		return true;
	}

	private static void normalizeActiveScale(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		data.setSizeMorphOption(SizeMorphRules.normalOption());
		removeModifier(player);
	}

	private static void removeModifier(ServerPlayer player) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.removeModifier(MODIFIER_ID);
			player.refreshDimensions();
		}
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
