package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.progression.PowerScalingService;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.thermal.ThermalBlockPlan;
import com.powers.thermal.ThermalInfluence;
import com.powers.thermal.ThermalWorldService;
import com.powers.fx.VisualScarRules;
import com.powers.fx.VisualScarService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ice Manipulation: a freezing beam that hurts whatever you hit and coats
 * the ground in ice. Freezes water into ice, lava into obsidian, and adds
 * snow on the way past.
 */
public class IceManipulationAbility extends Ability {
	public IceManipulationAbility() {
		super(PowersMod.id("ice_manipulation"),
				Component.translatable("ability.powers.ice_manipulation"),
				100, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		double range = PowerScalingService.range(player, "ice_manipulation", 32.0);
		Vec3 end = origin.add(look.scale(range));
		HitResult hit = PowerTargeting.raycast(player, range);
		if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();
		Vec3 dir = end.subtract(origin).normalize();
		double dist = origin.distanceTo(end);

		Map<BlockPos, ThermalBlockPlan> mutations = new LinkedHashMap<>();
		for (double d = 0; d < dist; d += 0.5) {
			Vec3 point = origin.add(dir.scale(d));
			BlockPos pos = BlockPos.containing(point);
			BlockState state = level.getBlockState(pos);
			var direct = ThermalWorldService.plan(level, pos, ThermalInfluence.ICE);
			if (direct.isPresent()) {
				mutations.putIfAbsent(pos.immutable(), direct.get());
			} else if (!state.isAir() && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
					&& d < dist - 1.0
					&& level.getBlockState(pos.above()).isAir()) {
				ThermalWorldService.plan(level, pos.above(), ThermalInfluence.ICE)
						.ifPresent(plan -> mutations.putIfAbsent(pos.above().immutable(), plan));
			}
		}
		// The authorized set is the applied set: later samples cannot reinterpret
		// an earlier fluid mutation as a new solid/snow candidate.
		for (ThermalBlockPlan mutation : mutations.values()) {
			if (!ThermalWorldService.authorized(player, level, mutation)) return false;
		}

		if (hit instanceof net.minecraft.world.phys.EntityHitResult entHit
				&& entHit.getEntity() instanceof LivingEntity target) {
			// looking at a protected entity means the cast fails and energy is refunded
			if (AmethystDampening.isDampened(target)
					|| !PowerProtection.mayHarm(player, target)
					|| SpellFieldManager.isSanctuaryProtected(level, target)) return false;
			// 8 damage that scales with skill, a heavy slow, weakness and a deep freeze
			target.hurtServer(level, PowerDamage.source(player),
					PowerScalingService.damage(player, "ice_manipulation", 8.0f));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS,
					scaledDuration(player, 120), 4, false, true));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS,
					scaledDuration(player, 100), 1, false, true));
			target.setTicksFrozen(scaledDuration(player, 160));
		}

		BlockPos scarSupport = hit instanceof BlockHitResult blockHit
				? blockHit.getBlockPos() : BlockPos.containing(end).below();
		Direction scarFace = hit instanceof BlockHitResult blockHit
				? blockHit.getDirection() : Direction.UP;
		VisualScarService.request(level, player, scarSupport, scarFace,
				VisualScarRules.Impact.ICE, Long.hashCode(level.getServer().getTickCount()));
		for (ThermalBlockPlan mutation : mutations.values()) {
			ThermalWorldService.commit(level, mutation);
		}

		com.powers.fx.PowerFx.beam(level, origin, end,
				com.powers.fx.PowerFx.dust(0x81D4FA, 1.05F), 28);
		com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PLAYER_HURT_FREEZE, 1.2f, 0.9f);
		return true;
	}
}
