package com.powers.magic.participant;

import com.powers.knowledge.MagicFailureReason;

/** Pure support matrix for player-like mechanics and arbitrary living entities. */
public final class ParticipantCapabilityContract {
	public record Result(boolean supported, MagicFailureReason failure) { }

	private ParticipantCapabilityContract() { }

	public static Result check(MagicParticipants.Kind kind, ParticipantCapability capability) {
		boolean supported = switch (kind) {
			case PLAYER -> true;
			case TEST_ACTOR -> capability != ParticipantCapability.PLAYER_POWER_STATE
					&& capability != ParticipantCapability.CONSENT_OWNER;
			case SHADOW -> capability != ParticipantCapability.PLAYER_POWER_STATE;
			case NONE -> false;
		};
		return new Result(supported, supported ? MagicFailureReason.NONE
				: MagicFailureReason.UNSUPPORTED_TARGET);
	}
}
