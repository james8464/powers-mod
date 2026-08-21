package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * inferno - the red crystal's power: for 8 seconds (160 ticks) the world
 * around you becomes a firestorm - blazing meteors rain down, everything
 * within 12 blocks is set ablaze, and no one inside escapes the flames
 */
public class InfernoAbility extends Ability {
	// The finite eight-second lease prevents an abandoned firestorm.
	private static final int DURATION_TICKS = 160;
	// The authored ninety-second cooldown bounds repeated terrain pressure.
	private static final int COOLDOWN_TICKS = 1800;
	// A hard radius bounds both target inspection and presentation work.
	private static final int RADIUS = 12;

	// one inferno per owner uuid, cleaned up on disconnect and server stop so it can't leak
	private record InfernoField(CastSource castSource, int remainingTicks,
			double radius, float damage, int fireSeconds) {
	}
	private static final Map<UUID, InfernoField> ACTIVE = new HashMap<>();

	public InfernoAbility() {
		super(PowersMod.id("inferno"),
				Component.translatable("ability.powers.inferno"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// no stacking - a second cast while already burning is refused
		if (ACTIVE.containsKey(player.getUUID())) {
			return false;
		}
		ACTIVE.put(player.getUUID(), new InfernoField(CastScalingContext.currentSource(),
				scaledDuration(player, DURATION_TICKS),
				scaledRange(player, RADIUS), scaledPotency(player, 2.0f),
				Math.max(1, scaledDuration(player, 160) / 20)));
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFF3D00, 30, 1.5);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.FLAME, 40, 1.2, 0.4);
		PowerFx.sound(level, player.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
		return true;
	}

	/** Called every server tick while any inferno is active. */
	public static void tickAll(MinecraftServer server) {
		var it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, InfernoField> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			InfernoField field = entry.getValue();
			int left = field.remainingTicks();

			// the owner logged off or died - drop the inferno instead of leaving it burning forever
			if (!MagicUseGate.ongoingAllowed(player) || !ServerCastLifecycle.mayContinue(
					player, field.castSource(), false)) {
				it.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			// Fixed pulse cadence bounds repeated damage and FX work.
			if (left % 8 == 0) {
				Vec3 origin = player.position().add(0, 1.2, 0);
				for (int i = 0; i < 6; i++) {
					Vec3 impact = origin.add((level.getRandom().nextDouble() - 0.5) * 2 * field.radius(),
							-1.0, (level.getRandom().nextDouble() - 0.5) * 2 * field.radius());
					PowerFx.beam(level, impact.add(0, 8, 0), impact, ParticleTypes.FLAME, 10);
					PowerFx.burst(level, impact, ParticleTypes.LARGE_SMOKE, 4, 0.4, 0.04);
				}
				// Keep the ignition window finite so one cast cannot leave permanent pulse ownership.
				for (LivingEntity target : BoundedEntityCandidates.living(level,
						AABB.ofSize(origin, field.radius() * 2, 8, field.radius() * 2),
						192,
						e -> e.isAlive() && e != player && e.distanceToSqr(player) <= field.radius() * field.radius()
								&& !AmethystDampening.isDampened(e)
								&& PowerProtection.mayHarm(player, e)
								&& !SpellFieldManager.isSanctuaryProtected(level, e))) {
					target.hurtServer(level, PowerDamage.source(player), field.damage());
					target.igniteForSeconds(field.fireSeconds());
				}
				PowerFx.burst(level, origin, ParticleTypes.FLAME, 20, 2.5, 0.2);
			}

			if (--left <= 0) {
				// Emit one terminal cue after removing ownership from the active map.
				it.remove();
				PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.SMOKE, 24, 1.2, 0.1);
			} else {
				entry.setValue(new InfernoField(field.castSource(), left,
						field.radius(), field.damage(), field.fireSeconds()));
			}
		}
	}

	// Disconnect cleanup prevents an ownerless field from surviving its player.
	public static void clear(UUID player) {
		ACTIVE.remove(player);
	}

	// Server-stop cleanup prevents active fields from crossing world epochs.
	public static void clearAll() {
		ACTIVE.clear();
	}
}
