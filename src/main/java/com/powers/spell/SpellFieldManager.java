package com.powers.spell;

import com.powers.fx.PowerFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Temporary, visible counterplay zones created by ritual spells. */
public final class SpellFieldManager {
	private static final double RADIUS = 7.0;
	private static final List<Field> FIELDS = new ArrayList<>();

	private record Field(SpellFieldKind kind, ResourceKey<Level> dimension, Vec3 center,
			UUID owner, long expiresAt) {
	}

	private SpellFieldManager() {
	}

	public static void add(SpellFieldKind kind, ServerPlayer owner, int durationTicks) {
		FIELDS.add(new Field(kind, owner.level().dimension(), owner.position(), owner.getUUID(),
				owner.level().getGameTime() + durationTicks));
	}

	public static boolean blocksTravel(ServerPlayer subject, ServerLevel destinationLevel, Vec3 destination) {
		for (Field field : FIELDS) {
			if (field.owner().equals(subject.getUUID())) continue;
			if (field.kind() != SpellFieldKind.ANTI_PORTAL && field.kind() != SpellFieldKind.INFERNAL_SEAL) continue;
			if (field.dimension().equals(subject.level().dimension())
					&& field.center().distanceToSqr(subject.position()) <= RADIUS * RADIUS) return true;
			if (field.dimension().equals(destinationLevel.dimension())
					&& field.center().distanceToSqr(destination) <= RADIUS * RADIUS) return true;
		}
		return false;
	}

	public static boolean isSanctuaryProtected(ServerLevel level, LivingEntity entity) {
		for (Field field : FIELDS) {
			if (field.kind() == SpellFieldKind.SANCTUARY && field.dimension().equals(level.dimension())
					&& field.center().distanceToSqr(entity.position()) <= RADIUS * RADIUS) return true;
		}
		return false;
	}

	public static boolean dispelNearest(ServerPlayer caster, double range) {
		Field nearest = null;
		double distance = range * range;
		for (Field field : FIELDS) {
			if (!field.dimension().equals(caster.level().dimension())) continue;
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
			PowerFx.ring(level, field.center().add(0, 0.08, 0), RADIUS, color, 18,
					server.getTickCount() * 0.035);
			applyField(level, field);
		}
	}

	private static void applyField(ServerLevel level, Field field) {
		AABB area = AABB.ofSize(field.center(), RADIUS * 2, 5, RADIUS * 2);
		if (field.kind() == SpellFieldKind.KINETIC_WARD) {
			for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area, Projectile::isAlive)) {
				if (projectile.getOwner() != null && projectile.getOwner().getUUID().equals(field.owner())) continue;
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.65));
				PowerFx.burst(level, projectile.position(), ParticleTypes.ELECTRIC_SPARK, 2, 0.1, 0.03);
			}
		}
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
			if (field.center().distanceToSqr(entity.position()) > RADIUS * RADIUS) continue;
			switch (field.kind()) {
				case SANCTUARY -> {
					entity.clearFire();
					entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, true, false));
				}
				case KINETIC_WARD -> entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, true, false));
				case INFERNAL_SEAL -> {
					if (!entity.getUUID().equals(field.owner())) {
						entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, true, false));
					}
				}
				case ANTI_PORTAL -> { }
			}
		}
	}

	public static void clearAll() {
		FIELDS.clear();
	}
}
