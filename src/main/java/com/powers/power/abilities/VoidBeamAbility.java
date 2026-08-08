package com.powers.power.abilities;

import com.powers.PowersBlocks;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.Power;
import com.powers.power.PowerDamage;
import com.powers.power.state.MagicShieldManager;
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Charges and releases a finite penetrating abyssal ray with a living scar. */
public final class VoidBeamAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("void_beam");
	private static final double BASE_RANGE = 48.0;
	private static final float BASE_DAMAGE = 8.0F;
	private static final int BASE_WITHER_TICKS = 100;
	private static final double BASE_SCAR_RADIUS = 2.75;
	private static final int BASE_SCAR_TICKS = 80;
	private static final float BASE_SCAR_DAMAGE = 1.5F;
	private static final Map<UUID, Charge> CHARGES = new HashMap<>();

	/** Immutable interaction- and rank-scaled values captured when payment commits. */
	private record Charge(ResourceKey<Level> dimension, long startedAt, double range,
			float damage, int witherTicks, int witherAmplifier, double scarRadius,
			int scarTicks, float scarDamage, int penetrations, boolean empoweredImpact,
			boolean ancientMastery, boolean darkResurgence) {
		private Charge {
			if (dimension == null || startedAt < 0L || !Double.isFinite(range) || range <= 0.0
					|| !Float.isFinite(damage) || damage <= 0.0F || witherTicks <= 0
					|| witherAmplifier < 0 || !Double.isFinite(scarRadius) || scarRadius <= 0.0
					|| scarTicks <= 0 || !Float.isFinite(scarDamage) || scarDamage <= 0.0F
					|| penetrations <= 0 || penetrations > VoidBeamRules.MAX_PENETRATIONS) {
				throw new IllegalArgumentException("Invalid void charge");
			}
		}
	}

	public VoidBeamAbility() {
		super(POWER_ID, Component.translatable("ability.powers.void_beam"), 120, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (CHARGES.containsKey(player.getUUID())) return false;
		ScaledMagicValues scaled = scaling(player);
		Set<String> variants = scaled.unlockedVariants();
		boolean empowered = variants.contains("empowered_impact");
		boolean ancient = variants.contains("ancient_mastery");
		boolean resurgence = variants.contains("dark_resurgence");
		long now = player.level().getServer().getTickCount();
		Charge charge = new Charge(player.level().dimension(), now,
				Math.min(96.0, BASE_RANGE * scaled.rangeMultiplier()),
				(float) VoidBeamRules.releaseDamage(BASE_DAMAGE * scaled.potencyMultiplier(), 0),
				Math.max(20, (int) Math.round(BASE_WITHER_TICKS * scaled.durationMultiplier())),
				resurgence ? 2 : 1,
				VoidBeamRules.scarRadius(BASE_SCAR_RADIUS * scaled.rangeMultiplier()),
				Math.max(20, (int) Math.round(BASE_SCAR_TICKS * scaled.durationMultiplier())),
				(float) VoidBeamRules.scarPulseDamage(
						BASE_SCAR_DAMAGE * scaled.potencyMultiplier(), resurgence),
				VoidBeamRules.penetrationLimit(empowered, ancient), empowered, ancient, resurgence);
		CHARGES.put(player.getUUID(), charge);
		PowerFx.voidBeamCharge((ServerLevel) player.level(), player.getEyePosition(),
				VoidBeamRules.CHARGE_TICKS, ancient);
		return true;
	}

	/** Advances every charge and all aftermath scars from the common server tick. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, Charge>> iterator = CHARGES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Charge> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Charge charge = entry.getValue();
			if (!validOwner(player, charge)) {
				if (player != null && player.isAlive() && AmethystDampening.isDampened(player)) {
					PowerFx.voidBeamCountered((ServerLevel) player.level(), player.getEyePosition(),
							VoidBeamRules.Counterplay.AMETHYST);
				}
				iterator.remove();
				continue;
			}
			int remaining = VoidBeamRules.chargeRemaining(charge.startedAt(), now);
			if (remaining > 0) {
				if (remaining != VoidBeamRules.CHARGE_TICKS && (remaining & 1) == 0) {
					PowerFx.voidBeamCharge((ServerLevel) player.level(), player.getEyePosition(),
							remaining, charge.ancientMastery());
				}
				continue;
			}
			iterator.remove();
			release(player, charge);
		}
		VoidScarManager.tickAll(server);
	}

	/** Revalidates every fact that may legitimately interrupt a paid charge. */
	private static boolean validOwner(ServerPlayer player, Charge charge) {
		if (player == null || !player.isAlive() || !player.level().dimension().equals(charge.dimension())
				|| AmethystDampening.isDampened(player)) return false;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && power.id().equals(POWER_ID)) return true;
		}
		return false;
	}

	/** Resolves the final server aim, ordered targets, counters, and aftermath. */
	private static void release(ServerPlayer player, Charge charge) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.getEyePosition();
		Vec3 direction = player.getLookAngle();
		if (!finiteDirection(direction)) return;
		direction = direction.normalize();
		HitResult picked = player.pick(charge.range(), 0.0F, false);
		double blockDistance = picked.getType() == HitResult.Type.MISS
				? charge.range() : Math.min(charge.range(), origin.distanceTo(picked.getLocation()));
		Vec3 blockEnd = origin.add(direction.scale(blockDistance));
		Optional<SpellFieldManager.RayWardHit> ward = SpellFieldManager
				.firstHarmfulRayIntercept(level, player.getUUID(), origin, blockEnd);

		double terminalDistance = ward.map(SpellFieldManager.RayWardHit::distance)
				.orElse(blockDistance);
		Vec3 terminal = ward.map(SpellFieldManager.RayWardHit::point)
				.orElse(origin.add(direction.scale(terminalDistance)));
		VoidBeamRules.Counterplay counterplay = ward.map(SpellFieldManager.RayWardHit::counterplay)
				.orElseGet(() -> blockCounter(level, picked));
		if (counterplay == VoidBeamRules.Counterplay.NONE
				&& PowerProtection.isSafeZone(level, terminal)) {
			counterplay = VoidBeamRules.Counterplay.SAFE_ZONE;
		}

		List<VoidBeamRules.RayCandidate<LivingEntity>> candidates = rayCandidates(
				level, player, origin, direction, terminalDistance);
		List<VoidBeamRules.RayCandidate<LivingEntity>> selected = VoidBeamRules
				.selectPenetrations(candidates, terminalDistance, charge.penetrations());
		int successfulHits = 0;
		for (int index = 0; index < selected.size(); index++) {
			VoidBeamRules.RayCandidate<LivingEntity> candidate = selected.get(index);
			LivingEntity target = candidate.target();
			Vec3 impact = origin.add(direction.scale(candidate.distance()));
			VoidBeamRules.Counterplay targetCounter = targetCounter(level, player, target);
			float damage = (float) VoidBeamRules.releaseDamage(charge.damage(), index);
			if (targetCounter != VoidBeamRules.Counterplay.NONE) {
				if (targetCounter == VoidBeamRules.Counterplay.FORCEFIELD && damage > 0.0F) {
					target.hurtServer(level, PowerDamage.source(player), damage);
				}
				terminal = impact;
				counterplay = targetCounter;
				break;
			}
			if (damage <= 0.0F || !target.hurtServer(level, PowerDamage.source(player), damage)) {
				terminal = impact;
				counterplay = VoidBeamRules.Counterplay.FORCEFIELD;
				break;
			}
			target.addEffect(new MobEffectInstance(MobEffects.WITHER, charge.witherTicks(),
					charge.witherAmplifier(), true, false, true));
			PowerFx.voidBeamPenetration(level, impact, index, charge.darkResurgence());
			successfulHits++;
			if (successfulHits == charge.penetrations()) {
				terminal = impact;
				counterplay = VoidBeamRules.Counterplay.NONE;
				break;
			}
		}

		PowerFx.voidBeamRelease(level, origin, terminal,
				charge.empoweredImpact(), charge.ancientMastery());
		if (counterplay != VoidBeamRules.Counterplay.NONE) {
			PowerFx.voidBeamCountered(level, terminal, counterplay);
			return;
		}
		VoidScarManager.create(player, terminal, charge.scarRadius(), charge.scarTicks(),
				charge.scarDamage(), charge.witherAmplifier(),
				Math.max(20, charge.witherTicks() / 3), charge.ancientMastery());
	}

	/** Intersects living body volumes only inside the block/ward-bounded segment. */
	private static List<VoidBeamRules.RayCandidate<LivingEntity>> rayCandidates(
			ServerLevel level, ServerPlayer caster, Vec3 origin, Vec3 direction,
			double terminalDistance) {
		Vec3 end = origin.add(direction.scale(terminalDistance));
		AABB envelope = caster.getBoundingBox().expandTowards(direction.scale(terminalDistance)).inflate(1.0);
		List<VoidBeamRules.RayCandidate<LivingEntity>> result = new ArrayList<>();
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, envelope,
				entity -> entity.isAlive() && entity != caster && !entity.isSpectator())) {
			target.getBoundingBox().inflate(0.3).clip(origin, end).ifPresent(point ->
					result.add(new VoidBeamRules.RayCandidate<>(target, origin.distanceTo(point))));
		}
		return result;
	}

	/** Resolves entity protections before any direct damage or Wither is applied. */
	private static VoidBeamRules.Counterplay targetCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target) {
		if (AmethystDampening.isDampened(target)) return VoidBeamRules.Counterplay.AMETHYST;
		if (!PowerProtection.mayHarm(caster, target)) return VoidBeamRules.Counterplay.SAFE_ZONE;
		if (SpellFieldManager.isSanctuaryProtected(level, target)) {
			return VoidBeamRules.Counterplay.SANCTUARY;
		}
		if (target instanceof ServerPlayer player && MagicShieldManager.global()
				.active(player.getUUID(), level.getServer().getTickCount())) {
			return VoidBeamRules.Counterplay.FORCEFIELD;
		}
		return VoidBeamRules.Counterplay.NONE;
	}

	/** Gives opposed realm matter and tagged amethyst hard terminal behavior. */
	private static VoidBeamRules.Counterplay blockCounter(ServerLevel level, HitResult hit) {
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
			return VoidBeamRules.Counterplay.NONE;
		}
		BlockState state = level.getBlockState(blockHit.getBlockPos());
		if (state.is(PowersBlocks.PURE_LIGHT)) return VoidBeamRules.Counterplay.LIGHT;
		if (state.is(AmethystDampening.AMETHYST_BLOCKS)) return VoidBeamRules.Counterplay.AMETHYST;
		return VoidBeamRules.Counterplay.NONE;
	}

	/** Rejects malformed look vectors before normalization can create NaN state. */
	private static boolean finiteDirection(Vec3 direction) {
		return direction != null && Double.isFinite(direction.x) && Double.isFinite(direction.y)
				&& Double.isFinite(direction.z) && direction.lengthSqr() > 1.0E-8;
	}

	/** Clears one owner's charge and every owned scar at a lifecycle boundary. */
	public static void clear(UUID owner) {
		CHARGES.remove(owner);
		VoidScarManager.clear(owner);
	}

	/** Clears all runtime-only Void Beam state during server shutdown. */
	public static void clearAll() {
		CHARGES.clear();
		VoidScarManager.clearAll();
	}
}
