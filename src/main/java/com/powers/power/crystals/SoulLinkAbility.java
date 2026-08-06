package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import net.minecraft.network.chat.Component;
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

/**
 * Soul Link: the Violet Crystal's power of empathy turned lethal. You bind
 * the souls of up to eight foes - whatever one suffers, all of them suffer.
 * Damage one linked enemy and every other bound soul takes the same wound.
 */
public class SoulLinkAbility extends Ability {
	private static final int DURATION_TICKS = 200;
	private static final int COOLDOWN_TICKS = 2400;
	private static final int RADIUS = 15;
	private static final int MAX_LINKS = 8;

	private record Link(LivingEntity entity, float lastHealth) {
	}

	private record ActiveLink(int ticksLeft, List<Link> links) {
	}

	private static final Map<ServerPlayer, ActiveLink> ACTIVE = new HashMap<>();

	public SoulLinkAbility() {
		super(PowersMod.id("soul_link"),
				Component.translatable("ability.powers.soul_link"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (ACTIVE.containsKey(player)) {
			return false;
		}
		ServerLevel level = (ServerLevel) player.level();
		List<Link> links = new ArrayList<>();
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(player.position().add(0, 1, 0), RADIUS * 2, RADIUS * 2, RADIUS * 2),
				 e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e))) {
			links.add(new Link(target, target.getHealth()));
			PowerFx.coloredBurst(level, target.position().add(0, 1, 0), 0x9C27B0, 10, 0.5);
			if (links.size() >= MAX_LINKS) {
				break;
			}
		}
		if (links.isEmpty()) {
			return false;
		}
		ACTIVE.put(player, new ActiveLink(DURATION_TICKS, links));
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x9C27B0, 24, 1.2);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		return true;
	}

	/** Called every server tick while links are active; mirrors wounds between souls. */
	public static void tickAll() {
		Iterator<Map.Entry<ServerPlayer, ActiveLink>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ServerPlayer, ActiveLink> entry = it.next();
			ServerPlayer caster = entry.getKey();
			ActiveLink active = entry.getValue();

			if (!caster.isAlive()) {
				it.remove();
				continue;
			}

			List<Link> updated = new ArrayList<>();
			LivingEntity wounded = null;
			float damage = 0.0f;
			for (Link link : active.links()) {
				LivingEntity entity = link.entity();
				if (!entity.isAlive() || entity.isRemoved() || entity.level() != caster.level()) {
					continue;
				}
				float suffered = link.lastHealth() - entity.getHealth();
				if (suffered > damage) {
					damage = suffered;
					wounded = entity;
				}
				updated.add(new Link(entity, entity.getHealth()));
			}
			if (damage > 0) {
				ServerLevel level = (ServerLevel) caster.level();
				for (Link link : updated) {
					LivingEntity entity = link.entity();
					if (entity != null && entity != wounded) {
						entity.hurtServer(level, caster.damageSources().magic(), damage);
						PowerFx.coloredBurst(level, entity.position().add(0, 1, 0), 0x9C27B0, 6, 0.4);
					}
				}
			}

			int left = active.ticksLeft() - 1;
			if (left <= 0 || updated.isEmpty()) {
				it.remove();
			} else {
				entry.setValue(new ActiveLink(left, updated));
			}
		}
	}
}
