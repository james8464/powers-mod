package com.powers.magic;

import com.powers.knowledge.MagicFailureReason;
import com.powers.magic.participant.MagicParticipants;
import com.powers.magic.participant.ParticipantCapability;
import net.minecraft.world.entity.LivingEntity;

/** Central bridge from the exhaustive action catalogue to participant capability resolution. */
public final class ActionTargetCapabilities {
	private static final MagicActionCatalogue ACTIONS = MagicActionCatalogue.defaults();

	private ActionTargetCapabilities() { }

	public static MagicParticipants.CapabilityResolution resolveParticipant(String actionId,
			LivingEntity target, ParticipantCapability capability) {
		MagicActionDefinition action = ACTIONS.definition(new MagicActionId(actionId));
		if (action == null || action.targetContract() == ActionTargetContract.NONE || target == null) {
			return new MagicParticipants.CapabilityResolution(java.util.Optional.empty(),
					MagicFailureReason.UNSUPPORTED_TARGET);
		}
		if (action.targetContract() == ActionTargetContract.PLAYER_PARTICIPANT
				&& MagicParticipants.kind(target) != MagicParticipants.Kind.PLAYER) {
			return new MagicParticipants.CapabilityResolution(java.util.Optional.empty(),
					MagicFailureReason.UNSUPPORTED_TARGET);
		}
		return MagicParticipants.resolve(target, capability);
	}
}
