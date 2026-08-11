package com.powers.item;

import com.powers.PowersDataComponents;
import com.powers.fx.PowerFx;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

/** Recharges dropped Miniportals from one overlapping amethyst shard. */
public final class MiniportalRechargeManager {
	private static final int PULSE_TICKS = 10;
	private static final int MAX_NEARBY_ITEMS = 8;

	private MiniportalRechargeManager() {
	}

	public static void tick(ItemEntity device) {
		if (!(device.level() instanceof ServerLevel level)
				|| Math.floorMod(device.getId(), PULSE_TICKS)
						!= Math.floorMod(device.tickCount, PULSE_TICKS)
				|| !isMiniportal(device)
				|| MiniportalRules.charges(device.getItem().get(PowersDataComponents.MINIPORTAL_CHARGES))
						>= MiniportalRules.MAX_CHARGES) return;
		for (ItemEntity candidate : BoundedEntityCandidates.ofClass(level, ItemEntity.class,
				device.getBoundingBox().inflate(0.35), MAX_NEARBY_ITEMS,
				entity -> entity != device && entity.isAlive()
						&& entity.getItem().is(Items.AMETHYST_SHARD))) {
			device.getItem().set(PowersDataComponents.MINIPORTAL_CHARGES,
					MiniportalRules.afterRecharge());
			candidate.getItem().shrink(1);
			if (candidate.getItem().isEmpty()) candidate.discard();
			PowerFx.rune(level, device.position().add(0.0, 0.2, 0.0), 0.8,
					0xC99CFF, 18, level.getGameTime() * 0.08);
			PowerFx.sound(level, device.position(), SoundEvents.AMETHYST_BLOCK_RESONATE,
					0.7F, 1.25F);
			return;
		}
	}

	private static boolean isMiniportal(ItemEntity entity) {
		return entity.getItem().getItem() instanceof ImportedArtifactItem relic
				&& relic.texture().equals("device_miniportal");
	}
}
