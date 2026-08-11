package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.ArtifactWeaponManager;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.BoundedRoundRobinQueue;
import com.powers.util.ChunkSpatialIndex;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded one-per-owner singularities and solar firmaments. */
public final class ArtifactFieldManager {
	private static final int DURATION_TICKS = 160;
	private static final double RADIUS = 24.0;
	private static final int MAX_FIELD_WORK_PER_TICK = 32;
	private static final Map<UUID, Field> FIELDS = new HashMap<>();
	private static final ChunkSpatialIndex<UUID, Field> INDEX = new ChunkSpatialIndex<>(16);
	private static final BoundedRoundRobinQueue<UUID> WORK = new BoundedRoundRobinQueue<>();

	private record Field(ResourceKey<Level> dimension, Vec3 center,
			ArtifactAlignment alignment, long expiresAt) {
	}

	private ArtifactFieldManager() {
	}

	public static boolean start(ServerPlayer owner, Vec3 center, ArtifactAlignment alignment) {
		boolean replacing = FIELDS.containsKey(owner.getUUID());
		if (!ArtifactDominionRules.mayStartField(FIELDS.size(), replacing)) return false;
		UUID ownerId = owner.getUUID();
		remove(ownerId);
		Field field = new Field(owner.level().dimension(), center,
				alignment, owner.level().getServer().getTickCount() + DURATION_TICKS);
		FIELDS.put(ownerId, field);
		INDEX.put(ownerId, dimensionId(field.dimension()), center.x, center.z, RADIUS, field);
		WORK.offer(ownerId);
		PowerFx.rune((ServerLevel) owner.level(), center, RADIUS,
				alignment == ArtifactAlignment.DARKNESS ? 0x21002E : 0xFFF2B2, 64, 0.0);
		return true;
	}

	public static void tick(MinecraftServer server) {
		for (UUID ownerId : WORK.pollBatch(MAX_FIELD_WORK_PER_TICK)) {
			Field field = FIELDS.get(ownerId);
			if (field == null) continue;
			ServerLevel level = server.getLevel(field.dimension());
			ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
			if (level == null || owner == null || !owner.isAlive()
					|| owner.level() != level
					|| !ArtifactWeaponManager.maySustain(owner, field.alignment())
					|| server.getTickCount() >= field.expiresAt()) {
				remove(ownerId);
				continue;
			}
			WORK.offer(ownerId);
			if (ArtifactFieldPulseRules.shouldPulse(server.getTickCount(), ownerId.hashCode())) {
				pulse(level, owner, field, server.getTickCount(), ownerId.hashCode());
			}
		}
	}

