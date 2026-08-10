package com.powers.item;

import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Indestructible interactive base shared by the Shadow Sword and Heavenly Partisan. */
public abstract class MythicArtifactItem extends Item {
	protected MythicArtifactItem(Properties properties) {
		super(properties.stacksTo(1).fireResistant());
	}

	public abstract ArtifactAlignment alignment();

	@Override
	public final InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			if (player.isCrouching()) ArtifactWeaponManager.openMenu(player, alignment());
			else ArtifactWeaponManager.activateSelected(player, alignment());
		}
		return InteractionResult.SUCCESS;
	}
}
