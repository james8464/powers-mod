package com.powers.power;

import com.powers.PowerStatusEffects;
import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.protection.PowerProtection;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.PowerMessages;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Matches two nearby allied innate casts into one bounded, boss-capable Concord pulse. */
public final class ConcordCastManager {
	private static final ArrayDeque<RecentCast> RECENT = new ArrayDeque<>();
	private static final Map<PairKey, Long> PAIR_COOLDOWNS = new HashMap<>();

	private ConcordCastManager() {
	}

	public static boolean record(ServerPlayer player, Ability ability) {
		ServerLevel level = (ServerLevel) player.level();
		long tick = level.getServer().getTickCount();
		prune(tick);
		boolean darkness = SkillSystem.hasDarknessTag(player);
		Iterator<RecentCast> iterator = RECENT.descendingIterator();
		while (iterator.hasNext()) {
			RecentCast recent = iterator.next();
			ServerPlayer partner = level.getServer().getPlayerList().getPlayer(recent.owner());
			PairKey pair = PairKey.of(player.getUUID(), recent.owner());
			boolean cooling = PAIR_COOLDOWNS.getOrDefault(pair, 0L) > tick;
			double distance = partner == null || partner.level() != level
					? Double.POSITIVE_INFINITY : partner.position().distanceTo(player.position());
			if (!ConcordCastRules.mayConcord(recent.ability().equals(ability.id()),
					recent.darkness() == darkness, !recent.owner().equals(player.getUUID()),
					distance, tick - recent.tick(), cooling)) continue;
			iterator.remove();
			PAIR_COOLDOWNS.put(pair, tick + ConcordCastRules.PAIR_COOLDOWN_TICKS);
			concord(level, partner, player, ability, darkness);
			return true;
		}
		RECENT.addLast(new RecentCast(player.getUUID(), ability.id(), darkness, tick));
		while (RECENT.size() > ConcordCastRules.MAX_RECENT_CASTS) RECENT.removeFirst();
		return false;
	}

	private static void concord(ServerLevel level, ServerPlayer first, ServerPlayer second,
			Ability ability, boolean darkness) {
		Vec3 center = first.position().add(second.position()).scale(0.5).add(0.0, 1.0, 0.0);
		for (ServerPlayer player : new ServerPlayer[] {first, second}) {
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			data.refundEnergy(Math.max(50, data.energyCapacity() / 5));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 200, 4, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 200, 1, true, true));
			PowersPackets.syncTo(player);
			PowerMessages.overlay(player, net.minecraft.network.chat.Component.translatable(
					darkness ? "power.powers.umbral_concord" : "power.powers.radiant_concord",
					ability.name()));
		}
		impact(level, first, second, center, darkness);
		int primary = darkness ? 0x24002F : 0xFFF2AD;
		int secondary = darkness ? 0x8E3CB0 : 0xFFFFFF;
		PowerFx.beam(level, first.getEyePosition(), second.getEyePosition(),
				PowerFx.dust(secondary, 1.25F), 24);
		PowerFx.rune(level, center.subtract(0.0, 1.0, 0.0), 6.0, primary, 48, 0.0);
		PowerFx.spiral(level, center.subtract(0.0, 2.5, 0.0), 2.2, 7.0, secondary, 42, 0.0);
		PowerFx.burst(level, center, darkness ? PowersParticles.ECLIPSE : PowersParticles.MOTE,
				24, 2.2, 0.08);
		PowerFx.sound(level, center, darkness ? PowersSounds.DARK_WHISPER : PowersSounds.LIGHT_CHORUS,
				1.8F, darkness ? 0.48F : 1.35F);
	}

	private static void impact(ServerLevel level, ServerPlayer first, ServerPlayer second,
			Vec3 center, boolean darkness) {
		AABB bounds = AABB.ofSize(center, 20.0, 14.0, 20.0);
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds,
				ConcordCastRules.MAX_IMPACT_TARGETS, target -> target != first && target != second
						&& target.isAlive()
						&& target.entityTags().contains(SkillSystem.DARKNESS_TAG) != darkness,
				Comparator.comparingDouble((LivingEntity target) -> target.position().distanceToSqr(center))
						.thenComparing(target -> target.getUUID().toString()))) {
			if (PowerProtection.isSafeZone(level, target.position())) continue;
			target.hurtServer(level, PowerDamage.source(second), 48.0F);
			Vec3 away = target.position().subtract(center);
			if (away.lengthSqr() > 1.0E-4) target.push(away.normalize().scale(1.4));
			PowerFx.burst(level, target.getEyePosition(), PowersParticles.FRACTURE, 8, 0.45, 0.12);
		}
	}

	private static void prune(long tick) {
		RECENT.removeIf(cast -> tick - cast.tick() > ConcordCastRules.WINDOW_TICKS);
		PAIR_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= tick);
	}

	public static void forget(UUID owner) {
		RECENT.removeIf(cast -> cast.owner().equals(owner));
		PAIR_COOLDOWNS.keySet().removeIf(pair -> pair.contains(owner));
	}

	public static void clear() {
		RECENT.clear();
		PAIR_COOLDOWNS.clear();
	}

	public static Diagnostics diagnostics() {
		return new Diagnostics(RECENT.size(), PAIR_COOLDOWNS.size());
	}

	public record Diagnostics(int recentCasts, int coolingPairs) {
	}

	private record RecentCast(UUID owner, Identifier ability, boolean darkness, long tick) {
	}

	private record PairKey(UUID first, UUID second) {
		private static PairKey of(UUID left, UUID right) {
			return left.compareTo(right) <= 0 ? new PairKey(left, right) : new PairKey(right, left);
		}

		private boolean contains(UUID owner) {
			return first.equals(owner) || second.equals(owner);
		}
	}
}
