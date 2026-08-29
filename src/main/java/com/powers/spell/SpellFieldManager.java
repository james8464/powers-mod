package com.powers.spell;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.power.abilities.VoidBeamRules;
import com.powers.power.state.PowerEntityState;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.time.TemporalClocks;
import com.powers.time.TemporalSubsystem;
import com.powers.time.WorldTick;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.BoundedRoundRobinQueue;
import com.powers.util.ChunkSpatialIndex;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Temporary, visible counterplay zones created by ritual spells. */
public final class SpellFieldManager {
	private static final int MAX_FIELDS = 256;
	private static final int MAX_FIELD_WORK_PER_TICK = 32;
	private static final double MAX_FIELD_RADIUS = 128.0;
	private static final Map<FieldKey, Field> FIELDS = new LinkedHashMap<>();
	private static final ChunkSpatialIndex<FieldKey, Field> INDEX = new ChunkSpatialIndex<>(16);
	private static final BoundedRoundRobinQueue<FieldKey> WORK = new BoundedRoundRobinQueue<>();

	/** First hostile ward surface touched by a finite harmful ray. */
	public record RayWardHit(Vec3 point, double distance, VoidBeamRules.Counterplay counterplay) {
	}

	/** Immutable server snapshot captured during Dispel inspection. */
	public record DispelTarget(UUID owner, SpellFieldKind kind, String dimension,
			Vec3 center, long expiresAt) {
		public Component displayName() {
			return switch (kind) {
				case ANTI_PORTAL -> Component.translatable("spell.powers.field.anti_portal");
				case KINETIC_WARD -> Component.translatable("spell.powers.field.kinetic_ward");
				case SANCTUARY -> Component.translatable("spell.powers.field.sanctuary");
				case INFERNAL_SEAL -> Component.translatable("spell.powers.field.infernal_seal");
			};
		}
	}

	private record FieldKey(UUID owner, SpellFieldKind kind) {
	}

	private static final class Field {
		private final SpellFieldKind kind;
		private final ResourceKey<Level> dimension;
		private final Vec3 center;
		private final UUID owner;
		private final long expiresAt;
		private final double radius;
		private final int potencyTier;
		private final MagicPresenceHandle presence;
		private WorldTick nextPulseAt;

		private Field(SpellFieldKind kind, ResourceKey<Level> dimension, Vec3 center,
				UUID owner, long expiresAt, double radius, int potencyTier, WorldTick nextPulseAt,
				MagicPresenceHandle presence) {
			if (!Double.isFinite(radius) || radius <= 0.0 || radius > MAX_FIELD_RADIUS || potencyTier < 0) {
				throw new IllegalArgumentException("Field values must be finite and positive");
			}
			this.kind = kind;
			this.dimension = dimension;
			this.center = center;
			this.owner = owner;
			this.expiresAt = expiresAt;
			this.radius = radius;
			this.potencyTier = potencyTier;
			this.nextPulseAt = nextPulseAt;
			this.presence = presence;
		}

		private SpellFieldKind kind() { return kind; }
		private ResourceKey<Level> dimension() { return dimension; }
		private Vec3 center() { return center; }
		private UUID owner() { return owner; }
		private long expiresAt() { return expiresAt; }
		private double radius() { return radius; }
		private int potencyTier() { return potencyTier; }
	}

	private SpellFieldManager() {
	}

	public static void add(SpellFieldKind kind, ServerPlayer owner, int durationTicks,
			double radius, int potencyTier) {
		// A recast replaces the owner's earlier copy instead of accumulating
		// overlapping fields; the global cap protects large servers and old saves.
		FieldKey key = new FieldKey(owner.getUUID(), kind);
		remove(key);
		if (FIELDS.size() >= MAX_FIELDS) remove(FIELDS.keySet().iterator().next());
		double boundedRadius = Math.clamp(radius, 0.25, MAX_FIELD_RADIUS);
		WorldTick worldTick = TemporalClocks.world((ServerLevel) owner.level());
		long expiresAt = worldTick.plus(Math.max(1, durationTicks)).value();
		MagicPresenceHandle presence = PhysicalMagicPresences.registerFixed(
				new MagicActionId(actionId(kind)), owner.getUUID(), (ServerLevel) owner.level(),
				owner.position(), boundedRadius, expiresAt, MagicPresenceHandle.Kind.FIELD);
		Field field = new Field(kind, owner.level().dimension(), owner.position(), owner.getUUID(),
				expiresAt, boundedRadius, Math.max(0, potencyTier), worldTick, presence);
		FIELDS.put(key, field);
		INDEX.put(key, dimensionId(field.dimension()), field.center().x, field.center().z,
				field.radius(), field);
		WORK.offer(key);
	}

