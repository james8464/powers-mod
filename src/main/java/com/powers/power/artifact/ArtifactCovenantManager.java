package com.powers.power.artifact;

import com.powers.fx.PowerFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns the bounded, temporary ally damage-sharing links created by Covenant Chain. */
public final class ArtifactCovenantManager {
	private record Link(UUID ownerId, ResourceKey<Level> dimension, long expiresAt) {
	}

	private static final Map<UUID, Link> LINKS = new HashMap<>();
	private static final Set<UUID> TRANSFERRING = new HashSet<>();

	private ArtifactCovenantManager() {
	}

	/** Replaces any prior link on the ally; one owner may maintain several deliberately cast links. */
	public static boolean link(ServerPlayer owner, LivingEntity ally, int durationTicks) {
		if (owner == null || ally == null || owner == ally || !owner.isAlive() || !ally.isAlive()
				|| owner.level() != ally.level()) return false;
		boolean replacing = LINKS.containsKey(ally.getUUID());
		long ownerLinks = LINKS.values().stream()
				.filter(link -> link.ownerId().equals(owner.getUUID())).count();
		if (!ArtifactCovenantRules.mayAddLink((int) ownerLinks, replacing)) return false;
		LINKS.put(ally.getUUID(), new Link(owner.getUUID(), ally.level().dimension(),
				owner.level().getGameTime() + Math.max(1, durationTicks)));
		return true;
	}

	/** Removes expired, unloaded, departed, or dead links even if no ally is hit. */
	public static void tick(MinecraftServer server) {
		LINKS.entrySet().removeIf(entry -> {
			Link link = entry.getValue();
			ServerLevel level = server.getLevel(link.dimension());
			ServerPlayer owner = server.getPlayerList().getPlayer(link.ownerId());
			LivingEntity ally = level == null ? null
					: level.getEntity(entry.getKey()) instanceof LivingEntity living ? living : null;
			return owner == null || ally == null || !owner.isAlive() || !ally.isAlive()
					|| owner.level() != level
					|| ArtifactCovenantRules.expired(level.getGameTime(), link.expiresAt());
		});
	}

	/** Compensates half the ally's real damage and applies that half to the owner exactly once. */
	public static void shareDamage(LivingEntity ally, DamageSource source, float damageTaken) {
		Link link = LINKS.get(ally.getUUID());
		if (link == null || TRANSFERRING.contains(ally.getUUID())) return;
		if (ArtifactCovenantRules.expired(ally.level().getGameTime(), link.expiresAt())) {
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
