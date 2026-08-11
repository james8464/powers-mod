package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.SoulLinkMath;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Soul Link: the Violet Crystal's power of empathy turned lethal. You bind
 * the souls of up to eight foes - whatever one suffers, all of them suffer.
 * Damage one linked enemy and every other bound soul takes the same wound.
 */
public class SoulLinkAbility extends Ability {
	// the link holds for 200 ticks = 10 seconds, reaches 15 blocks and binds up to eight souls
	private static final int DURATION_TICKS = 200;
	private static final int COOLDOWN_TICKS = 2400;
	private static final int RADIUS = 15;
	private static final float MIRROR_CAP_PER_TARGET = 20.0F;
	private static final Map<UUID, MirrorBaseline> MIRROR_BASELINES = new HashMap<>();

	// lastHealth is remembered each tick so fresh wounds can be measured
	private record Link(LivingEntity entity, float lastHealth, float remainingMirrorCap) {
	}

	private record ActiveLink(CastSource castSource, int ticksLeft,
			List<Link> links, double damageMultiplier) {
	}

	private record MirrorBaseline(float health, long tick) { }

	// one active link per caster, keyed by uuid so a logged-off player can be dropped
	private static final Map<UUID, ActiveLink> ACTIVE = new HashMap<>();

	public SoulLinkAbility() {
		super(PowersMod.id("soul_link"),
				Component.translatable("ability.powers.soul_link"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// one link at a time: a second use while linked does nothing
		if (ACTIVE.containsKey(player.getUUID())) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		double radius = scaledRange(player, RADIUS);
		int maxLinks = SoulLinkMath.maximumLinks();
		List<Link> links = new ArrayList<>();
		for (LivingEntity target : BoundedEntityCandidates.living(level,
				AABB.ofSize(player.position().add(0, 1, 0), radius * 2, radius * 2, radius * 2),
				128,
				 e -> e.isAlive() && e != player && !player.isAlliedTo(e)
						 && e.distanceToSqr(player) <= radius * radius
						 && !AmethystDampening.isDampened(e)
						 && PowerProtection.mayHarm(player, e)
						 && !SpellFieldManager.isSanctuaryProtected(level, e))) {
			links.add(new Link(target, target.getHealth(), MIRROR_CAP_PER_TARGET));
			PowerFx.coloredBurst(level, target.position().add(0, 1, 0), 0x9C27B0, 10, 0.5);
			if (links.size() >= maxLinks) {
				break;
			}
		}
		if (links.isEmpty()) {
			return false;
		}
		Vec3 previous = player.getEyePosition();
		for (Link link : links) {
			PowerFx.beam(level, previous, link.entity().getEyePosition(), PowerFx.dust(0x9C27B0, 0.75F), 12);
			previous = link.entity().getEyePosition();
		}
		ACTIVE.put(player.getUUID(), new ActiveLink(CastScalingContext.currentSource(),
				scaledDuration(player, DURATION_TICKS), links,
				scaling(player).potencyMultiplier()));
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x9C27B0, 24, 1.2);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		return true;
	}

	/** Called every server tick while links are active; mirrors wounds between souls. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		MIRROR_BASELINES.entrySet().removeIf(entry -> now - entry.getValue().tick() > 1L);
		Iterator<Map.Entry<UUID, ActiveLink>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveLink> entry = it.next();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
			ActiveLink active = entry.getValue();

			// caster logged off or died, so the link dies with them
			if (!MagicUseGate.ongoingAllowed(caster) || !ServerCastLifecycle.mayContinue(
					caster, active.castSource(), false)) {
				it.remove();
				continue;
			}

			List<Link> updated = new ArrayList<>();
			LivingEntity wounded = null;
			float damage = 0.0f;
			for (Link link : active.links()) {
				LivingEntity entity = link.entity();
				// skip souls that died, despawned or left the caster's dimension
				if (!entity.isAlive() || entity.isRemoved() || entity.level() != caster.level()
						|| AmethystDampening.isDampened(entity)
						|| !PowerProtection.mayHarm(caster, entity)
						|| SpellFieldManager.isSanctuaryProtected((ServerLevel) caster.level(), entity)) {
					continue;
				}
				// track the biggest wound suffered this tick
				MirrorBaseline mirror = MIRROR_BASELINES.get(entity.getUUID());
				float suffered = SoulLinkMath.woundAfterMirror(link.lastHealth(),
						mirror == null ? null : mirror.health(), entity.getHealth());
				if (suffered > damage) {
					damage = suffered;
					wounded = entity;
				}
				updated.add(new Link(entity, entity.getHealth(), link.remainingMirrorCap()));
			}
			if (damage > 0) {
				ServerLevel level = (ServerLevel) caster.level();
				Map<UUID, Float> appliedThisPass = new HashMap<>();
				// the biggest wound is shared: every other bound soul takes the same hit
				for (Link link : updated) {
					LivingEntity entity = link.entity();
					// the wounded soul itself doesn't take its own wound twice
					if (entity != null && entity != wounded) {
						float mirrored = SoulLinkMath.cappedMirror(
								(float) (damage * active.damageMultiplier()), link.remainingMirrorCap());
						if (mirrored <= 0.0F) continue;
						float beforeMirror = entity.getHealth();
						entity.hurtServer(level, PowerDamage.source(caster), mirrored);
						appliedThisPass.put(entity.getUUID(),
								SoulLinkMath.wound(beforeMirror, entity.getHealth()));
						MIRROR_BASELINES.put(entity.getUUID(),
								new MirrorBaseline(entity.getHealth(), now));
						PowerFx.coloredBurst(level, entity.position().add(0, 1, 0), 0x9C27B0, 6, 0.4);
					}
				}
				// Mirror damage happened after the first snapshot pass. Refresh every
				// survivor now so mirrored wounds cannot be mistaken for new wounds
				// and bounce around the link again on the next server tick.
				List<Link> postMirror = new ArrayList<>();
				for (Link link : updated) {
					if (link.entity().isAlive() && !link.entity().isRemoved()) {
						// Consume only health actually removed after shield/event interception.
						float spent = appliedThisPass.getOrDefault(link.entity().getUUID(), 0.0F);
						postMirror.add(new Link(link.entity(), link.entity().getHealth(),
								SoulLinkMath.remainingCap(link.remainingMirrorCap(), spent)));
					}
				}
				updated = postMirror;
			}
			if (server.getTickCount() % 20 == 0 && updated.size() > 1) {
				ServerLevel level = (ServerLevel) caster.level();
				Vec3 previous = caster.getEyePosition();
				for (Link link : updated) {
					PowerFx.beam(level, previous, link.entity().getEyePosition(),
							PowerFx.dust(0x9C27B0, 0.65F), 8);
					previous = link.entity().getEyePosition();
				}
				var topology = Component.translatable("crystal.powers.soul_link.status", updated.size());
				for (Link link : updated) {
					topology.append(Component.literal("  ")).append(Component.translatable(
							"crystal.powers.soul_link.target", link.entity().getDisplayName(),
							Math.round(link.remainingMirrorCap())));
				}
				caster.sendSystemMessage(topology, true);
			}

			// the link ends when time runs out or no souls remain
			int left = active.ticksLeft() - 1;
			if (left <= 0 || updated.isEmpty()) {
				it.remove();
			} else {
				entry.setValue(new ActiveLink(active.castSource(), left,
						updated, active.damageMultiplier()));
			}
		}
	}

	/** drop one caster's link when they log off */
	public static void clear(UUID player) {
		ACTIVE.remove(player);
	}

	/** Unweaves every active link containing the purified soul. */
	public static void clearLinksTouching(UUID entityId) {
		ACTIVE.entrySet().removeIf(entry -> entry.getKey().equals(entityId)
				|| entry.getValue().links().stream()
						.anyMatch(link -> link.entity().getUUID().equals(entityId)));
	}

	/** release every link on server stop */
	public static void clearAll() {
		ACTIVE.clear();
		MIRROR_BASELINES.clear();
	}

	/** Bounded owner-facing topology/cap state sourced from the authoritative links. */
	public static List<SoulLinkSnapshot> snapshots() {
		return ACTIVE.entrySet().stream().map(entry -> new SoulLinkSnapshot(entry.getKey(),
				entry.getValue().ticksLeft(), entry.getValue().links().stream()
						.map(link -> new TargetCap(link.entity().getUUID(), link.remainingMirrorCap())).toList()))
				.toList();
	}

	public record TargetCap(UUID target, float remainingMirrorDamage) { }
	public record SoulLinkSnapshot(UUID owner, int ticksLeft, List<TargetCap> topology) { }
}
