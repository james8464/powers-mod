package com.powers.forge;

import com.powers.PowersDataComponents;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.network.PowersPackets;
import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.magic.runtime.CastSource;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Server-owned, zero-cooldown lightning for weapons bound to an animated artifact star. */
public final class CrucibleWeaponRuntime {
	private static final CrucibleCastRateLimiter RATE_LIMITER = new CrucibleCastRateLimiter();

	private CrucibleWeaponRuntime() {
	}

	public static void initialize() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			CrucibleWeaponData weapon = stack.get(PowersDataComponents.CRUCIBLE_WEAPON);
			if (weapon == null || !weapon.starBound()) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
			cast(serverPlayer, weapon);
			return InteractionResult.SUCCESS;
		});
	}

	public static boolean cast(ServerPlayer player, CrucibleWeaponData weapon) {
		int tick = player.level().getServer().getTickCount();
		if (!RATE_LIMITER.allow(player.getUUID(), tick)) return false;
		AmethystDampening.update(player);
		if (AmethystDampening.isDampened(player)) {
			AmethystDampening.punish(player);
			return false;
		}
		LivingEntity target = PowerTargeting.findLivingTarget(player, 96.0);
		if (target == null || !PowerProtection.mayHarm(player, target)) return false;
		ServerLevel level = (ServerLevel) player.level();
		if (AmethystDampening.isDampened(target)
				|| SpellFieldManager.isSanctuaryProtected(level, target)) return false;
		PreparedMagicCast magic = ServerMagicCasts.prepare(player,
				weapon.alignment() == ArtifactAlignment.DARKNESS
						? "starbound_dark_lightning" : "starbound_light_lightning",
				CastSource.ARTIFACT);
		if (!magic.allowed()) return false;
		int energy = CrucibleLightningRules.energyCost(weapon.level());
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		if (!powers.consumeEnergy(energy)) {
			PowerMessages.send(player, "energy.powers.empty", 6);
			return false;
		}
		boolean targetDark = target.entityTags().contains(SkillSystem.DARKNESS_TAG);
		boolean opposed = weapon.alignment() == ArtifactAlignment.DARKNESS ? !targetDark : targetDark;
		float damage = CrucibleLightningRules.damage(weapon.level(), opposed, target instanceof Player);
		boolean damaged = ServerMagicCasts.execute(magic,
				() -> target.hurtServer(level, PowerDamage.source(player), damage));
		if (!damaged) {
			powers.refundEnergy(energy);
			PowersPackets.syncTo(player);
			return false;
		}
		var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(target.position());
			level.addFreshEntity(bolt);
		}
		int color = weapon.alignment() == ArtifactAlignment.DARKNESS ? 0x6C2383 : 0xFFF2B2;
		PowerFx.rune(level, target.position().add(0.0, 0.08, 0.0), 1.6, color, 20, 0.0);
		PowerFx.burst(level, target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 6, 0.25, 0.03);
		PowerFx.sound(level, target.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6F,
				weapon.alignment() == ArtifactAlignment.DARKNESS ? 0.75F : 1.25F);
		ServerMagicCasts.commit(magic, player);
		PowersPackets.syncTo(player);
		return true;
	}

	public static void forget(UUID playerId) {
		RATE_LIMITER.forget(playerId);
	}

	public static void clear() {
		RATE_LIMITER.clear();
	}
}
