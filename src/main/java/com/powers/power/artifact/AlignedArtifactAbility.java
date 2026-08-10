package com.powers.power.artifact;

import com.powers.PowersMod;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Ability adapter that routes one immutable dominion definition to its server handler. */
public final class AlignedArtifactAbility extends Ability {
	private final ArtifactActionDefinition definition;

	public AlignedArtifactAbility(ArtifactActionDefinition definition) {
		super(PowersMod.id(definition.abilityId()),
				Component.translatable("ability.powers." + definition.abilityId()),
				definition.baseCooldownTicks(), false, false);
		this.definition = definition;
	}

	public ArtifactActionDefinition definition() {
		return definition;
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return ArtifactWorldActions.activate(player, definition);
	}
}
