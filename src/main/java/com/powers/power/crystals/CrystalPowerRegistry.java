package com.powers.power.crystals;

import com.powers.PowersItems;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.network.PowersPackets;
import com.powers.util.PowerMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * the crystal tier of powers - a rank above regular steve powers with
 * game-changing abilities that can turn a fight in an instant; never handed
 * out by the rainbow and never assigned randomly, the only way to hold one
 * is to craft the crystal itself
 */
public final class CrystalPowerRegistry {
	// crystal item -> the ability bound to it
	private static final Map<Item, Ability> POWERS = new HashMap<>();

	private CrystalPowerRegistry() {
	}

	public static void initialize() {
		// red, yellow and violet stay deliberately inert until their lore is defined
		POWERS.put(PowersItems.ORANGE_CRYSTAL, new CreativityManifestationAbility());
		POWERS.put(PowersItems.GREEN_CRYSTAL, new SpaceTimeAbility());
		POWERS.put(PowersItems.BLUE_CRYSTAL, new DreamwalkingAbility());
		POWERS.put(PowersItems.INDIGO_CRYSTAL, new MiddleworldAbility());
		POWERS.put(PowersItems.LIGHT_CRYSTAL, new LightCrystalAbility());
		POWERS.put(PowersItems.DARK_CRYSTAL, new DarkCrystalAbility());
		// the infected rainbow crystal is intentionally inert for now
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
		// not ready yet - tell the player how long is left
		if (!ActivationCooldowns.isReady(player, ability)) {
			PowerMessages.send(player, "ability.powers.cooldown", 4,
					seconds(ActivationCooldowns.remainingTicks(player, ability)));
			return false;
		}
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		// pay the energy up front, then give it back if the ability itself failed
		if (!data.spendEnergy(player, ability)) return false;
		boolean activated = ability.activate(player, data);
		if (!activated) {
			data.refundEnergy(ability);
			PowerMessages.send(player, "crystal.powers.unavailable", 4);
		} else {
			ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
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
}
