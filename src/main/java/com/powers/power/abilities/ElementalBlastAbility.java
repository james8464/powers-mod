package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Elemental Blast: cycles through the four elements in order - fire, frost,
 * storm, earth. The active element advances on every use and its own cooldown
 * is applied. Inspired by Elemental Steve, fused from Galaxy Steve's
 * elementally-charged essence.
 */
public class ElementalBlastAbility extends Ability {
	private static final Ability[] ELEMENTS = {
			new FireballAbility(),
			new FrostNovaAbility(),
			new LightningStrikeAbility(),
			new GroundSlamAbility()
	};

	public ElementalBlastAbility() {
		super(PowersMod.id("elemental_blast"),
				Component.translatable("ability.powers.elemental_blast"),
				120, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		int phase = data.getPhase();
		boolean success = ELEMENTS[phase].activate(player, data);
		// Only advance when the element actually fired, so a failed cast
		// (e.g. lightning with no valid target) is retried, not skipped.
		if (success) {
			data.nextPhase();
		}
		return success;
	}

	@Override
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// The phase was already advanced on success; rewind to the one used.
		int phase = (data.getPhase() + 3) % 4;
		return ELEMENTS[phase].cooldownTicks();
	}
}
