package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Elemental Blast: the player explicitly primes fire, frost, storm or earth,
 * then repeatedly casts that element until selecting another. Each element
 * retains its own cooldown and canonical action identity.
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
	public int selectionOptionCount() {
		return ElementalPhase.values().length;
	}

	@Override
	public Component selectionOptionName(int option) {
		ElementalPhase phase = ElementalPhase.fromIndex(option);
		return Component.translatable("hud.powers.element." + phase.name().toLowerCase(Locale.ROOT));
	}

	@Override
	public boolean selectOption(ServerPlayer player, PlayerPowers.PlayerPowersData data, int option) {
		if (option < 0 || option >= ElementalPhase.values().length) return false;
		data.setPhase(option);
		return true;
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ElementalPhase phase = ElementalPhase.fromIndex(data.getPhase());
		Ability selected = ELEMENTS[phase.index()];
		boolean success = selected instanceof LightningStrikeAbility lightning
				? lightning.activateFromElemental(player, data)
				: selected.activate(player, data);
		// Successful casts deliberately retain the selected phase; only the
		// selection packet may change it, so failed casts cannot desync the menu.
		if (success) {
			data.setPhase(ElementalBlastRules.phaseAfterCast(phase.index()));
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
		ElementalPhase phase = ElementalPhase.fromIndex(data.getPhase());
		return ELEMENTS[phase.index()].cooldownTicksFor(player, data);
	}
}
