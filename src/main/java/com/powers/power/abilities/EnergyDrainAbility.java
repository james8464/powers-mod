package com.powers.power.abilities;

import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnergyDrainAbility extends Ability {
	private static final int EXHAUSTION_TICKS = 600;
	private static final int RITUAL_TICKS = 40;
	private static final Map<UUID, Ritual> RITUALS = new HashMap<>();

	private record Ritual(ServerPlayer caster, ServerPlayer target, long endsAt) {}

	public EnergyDrainAbility() {
		super(PowersMod.id("energy_drain"),
				Component.translatable("ability.powers.energy_drain"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer caster, PlayerPowers.PlayerPowersData data) {
		HitResult hit = caster.pick(32.0, 0.0f, false);
		if (!(hit instanceof EntityHitResult entityHit)
				|| !(entityHit.getEntity() instanceof ServerPlayer target)
				|| target == caster) {
			caster.sendSystemMessage(Component.translatable("ability.powers.no_player_target"));
			return false;
		}
		if (AmethystDampening.isDampened(target)) {
			caster.sendSystemMessage(Component.translatable("amethyst.powers.target_protected"));
			return false;
		}

		PlayerPowers.get(target).emptyEnergy();
		target.addEffect(new MobEffectInstance(PowersEffects.EXHAUSTION, EXHAUSTION_TICKS, 0,
				false, false, true));
		ServerLevel level = (ServerLevel) caster.level();
		long endsAt = level.getServer().getTickCount() + RITUAL_TICKS;
		RITUALS.put(caster.getUUID(), new Ritual(caster, target, endsAt));
		PowerFx.sound(level, target.position(), net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f, 0.45f);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = RITUALS.entrySet().iterator(); it.hasNext();) {
			Ritual ritual = it.next().getValue();
			if (!ritual.caster().isAlive() || !ritual.target().isAlive()
					|| ritual.caster().level() != ritual.target().level() || now >= ritual.endsAt()) {
				it.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) ritual.caster().level();
			Vec3 from = ritual.caster().getEyePosition();
			Vec3 to = ritual.target().getEyePosition();
			PowerFx.beam(level, from, to,
					ParticleTypes.SOUL_FIRE_FLAME, 12);
			for (int i = 0; i < 8; i++) {
				double angle = Math.PI * 2.0 * i / 8.0 + now * 0.08;
				Vec3 rune = ritual.target().position().add(Math.cos(angle) * 1.4, 0.15, Math.sin(angle) * 1.4);
				PowerFx.burst(level, rune, ParticleTypes.ENCHANT, 2, 0.03, 0.01);
			}
		}
	}

	public static void clearAll() {
		RITUALS.clear();
	}
}
