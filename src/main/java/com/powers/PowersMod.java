package com.powers;

import com.powers.command.PowerCommand;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PassiveEffect;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.abilities.SlowWorldAbility;
import com.powers.power.abilities.TeleportAbility;
import com.powers.power.abilities.FlightAbility;
import com.powers.power.abilities.TimeFreezeToggleAbility;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.CrystalPowerRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PowersMod implements ModInitializer {
	public static final String MOD_ID = "powers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final int PASSIVE_REFRESH_TICKS = 100;

	/** A storm of visual lightning bolts at a position (optionally following a player). */
	private static final class LightningStorm {
		private final ServerLevel level;
		private Vec3 position;
		private final ServerPlayer follow;
		private final int followTicks;
		private final int ticks;
		private int remaining;
		private boolean firstBolt = true;

		private LightningStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks) {
			this.level = level;
			this.position = position;
			this.follow = follow;
			this.ticks = ticks;
			this.followTicks = followTicks;
			this.remaining = ticks;
		}

		private void tick() {
			if (this.follow != null && this.follow.isAlive() && this.follow.level() == this.level
					&& this.remaining > this.ticks - this.followTicks) {
				this.position = this.follow.position();
			}
			if (this.remaining % 2 == 0) {
				LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(this.level, EntitySpawnReason.TRIGGERED);
				if (bolt != null) {
					bolt.setVisualOnly(true);
					bolt.setSilent(!this.firstBolt);
					bolt.setPos(this.position.x, this.position.y, this.position.z);
					this.level.addFreshEntity(bolt);
					this.firstBolt = false;
				}
			}
			this.remaining--;
		}
	}

	private record DelayedTask(int executeAt, Runnable action) {
	}

	private static final List<LightningStorm> STORMS = new ArrayList<>();
	private static final List<DelayedTask> DELAYED = new ArrayList<>();
	private static final Map<UUID, GameType> PREVIOUS_GAMEMODE = new HashMap<>();
	private static final Map<UUID, Boolean> WAS_SLEEPING = new HashMap<>();

	@Override
	public void onInitialize() {
		PowersEffects.initialize();
		PowerRegistry.initialize();
		PowersItems.initialize();
		PowersWeapons.initialize();
		PowersBlocks.initialize();
		ImportedPackItems.initialize();
		PowersCreativeTab.initialize();
		CrystalPowerRegistry.initialize();
		PowersPackets.initialize();
		PowerCommand.register();

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (ForcefieldAbility.protects(entity)) return false;
			if (AmethystDampening.isDampened(entity) && source.getEntity() instanceof ServerPlayer) return false;
			String dim = entity.level().dimension().identifier().toString();
			return !dim.equals("powers:dark_realm") && !dim.equals("powers:light_realm");
		});

		// First login: assign three random powers that persist for good.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			PlayerPowers.get(player).assignRandom(player, false);
			PowersPackets.syncTo(player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			PREVIOUS_GAMEMODE.remove(player.getUUID());
			WAS_SLEEPING.remove(player.getUUID());
			TeleportAbility.clearMarking(player);
			FlightAbility.clear(player.getUUID());
			TimeFreezeToggleAbility.clear(player.getUUID());
			DimensionalAnchorAbility.clear(player.getUUID());
			ForcefieldAbility.clear(player.getUUID());
			VesselPossessionAbility.clear(player.getUUID());
			AstralProjectionAbility.clear(player.getUUID());
			DreamwalkingAbility.clear(player.getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STORMS.clear();
			DELAYED.clear();
			PREVIOUS_GAMEMODE.clear();
			WAS_SLEEPING.clear();
			TeleportAbility.clearAllMarking();
			TimeFreezeToggleAbility.clearAll();
			DimensionalAnchorAbility.clearAll();
			ForcefieldAbility.clearAll();
			VesselPossessionAbility.clearAll();
			AstralProjectionAbility.clearAll();
			EnergyDrainAbility.clearAll();
			SpaceTimeAbility.clearAll();
			DreamwalkingAbility.clearAll();
		});

		// Passives are re-applied on a schedule so they never expire; toggle
		// abilities are re-asserted every few ticks (e.g. flight); time stops,
		// lightning storms and delayed actions advance every tick.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			if (tick % PASSIVE_REFRESH_TICKS == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					refreshPassives(player);
					PowersPackets.syncTo(player);
				}
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				enforceRealmGamemode(player);
				PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
				boolean sleeping = player.isSleeping();
				boolean wasSleeping = WAS_SLEEPING.getOrDefault(player.getUUID(), false);
				WAS_SLEEPING.put(player.getUUID(), sleeping);
				if (wasSleeping && !sleeping) {
					data.restoreEnergy();
					PowersPackets.syncTo(player);
				} else if (tick % 20 == 0 && data.regenerateEnergy(1)) {
					PowersPackets.syncTo(player);
				}
			}
			if (tick % 5 == 0) {
				ForcefieldAbility.tickAll(server);
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					AmethystDampening.update(player);
					tickToggles(player);
					tickAuras(player, tick);
				}
			}
			if (tick % 20 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					drainToggleEnergy(player);
				}
			}
			SlowWorldAbility.tickAll();
			VesselPossessionAbility.tickAll(server);
			AstralProjectionAbility.tickAll(server);
			EnergyDrainAbility.tickAll(server);
			SpaceTimeAbility.tickAll(server);
			DreamwalkingAbility.tickAll(server);
			CrystalPowerRegistry.tick();
			TeleportAbility.tickMarking();
			tickStorms();
			tickDelayed(tick);
		});

		LOGGER.info("POWERS framework initialized with {} power(s)", PowerRegistry.getAll().size());
	}

	/** Starts a storm of visual lightning lasting {@code ticks} ticks. */
	public static void startStorm(ServerLevel level, Vec3 position, int ticks) {
		startStorm(level, position, null, ticks, 0);
	}

	/**
	 * Starts a storm that follows the given player while it lasts, or only
	 * for the first {@code followTicks} ticks of it.
	 */
	public static void startStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks) {
		STORMS.add(new LightningStorm(level, position, follow, ticks, followTicks));
	}

	/** Runs {@code action} once after {@code ticks} server ticks. */
	public static void scheduleDelayed(MinecraftServer server, int ticks, Runnable action) {
		DELAYED.add(new DelayedTask(server.getTickCount() + ticks, action));
	}

	private static void tickStorms() {
		for (LightningStorm storm : new ArrayList<>(STORMS)) {
			storm.tick();
			if (storm.remaining <= 0) {
				STORMS.remove(storm);
			}
		}
	}

	private static void tickDelayed(int tick) {
		for (DelayedTask task : new ArrayList<>(DELAYED)) {
			if (tick >= task.executeAt()) {
				DELAYED.remove(task);
				task.action().run();
			}
		}
	}

	private static void refreshPassives(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) {
				continue;
			}
			for (PassiveEffect passive : power.passives()) {
				player.addEffect(new MobEffectInstance(passive.effect(), PASSIVE_REFRESH_TICKS * 3,
						passive.amplifier(), true, false));
			}
		}
	}

	private static void enforceRealmGamemode(ServerPlayer player) {
		if (TeleportAbility.MARKING.containsKey(player.getUUID())
				|| AstralProjectionAbility.isActive(player.getUUID())) return;
		String dim = player.level().dimension().identifier().getPath();
		boolean inRealm = dim.equals("dark_realm") || dim.equals("light_realm") || dim.equals("middleworld");
		UUID id = player.getUUID();
		if (inRealm) {
			if (!PREVIOUS_GAMEMODE.containsKey(id)) {
				PREVIOUS_GAMEMODE.put(id, player.gameMode());
			}
			if (player.gameMode() != GameType.ADVENTURE) {
				player.setGameMode(GameType.ADVENTURE);
			}
		} else if (PREVIOUS_GAMEMODE.containsKey(id)) {
			GameType prev = PREVIOUS_GAMEMODE.remove(id);
			if (player.gameMode() == GameType.ADVENTURE) {
				player.setGameMode(prev);
			}
		}
	}

	private static void tickToggles(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) {
				continue;
			}
			Ability ability = power.ability();
			if (ability != null && ability.isToggle() && data.isToggleActive(power.id().toString())) {
				ability.tickActive(player, data);
			}
		}
	}

	private static void drainToggleEnergy(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null || power.ability() == null || !power.ability().isToggle()
					|| !data.isToggleActive(power.id().toString())) continue;
			int cost = com.powers.power.PowerEnergy.ongoingCost(power.ability());
			if (cost > 0 && !data.consumeEnergy(cost)) {
				power.ability().activateToggleOff(player, data);
				data.setToggleActive(player, power.id().toString(), false);
			}
		}
	}

	/** Drifting colored motes around each player, one hue per assigned power. */
	private static void tickAuras(ServerPlayer player, int tick) {
		ServerLevel level = (ServerLevel) player.level();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) {
				continue;
			}
			int rgb = power.id().getPath().equals("flight")
					? com.powers.fx.PowerFx.rainbow(tick, 6)
					: power.color() & 0xFFFFFF;
			Vec3 pos = player.getEyePosition().add(
					(level.getRandom().nextDouble() - 0.5) * 0.8,
					(level.getRandom().nextDouble() - 0.5) * 0.8,
					(level.getRandom().nextDouble() - 0.5) * 0.8);
			com.powers.fx.PowerFx.coloredBurst(level, pos, rgb, 1, 0.02);
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
