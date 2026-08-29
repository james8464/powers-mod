package com.powers.realm;

import com.powers.PowerStatusEffects;
import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.util.PowerMessages;
import com.powers.time.TemporalClocks;
import com.powers.time.TemporalSubsystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Applies Force Pressure and presents Dark Eclipse/Whiteout without unbounded scans. */
public final class RealmEventManager {
	private static final Map<UUID, RealmEventType> LAST_EVENT = new HashMap<>();

	private RealmEventManager() {
	}

	public static void tickPlayer(ServerPlayer player, ServerLevel level, RealmKind kind) {
		if (!TemporalClocks.worldAdvances(level.getServer(), TemporalSubsystem.REALM_CYCLES)) return;
		RealmEventType event = RealmEventRules.eventAt(kind, level.getGameTime());
		announceTransition(player, event);
		if (!TemporalClocks.worldPulse(level.getServer(), level, 20L,
				TemporalSubsystem.REALM_CYCLES)) return;
		double dx = player.getX() - RealmLayout.ENTRY_X;
		double dz = player.getZ() - RealmLayout.ENTRY_Z;
		int tier = RealmEventRules.pressureTier(Math.sqrt(dx * dx + dz * dz),
				event != RealmEventType.NONE);
		boolean dark = SkillSystem.hasDarknessTag(player);
		boolean aligned = kind == RealmKind.DARK ? dark : !dark;
		applyPressure(player, tier, aligned, event != RealmEventType.NONE);
		present(level, player.position().add(0.0, 1.0, 0.0), kind, event, tier);
	}

	private static void announceTransition(ServerPlayer player, RealmEventType event) {
		RealmEventType previous = LAST_EVENT.put(player.getUUID(), event);
		if (previous == event || previous == null && event == RealmEventType.NONE) return;
		if (event == RealmEventType.NONE) {
			PowerMessages.overlay(player, Component.translatable("realm.powers.event_clear"));
		} else {
			PowerMessages.sendImportant(player, event == RealmEventType.DARK_ECLIPSE
					? "realm.powers.dark_eclipse" : "realm.powers.whiteout", 1);
		}
	}

	private static void applyPressure(ServerPlayer player, int tier, boolean aligned,
			boolean eventActive) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int before = data.energy();
		if (aligned) {
			data.regenerateEnergy(2 + tier * 2 + (eventActive ? 4 : 0));
			if (tier >= 1) player.addEffect(PowerStatusEffects.hidden(
					MobEffects.RESISTANCE, 30, Math.min(2, tier - 1), true, true));
			if (eventActive) player.addEffect(PowerStatusEffects.hidden(
					MobEffects.STRENGTH, 30, Math.max(0, tier - 1), true, true));
		} else {
			data.drainEnergy(2 + tier * 3 + (eventActive ? 5 : 0));
			if (tier >= 1) player.addEffect(PowerStatusEffects.hidden(
					MobEffects.WEAKNESS, 30, Math.min(2, tier - 1), true, true));
			if (tier >= 2) player.addEffect(PowerStatusEffects.hidden(
					MobEffects.SLOWNESS, 30, tier - 2, true, true));
			if (tier >= 3) player.addEffect(PowerStatusEffects.hidden(
					MobEffects.WITHER, 30, eventActive ? 2 : 1, true, true));
		}
		if (data.energy() != before) PowersPackets.syncTo(player);
	}

	private static void present(ServerLevel level, Vec3 center, RealmKind kind,
			RealmEventType event, int tier) {
		boolean light = kind == RealmKind.LIGHT;
		int color = light ? 0xFFF8E8 : 0x260735;
		PowerFx.rune(level, center.subtract(0.0, 0.95, 0.0), 1.2 + tier * 0.25,
				color, 12 + tier * 4, level.getGameTime() * (light ? 0.035 : -0.035));
		PowerFx.burst(level, center, light ? PowersParticles.MOTE : PowersParticles.ECLIPSE,
				2 + tier, 2.0 + tier, 0.01);
		if (event == RealmEventType.NONE || level.getGameTime() % 40L != 0L) return;
		PowerFx.ring(level, center, 8.0 + tier * 2.0, color, 28,
				level.getGameTime() * 0.02);
		PowerFx.spiral(level, center.subtract(0.0, 6.0, 0.0), 5.0, 24.0,
				light ? 0xFFFFFF : 0x09000F, 36, level.getGameTime() * 0.02);
		PowerFx.burst(level, center, light ? com.powers.PowersParticles.GLYPH : com.powers.PowersParticles.ECLIPSE,
				6, 7.0, 0.02);
		if (level.getGameTime() % 200L == 0L) {
			PowerFx.sound(level, center, light ? PowersSounds.LIGHT_CHORUS : PowersSounds.DARK_WHISPER,
					0.85F, light ? 1.45F : 0.42F);
			PowerFx.sound(level, center, light ? SoundEvents.BEACON_AMBIENT : SoundEvents.SCULK_SHRIEKER_SHRIEK,
					0.35F, light ? 1.65F : 0.55F);
		}
	}

	public static void clear() {
		LAST_EVENT.clear();
	}

	public static void forget(UUID owner) {
		LAST_EVENT.remove(owner);
	}
}
