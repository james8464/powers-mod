package com.powers.spell;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.power.abilities.VoidBeamRules;
import com.powers.power.state.PowerEntityState;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Temporary, visible counterplay zones created by ritual spells. */
public final class SpellFieldManager {
	private static final int MAX_FIELDS = 256;
	private static final List<Field> FIELDS = new ArrayList<>();

	/** First hostile ward surface touched by a finite harmful ray. */
	public record RayWardHit(Vec3 point, double distance, VoidBeamRules.Counterplay counterplay) {
	}

	private record Field(SpellFieldKind kind, ResourceKey<Level> dimension, Vec3 center,
			UUID owner, long expiresAt, double radius, int potencyTier) {
		private Field {
			if (!Double.isFinite(radius) || radius <= 0 || potencyTier < 0) {
				throw new IllegalArgumentException("Field values must be finite and positive");
			}
		}
	}

	private SpellFieldManager() {
	}

	public static void add(SpellFieldKind kind, ServerPlayer owner, int durationTicks,
			double radius, int potencyTier) {
		// A recast replaces the owner's earlier copy instead of accumulating
		// overlapping fields; the global cap protects large servers and old saves.
		FIELDS.removeIf(field -> field.owner().equals(owner.getUUID()) && field.kind() == kind);
		if (FIELDS.size() >= MAX_FIELDS) FIELDS.removeFirst();
		FIELDS.add(new Field(kind, owner.level().dimension(), owner.position(), owner.getUUID(),
				owner.level().getGameTime() + durationTicks, radius, potencyTier));
	}

	public static boolean blocksTravel(ServerPlayer subject, ServerLevel destinationLevel, Vec3 destination) {
		for (Field field : FIELDS) {
			long gameTime = field.dimension().equals(destinationLevel.dimension())
					? destinationLevel.getGameTime() : subject.level().getGameTime();
			if (field.expiresAt() <= gameTime) continue;
			if (field.owner().equals(subject.getUUID())) continue;
			if (field.kind() != SpellFieldKind.ANTI_PORTAL && field.kind() != SpellFieldKind.INFERNAL_SEAL) continue;
			if (field.dimension().equals(subject.level().dimension())
					&& within(field, subject.position())) return true;
			if (field.dimension().equals(destinationLevel.dimension())
					&& within(field, destination)) return true;
		}
		return false;
	}

	public static boolean isSanctuaryProtected(ServerLevel level, LivingEntity entity) {
		for (Field field : FIELDS) {
			if (field.expiresAt() > level.getGameTime()
					&& field.kind() == SpellFieldKind.SANCTUARY && field.dimension().equals(level.dimension())
					&& within(field, entity.position())) return true;
		}
		return false;
	}

	/** Returns whether another caster's Sanctuary or Kinetic Ward grounds forced movement here. */
	public static boolean blocksForcedMovement(ServerLevel level, LivingEntity entity, UUID caster) {
		if (level == null || entity == null || caster == null) return false;
		for (Field field : FIELDS) {
			if (field.expiresAt() <= level.getGameTime() || field.owner().equals(caster)
					|| !field.dimension().equals(level.dimension())) continue;
			if ((field.kind() == SpellFieldKind.SANCTUARY || field.kind() == SpellFieldKind.KINETIC_WARD)
					&& within(field, entity.position())) return true;
		}
		return false;
	}

	/**
	 * Finds the nearest non-owner Sanctuary or Kinetic Ward crossed by a beam.
	 * Expired fields are ignored immediately even if the periodic tick has not
	 * removed them yet, so a boundary can never outlive its authored duration.
	 */
	public static Optional<RayWardHit> firstHarmfulRayIntercept(ServerLevel level,
			UUID caster, Vec3 start, Vec3 end) {
		if (level == null || caster == null || start == null || end == null) return Optional.empty();
		double segmentLength = start.distanceTo(end);
		if (!Double.isFinite(segmentLength) || segmentLength <= 1.0E-6) return Optional.empty();
		List<VoidBeamRules.RayIntercept> candidates = new ArrayList<>();
		for (Field field : FIELDS) {
			if (field.expiresAt() <= level.getGameTime() || field.owner().equals(caster)
					|| !field.dimension().equals(level.dimension())) continue;
			VoidBeamRules.Counterplay counterplay = switch (field.kind()) {
				case KINETIC_WARD -> VoidBeamRules.Counterplay.KINETIC_WARD;
				case SANCTUARY -> VoidBeamRules.Counterplay.SANCTUARY;
				case ANTI_PORTAL, INFERNAL_SEAL -> VoidBeamRules.Counterplay.NONE;
			};
			if (counterplay == VoidBeamRules.Counterplay.NONE) continue;
			Vec3 center = field.center().add(0.0, 1.0, 0.0);
			double distance = VoidBeamRules.segmentSphereEntry(start.x, start.y, start.z,
					end.x, end.y, end.z, center.x, center.y, center.z, field.radius());
			candidates.add(new VoidBeamRules.RayIntercept(counterplay, distance));
		}
		return VoidBeamRules.nearestIntercept(candidates, segmentLength).map(hit -> {
			Vec3 point = start.add(end.subtract(start).scale(hit.distance() / segmentLength));
			return new RayWardHit(point, hit.distance(), hit.counterplay());
		});
	}

