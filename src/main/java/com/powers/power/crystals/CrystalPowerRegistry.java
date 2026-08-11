package com.powers.power.crystals;

import com.powers.PowersItems;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.network.PowersPackets;
import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.CastTransaction;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.player.EnergyPaymentSnapshot;
import com.powers.util.PowerMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * the crystal tier of powers - a rank above regular steve powers with
 * game-changing abilities that can turn a fight in an instant; never handed
 * out by the rainbow and never assigned randomly, the only way to hold one
 * is to obtain the crystal item through server-authored progression; recipes
 * are intentionally deferred until the mod author supplies them
 */
public final class CrystalPowerRegistry {
	// crystal item -> the ability bound to it
	private static final Map<Item, Ability> POWERS = new HashMap<>();
	private static final Map<String, Ability> ALL_ABILITIES = new LinkedHashMap<>();
	private static final List<ModeCrystalAbility> MODE_CRYSTALS = new ArrayList<>();

	private CrystalPowerRegistry() {
	}

	public static void initialize() {
		POWERS.clear();
		ALL_ABILITIES.clear();
		MODE_CRYSTALS.clear();
		for (var entry : CrystalAbilityCatalog.defaults().entrySet()) {
			List<Ability> abilities = entry.getValue().stream()
					.map(id -> ALL_ABILITIES.computeIfAbsent(id, CrystalPowerRegistry::createAbility)).toList();
			Ability binding;
			if (abilities.size() == 1) {
				binding = abilities.getFirst();
			} else {
				ModeCrystalAbility convergence = new ModeCrystalAbility(entry.getKey(), abilities);
				MODE_CRYSTALS.add(convergence);
				binding = convergence;
			}
			POWERS.put(item(entry.getKey()), binding);
		}
	}

	private static Item item(String crystal) {
		return switch (crystal) {
			case "red_crystal" -> PowersItems.RED_CRYSTAL;
			case "orange_crystal" -> PowersItems.ORANGE_CRYSTAL;
			case "yellow_crystal" -> PowersItems.YELLOW_CRYSTAL;
			case "green_crystal" -> PowersItems.GREEN_CRYSTAL;
			case "blue_crystal" -> PowersItems.BLUE_CRYSTAL;
			case "indigo_crystal" -> PowersItems.INDIGO_CRYSTAL;
			case "violet_crystal" -> PowersItems.VIOLET_CRYSTAL;
			case "rainbow_crystal" -> PowersItems.RAINBOW_CRYSTAL;
			case "light_crystal" -> PowersItems.LIGHT_CRYSTAL;
			case "dark_crystal" -> PowersItems.DARK_CRYSTAL;
			default -> throw new IllegalArgumentException("Unknown crystal binding: " + crystal);
		};
	}

	private static Ability createAbility(String ability) {
		return switch (ability) {
			case "inferno" -> new InfernoAbility();
			case "creativity_manifestation" -> new CreativityManifestationAbility();
			case "clone_swarm" -> new CloneSwarmAbility();
			case "size_shift" -> new SizeShiftAbility();
			case "life_bloom" -> new LifeBloomAbility();
			case "dreamwalking" -> new DreamwalkingAbility();
			case "middleworld" -> new MiddleworldAbility();
			case "soul_link" -> new SoulLinkAbility();
			case "chrono_stop" -> new ChronoStopAbility();
			case "light_crystal" -> new LightCrystalAbility();
			case "dark_crystal" -> new DarkCrystalAbility();
			default -> throw new IllegalArgumentException("Unknown crystal ability: " + ability);
		};
	}

	/** The ability bound to a crystal item, or null if the crystal is inert. */
	public static Ability get(Item item) {
		return POWERS.get(item);
	}

	/** Resolves one underlying crystal action for artifacts without exposing a mode wrapper. */
	public static Ability getAbility(String abilityId) {
		return ALL_ABILITIES.get(abilityId);
	}

	/** Immutable view of every unique underlying crystal action. */
	public static Map<String, Ability> allAbilities() {
		return Map.copyOf(ALL_ABILITIES);
	}

