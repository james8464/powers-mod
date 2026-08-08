package com.powers.power.abilities;

import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityArithmetic;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.util.PowerMessages;
import com.powers.network.PowersPackets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Energy Drain: lock onto a player and siphon their energy for 2 seconds;
// if you keep the ritual going to the end they're left exhausted for 30s.
public class EnergyDrainAbility extends Ability {
	// 30 seconds of exhaustion after a full drain
	private static final int EXHAUSTION_TICKS = 600;
	// 2 seconds to drain the whole bar
	private static final int RITUAL_TICKS = 40;
	// the link breaks past 48 blocks
	private static final double MAX_RANGE_SQ = 48.0 * 48.0;
	private static final Map<UUID, Ritual> RITUALS = new HashMap<>();

	private record Ritual(ServerPlayer caster, ServerPlayer target, long endsAt) {}

	public EnergyDrainAbility() {
		super(PowersMod.id("energy_drain"),
				Component.translatable("ability.powers.energy_drain"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(caster, 32.0);
		if (!(target instanceof ServerPlayer targetSP) || targetSP == caster) {
			PowerMessages.send(caster, "ability.powers.no_player_target", 4);
			return false;
		}
		if (AmethystDampening.isDampened(targetSP)) {
			PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
			return false;
		}

		long endsAt = ((ServerLevel) caster.level()).getServer().getTickCount() + RITUAL_TICKS;
		RITUALS.put(caster.getUUID(), new Ritual(caster, targetSP, endsAt));
		PowerFx.sound((ServerLevel) caster.level(), targetSP.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 0.45f);
		return true;
	}

	/** Runs every server tick; drains a fraction of the target's energy while the ritual holds. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = RITUALS.entrySet().iterator(); it.hasNext();) {
			Ritual ritual = it.next().getValue();
			ServerPlayer caster = ritual.caster();
			ServerPlayer target = ritual.target();
			// the ritual breaks if either player logs off, dies, or drifts apart
			boolean casterOnline = server.getPlayerList().getPlayer(caster.getUUID()) == caster;
			boolean targetOnline = server.getPlayerList().getPlayer(target.getUUID()) == target;
			if (!casterOnline || !targetOnline || !caster.isAlive() || !target.isAlive()
					|| caster.level() != target.level()
					|| caster.distanceToSqr(target) > MAX_RANGE_SQ) {
				it.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) caster.level();
			Vec3 from = caster.getEyePosition();
			Vec3 to = target.getEyePosition();
			PowerFx.beam(level, from, to, ParticleTypes.SOUL_FIRE_FLAME, 12);
			for (int i = 0; i < 8; i++) {
				double angle = Math.PI * 2.0 * i / 8.0 + now * 0.08;
				Vec3 rune = target.position().add(Math.cos(angle) * 1.4, 0.15, Math.sin(angle) * 1.4);
				PowerFx.burst(level, rune, ParticleTypes.ENCHANT, 2, 0.03, 0.01);
			}
			if (now >= ritual.endsAt()) {
				// full drain landed, hit the target with exhaustion
				PlayerPowers.get(target).emptyEnergy();
				PowersPackets.syncTo(target);
				target.addEffect(new MobEffectInstance(PowersEffects.EXHAUSTION, EXHAUSTION_TICKS, 0,
						false, false, true));
				PowerMessages.send(caster, "ability.powers.energy_drained", 3,
						target.getName().getString());
				it.remove();
				continue;
			}
			PlayerPowers.PlayerPowersData targetData = PlayerPowers.get(target);
			if (targetData.energy() > 0) {
				int ticksRemaining = (int) Math.max(1L, ritual.endsAt() - now);
				targetData.consumeEnergy(AbilityArithmetic.drainStep(targetData.energy(), ticksRemaining));
				PowersPackets.syncTo(target);
			}
		}
	}

	public static void clearAll() {
		RITUALS.clear();
	}
}
