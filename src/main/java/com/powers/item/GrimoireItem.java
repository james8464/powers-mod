package com.powers.item;

import com.powers.spell.SpellCastingManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Selectable ritual book; sneak-use turns pages and normal use channels the chosen spell. */
public class GrimoireItem extends Item {
	private final String key;

	public GrimoireItem(Properties properties, String key) {
		super(properties.stacksTo(1));
		this.key = key;
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			SpellCastingManager.use(player, key);
		}
		return InteractionResult.SUCCESS;
	}

	public String key() {
		return key;
	}
}
