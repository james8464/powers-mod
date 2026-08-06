package com.powers.power.crystals;

import com.powers.PowersItems;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerEnergy;
import com.powers.power.AmethystDampening;
import com.powers.network.PowersPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * The crystal tier of powers. These are a rank above regular Steve powers:
 * game-changing abilities that can turn a fight in an instant. They are
 * never handed out by the Rainbow Crystal and never assigned randomly -
 * the only way to hold one is to craft the crystal itself.
 */
public final class CrystalPowerRegistry {
	private static final Map<Item, Ability> POWERS = new HashMap<>();

	private CrystalPowerRegistry() {
	}

	public static void initialize() {
		// Red, Yellow, and Violet remain deliberately inert until their lore is defined.
		POWERS.put(PowersItems.ORANGE_CRYSTAL, new CreativityManifestationAbility());
		POWERS.put(PowersItems.GREEN_CRYSTAL, new SpaceTimeAbility());
		POWERS.put(PowersItems.BLUE_CRYSTAL, new DreamwalkingAbility());
		POWERS.put(PowersItems.INDIGO_CRYSTAL, new MiddleworldAbility());
		POWERS.put(PowersItems.LIGHT_CRYSTAL, new LightCrystalAbility());
		POWERS.put(PowersItems.DARK_CRYSTAL, new DarkCrystalAbility());
		// The Infected Rainbow Crystal is intentionally inert for now.
	}

	/** The ability bound to a crystal item, or null. */
	public static Ability get(Item item) {
		return POWERS.get(item);
	}

	/**
	 * Activates the crystal's power. Called from the item use on the server.
	 * Returns false (and starts nothing) if the power is on cooldown.
	 */
	public static boolean tryActivate(ServerPlayer player, Item item) {
		Ability ability = POWERS.get(item);
		if (ability == null) {
			return false;
		}
		AmethystDampening.update(player);
		if (AmethystDampening.isDampened(player)) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("amethyst.powers.suppressed"));
			return false;
		}
		if (SpaceTimeAbility.isFrozen(player)) return false;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (!data.spendEnergy(player, ability)) return false;
		boolean activated = ability.activate(player, data);
		if (!activated) {
			data.refundEnergy(PowerEnergy.cost(ability));
		}
		PowersPackets.syncTo(player);
		return activated;
	}

	/** Advances every ongoing crystal effect; called every server tick. */
	public static void tick() {
		ChronoStopAbility.tickStops();
		InfernoAbility.tickAll();
		SoulLinkAbility.tickAll();
	}

	public static Map<Item, Ability> getAll() {
		return new HashMap<>(POWERS);
	}
}
