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
	public String magicActionId(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return ElementalPhase.fromIndex(data.getPhase()).actionId();
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ElementalPhase phase = ElementalPhase.fromIndex(data.getPhase());
		Ability selected = ELEMENTS[phase.index()];
		boolean success = selected instanceof LightningStrikeAbility lightning
				? lightning.activateFromElemental(player, data)
				: selected.activate(player, data);
		// only advance when the element actually fired, so a failed cast
		// (e.g. lightning with no valid target) is retried, not skipped
		if (success) {
			data.nextPhase();
			if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
				com.powers.fx.PowerFx.rune(level, player.position(),
						1.0 + scaling(player).potencyMultiplier() * 0.25, phase.color(), 20,
						phase.index() * Math.PI / 2.0);
			}
		}
		return success;
	}

	@Override
	public int cooldownTicksFor(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// the phase already advanced on success, so rewind one to report the
		// cooldown of the element that was actually cast
		ElementalPhase phase = ElementalPhase.fromIndex(
				ElementalPhase.previousIndex(data.getPhase()));
		return ELEMENTS[phase.index()].cooldownTicksFor(player, data);
	}
}
