package com.powers.power.artifact;

import com.powers.fx.PowerFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns the bounded, temporary ally damage-sharing links created by Covenant Chain. */
public final class ArtifactCovenantManager {
	private record Link(UUID ownerId, long expiresAt) {
	}

	private static final Map<UUID, Link> LINKS = new HashMap<>();
	private static final Set<UUID> TRANSFERRING = new HashSet<>();

	private ArtifactCovenantManager() {
	}

	/** Replaces any prior link on the ally; one owner may maintain several deliberately cast links. */
	public static void link(ServerPlayer owner, LivingEntity ally, int durationTicks) {
		LINKS.put(ally.getUUID(), new Link(owner.getUUID(),
				owner.level().getGameTime() + Math.max(1, durationTicks)));
	}

	/** Compensates half the ally's real damage and applies that half to the owner exactly once. */
	public static void shareDamage(LivingEntity ally, DamageSource source, float damageTaken) {
		Link link = LINKS.get(ally.getUUID());
		if (link == null || TRANSFERRING.contains(ally.getUUID())) return;
		if (ally.level().getGameTime() >= link.expiresAt()) {
			LINKS.remove(ally.getUUID());
			return;
		}
		ServerPlayer owner = ally.level().getServer().getPlayerList().getPlayer(link.ownerId());
		if (owner == null || !owner.isAlive() || owner.level() != ally.level()) {
			LINKS.remove(ally.getUUID());
			return;
		}
		float shared = ArtifactCovenantRules.sharedDamage(damageTaken);
		if (shared <= 0.0F) return;
		TRANSFERRING.add(ally.getUUID());
		TRANSFERRING.add(owner.getUUID());
		try {
			ally.heal(shared);
			owner.hurtServer((ServerLevel) owner.level(), source, shared);
			PowerFx.beam((ServerLevel) ally.level(), ally.getEyePosition(), owner.getEyePosition(),
					ParticleTypes.END_ROD, 18);
		} finally {
			TRANSFERRING.remove(ally.getUUID());
			TRANSFERRING.remove(owner.getUUID());
		}
	}

	public static void forget(UUID ownerOrAlly) {
		LINKS.remove(ownerOrAlly);
		LINKS.entrySet().removeIf(entry -> entry.getValue().ownerId().equals(ownerOrAlly));
	}

	public static void clear() {
		LINKS.clear();
		TRANSFERRING.clear();
	}
}
