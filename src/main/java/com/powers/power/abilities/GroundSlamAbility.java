package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.GroundSlamFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.Power;
import com.powers.power.state.EntityFreezeController;
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns Faultbound Verdict from its warning fault clock through its last beat. */
public final class GroundSlamAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("ground_slam");
	private static final Identifier ELEMENTAL_POWER_ID = PowersMod.id("elemental_blast");
	private static final double BASE_RADIUS = 5.0;
	private static final float BASE_DAMAGE = 6.0F;
	private static final int BASE_MANTLE_TICKS = 100;
	private static final int MAX_ACTIVE_RITES = 32;
	private static final double TRACKING_LEASH = 6.0;
	private static final double TRACKING_STEP = 0.75;
	private static final Map<UUID, FaultboundVerdict> ACTIVE = new LinkedHashMap<>();

	public GroundSlamAbility() {
		super(POWER_ID, Component.translatable("ability.powers.ground_slam"), 200, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!player.isAlive()) return false;
		if (ACTIVE.containsKey(player.getUUID())) {
			PowerMessages.send(player, "ability.powers.ground_slam.active", 3);
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		if (ACTIVE.size() >= MAX_ACTIVE_RITES) {
			GroundSlamFx.counter(level, player.position(),
					GroundSlamRules.Counterplay.RESISTED);
			PowerMessages.send(player, "ability.powers.ground_slam.blocked", 4);
			return false;
		}

		GroundSlamImpactResolver.StrikeSite site =
				GroundSlamImpactResolver.initialSite(level, player.position());
		if (site == null || !GroundSlamRules.impactAllowed(site.counterplay())) {
			GroundSlamRules.Counterplay counter = site == null
					? GroundSlamRules.Counterplay.UNLOADED : site.counterplay();
			GroundSlamFx.counter(level, site == null ? player.position() : site.point(), counter);
			PowerMessages.send(player, "ability.powers.ground_slam.blocked", 4);
			return false;
		}

		ScaledMagicValues profile = scaling(player);
		Set<String> variants = profile.unlockedVariants();
		boolean empoweredImpact = variants.contains("empowered_impact");
		boolean secondStep = variants.contains("second_step");
		boolean trueSight = variants.contains("true_sight");
		boolean reflectiveWard = variants.contains("reflective_ward");
		boolean soulEcho = variants.contains("soul_echo");
		boolean afterimage = variants.contains("afterimage");
		boolean ancientMastery = variants.contains("ancient_mastery");
		long now = level.getServer().getTickCount();
		long expiresAt = now + GroundSlamRules.finishAge(soulEcho, ancientMastery);
		double radius = Math.max(4.0,
				Math.min(10.0, BASE_RADIUS * profile.rangeMultiplier()));
		int mantleDuration = Math.max(20,
				(int) Math.round(BASE_MANTLE_TICKS * profile.durationMultiplier()));
		FaultboundVerdict rite = new FaultboundVerdict(player.getUUID(), level.dimension(),
				now, expiresAt, site.point(), player.getLookAngle(), radius,
				(float) (BASE_DAMAGE * profile.potencyMultiplier()), mantleDuration,
				empoweredImpact, secondStep, trueSight, reflectiveWard,
				soulEcho, afterimage, ancientMastery);
		ACTIVE.put(rite.owner, rite);
		GroundSlamFx.open(level, rite.center, rite.baseRadius,
				rite.secondStep, rite.soulEcho, rite.ancientMastery);
		PowerMessages.send(player, "ability.powers.ground_slam.cast", 4);
		return true;
	}

	/** Advances every owned rite exactly once from the common server tick. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, FaultboundVerdict>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			FaultboundVerdict rite = iterator.next().getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(rite.owner);
			ServerLevel level = server.getLevel(rite.dimension);
			boolean sameDimension = owner != null
					&& owner.level().dimension().equals(rite.dimension);
			boolean dampened = owner != null && AmethystDampening.isDampened(owner);
			boolean frozen = owner != null && EntityFreezeController.isFrozen(owner);
			boolean ownsPower = owner != null && ownsSource(owner);
			boolean continues = GroundSlamRules.riteContinues(owner != null, sameDimension,
					owner != null && owner.isAlive() && !owner.isRemoved(), dampened, frozen,
					ownsPower, now, rite.expiresAt);
			if (level == null || !continues) {
				boolean completed = level != null && owner != null && sameDimension
						&& owner.isAlive() && !dampened && !frozen && ownsPower
						&& now >= rite.expiresAt;
				if (level != null) {
					GroundSlamFx.close(level, rite.center, rite.baseRadius,
							completed, dampened, frozen);
				}
				if (completed && owner != null) {
					PowerMessages.send(owner, "ability.powers.ground_slam.complete", 3);
				} else if (owner != null && sameDimension && owner.isAlive()) {
					PowerMessages.send(owner, "ability.powers.ground_slam.interrupted", 3);
				}
				iterator.remove();
				continue;
			}

			int age = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, now - rite.startedAt));
			if (!rite.primaryResolved) updateTrackedCenter(level, owner, rite, age);
			if (age < GroundSlamRules.beatAge(GroundSlamRules.Beat.PRIMARY)
					&& (age & 1) == 0) {
				GroundSlamFx.omen(level, rite.center, rite.baseRadius,
						age, rite.afterimage, rite.ancientMastery);
			}
			telegraphNext(level, rite, age);

			if (!rite.primaryResolved
					&& age >= GroundSlamRules.beatAge(GroundSlamRules.Beat.PRIMARY)) {
				rite.primaryResolved = true;
				GroundSlamImpactResolver.ImpactResult result = resolveBeat(
						level, owner, rite, rite.center, GroundSlamRules.Beat.PRIMARY);
				if (!GroundSlamRules.impactAllowed(result.counterplay())) {
					GroundSlamFx.close(level, rite.center, rite.baseRadius,
							false, result.counterplay() == GroundSlamRules.Counterplay.AMETHYST, false);
					PowerMessages.send(owner, "ability.powers.ground_slam.interrupted", 3);
					iterator.remove();
					continue;
				}
				applyRankMantles(level, owner, rite);
			}

			if (rite.soulEcho && !rite.echoResolved
					&& age >= GroundSlamRules.beatAge(GroundSlamRules.Beat.SOUL_ECHO)) {
				rite.echoResolved = true;
				Vec3 echoCenter = GroundSlamRules.echoCenter(
						rite.center, rite.lookDirection, rite.baseRadius);
				resolveBeat(level, owner, rite, echoCenter, GroundSlamRules.Beat.SOUL_ECHO);
			}

			if (rite.ancientMastery && !rite.crownResolved
					&& age >= GroundSlamRules.beatAge(GroundSlamRules.Beat.CROWN)) {
				rite.crownResolved = true;
				resolveBeat(level, owner, rite, rite.center, GroundSlamRules.Beat.CROWN);
			}
		}
	}

	/** Removes one rite during respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		FaultboundVerdict rite = ACTIVE.remove(owner);
		if (rite == null || server == null) return;
		ServerLevel level = server.getLevel(rite.dimension);
		if (level != null) {
			GroundSlamFx.close(level, rite.center, rite.baseRadius,
					false, false, false);
		}
	}

	/** Discards every rite before server world state is released. */
	public static void clearAll(MinecraftServer server) {
		if (server != null) {
			for (FaultboundVerdict rite : new ArrayList<>(ACTIVE.values())) {
				ServerLevel level = server.getLevel(rite.dimension);
				if (level != null) {
					GroundSlamFx.close(level, rite.center, rite.baseRadius,
							false, false, false);
				}
			}
		}
		ACTIVE.clear();
	}

	/** Resolves one beat through the shared surface and body pipeline. */
	private static GroundSlamImpactResolver.ImpactResult resolveBeat(ServerLevel level,
			ServerPlayer owner, FaultboundVerdict rite, Vec3 requested,
			GroundSlamRules.Beat beat) {
		return GroundSlamImpactResolver.resolve(level, owner, rite, requested, beat);
	}

	/** Motion carries only the warning clock, never an already released quake. */
	private static void updateTrackedCenter(ServerLevel level, ServerPlayer owner,
			FaultboundVerdict rite, int age) {
		Vec3 previous = rite.center;
		Vec3 desired = new Vec3(owner.getX(), previous.y, owner.getZ());
		rite.center = GroundSlamRules.trackedCenter(previous, desired, rite.origin,
				rite.secondStep, TRACKING_LEASH, TRACKING_STEP);
		if ((age & 1) == 0) GroundSlamFx.tracking(level, previous, rite.center, age);
	}

	/** Telegraphs the next enabled unresolved beat for its final three ticks. */
	private static void telegraphNext(ServerLevel level, FaultboundVerdict rite, int age) {
		GroundSlamRules.Beat next = null;
		Vec3 requested = rite.center;
		if (!rite.primaryResolved) {
			next = GroundSlamRules.Beat.PRIMARY;
		} else if (rite.soulEcho && !rite.echoResolved) {
			next = GroundSlamRules.Beat.SOUL_ECHO;
			requested = GroundSlamRules.echoCenter(
					rite.center, rite.lookDirection, rite.baseRadius);
		} else if (rite.ancientMastery && !rite.crownResolved) {
			next = GroundSlamRules.Beat.CROWN;
		}
		if (next == null) return;
		int until = GroundSlamRules.beatAge(next) - age;
		if (until < 1 || until > 3) return;
		Vec3 point = GroundSlamImpactResolver.previewPoint(level, requested);
		GroundSlamFx.telegraph(level, point, until, next);
	}

	/** Applies Wardcraft and Veil only after a lawful primary beat. */
	private static void applyRankMantles(ServerLevel level, ServerPlayer owner,
			FaultboundVerdict rite) {
		if (rite.reflectiveWard) {
			owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
					rite.mantleDuration, 0, true, false, true));
		}
		slipHostileMemories(level, owner, rite);
		GroundSlamFx.mantle(level, owner.position(), rite.reflectiveWard, rite.afterimage);
	}

	/** Veil clears a bounded set of hostile mobs that can still see the caster. */
	private static void slipHostileMemories(ServerLevel level, ServerPlayer owner,
			FaultboundVerdict rite) {
		int limit = GroundSlamRules.afterimageTargetLimit(rite.afterimage);
		if (limit <= 0) return;
		double radius = Math.min(12.0, rite.baseRadius * 1.6);
		AABB bounds = AABB.ofSize(owner.position(), radius * 2.0,
				radius * 1.5, radius * 2.0);
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, bounds,
				mob -> mob.isAlive() && mob.getTarget() == owner && mob.hasLineOfSight(owner)
						&& !EntityFreezeController.isFrozen(mob)
						&& !PowerProtection.isSafeZone(level, mob.position())
						&& !SpellFieldManager.isSanctuaryProtected(level, mob));
		mobs.sort(Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(owner))
				.thenComparing(mob -> mob.getUUID().toString()));
		for (int index = 0; index < Math.min(limit, mobs.size()); index++) {
			mobs.get(index).setTarget(null);
		}
	}

	/** Accepts direct Ground Slam or the delegated earth phase of Elemental Blast. */
	private static boolean ownsSource(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && (power.id().equals(POWER_ID)
					|| power.id().equals(ELEMENTAL_POWER_ID))) return true;
		}
		return false;
	}
}
