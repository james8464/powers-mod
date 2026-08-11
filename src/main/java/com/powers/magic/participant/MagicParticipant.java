package com.powers.magic.participant;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/** Player-like server magic state without pretending every participant is a ServerPlayer. */
public interface MagicParticipant {
	enum Alignment { NEUTRAL, RADIANT, DARKNESS }

	UUID id();
	LivingEntity entity();
	Alignment alignment();
	MagicConsentAuthority consentAuthority();
	int energy();
	int capacity();
	void setEnergy(int value);
	boolean suppressed();
	Optional<ServerPlayer> consentOwner();

	default boolean consume(int amount) {
		int bounded = Math.max(0, amount);
		if (energy() < bounded) return false;
		setEnergy(energy() - bounded);
		return true;
	}

	default void refund(int amount) {
		setEnergy(Math.min(capacity(), energy() + Math.max(0, amount)));
	}
}
