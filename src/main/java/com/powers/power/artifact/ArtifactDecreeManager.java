package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tick-owned five-second judgement marks with explicit cancellation counterplay. */
public final class ArtifactDecreeManager {
	private static final int MARK_TICKS = 100;
	private static final Map<UUID, Decree> ACTIVE = new HashMap<>();

	private record Decree(UUID targetId, ResourceKey<Level> dimension,
			ArtifactAlignment alignment, long executeAt) {
	}

	private ArtifactDecreeManager() {
	}

	public static boolean mark(ServerPlayer caster, LivingEntity target, ArtifactAlignment alignment) {
		if (target == null || target == caster || !target.isAlive() || caster.level() != target.level()) return false;
		ACTIVE.put(caster.getUUID(), new Decree(target.getUUID(), target.level().dimension(),
				alignment, caster.level().getServer().getTickCount() + MARK_TICKS));
		target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, MARK_TICKS, 0, false, true));
		ServerLevel level = (ServerLevel) caster.level();
		PowerFx.beam(level, caster.getEyePosition(), target.getEyePosition(),
				alignment == ArtifactAlignment.DARKNESS ? ParticleTypes.SOUL_FIRE_FLAME
						: ParticleTypes.END_ROD, 24);
		PowerFx.rune(level, target.position(), 1.8,
				alignment == ArtifactAlignment.DARKNESS ? 0x48105D : 0xFFE89B, 28, 0.0);
		return true;
	}

	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		var iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
			Decree decree = entry.getValue();
			ServerLevel level = server.getLevel(decree.dimension());
			LivingEntity target = level == null ? null : level.getEntity(decree.targetId()) instanceof LivingEntity living
					? living : null;
			if (!valid(caster, target, level)) {
				iterator.remove();
				continue;
			}
			long remaining = decree.executeAt() - server.getTickCount();
			if (remaining > 0) {
				if (remaining % 20 == 0) PowerFx.rune(level, target.position(),
						1.5 + (MARK_TICKS - remaining) * 0.01,
						decree.alignment() == ArtifactAlignment.DARKNESS ? 0x6C2383 : 0xFFF2B2,
						24, remaining * 0.05);
				continue;
			}
			execute(caster, target, level, decree.alignment());
			iterator.remove();
		}
	}

	private static boolean valid(ServerPlayer caster, LivingEntity target, ServerLevel level) {
		return caster != null && caster.isAlive() && target != null && target.isAlive()
				&& caster.level() == level && caster.hasLineOfSight(target)
				&& !AmethystDampening.isDampened(caster) && !AmethystDampening.isDampened(target)
				&& !SpellFieldManager.isSanctuaryProtected(level, target);
	}

	private static void execute(ServerPlayer caster, LivingEntity target, ServerLevel level,
			ArtifactAlignment alignment) {
		boolean ally = caster.isAlliedTo(target);
		if (alignment == ArtifactAlignment.LIGHT && ally) {
			target.heal(Math.max(30.0F, target.getMaxHealth() * 0.45F));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 400, 4, false, true));
		} else {
			int rank = alignment == ArtifactAlignment.DARKNESS
					? PlayerPowers.get(caster).darknessLevel() : PlayerPowers.get(caster).skillLevel();
			target.hurtServer(level, PowerDamage.source(caster), ArtifactDominionRules.decreeDamage(
					alignment, target.getMaxHealth(), target instanceof ServerPlayer, rank));
		}
		PowerFx.burst(level, target.position().add(0.0, 1.0, 0.0),
				alignment == ArtifactAlignment.DARKNESS ? ParticleTypes.SOUL_FIRE_FLAME
						: ParticleTypes.END_ROD, 36, 1.0, 0.16);
		PowerFx.rune(level, target.position(), 4.5,
				alignment == ArtifactAlignment.DARKNESS ? 0x21002E : 0xFFFFFF, 52, Math.PI);
	}

	public static void forget(UUID playerId) {
		ACTIVE.remove(playerId);
	}

	public static void clear() {
		ACTIVE.clear();
	}
}
