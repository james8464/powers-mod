package com.powers.mind;

import com.powers.knowledge.MagicAttemptReporter;
import com.powers.knowledge.MagicFailureReason;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

/** Renders one precise player-facing refusal from the authoritative admission result. */
public final class MindSessionFeedback {
	private MindSessionFeedback() {
	}

	public static void refuse(ServerPlayer requester, String actionId,
			MindSessionManager.Kind requested, MindSessionManager.Admission admission,
			Collection<? extends LivingEntity> candidates) {
		if (requester == null || admission == null
				|| admission.refusal() != MindSessionManager.Refusal.PARTICIPANT_BUSY) return;
		LivingEntity conflict = candidates == null ? null : candidates.stream()
				.filter(candidate -> candidate != null
						&& BodyProxyManager.mindIdentity(candidate)
								.equals(admission.conflictingParticipant()))
				.findFirst().orElse(null);
		Component participant = conflict == null
				? Component.literal(String.valueOf(admission.conflictingParticipant()))
				: conflict.getDisplayName();
		MindSessionManager.Kind existing = admission.existingKind();
		PowerMessages.overlay(requester, Component.translatable("ability.powers.mind_session_nested",
				Component.translatable("mind_session.powers.kind." + requested.serializedName()),
				participant,
				existing == null ? Component.literal("unknown")
						: Component.translatable("mind_session.powers.kind." + existing.serializedName())));
		MagicAttemptReporter.failure(requester, actionId, MagicFailureReason.MIND_SESSION_ACTIVE);
	}
}
