package com.powers.power.crystals;

import com.powers.PowersItems;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.network.PowersPackets;
import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.util.PowerMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * the crystal tier of powers - a rank above regular steve powers with
 * game-changing abilities that can turn a fight in an instant; never handed
 * out by the rainbow and never assigned randomly, the only way to hold one
 * is to craft the crystal itself
 */
public final class CrystalPowerRegistry {
	// crystal item -> the ability bound to it
	private static final Map<Item, Ability> POWERS = new HashMap<>();
	private static final List<ModeCrystalAbility> MODE_CRYSTALS = new ArrayList<>();

	private CrystalPowerRegistry() {
	}

	public static void initialize() {
		POWERS.clear();
		MODE_CRYSTALS.clear();
		for (var entry : CrystalAbilityCatalog.defaults().entrySet()) {
			List<Ability> abilities = entry.getValue().stream().map(CrystalPowerRegistry::createAbility).toList();
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
			case "infected_rainbow_crystal" -> PowersItems.INFECTED_RAINBOW_CRYSTAL;
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
			case "space_time" -> new SpaceTimeAbility(true);
			case "life_bloom" -> new LifeBloomAbility();
			case "dreamwalking" -> new DreamwalkingAbility();
			case "middleworld" -> new MiddleworldAbility();
			case "portal_rift" -> new PortalRiftAbility();
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

	/**
	 * Activates the crystal's power from its item use on the server; refuses
	 * the cast and returns false while on cooldown, dampened or time-frozen.
	 */
	public static boolean tryActivate(ServerPlayer player, Item item) {
		Ability ability = POWERS.get(item);
		if (ability == null) {
			return false;
		}
		AmethystDampening.update(player);
		// amethyst dampening blocks crystal powers and punishes the offender
		if (AmethystDampening.isDampened(player)) {
			AmethystDampening.punish(player);
			return false;
		}
		// frozen by space time you can't use crystal powers at all
		if (SpaceTimeAbility.isFrozen(player)) {
			SpaceTimeAbility.reject(player);
			return false;
		}
		// Crouch-use only selects a mode. It does not cast, spend energy, or
		// start a cooldown, so players can prepare a crystal before a fight.
		if (ability.isSelectionAction(player)) {
			return ability.activate(player, PlayerPowers.get(player));
		}
		// not ready yet - tell the player how long is left
		if (!ActivationCooldowns.isReady(player, ability)) {
			PowerMessages.send(player, "ability.powers.cooldown", 4,
					seconds(ActivationCooldowns.remainingTicks(player, ability)));
			return false;
		}
		String actionId = ability instanceof ModeCrystalAbility convergence
				? convergence.selectedActionId(player) : ability.id().getPath();
		PreparedMagicCast magic = ServerMagicCasts.prepare(player, actionId);
		if (!magic.allowed()) return false;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		// pay the energy up front, then give it back if the ability itself failed
		if (!data.spendEnergy(player, ability)) return false;
		boolean activated = ability.activate(player, data);
		if (!activated) {
			data.refundEnergy(ability);
			PowerMessages.send(player, "crystal.powers.unavailable", 4);
		} else {
			ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
			ServerMagicCasts.commit(magic, player);
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
		ChronoStopAbility.tickStops(server);
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
