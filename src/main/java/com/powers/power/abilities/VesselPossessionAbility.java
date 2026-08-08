package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * vessel possession - take over another player for 10 seconds (200 ticks):
 * you watch through their eyes while their body keeps playing along
 */
public class VesselPossessionAbility extends Ability {
	// 10 seconds of possession
	private static final int POSSESS_TICKS = 200;
	private record Possession(ServerPlayer owner, ServerPlayer target, long endsAt) {}
	// one possession per owner uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, Possession> POSSESSING = new HashMap<>();

	public VesselPossessionAbility() {
		super(PowersMod.id("vessel_possession"),
				Component.translatable("ability.powers.vessel_possession"),
				600, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// already possessing someone - a second cast would strand the first target's camera
		if (POSSESSING.containsKey(player.getUUID())) return false;

		LivingEntity target = PowerTargeting.findLivingTarget(player, scaledRange(player, 32.0));
		// must be another player, not yourself
		if (!(target instanceof ServerPlayer targetSP) || targetSP == player) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}
		// amethyst-dampened players are protected from possession
		if (AmethystDampening.isDampened(targetSP)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (!PowerProtection.mayPossess(player, targetSP)) {
			PowerMessages.send(player, "powers.packet.consent_denied", 1, targetSP.getName().getString());
			return false;
		}
		if (!BodyProxyManager.start(player, BodyProxyKind.POSSESSION)) return false;

		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		POSSESSING.put(player.getUUID(), new Possession(player, targetSP,
				server.getTickCount() + scaledDuration(player, POSSESS_TICKS)));
		// watch the world through the target's eyes
		player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
		player.setCamera(targetSP);
		ServerLevel level = (ServerLevel) player.level();
		com.powers.fx.PowerFx.beam(level, player.getEyePosition(), targetSP.getEyePosition(),
				net.minecraft.core.particles.ParticleTypes.ENCHANT, 14);
		com.powers.fx.PowerFx.burst(level, targetSP.position().add(0, 1, 0),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18, 0.5, 0.01);
		com.powers.fx.PowerFx.sound(level, targetSP.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
		com.powers.fx.PowerFx.rune(level, player.position(), 1.5, 0xC27CFF, 22, 0.0);
		com.powers.fx.PowerFx.rune(level, targetSP.position(), 1.5, 0x8FE9FF, 22, Math.PI);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = POSSESSING.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			Possession possession = entry.getValue();
			boolean targetOnline = server.getPlayerList().getPlayer(possession.target().getUUID()) == possession.target();
			// end early if the owner or the target dies or logs off, or when time runs out
			if (owner == null || !owner.isAlive() || !possession.target().isAlive()
					|| !targetOnline || now >= possession.endsAt()
					|| AmethystDampening.isDampened(possession.target())
					|| !PowerProtection.mayPossess(possession.owner(), possession.target())) {
				// reset the owner's camera before dropping the possession
				if (owner != null) end(owner);
				it.remove();
			} else if (now % 20 == 0 && owner.level() == possession.target().level()) {
				com.powers.fx.PowerFx.clash((ServerLevel) owner.level(), owner.getEyePosition(),
						possession.target().getEyePosition(), 0xC27CFF, 0x8FE9FF);
			}
		}
	}

	/** Ends any possession by the given player and resets their camera, used on disconnect. */
	public static void clear(ServerPlayer owner) {
		if (POSSESSING.remove(owner.getUUID()) != null) {
			end(owner);
		}
	}

	private static void end(ServerPlayer owner) {
		owner.setCamera(null);
		BodyProxyManager.returnToBody(owner);
	}

	public static void clearAll() {
		for (Possession possession : POSSESSING.values()) end(possession.owner());
		POSSESSING.clear();
	}
}
