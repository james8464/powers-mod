package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Elemental Blast: cycles fire, frost, storm and earth in order, one element
 * per press, each cast using that element's own cooldown. Fused from Galaxy
 * Steve's elementally-charged essence, inspired by Elemental Steve.
 */
public class ElementalBlastAbility extends Ability {
	private static final Ability[] ELEMENTS = {
			// the cycle order: fire, then frost, then storm, then earth
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
		// only advance when the element actually fired, so a failed cast
		// (e.g. lightning with no valid target) is retried, not skipped
		if (success) {
			data.nextPhase();
		}
		return success;
	}

	@Override
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// the phase already advanced on success, so rewind one to report the
		// cooldown of the element that was actually cast
		int phase = (data.getPhase() + 3) % 4;
		return ELEMENTS[phase].cooldownTicks();
	}
}
