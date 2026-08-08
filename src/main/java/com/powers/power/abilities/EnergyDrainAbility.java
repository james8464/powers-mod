package com.powers.power.abilities;

import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityArithmetic;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import com.powers.network.PowersPackets;
import com.powers.spell.ChannelRules;
import com.powers.spell.ChannelState;
import com.powers.spell.ChannelStatus;
import com.powers.spell.SpellFieldManager;
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

/**
 * Maintains interruptible soul-tethers that transfer energy and leave a fully
 * drained target exhausted; transient links are cleared at lifecycle edges.
 */
public class EnergyDrainAbility extends Ability {
	// 30 seconds of exhaustion after a full drain
	private static final int EXHAUSTION_TICKS = 600;
	// 2 seconds to drain the whole bar
	private static final int RITUAL_TICKS = 40;
	// the link breaks past 48 blocks
	private static final double MAX_RANGE_SQ = 48.0 * 48.0;
	private static final Map<UUID, Ritual> RITUALS = new HashMap<>();

	private record Ritual(ServerPlayer caster, ServerPlayer target, ChannelState state,
			double maximumRangeSquared, int exhaustionTicks, double transferRatio) {}

	public EnergyDrainAbility() {
		super(PowersMod.id("energy_drain"),
				Component.translatable("ability.powers.energy_drain"), 600, false);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(caster, scaledRange(caster, 32.0));
		if (!(target instanceof ServerPlayer targetSP) || targetSP == caster) {
			PowerMessages.send(caster, "ability.powers.no_player_target", 4);
			return false;
		}
		if (AmethystDampening.isDampened(targetSP)) {
			PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (!PowerProtection.mayHarm(caster, targetSP)) return false;

		long endsAt = caster.level().getGameTime() + scaledDuration(caster, RITUAL_TICKS);
		double maximumRange = scaledRange(caster, Math.sqrt(MAX_RANGE_SQ));
		double transferRatio = scaling(caster).unlockedVariants().contains("soul_echo") ? 0.75 : 0.50;
		RITUALS.put(caster.getUUID(), new Ritual(caster, targetSP,
				new ChannelState(endsAt, caster.getX(), caster.getY(), caster.getZ(), "energy_drain", false),
				maximumRange * maximumRange, scaledDuration(caster, EXHAUSTION_TICKS), transferRatio));
		PowerFx.sound((ServerLevel) caster.level(), targetSP.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 0.45f);
		return true;
	}

	/** Runs every server tick; drains a fraction of the target's energy while the ritual holds. */
	public static void tickAll(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (var it = RITUALS.entrySet().iterator(); it.hasNext();) {
			Ritual ritual = it.next().getValue();
			ServerPlayer caster = ritual.caster();
			ServerPlayer target = ritual.target();
			// the ritual breaks if either player logs off, dies, or drifts apart
			boolean casterOnline = server.getPlayerList().getPlayer(caster.getUUID()) == caster;
			boolean targetOnline = server.getPlayerList().getPlayer(target.getUUID()) == target;
			if (!casterOnline || !targetOnline || !caster.isAlive() || !target.isAlive()
					|| caster.level() != target.level()
					|| caster.distanceToSqr(target) > ritual.maximumRangeSquared()
					|| AmethystDampening.isDampened(target)
					|| !PowerProtection.mayHarm(caster, target)
					|| SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target)
					|| ChannelRules.status(ritual.state(), now, caster.getX(), caster.getY(), caster.getZ(),
							true, AmethystDampening.isDampened(caster)) == ChannelStatus.INTERRUPTED) {
				it.remove();
				caster.sendSystemMessage(Component.translatable("spell.powers.interrupted"));
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
			if (now >= ritual.state().finishesAt()) {
				// full drain landed, hit the target with exhaustion
				PlayerPowers.get(target).emptyEnergy();
				PowersPackets.syncTo(target);
				target.addEffect(new MobEffectInstance(PowersEffects.EXHAUSTION, ritual.exhaustionTicks(), 0,
						false, false, true));
				PowerMessages.send(caster, "ability.powers.energy_drained", 3,
						target.getName().getString());
				it.remove();
				continue;
			}
			PlayerPowers.PlayerPowersData targetData = PlayerPowers.get(target);
			if (targetData.energy() > 0) {
				int ticksRemaining = (int) Math.max(1L, ritual.state().finishesAt() - now);
				int drained = AbilityArithmetic.drainStep(targetData.energy(), ticksRemaining);
				targetData.consumeEnergy(drained);
				PlayerPowers.PlayerPowersData casterData = PlayerPowers.get(caster);
				casterData.refundEnergy(Math.max(1, (int) Math.floor(drained * ritual.transferRatio())));
				PowersPackets.syncTo(target);
				PowersPackets.syncTo(caster);
			}
		}
	}

	public static void clearAll() {
		RITUALS.clear();
	}

	public static void clear(UUID player) {
		RITUALS.entrySet().removeIf(entry -> entry.getKey().equals(player)
				|| entry.getValue().target().getUUID().equals(player));
	}

	public static void markDamaged(LivingEntity entity) {
		Ritual ritual = RITUALS.get(entity.getUUID());
		if (ritual != null) RITUALS.put(entity.getUUID(), new Ritual(ritual.caster(), ritual.target(),
				ritual.state().withDamaged(true), ritual.maximumRangeSquared(), ritual.exhaustionTicks(),
				ritual.transferRatio()));
	}

	public static boolean counterNearest(ServerPlayer counter, double range) {
		UUID nearest = null;
		double best = range * range;
		for (Ritual ritual : RITUALS.values()) {
			if (ritual.caster() == counter || ritual.caster().level() != counter.level()) continue;
			double distance = ritual.caster().distanceToSqr(counter);
			if (distance <= best) {
				best = distance;
				nearest = ritual.caster().getUUID();
			}
		}
		if (nearest == null) return false;
		Ritual ritual = RITUALS.remove(nearest);
		PowerFx.clash((ServerLevel) counter.level(), counter.getEyePosition(), ritual.caster().getEyePosition(),
				0x7455A8, 0x6A1B9A);
		ritual.caster().sendSystemMessage(Component.translatable("spell.powers.countered"));
		return true;
	}
}
