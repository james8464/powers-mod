package com.powers.item;

import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Indestructible pure-light peer to the Shadow Sword. */
public final class HeavenlyPartisanItem extends MythicArtifactItem {
	public HeavenlyPartisanItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactAlignment alignment() {
		return ArtifactAlignment.LIGHT;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId())
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
	}
}
