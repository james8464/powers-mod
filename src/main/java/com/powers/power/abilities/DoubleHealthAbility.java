package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Double Health: while toggled on your max health is doubled, and you get a
// free 20-heart top-up so turning it on doesn't leave you at half a bar.
public class DoubleHealthAbility extends ToggleAbility {
	private static final net.minecraft.resources.Identifier MODIFIER_ID = PowersMod.id("double_health");
	private static final AttributeModifier MODIFIER = new AttributeModifier(
			MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	public DoubleHealthAbility() {
		super(PowersMod.id("double_health"), Component.translatable("ability.powers.double_health"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		applyModifier(player);
		// heal up to 20 so the jump in max health actually fills the bar
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 20.0f));
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.spiral(level, player.position(), 0.8, 2.0, 0xFF1744, 20, 0);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) maxHealth.removeModifier(MODIFIER_ID);
		// clamp current health down to the new lower max, no free healing
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth()));
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		applyModifier(player);
	}

	private static void applyModifier(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null && !maxHealth.hasModifier(MODIFIER_ID)) {
			maxHealth.addTransientModifier(MODIFIER);
		}
	}
}
