package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.entity.PlayerLikeTarget;
import com.powers.entity.TestActorPowerState;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityArithmetic;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.PowerTargeting;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import com.powers.network.PowersPackets;
import com.powers.spell.ChannelRules;
import com.powers.spell.ChannelState;
import com.powers.spell.ChannelStatus;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains interruptible soul-tethers that transfer energy and leave a fully
 * drained target exhausted; transient links are cleared at lifecycle edges.
 */
public class EnergyDrainAbility extends Ability {
	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("energy_drain");
	// 30 seconds of exhaustion after a full drain
	private static final int EXHAUSTION_TICKS = 600;
	// 2 seconds to drain the whole bar
	private static final int RITUAL_TICKS = 40;
	// the link breaks past 48 blocks
	private static final double MAX_RANGE_SQ = 48.0 * 48.0;
	private static final Map<UUID, Ritual> RITUALS = new HashMap<>();

	private record Ritual(UUID casterId, UUID targetId, ResourceKey<Level> dimension,
			CastSource castSource, ChannelState state,
			double maximumRangeSquared, int exhaustionTicks, double transferRatio) {}

	public EnergyDrainAbility() {
		super(POWER_ID,
				Component.translatable("ability.powers.energy_drain"), 600, false);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		LivingEntity target = PowerTargeting.findLivingTarget(caster, scaledRange(caster, 32.0));
		if (target == null || target == caster || !target.isAlive()) {
			PowerMessages.send(caster, "ability.powers.no_living_target", 4);
			return false;
		}
		if (AmethystDampening.isDampened(target)) {
			PowerMessages.send(caster, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (!PowerProtection.mayHarm(caster, target)) return false;

		long endsAt = caster.level().getServer().getTickCount() + scaledDuration(caster, RITUAL_TICKS);
		double maximumRange = scaledRange(caster, Math.sqrt(MAX_RANGE_SQ));
		double transferRatio = scaling(caster).unlockedVariants().contains("soul_echo") ? 0.75 : 0.50;
		RITUALS.put(caster.getUUID(), new Ritual(caster.getUUID(), target.getUUID(),
				target.level().dimension(), CastScalingContext.currentSource(),
				new ChannelState(endsAt, caster.getX(), caster.getY(), caster.getZ(), "energy_drain", false),
				maximumRange * maximumRange, scaledDuration(caster, EXHAUSTION_TICKS), transferRatio));
		PowerFx.sound((ServerLevel) caster.level(), target.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 0.45f);
		return true;
	}

	/** Runs every server tick; drains a fraction of the target's energy while the ritual holds. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = RITUALS.entrySet().iterator(); it.hasNext();) {
			Ritual ritual = it.next().getValue();
			ServerPlayer caster = server.getPlayerList().getPlayer(ritual.casterId());
			ServerLevel targetLevel = server.getLevel(ritual.dimension());
			LivingEntity target = targetLevel == null ? null
					: targetLevel.getEntity(ritual.targetId()) instanceof LivingEntity living ? living : null;
			boolean casterOnline = caster != null;
			boolean targetOnline = target != null && (!(target instanceof ServerPlayer targetPlayer)
					|| server.getPlayerList().getPlayer(targetPlayer.getUUID()) == targetPlayer);
			if (!casterOnline || !targetOnline || !caster.isAlive() || !target.isAlive()
					|| !MagicUseGate.ongoingAllowed(caster)
					|| !ServerCastLifecycle.mayContinue(caster, ritual.castSource(), ownsPower(caster))
					|| caster.level() != target.level()
					|| caster.distanceToSqr(target) > ritual.maximumRangeSquared()
					|| AmethystDampening.isDampened(target)
					|| !PowerProtection.mayHarm(caster, target)
					|| SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target)
					|| ChannelRules.status(ritual.state(), now, caster.getX(), caster.getY(), caster.getZ(),
							true, AmethystDampening.isDampened(caster)) == ChannelStatus.INTERRUPTED) {
				it.remove();
				if (caster != null) PowerMessages.overlay(caster,
						net.minecraft.network.chat.Component.translatable("spell.powers.interrupted"));
				continue;
			}
			ServerLevel level = (ServerLevel) caster.level();
			Vec3 from = caster.getEyePosition();
			Vec3 to = target.getEyePosition();
			PowerFx.beam(level, from, to, ParticleTypes.SOUL_FIRE_FLAME, 12);
			for (int i = 0; i < 8; i++) {
				double angle = Math.PI * 2.0 * i / 8.0 + now * 0.08;
				Vec3 rune = target.position().add(Math.cos(angle) * 1.4, 0.15, Math.sin(angle) * 1.4);
				PowerFx.burst(level, rune, PowerFx.dust(0x7A22A8, 0.8F), 2, 0.03, 0.0);
			}
			if (now >= ritual.state().finishesAt()) {
				// full drain landed, hit the target with exhaustion
				if (PlayerLikeTarget.isCompatible(target)) {
					if (target instanceof ServerPlayer targetPlayer) {
						PlayerPowers.get(targetPlayer).emptyEnergy();
						PowersPackets.syncTo(targetPlayer);
					} else {
						TestActorPowerState.empty(target.getUUID());
					}
					target.addEffect(PowerStatusEffects.hidden(PowersEffects.EXHAUSTION,
							ritual.exhaustionTicks(), 0, false, true));
				} else {
					target.hurtServer(level, PowerDamage.source(caster),
							EnergyDrainRules.mobCompletionDamage(target.getMaxHealth()));
					target.addEffect(PowerStatusEffects.hidden(net.minecraft.world.effect.MobEffects.WITHER,
							200, 3, false, true));
					PlayerPowers.get(caster).refundEnergy(120);
					PowersPackets.syncTo(caster);
				}
				PowerMessages.sendImportant(caster, "ability.powers.energy_drained", 3,
						target.getName().getString());
				it.remove();
				continue;
			}
			if (PlayerLikeTarget.isCompatible(target)) {
				int targetEnergy = target instanceof ServerPlayer targetPlayer
						? PlayerPowers.get(targetPlayer).energy()
						: TestActorPowerState.energy(target.getUUID());
				if (targetEnergy <= 0) continue;
				int ticksRemaining = (int) Math.max(1L, ritual.state().finishesAt() - now);
				int requested = AbilityArithmetic.drainStep(targetEnergy, ticksRemaining);
				int drained;
				if (target instanceof ServerPlayer targetPlayer) {
					PlayerPowers.get(targetPlayer).consumeEnergy(requested);
					drained = requested;
					PowersPackets.syncTo(targetPlayer);
				} else {
					drained = TestActorPowerState.drain(target.getUUID(), requested);
				}
				PlayerPowers.PlayerPowersData casterData = PlayerPowers.get(caster);
				casterData.refundEnergy(Math.max(1, (int) Math.floor(drained * ritual.transferRatio())));
				PowersPackets.syncTo(caster);
			} else if (now % 10 == 0 && target.hurtServer(level, PowerDamage.source(caster),
					EnergyDrainRules.mobPulseDamage(target.getMaxHealth()))) {
				PlayerPowers.get(caster).refundEnergy(5);
				PowersPackets.syncTo(caster);
			}
		}
	}

	public static void clearAll() {
		RITUALS.clear();
	}

	public static void clear(UUID player) {
		RITUALS.entrySet().removeIf(entry -> entry.getKey().equals(player)
				|| entry.getValue().targetId().equals(player));
	}

	public static void markDamaged(LivingEntity entity) {
		Ritual ritual = RITUALS.get(entity.getUUID());
		if (ritual != null) RITUALS.put(entity.getUUID(), new Ritual(ritual.casterId(),
				ritual.targetId(), ritual.dimension(), ritual.castSource(),
				ritual.state().withDamaged(true), ritual.maximumRangeSquared(), ritual.exhaustionTicks(),
				ritual.transferRatio()));
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && POWER_ID.equals(power.id())) return true;
		}
		return false;
	}

	public static boolean counterNearest(ServerPlayer counter, double range) {
		UUID nearest = null;
		double best = range * range;
		for (Ritual ritual : RITUALS.values()) {
			ServerPlayer caster = counter.level().getServer().getPlayerList().getPlayer(ritual.casterId());
			if (caster == null || caster == counter || caster.level() != counter.level()) continue;
			double distance = caster.distanceToSqr(counter);
			if (distance <= best) {
				best = distance;
				nearest = caster.getUUID();
			}
		}
		if (nearest == null) return false;
		Ritual ritual = RITUALS.remove(nearest);
		ServerPlayer caster = counter.level().getServer().getPlayerList().getPlayer(ritual.casterId());
		if (caster == null) return false;
		PowerFx.clash((ServerLevel) counter.level(), counter.getEyePosition(), caster.getEyePosition(),
				0x7455A8, 0x6A1B9A);
		PowerMessages.overlay(caster,
				net.minecraft.network.chat.Component.translatable("spell.powers.countered"));
		return true;
	}
}
