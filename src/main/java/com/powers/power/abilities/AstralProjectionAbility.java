package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.BodyReturnFallbackRules;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Astral Projection: your spirit leaves your body and you scout around as an
 * untouchable ghost, yanked back if you drift more than 150 blocks from home.
 */
public class AstralProjectionAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("astral_projection");
	// A finite thirty-second lease prevents abandoned projection ownership.
	private static final int DURATION = 600;
	// The hard leash bounds observation without loading remote chunks.
	private static final double RADIUS = 150.0;
	private static final Map<UUID, Projection> ACTIVE = new HashMap<>();

	private record Projection(ResourceKey<Level> dimension, Vec3 origin,
			GameType gameMode, CastSource castSource, long endsAt, double radius) {}

	public AstralProjectionAbility() {
		super(POWER_ID,
				Component.translatable("ability.powers.astral_projection"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (ACTIVE.containsKey(player.getUUID())) {
			// activating again while projecting ends it early and returns home
			end(player, ACTIVE.remove(player.getUUID()));
			return true;
		}

		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.position();
		if (!BodyProxyManager.start(player, BodyProxyKind.ASTRAL)) return false;
		Projection projection = new Projection(level.dimension(), origin, player.gameMode(),
				CastScalingContext.currentSource(),
				level.getServer().getTickCount() + scaledDuration(player, DURATION),
				scaledRange(player, RADIUS));
		ACTIVE.put(player.getUUID(), projection);
		player.setGameMode(GameType.SPECTATOR);
		player.teleport(new TeleportTransition(level,
				initialSpiritPosition(origin, player.getLookAngle()), Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
		PowerFx.rune(level, origin, 1.8, 0x7C4DFF, 28, 0.0);
		PowerFx.burst(level, origin.add(0, 1, 0), com.powers.PowersParticles.MOTE, 24, 0.8, 0.03);
		PowerFx.sound(level, origin, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
		PowerMessages.send(player, "ability.powers.astral_started", 3);
		return true;
	}

	/** Starts the camera beyond and above the vulnerable body instead of inside its skin. */
	static Vec3 initialSpiritPosition(Vec3 origin, Vec3 look) {
		Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
		if (horizontal.lengthSqr() < 1.0E-8) horizontal = new Vec3(0.0, 0.0, 1.0);
		return origin.add(horizontal.normalize().scale(1.25)).add(0.0, 0.65, 0.0);
	}

	public static boolean isActive(UUID player) {
		return ACTIVE.containsKey(player);
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			Projection projection = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			boolean sourceOwned = player != null && ServerCastLifecycle.mayContinue(
					player, projection.castSource(), ownsPower(player));
			if (!MagicUseGate.ongoingAllowed(player) || !sourceOwned) {
				if (player != null && player.isAlive()) end(player, projection);
				else if (player != null) {
					com.powers.power.travel.TravelChunkLoader.cancel(server, player.getUUID());
					PowersMod.cancelDelayedTasks(player.getUUID());
					com.powers.power.PowerAbilityRuntime.deactivateToggles(player);
					BodyProxyManager.discardOnDeath(player);
				}
				it.remove();
				continue;
			}
			if (now >= projection.endsAt()
					|| !player.level().dimension().equals(projection.dimension())) {
				// End expired or displaced projections so remote ownership cannot persist.
				end(player, projection);
				it.remove();
				continue;
			}
			if (player.position().distanceToSqr(projection.origin()) > projection.radius() * projection.radius()) {
				// Enforce the hard leash without loading or searching a replacement destination.
				PowerFx.sound((ServerLevel) player.level(), player.position(), SoundEvents.ENDERMAN_TELEPORT, 0.75f, 0.75f);
				PowerFx.rune((ServerLevel) player.level(), player.position(), 1.2, 0x7C4DFF, 18, now * 0.125);
				player.teleport(new TeleportTransition((ServerLevel) player.level(), projection.origin(), Vec3.ZERO,
						player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
				PowerMessages.send(player, "ability.powers.astral_boundary", 3);
			}
			if (now % 5 == 0) {
				// every 5 ticks trail soul flames behind the ghost so it's visible
				ServerLevel activeLevel = (ServerLevel) player.level();
				PowerFx.rune(activeLevel, player.position().add(0, 0.5, 0), 1.0, 0x7C4DFF, 16, now * 0.1);
				PowerFx.burst(activeLevel, player.position().add(0, 1, 0),
						ParticleTypes.SOUL_FIRE_FLAME, 2, 0.35, 0.01);
			}
		}
	}

	private static void end(ServerPlayer player, Projection projection) {
		// Restore the recorded body origin before releasing the spectator hold.
		boolean hadAnchor = BodyProxyManager.hasSession(player, BodyProxyKind.ASTRAL);
		boolean returned = BodyProxyManager.returnToBody(player);
		if (BodyReturnFallbackRules.mayUseLegacyFallback(hadAnchor, returned)) {
			player.setGameMode(projection.gameMode());
		} else if (!returned) {
			// Preserve the vulnerable anchor and spectator hold. The player may retry
			// /powers return after satisfying realm requirements; an operator may use
			// /powers recover for broken worlds.
			player.setGameMode(GameType.SPECTATOR);
			PowerMessages.send(player, "realm.powers.return_restricted", 4);
		}
		PowerMessages.send(player, "ability.powers.astral_ended", 3);
	}

	public static void clear(MinecraftServer server, UUID player) {
		Projection projection = ACTIVE.remove(player);
		if (projection == null) return;
		ServerPlayer owner = server.getPlayerList().getPlayer(player);
		if (owner != null && owner.isAlive()) end(owner, projection);
		else if (owner != null) BodyProxyManager.discardOnDeath(owner);
	}

	public static void clearAll(MinecraftServer server) {
		for (UUID player : java.util.List.copyOf(ACTIVE.keySet())) {
			clear(server, player);
		}
		ACTIVE.clear();
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && POWER_ID.equals(power.id())) return true;
		}
		return false;
	}
}
