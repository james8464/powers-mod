package com.powers.fx;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.PowersSounds;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.abilities.PossessionEndRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

/**
 * dramatic god-like punishment effects for moments of judgement: burnt-out
 * toggles, amethyst rejection, broken travel. they paint a picture of unseen
 * powers looking down on the player
 */
public final class GodlyPunishment {
	private GodlyPunishment() {
	}

	/**
	 * the full divine-wrath sequence: a rune circle and shockwave rings on the
	 * ground, a rising pillar of sparks, a burst of golden light, thunderous
	 * sounds and, optionally, a lightning storm that chases the player.
	 * a delayed second wave lands shortly after, as if the judgement follows
	 */
	public static void strike(ServerLevel level, ServerPlayer player, int rgb, boolean storm) {
		Vec3 pos = player.position().add(0, 1, 0);
		double phase = level.getServer().getTickCount() * 0.06;

		PowerFx.rune(level, pos.add(0, -0.3, 0), 3.0, rgb, 28, phase);
		PowerFx.ring(level, pos.add(0, -0.3, 0), 5.0, rgb, 36, phase + 0.35);
		PowerFx.spiral(level, pos, 1.6, 7.0, rgb, 42, 0.0);
		PowerFx.burst(level, pos, ParticleTypes.EXPLOSION, 26, 2.4, 0.3);
		PowerFx.coloredBurst(level, pos, rgb, 60, 1.6);
		PowerFx.burst(level, pos.add(0, 3, 0), ParticleTypes.END_ROD, 34, 0.6, 0.35);
		PowerFx.sound(level, pos, SoundEvents.BEACON_ACTIVATE, 1.0f, 0.5f);
		PowerFx.sound(level, pos, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.5f);
		PowerFx.sound(level, pos, SoundEvents.WITHER_SPAWN, 1.0f, 0.7f);
		if (storm) {
			PowersMod.startStorm(level, pos, player, 100, 100);
		}

		PowersMod.scheduleDelayed(level.getServer(), 25, () -> {
			// the follow-up wave lands a moment later; skip it if the player died first
			if (!player.isAlive() || player.isRemoved() || player.level() != level) return;
			Vec3 follow = player.position().add(0, 1, 0);
			PowerFx.ring(level, follow.add(0, -0.3, 0), 6.0, rgb, 40, phase + 1.3);
			PowerFx.coloredBurst(level, follow, rgb, 32, 1.2);
			PowerFx.burst(level, follow, ParticleTypes.ELECTRIC_SPARK, 18, 0.9, 0.12);
			PowerFx.sound(level, follow, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.6f);
		});
	}

	/** Nonlethal but severe judgement after a controlled vessel is killed. */
	public static void deadVesselWrath(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.drainEnergy(PossessionEndRules.wrathEnergyDrain(data.energyCapacity()));
		float damage = PossessionEndRules.wrathDamage(player.getHealth());
		if (damage > 0.0F) player.setHealth(player.getHealth() - damage);
		player.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS,
				PossessionEndRules.WRATH_TICKS, 3, false, true));
		player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS,
				PossessionEndRules.WRATH_TICKS, 2, false, true));
		player.addEffect(PowerStatusEffects.hidden(MobEffects.DARKNESS,
				PossessionEndRules.WRATH_TICKS, 0, false, true));
		PowersPackets.syncTo(player);

		Vec3 center = player.position().add(0.0, 1.0, 0.0);
		PowerFx.rune(level, player.position().add(0.0, 0.05, 0.0), 3.4, 0xC27CFF, 36, 0.0);
		PowerFx.ring(level, player.position().add(0.0, 0.08, 0.0), 5.2, 0xFFF2B0, 44, Math.PI / 2.0);
		PowerFx.spiral(level, center, 1.5, 5.5, 0x6D32A8, 48, 0.0);
		PowerFx.coloredBurst(level, center, 0xFFF2B0, 42, 1.4);
		PowerFx.sound(level, center, PowersSounds.CELESTIAL_RING, 1.75F, 0.72F);
		var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(player.position());
			level.addFreshEntity(bolt);
		}
		com.powers.util.PowerMessages.overlay(player,
				net.minecraft.network.chat.Component.translatable("ability.powers.vessel_wrath"));
	}

	/** a cold, dragging rejection, used when the dark realm refuses entry */
	public static void voidReject(ServerLevel level, ServerPlayer player) {
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.burst(level, pos, ParticleTypes.REVERSE_PORTAL, 22, 0.9, 0.05);
		PowerFx.spiral(level, pos, 0.9, 2.8, 0x2E0854, 22, 0.0);
		PowerFx.coloredBurst(level, pos, 0x4A235A, 18, 0.6);
		PowerFx.sound(level, pos, SoundEvents.WITHER_AMBIENT, 0.9f, 0.5f);
	}

	/** crimson chain flash when a dimensional anchor forbids travel */
	public static void chainBlock(ServerLevel level, ServerPlayer player) {
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.burst(level, pos, ParticleTypes.ELECTRIC_SPARK, 14, 0.5, 0.06);
		PowerFx.coloredBurst(level, pos, 0xFF4D4D, 18, 0.6);
		PowerFx.ring(level, pos.add(0, -0.3, 0), 2.2, 0xFF4D4D, 18, 0.0);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.7f, 0.9f);
	}

	/** a shimmering wall flash when a dimension outright refuses entry */
	public static void barrier(ServerLevel level, ServerPlayer player, int rgb) {
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.ring(level, pos.add(0, -0.3, 0), 2.5, rgb, 24, 0.0);
		PowerFx.burst(level, pos, ParticleTypes.END_ROD, 16, 0.6, 0.25);
		PowerFx.coloredBurst(level, pos, rgb, 20, 0.8);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.1f);
	}
}