	public static boolean dispelNearest(ServerPlayer caster, double range) {
		Field nearest = null;
		double distance = range * range;
		for (Field field : FIELDS) {
			if (field.expiresAt() <= caster.level().getGameTime()
					|| !field.dimension().equals(caster.level().dimension())) continue;
			double candidate = field.center().distanceToSqr(caster.position());
			if (candidate <= distance) {
				distance = candidate;
				nearest = field;
			}
		}
		if (nearest == null) return false;
		FIELDS.remove(nearest);
		PowerFx.cancelled((ServerLevel) caster.level(), nearest.center().add(0, 0.5, 0), 0x7455A8);
		return true;
	}

	public static void tick(MinecraftServer server) {
		Iterator<Field> iterator = FIELDS.iterator();
		while (iterator.hasNext()) {
			Field field = iterator.next();
			ServerLevel level = server.getLevel(field.dimension());
			if (level == null || level.getGameTime() >= field.expiresAt()) {
				iterator.remove();
				continue;
			}
			if (server.getTickCount() % 5 != 0) continue;
			int color = switch (field.kind()) {
				case ANTI_PORTAL -> 0x3D2B73;
				case KINETIC_WARD -> 0x70D6FF;
				case SANCTUARY -> 0x8CFF98;
				case INFERNAL_SEAL -> 0xC62828;
			};
			PowerFx.ring(level, field.center().add(0, 0.08, 0), field.radius(), color, 18,
					server.getTickCount() * 0.035);
			PowerFx.rune(level, field.center().add(0, 0.1, 0), field.radius() * 0.55,
					color, 12, -server.getTickCount() * 0.025);
			if (server.getTickCount() % 40 == 0) {
				PowerFx.sound(level, field.center(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.35f,
						0.75f + field.potencyTier() * 0.08f);
			}
			applyField(level, field);
		}
	}

	private static void applyField(ServerLevel level, Field field) {
		AABB area = AABB.ofSize(field.center(), field.radius() * 2, 5, field.radius() * 2);
		if (field.kind() == SpellFieldKind.KINETIC_WARD) {
			for (Projectile projectile : BoundedEntityCandidates.ofClass(level, Projectile.class,
					area, 128, Projectile::isAlive)) {
				if (projectile.getOwner() != null && projectile.getOwner().getUUID().equals(field.owner())) continue;
				// One projectile may cross several ticks of the ring, but may only
				// reverse once; this prevents jitter and reflection ping-pong.
				if (!PowerEntityState.tryReflect(projectile, 1)) continue;
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.65));
				ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner());
				if (owner != null) projectile.setOwner(owner);
				PowerFx.burst(level, projectile.position(), ParticleTypes.ELECTRIC_SPARK, 2, 0.1, 0.03);
			}
		}
		for (LivingEntity entity : BoundedEntityCandidates.living(level, area, 128,
				LivingEntity::isAlive)) {
			if (!within(field, entity.position())) continue;
			switch (field.kind()) {
				case SANCTUARY -> {
					entity.clearFire();
					entity.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION, 30,
							Math.min(2, field.potencyTier()), true, true));
				}
				case KINETIC_WARD -> entity.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 30,
						Math.min(1, field.potencyTier()), true, true));
				case INFERNAL_SEAL -> {
					if (!entity.getUUID().equals(field.owner())) {
						entity.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, 30,
								Math.min(3, 1 + field.potencyTier()), true, true));
					}
				}
				case ANTI_PORTAL -> { }
			}
		}
	}

	private static boolean within(Field field, Vec3 position) {
		return field.center().distanceToSqr(position) <= field.radius() * field.radius();
	}

	public static void clearAll() {
		FIELDS.clear();
	}
}
