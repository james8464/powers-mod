package com.powers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.PowersBlocks;
import com.powers.boss.FirstVesselRitual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

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

	/** Crouch-use on a completed Arcane Crucible altar invokes the First Vessel. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.isSecondaryUseActive()
				&& context.getLevel().getBlockState(context.getClickedPos()).getBlock()
				== PowersBlocks.ARCANE_CRUCIBLE) {
			if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
			if (context.getPlayer() instanceof ServerPlayer player
					&& FirstVesselRitual.invoke(player, context.getClickedPos())) {
				return InteractionResult.SUCCESS_SERVER;
			}
			return InteractionResult.FAIL;
		}
		return super.useOn(context);
	}

}