	/**
	 * Activates the crystal's power from its item use on the server; refuses
	 * the cast and returns false while on cooldown, dampened or time-frozen.
	 */
	public static boolean tryActivate(ServerPlayer player, Item item) {
		Ability ability = POWERS.get(item);
		if (ability == null) {
			return false;
		}
		// Crouch-use only selects a mode. It does not cast, spend energy, or
		// start a cooldown, so players can prepare a crystal before a fight.
		if (ability.isSelectionAction(player)) {
			return ability.activate(player, PlayerPowers.get(player));
		}
		String actionId = ability instanceof ModeCrystalAbility convergence
				? convergence.selectedActionId(player) : ability.id().getPath();
		if (!MagicUseGate.passes(player, true, actionId)) return false;
		// not ready yet - tell the player how long is left
		if (!ActivationCooldowns.isReady(player, ability)) {
			int remaining = ActivationCooldowns.remainingTicks(player, ability);
			com.powers.knowledge.MagicAttemptReporter.failure(player, actionId,
					com.powers.knowledge.MagicFailureReason.COOLDOWN,
					java.util.Map.of("remaining_ticks", (long) remaining));
			PowerMessages.send(player, "ability.powers.cooldown", 4,
					seconds(remaining));
			return false;
		}
		Ability energyAbility = ability instanceof ModeCrystalAbility convergence
				? convergence.selectedAbility(player) : ability;
		PreparedMagicCast magic = ServerMagicCasts.prepare(player, actionId, CastSource.CRYSTAL);
		if (!magic.allowed()) return false;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int energyCost = com.powers.power.PowerEnergy.cost(player, energyAbility);
		long available = (long) data.energy()
				+ com.powers.item.ArtifactEnergyReservoir.totalStored(player);
		EnergyPaymentSnapshot energy = EnergyPaymentSnapshot.capture(player);
		long previousCooldown = data.cooldownReadyAt(ability.id().toString());
		AtomicReference<MagicPresenceId> presence = new AtomicReference<>();
		CastTransaction.Result result = new CastTransaction()
				.stage(CastTransaction.Phase.VALIDATION, () -> magic.allowed(), () -> { })
				.stage(CastTransaction.Phase.COST, () -> data.spendEnergy(player, energyAbility),
						() -> energy.restore(player))
				.stage(CastTransaction.Phase.EFFECT,
						() -> ServerMagicCasts.execute(magic, () -> ability.activate(player, data)),
						() -> ability.rollbackFailedActivation(player, data))
				.stage(CastTransaction.Phase.COOLDOWN, () -> {
					ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
					return true;
				}, () -> ActivationCooldowns.restore(player, ability, previousCooldown))
				.stage(CastTransaction.Phase.PRESENCE, () -> {
					MagicPresenceId id = ServerMagicCasts.commit(magic, player);
					presence.set(id);
					ability.bindPhysicalPresence(player, data, id);
					return true;
				}, () -> {
					MagicPresenceId id = presence.get();
					if (id != null) MagicRuntime.global().removePresence(id);
				})
				.execute();
		boolean activated = result.committed();
		if (!activated) {
			if (result.failedPhase() == CastTransaction.Phase.COST) {
				com.powers.knowledge.MagicAttemptReporter.failure(player, actionId,
						com.powers.knowledge.MagicFailureReason.INSUFFICIENT_ENERGY,
						java.util.Map.of("required", (long) energyCost, "available", available));
			} else {
				com.powers.knowledge.MagicAttemptReporter.executionFailure(player, actionId);
				PowerMessages.send(player, "crystal.powers.unavailable", 4);
			}
		}
		// push the updated energy and cooldown to the client
		PowersPackets.syncTo(player);
		return activated;
	}

	// round ticks up so even 1 remaining tick shows as 1 second
	private static String seconds(int ticks) {
		return String.valueOf((ticks + 19) / 20);
	}

	/** Advances every ongoing crystal effect; called every server tick. */
	public static void tick(MinecraftServer server) {
		InfernoAbility.tickAll(server);
		SoulLinkAbility.tickAll(server);
	}

	public static void clearSelections(UUID player) {
		MODE_CRYSTALS.forEach(crystal -> crystal.clear(player));
	}

	public static void clearAllSelections() {
		MODE_CRYSTALS.forEach(ModeCrystalAbility::clearAll);
	}
}