	public static boolean blocksTravel(LivingEntity subject, ServerLevel destinationLevel, Vec3 destination) {
		return blocksTravelAt(subject, (ServerLevel) subject.level(), subject.position())
				|| blocksTravelAt(subject, destinationLevel, destination);
	}

	public static boolean isSanctuaryProtected(ServerLevel level, LivingEntity entity) {
		for (Field field : nearby(level, entity.position(), 0.0)) {
			if (field.expiresAt() > level.getGameTime()
					&& field.kind() == SpellFieldKind.SANCTUARY
					&& within(field, entity.position())) return true;
		}
		return false;
	}

	/** Returns whether another caster's Sanctuary or Kinetic Ward grounds forced movement here. */
	public static boolean blocksForcedMovement(ServerLevel level, LivingEntity entity, UUID caster) {
		if (level == null || entity == null || caster == null) return false;
		for (Field field : nearby(level, entity.position(), 0.0)) {
			if (field.expiresAt() <= level.getGameTime() || field.owner().equals(caster)
					) continue;
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
		for (Field field : rayCandidates(level, start, end)) {
			if (field.expiresAt() <= level.getGameTime() || field.owner().equals(caster)
					) continue;
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

	public static Optional<DispelTarget> nearestDispelTarget(ServerPlayer caster, double range) {
		Field nearest = null;
		double distance = range * range;
		for (Field field : nearby((ServerLevel) caster.level(), caster.position(), Math.min(range, 256.0))) {
			if (field.expiresAt() <= caster.level().getGameTime()) continue;
			if (!com.powers.protection.PowerProtection.mayRitual(caster,
					(ServerLevel) caster.level(), BlockPos.containing(field.center()))) continue;
			double candidate = field.center().distanceToSqr(caster.position());
			if (candidate <= distance) {
				distance = candidate;
				nearest = field;
			}
		}
		return nearest == null ? Optional.empty() : Optional.of(new DispelTarget(nearest.owner(), nearest.kind(),
				dimensionId(nearest.dimension()), nearest.center(), nearest.expiresAt()));
	}

	public static boolean dispel(ServerPlayer caster, double range, DispelTarget target) {
		if (target == null) return false;
		FieldKey key = new FieldKey(target.owner(), target.kind());
		Field live = FIELDS.get(key);
		boolean valid = SpellTargetRules.dispelFieldRemainsValid(live != null
				&& live.expiresAt() == target.expiresAt() && live.center().equals(target.center()),
				dimensionId(caster.level().dimension()).equals(target.dimension()), caster.level().getGameTime(),
				target.expiresAt(), target.center().distanceToSqr(caster.position()), range);
		valid = valid && com.powers.protection.PowerProtection.mayRitual(caster,
				(ServerLevel) caster.level(), BlockPos.containing(target.center()));
		if (!valid) return false;
		remove(key);
		PowerFx.cancelled((ServerLevel) caster.level(), target.center().add(0, 0.5, 0), 0x7455A8);
		return true;
	}

	public static void tick(MinecraftServer server) {
		if (!TemporalClocks.worldAdvances(server, TemporalSubsystem.SPELL_FIELDS)) return;
		for (FieldKey key : WORK.pollBatch(MAX_FIELD_WORK_PER_TICK)) {
			Field field = FIELDS.get(key);
			if (field == null) continue;
			ServerLevel level = server.getLevel(field.dimension());
			if (level == null || level.getGameTime() >= field.expiresAt()) {
				remove(key);
				continue;
			}
			WORK.offer(key);
			WorldTick worldTick = TemporalClocks.world(level);
			if (!SpellFieldTiming.ready(worldTick, field.nextPulseAt)) continue;
			field.nextPulseAt = SpellFieldTiming.nextPulseAt(worldTick);
			int color = switch (field.kind()) {
				case ANTI_PORTAL -> 0x3D2B73;
				case KINETIC_WARD -> 0x70D6FF;
				case SANCTUARY -> 0x8CFF98;
				case INFERNAL_SEAL -> 0xC62828;
			};
			PowerFx.ring(level, field.center().add(0, 0.08, 0), field.radius(), color, 18,
					worldTick.value() * 0.035);
			PowerFx.rune(level, field.center().add(0, 0.1, 0), field.radius() * 0.55,
					color, 12, -worldTick.value() * 0.025);
			if (worldTick.value() % 40L == 0L) {
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

	private static boolean blocksTravelAt(LivingEntity subject, ServerLevel level, Vec3 position) {
		for (Field field : nearby(level, position, 0.0)) {
			if (field.expiresAt() <= level.getGameTime() || field.owner().equals(subject.getUUID())) continue;
			if ((field.kind() == SpellFieldKind.ANTI_PORTAL || field.kind() == SpellFieldKind.INFERNAL_SEAL)
					&& within(field, position)) return true;
		}
		return false;
	}

	private static List<Field> nearby(ServerLevel level, Vec3 center, double queryRadius) {
		return INDEX.nearby(dimensionId(level.dimension()), center.x, center.z,
				Math.clamp(queryRadius, 0.0, 256.0));
	}

	private static List<Field> rayCandidates(ServerLevel level, Vec3 start, Vec3 end) {
		double horizontalLength = Math.hypot(end.x - start.x, end.z - start.z);
		int samples = Math.max(1, (int) Math.ceil(horizontalLength / 128.0));
		Set<Field> result = new LinkedHashSet<>();
		for (int sample = 0; sample <= samples; sample++) {
			double progress = sample / (double) samples;
			Vec3 point = start.add(end.subtract(start).scale(progress));
			result.addAll(nearby(level, point, Math.min(128.0, horizontalLength / (2.0 * samples)
					+ MAX_FIELD_RADIUS)));
		}
		return List.copyOf(result);
	}

	private static String dimensionId(ResourceKey<Level> dimension) {
		return dimension.identifier().toString();
	}

	private static void remove(FieldKey key) {
		Field removed = FIELDS.remove(key);
		if (removed != null) PhysicalMagicPresences.remove(removed.presence);
		INDEX.remove(key);
		WORK.remove(key);
	}

	private static String actionId(SpellFieldKind kind) {
		return switch (kind) {
			case ANTI_PORTAL -> "dimensional_anchor";
			case KINETIC_WARD -> "forcefield";
			case SANCTUARY -> "hearth_sanctuary";
			case INFERNAL_SEAL -> "dispel";
		};
	}

	/** Active count exposed to the administrative diagnostics command. */
	public static int activeFieldCount() {
		return FIELDS.size();
	}

	/** Exact ownership probe for diagnostics and live acceptance tests. */
	public static boolean hasField(UUID owner, SpellFieldKind kind) {
		return owner != null && kind != null && FIELDS.containsKey(new FieldKey(owner, kind));
	}

	/** Maximum recurring field evaluations performed in one server tick. */
	public static int maxFieldWorkPerTick() {
		return MAX_FIELD_WORK_PER_TICK;
	}

	public static ChunkSpatialIndex.Diagnostics spatialDiagnostics() {
		return INDEX.diagnostics();
	}

	/** Per-dimension spatial work counters for bounded operator diagnostics. */
	public static java.util.Map<String, ChunkSpatialIndex.Diagnostics> spatialDiagnosticsByDimension() {
		return INDEX.diagnosticsByDimension();
	}

	public static void clearAll() {
		FIELDS.values().forEach(field -> PhysicalMagicPresences.remove(field.presence));
		FIELDS.clear();
		INDEX.clear();
		WORK.clear();
	}
}
