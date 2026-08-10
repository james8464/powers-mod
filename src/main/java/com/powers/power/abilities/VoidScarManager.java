package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicPresence;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Owns bounded, terrain-safe impact scars left by completed Void Beams. */
final class VoidScarManager {
	private static final MagicActionId ACTION = new MagicActionId("void_beam");
	private static final List<Scar> SCARS = new ArrayList<>();

	private record Scar(ResourceKey<Level> dimension, UUID owner, Vec3 center,
			long createdAt, long expiresAt, double radius, float pulseDamage,
			int witherAmplifier, int witherTicks, boolean ancientMastery,
			MagicPresenceId presenceId) {
		private Scar {
			if (dimension == null || owner == null || center == null || presenceId == null
					|| createdAt < 0L || expiresAt <= createdAt || !Double.isFinite(radius)
					|| radius <= 0.0 || !Float.isFinite(pulseDamage) || pulseDamage <= 0.0F
					|| witherAmplifier < 0 || witherTicks <= 0) {
				throw new IllegalArgumentException("Invalid void scar");
			}
		}
	}

	private VoidScarManager() {
	}

	/** Registers a scar and its real impact-position collision presence. */
	static boolean create(ServerPlayer owner, Vec3 center, double radius, int durationTicks,
			float pulseDamage, int witherAmplifier, int witherTicks, boolean ancientMastery) {
		if (owner == null || center == null || !VoidBeamRules.canCreateScar(SCARS.size())) return false;
		ServerLevel level = (ServerLevel) owner.level();
		if (PowerProtection.isSafeZone(level, center)
				|| !LoadedChunks.contains(level, BlockPos.containing(center))) return false;
		long now = level.getServer().getTickCount();
		int boundedDuration = VoidBeamRules.scarDuration(durationTicks, ancientMastery);
		double boundedRadius = VoidBeamRules.scarRadius(radius);
		float boundedDamage = (float) VoidBeamRules.scarPulseDamage(pulseDamage, false);
		if (boundedDamage <= 0.0F) return false;
		MagicPresenceId presenceId = MagicPresenceId.random();
		long expiresAt = now + boundedDuration;
		MagicRuntime.global().registerPresence(new MagicPresence(presenceId, ACTION, owner.getUUID(),
				level.dimension().identifier().toString(),
				PresenceAnchor.fixed(center.x, center.y, center.z), boundedRadius, expiresAt));
		SCARS.add(new Scar(level.dimension(), owner.getUUID(), center, now, expiresAt,
				boundedRadius, boundedDamage, Math.max(0, Math.min(4, witherAmplifier)),
				Math.max(10, Math.min(80, witherTicks)), ancientMastery, presenceId));
		PowerFx.voidScarPulse(level, center, boundedRadius, 5, ancientMastery);
		return true;
	}

	/** Advances presentation, pulse gameplay, expiry, and unload cleanup. */
	static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Scar> iterator = SCARS.iterator();
		while (iterator.hasNext()) {
			Scar scar = iterator.next();
			ServerLevel level = server.getLevel(scar.dimension());
			ServerPlayer owner = server.getPlayerList().getPlayer(scar.owner());
			boolean loaded = level != null
					&& LoadedChunks.contains(level, BlockPos.containing(scar.center()));
			if (now >= scar.expiresAt() || owner == null || !owner.isAlive() || !loaded) {
				MagicRuntime.global().removePresence(scar.presenceId());
				if (loaded) PowerFx.voidScarCollapse(level, scar.center(), scar.radius());
				iterator.remove();
				continue;
			}
			int age = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, now - scar.createdAt()));
			if (VoidBeamRules.shouldRenderScar(age)) {
				PowerFx.voidScarPulse(level, scar.center(), scar.radius(), age, scar.ancientMastery());
			}
			if (VoidBeamRules.shouldPulseScar(age)) pulse(level, owner, scar);
		}
	}

	/** Applies one bounded owner-attributed pulse to nearest permitted occupants. */
	private static void pulse(ServerLevel level, ServerPlayer owner, Scar scar) {
		AABB bounds = AABB.ofSize(scar.center(), scar.radius() * 2.0,
				scar.radius() * 2.0, scar.radius() * 2.0);
		List<VoidBeamRules.RayCandidate<LivingEntity>> candidates = new ArrayList<>();
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds, 128,
				LivingEntity::isAlive)) {
			if (target == owner) continue;
			candidates.add(new VoidBeamRules.RayCandidate<>(target,
					target.position().distanceTo(scar.center())));
		}
		for (VoidBeamRules.RayCandidate<LivingEntity> candidate
				: VoidBeamRules.selectScarTargets(candidates, scar.radius())) {
			LivingEntity target = candidate.target();
			if (AmethystDampening.isDampened(target)
					|| !PowerProtection.mayHarm(owner, target)
					|| SpellFieldManager.isSanctuaryProtected(level, target)) continue;
			if (target.hurtServer(level, PowerDamage.source(owner), scar.pulseDamage())) {
				target.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, scar.witherTicks(),
						scar.witherAmplifier(), true, true));
			}
		}
	}

	/** Clears every charge owner's scars on respawn or disconnect. */
	static void clear(UUID owner) {
		if (owner == null) return;
		SCARS.removeIf(scar -> {
			if (!scar.owner().equals(owner)) return false;
			MagicRuntime.global().removePresence(scar.presenceId());
			return true;
		});
	}

	/** Clears all managed presence tokens during server shutdown. */
	static void clearAll() {
		for (Scar scar : SCARS) MagicRuntime.global().removePresence(scar.presenceId());
		SCARS.clear();
	}
}
