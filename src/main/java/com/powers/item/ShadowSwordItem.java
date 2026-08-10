package com.powers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.powers.item.artifact.ArtifactAlignment;

/** Pure-darkness blade whose server-validated menu channels every registered power. */
public final class ShadowSwordItem extends MythicArtifactItem {
	public ShadowSwordItem(Properties properties) {
		super(properties.stacksTo(1).fireResistant());
	}

	@Override
	public ArtifactAlignment alignment() {
		return ArtifactAlignment.DARKNESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId())
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
	}

}
