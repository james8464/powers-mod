package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns exact-once, five-minute lethal-damage wards for both mythic artifacts. */
public final class ArtifactDeathWardManager {
	private static final int DURATION_TICKS = 20 * 60 * 5;
	private static final Map<UUID, Ward> WARDS = new HashMap<>();

	private record Ward(ArtifactAlignment alignment, long expiresAt) {
	}

	private ArtifactDeathWardManager() {
	}

	/** Arms or refreshes one player's single ward. */
	public static boolean arm(ServerPlayer player, ArtifactAlignment alignment) {
		WARDS.put(player.getUUID(), new Ward(alignment,
				player.level().getServer().getTickCount() + DURATION_TICKS));
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.rune(level, player.position(), 2.2,
				alignment == ArtifactAlignment.DARKNESS ? 0x3A0B52 : 0xFFE89B, 32, 0.0);
		return true;
	}

	/** Consumes one legal ward during ALLOW_DEATH; void and administrator kills bypass it. */
	public static boolean preventDeath(ServerPlayer player, DamageSource source) {
		if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;
		Ward ward = WARDS.get(player.getUUID());
		if (ward == null || !ArtifactDominionRules.wardActive(
				player.level().getServer().getTickCount(), ward.expiresAt())
				|| !WARDS.remove(player.getUUID(), ward)) return false;
		player.setHealth(ArtifactDominionRules.restoredHealth(ward.alignment(), player.getMaxHealth()));
		player.invulnerableTime = 40;
		player.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 40, 4, false, true));
		if (ward.alignment() == ArtifactAlignment.DARKNESS) {
			PlayerPowers.get(player).refundEnergy(40);
			PowersPackets.syncTo(player);
		} else {
			player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 20.0F));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 200, 4, false, true));
		}
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.burst(level, player.position().add(0.0, 1.0, 0.0),
				ward.alignment() == ArtifactAlignment.DARKNESS ? ParticleTypes.REVERSE_PORTAL
						: ParticleTypes.TOTEM_OF_UNDYING, 42, 1.1, 0.2);
		PowerFx.rune(level, player.position(), 3.4,
				ward.alignment() == ArtifactAlignment.DARKNESS ? 0x6C2383 : 0xFFF2B2, 44, Math.PI);
		return true;
	}

	public static void forget(UUID playerId) {
		WARDS.remove(playerId);
	}

	public static void forget(UUID playerId, ArtifactAlignment alignment) {
		Ward ward = WARDS.get(playerId);
		if (ward != null && ward.alignment() == alignment) WARDS.remove(playerId, ward);
	}

	public static void clear() {
		WARDS.clear();
	}
}
