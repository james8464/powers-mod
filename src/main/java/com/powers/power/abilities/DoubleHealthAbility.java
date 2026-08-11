package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Toggles an owned max-health modifier while preserving unrelated modifiers. */
public class DoubleHealthAbility extends ToggleAbility {
	private static final net.minecraft.resources.Identifier MODIFIER_ID = PowersMod.id("double_health");
	private static final java.util.Map<java.util.UUID, Long> LAST_HEAL = new java.util.HashMap<>();

	public DoubleHealthAbility() {
		super(PowersMod.id("double_health"), Component.translatable("ability.powers.double_health"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		float oldMaximum = player.getMaxHealth();
		applyModifier(player, innateLevel(player).capacityMultiplier());
		long now = player.level().getServer().getTickCount();
		long last = LAST_HEAL.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
		if (DoubleHealthRules.mayHeal(last, now)) {
			player.setHealth(player.getHealth() + DoubleHealthRules.healToCap(
					player.getHealth(), oldMaximum, player.getMaxHealth()));
			LAST_HEAL.put(player.getUUID(), now);
		}
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.spiral(level, player.position(), 0.8, 2.0, 0xFF1744, 20, 0);
			com.powers.fx.PowerFx.rune(level, player.position(), 1.4, 0x78E06B, 22,
					scaling(player).unlockedVariants().contains("ancient_mastery") ? Math.PI : 0.0);
			com.powers.fx.PowerFx.sound(level, player.position(),
					net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, 0.9f, 0.72f);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		float ratio = player.getMaxHealth() <= 0 ? 1.0f : player.getHealth() / player.getMaxHealth();
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) maxHealth.removeModifier(MODIFIER_ID);
		player.setHealth(Math.min(player.getMaxHealth(), Math.max(0.0f, player.getMaxHealth() * ratio)));
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.rune(level, player.position(), 1.0, 0x78E06B, 18, Math.PI);
			com.powers.fx.PowerFx.sound(level, player.position(),
					net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, 0.65f, 1.25f);
		}
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		applyModifier(player, innateLevel(player).capacityMultiplier());
	}

	private static void applyModifier(ServerPlayer player, double healthMultiplier) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) return;
		double amount = Math.max(1.0, healthMultiplier) - 1.0;
		AttributeModifier current = maxHealth.getModifier(MODIFIER_ID);
		if (current != null && Math.abs(current.amount() - amount) <= 1.0E-6) return;
		if (current != null) maxHealth.removeModifier(MODIFIER_ID);
		maxHealth.addTransientModifier(new AttributeModifier(
				MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}
}
