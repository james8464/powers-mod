package com.powers.client;

import com.powers.PowersDataComponents;
import com.powers.forge.CrucibleLightningRules;
import com.powers.forge.CrucibleWeaponData;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Adds progression details to any third-party or built-in weapon transformed by the Crucible. */
final class CrucibleWeaponTooltip {
	private CrucibleWeaponTooltip() {
	}

	static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			CrucibleWeaponData data = stack.get(PowersDataComponents.CRUCIBLE_WEAPON);
			if (data == null) return;
			lines.add(Component.translatable("tooltip.powers.crucible.alignment",
					Component.translatable("tooltip.powers.crucible." + data.alignment().serializedName()))
					.withStyle(data.alignment().serializedName().equals("darkness")
							? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD));
			lines.add(Component.translatable("tooltip.powers.crucible.level", data.level(), data.xp())
					.withStyle(ChatFormatting.GRAY));
			if (data.starBound()) {
				lines.add(Component.translatable("tooltip.powers.crucible.lightning",
						CrucibleLightningRules.damage(data.level(), false, false),
						CrucibleLightningRules.energyCost(data.level())).withStyle(ChatFormatting.AQUA));
			} else {
				lines.add(Component.translatable("tooltip.powers.crucible.unbound")
						.withStyle(ChatFormatting.DARK_GRAY));
			}
		});
	}
}
