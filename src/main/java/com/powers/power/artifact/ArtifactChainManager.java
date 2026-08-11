package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.ArtifactWeaponManager;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns one finite, revalidated hostile chain per artifact wielder. */
public final class ArtifactChainManager {
	private static final int DURATION_TICKS = 160;
	private static final Map<UUID, Chain> CHAINS = new HashMap<>();

	private record Chain(UUID targetId, ResourceKey<Level> dimension,
			ArtifactAlignment alignment, long expiresAt) {
	}

	private ArtifactChainManager() {
	}

	public static boolean bind(ServerPlayer owner, LivingEntity target, ArtifactAlignment alignment) {
		if (owner == null || target == null || alignment == null || owner == target
				|| !owner.isAlive() || !target.isAlive() || owner.level() != target.level()
				|| owner.distanceToSqr(target) > 64.0 * 64.0 || !owner.hasLineOfSight(target)
				|| AmethystDampening.isDampened(owner) || AmethystDampening.isDampened(target)
				|| !(owner.level() instanceof ServerLevel level)
				|| SpellFieldManager.isSanctuaryProtected(level, target)
				|| SpellFieldManager.blocksForcedMovement(level, target, owner.getUUID())
				|| !PowerProtection.mayHarm(owner, target)
				|| !PowerProtection.mayForceMove(owner, target)) return false;
		CHAINS.put(owner.getUUID(), new Chain(target.getUUID(), target.level().dimension(),
				alignment, owner.level().getServer().getTickCount() + DURATION_TICKS));
		return true;
	}

	public static void tick(MinecraftServer server) {
		var iterator = CHAINS.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			Chain chain = entry.getValue();
			ServerLevel level = server.getLevel(chain.dimension());
			LivingEntity target = level == null ? null
					: level.getEntity(chain.targetId()) instanceof LivingEntity living ? living : null;
			boolean valid = owner != null && target != null && owner.level() == level
					&& ArtifactWeaponManager.maySustain(owner, chain.alignment())
					&& ArtifactChainRules.active(server.getTickCount(), chain.expiresAt(),
					owner.isAlive(), target.isAlive(), owner.distanceToSqr(target),
					owner.hasLineOfSight(target), AmethystDampening.isDampened(owner)
							|| AmethystDampening.isDampened(target),
					SpellFieldManager.isSanctuaryProtected(level, target),
					!PowerProtection.mayHarm(owner, target)
							|| !PowerProtection.mayForceMove(owner, target)
							|| SpellFieldManager.blocksForcedMovement(level, target, owner.getUUID()));
			if (!valid) {
				iterator.remove();
				continue;
			}
			Vec3 pull = owner.position().subtract(target.position());
			if (pull.lengthSqr() > 4.0) {
				Vec3 force = pull.normalize().scale(0.14);
				target.setDeltaMovement(target.getDeltaMovement().scale(0.82).add(force.x, 0.03, force.z));
				target.hurtMarked = true;
			}
			if (server.getTickCount() % 20 == 0) {
				target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, 30, 6, false, true));
				target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, 30, 3, false, true));
			}
			if (server.getTickCount() % 10 == 0) {
				PowerFx.beam(level, owner.getEyePosition(), target.getEyePosition(),
						chain.alignment() == ArtifactAlignment.DARKNESS
								? ParticleTypes.SOUL_FIRE_FLAME : com.powers.PowersParticles.GLYPH, 24);
			}
		}
	}

	public static void forget(UUID ownerOrTarget) {
		CHAINS.remove(ownerOrTarget);
		CHAINS.entrySet().removeIf(entry -> entry.getValue().targetId().equals(ownerOrTarget));
	}

	public static void forget(UUID ownerId, ArtifactAlignment alignment) {
		Chain chain = CHAINS.get(ownerId);
		if (chain != null && chain.alignment() == alignment) CHAINS.remove(ownerId, chain);
	}

	public static void clear() {
		CHAINS.clear();
	}
}