	private static void pulse(ServerLevel level, ServerPlayer owner, Field field, int tick, int ownerHash) {
		AABB bounds = AABB.ofSize(field.center(), RADIUS * 2.0, RADIUS * 2.0, RADIUS * 2.0);
		for (Projectile projectile : BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), bounds, 128,
				projectile -> projectile.distanceToSqr(field.center()) <= RADIUS * RADIUS
						&& !PowerProtection.isSafeZone(level, projectile.position()),
				Comparator.comparingDouble(projectile -> projectile.distanceToSqr(field.center())))) {
			if (field.alignment() == ArtifactAlignment.DARKNESS) projectile.discard();
			else {
				Vec3 push = projectile.position().subtract(field.center()).normalize().scale(1.6);
				projectile.setDeltaMovement(push);
				projectile.hurtMarked = true;
			}
		}
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds, 192,
				candidate -> candidate != owner && candidate.isAlive()
						&& candidate.distanceToSqr(field.center()) <= RADIUS * RADIUS,
				Comparator.comparingDouble(candidate -> candidate.distanceToSqr(field.center())))) {
			boolean targetDark = target.entityTags().contains(SkillSystem.DARKNESS_TAG);
			boolean hostile = field.alignment() == ArtifactAlignment.DARKNESS ? !targetDark : targetDark;
			if (!hostile && field.alignment() == ArtifactAlignment.LIGHT) {
				if (ArtifactFieldPulseRules.heavyPulse(tick, ownerHash)) {
					target.heal(owner.isAlliedTo(target) ? 8.0F : 3.0F);
				}
				continue;
			}
			ArtifactImpactRules.Decision decision = ArtifactImpactRules.decide(hostile,
					AmethystDampening.isDampened(target),
					PowerProtection.mayHarm(owner, target)
							&& !SpellFieldManager.isSanctuaryProtected(level, target),
					PowerProtection.mayForceMove(owner, target),
					SpellFieldManager.blocksForcedMovement(level, target, owner.getUUID()));
			if (!decision.damage() && !decision.move()) continue;
			Vec3 direction = field.alignment() == ArtifactAlignment.DARKNESS
					? field.center().subtract(target.position()) : target.position().subtract(field.center());
			if (decision.move() && direction.lengthSqr() > 1.0E-6) {
				Vec3 force = direction.normalize().scale(0.55);
				target.setDeltaMovement(target.getDeltaMovement().scale(0.55).add(force));
				target.hurtMarked = true;
			}
			if (decision.damage() && ArtifactFieldPulseRules.heavyPulse(tick, ownerHash)) {
				target.hurtServer(level, PowerDamage.source(owner),
						field.alignment() == ArtifactAlignment.DARKNESS ? 28.0F : 22.0F);
				target.addEffect(PowerStatusEffects.hidden(field.alignment() == ArtifactAlignment.DARKNESS
						? MobEffects.WITHER : MobEffects.WEAKNESS, 40, 2, false, true));
			}
		}
		if (tick % 5 == 0) {
			double phase = tick * 0.08;
			PowerFx.ring(level, field.center(), RADIUS,
					field.alignment() == ArtifactAlignment.DARKNESS ? 0x3A0B52 : 0xFFE89B,
					56, phase);
			PowerFx.spiral(level, field.center().add(0.0, -1.0, 0.0), RADIUS * 0.25, 8.0,
					field.alignment() == ArtifactAlignment.DARKNESS ? 0x6C2383 : 0xFFFFFF,
					28, -phase);
			PowerFx.burst(level, field.center(), field.alignment() == ArtifactAlignment.DARKNESS
					? com.powers.PowersParticles.ECLIPSE : com.powers.PowersParticles.GLYPH, 10, 4.0, 0.05);
		}
	}

	public static void forget(UUID ownerId) {
		remove(ownerId);
	}

	public static void forget(UUID ownerId, ArtifactAlignment alignment) {
		Field field = FIELDS.get(ownerId);
		if (field != null && field.alignment() == alignment) remove(ownerId);
	}

	/** True when one live aligned dominion contains the queried position. */
	public static boolean contains(ServerLevel level, Vec3 position, ArtifactAlignment alignment) {
		if (level == null || position == null || alignment == null) return false;
		long tick = level.getServer().getTickCount();
		return INDEX.nearby(dimensionId(level.dimension()), position.x, position.z, RADIUS)
				.stream().anyMatch(field -> field.expiresAt() > tick
				&& field.alignment() == alignment
				&& field.center().distanceToSqr(position) <= RADIUS * RADIUS);
	}

	public static void clear() {
		FIELDS.clear();
		INDEX.clear();
		WORK.clear();
	}

	public static int activeFieldCount() {
		return FIELDS.size();
	}

	public static ChunkSpatialIndex.Diagnostics spatialDiagnostics() {
		return INDEX.diagnostics();
	}

	private static void remove(UUID ownerId) {
		FIELDS.remove(ownerId);
		INDEX.remove(ownerId);
		WORK.remove(ownerId);
	}

	private static String dimensionId(ResourceKey<Level> dimension) {
		return dimension.identifier().toString();
	}
}
