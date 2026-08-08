package com.powers.magic.runtime;

import com.powers.fx.PowerFx;
import com.powers.magic.InteractionContext;
import com.powers.magic.InteractionOutcome;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Minecraft adapter for the pure {@link MagicRuntime}. It derives identity,
 * dimension, position, and time from server state and translates semantic
 * reactions into the existing budgeted audiovisual primitives.
 */
public final class ServerMagicCasts {
	private ServerMagicCasts() {
	}

	/**
	 * Resolves nearby interactions before callers spend energy or start a
	 * cooldown. The supplied ID must come from a server registry, never a packet.
	 */
	public static PreparedMagicCast prepare(ServerPlayer player, String actionId) {
		MagicActionDefinition definition = MagicRuntime.catalogue().definition(new MagicActionId(actionId));
		if (definition == null) {
			throw new IllegalArgumentException("Unregistered server magic action: " + actionId);
		}
		MagicCastContext context = new MagicCastContext(definition, player.getUUID(),
				player.level().dimension().identifier().toString(),
				PresenceAnchor.fixed(player.getX(), player.getY() + 1.0, player.getZ()),
				Math.max(4.0, definition.baseRange()), player.level().getServer().getTickCount(),
				InteractionContext.DEFAULT);
		CastAdjustment adjustment = MagicRuntime.global().beforeCast(context,
				event -> emitReaction((ServerLevel) player.level(), event));
		if (!adjustment.allowed()) {
			player.sendSystemMessage(Component.translatable("ability.powers.interaction_blocked"));
		}
		return new PreparedMagicCast(context, adjustment);
	}

	/** Registers residue after, and only after, successful ability execution. */
	public static MagicPresenceId commit(PreparedMagicCast prepared, ServerPlayer player) {
		MagicCastContext completed = prepared.context().rebased(
				player.level().dimension().identifier().toString(),
				PresenceAnchor.fixed(player.getX(), player.getY() + 1.0, player.getZ()),
				player.level().getServer().getTickCount());
		return MagicRuntime.global().commitCast(completed, prepared.adjustment());
	}

	/** Executes gameplay while its resolved multipliers are visible to the scaling service. */
	public static <T> T execute(PreparedMagicCast prepared, Supplier<T> operation) {
		if (!prepared.allowed()) throw new IllegalStateException("Blocked magic cannot execute");
		return CastScalingContext.with(prepared.adjustment(), operation);
	}

	private static void emitReaction(ServerLevel level, MagicReactionEvent event) {
		Vec3 from = new Vec3(event.cast().anchor().x(), event.cast().anchor().y(), event.cast().anchor().z());
		Vec3 to = new Vec3(event.existing().anchor().x(), event.existing().anchor().y(),
				event.existing().anchor().z());
		Vec3 midpoint = from.add(to).scale(0.5);
		var cue = event.resolution().cue();

		PowerFx.clash(level, from, to, cue.primaryColor(), cue.secondaryColor());
		PowerFx.rune(level, midpoint, 0.7 + cue.intensity() * 0.22, cue.primaryColor(),
				8 + cue.intensity() * 4, Math.floorMod(cue.glyphSeed(), 360) * Math.PI / 180.0);
		PowerFx.coloredBurst(level, midpoint, cue.secondaryColor(), 4 + cue.intensity() * 3,
				0.25 + cue.intensity() * 0.08);
		MagicReactionEffects.apply(level, event, midpoint);
		if (event.resolution().outcome() == InteractionOutcome.TRANSFORM) {
			PowerFx.burst(level, midpoint, ParticleTypes.CLOUD, 8 + cue.intensity() * 2, 0.5, 0.04);
			PowerFx.sound(level, midpoint, SoundEvents.FIRE_EXTINGUISH, 0.9f, 0.75f);
		} else if (event.resolution().blocksFirst()) {
			PowerFx.cancelled(level, from, cue.primaryColor());
		} else if (event.resolution().outcome() == InteractionOutcome.RESONATE
				|| event.resolution().outcome() == InteractionOutcome.AMPLIFY) {
			PowerFx.sound(level, midpoint, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 1.35f);
		}
	}
}
