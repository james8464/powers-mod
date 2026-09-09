package com.powers.power.abilities;

import com.powers.PowersMod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Owns finite Ice brittleness modifiers without retaining entity instances. */
public final class BrittleArmorManager {
	private static final Identifier ARMOUR_ID = PowersMod.id("ice_brittle_armour");
	private static final Identifier TOUGHNESS_ID = PowersMod.id("ice_brittle_toughness");
	private static final Map<UUID, Long> EXPIRIES = new HashMap<>();

	private BrittleArmorManager() {
	}

	/** Applies or refreshes one finite armour fracture on a live target. */
	public static void apply(LivingEntity target) {
		if (!target.isAlive()) return;
		long now = target.level().getServer().getTickCount();
		EXPIRIES.compute(target.getUUID(), (id, expiry) ->
				IceLifecycleRules.refreshExpiry(expiry == null ? now : expiry, now));
		applyModifier(target.getAttribute(Attributes.ARMOR), ARMOUR_ID);
		applyModifier(target.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID);
	}

	/** Successful fire damage consumes brittleness after that incoming hit resolves. */
	public static boolean breakWithFire(LivingEntity target, boolean fireDamage) {
		if (!IceLifecycleRules.fireBreaksBrittleArmour(fireDamage)
				|| !EXPIRIES.containsKey(target.getUUID())) return false;
		clear(target);
		return true;
	}

	/** Removes expired or unloaded transient ownership once per server tick. */
	public static void tick(MinecraftServer server) {
		long now = server.getTickCount();
		var iterator = EXPIRIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Long> entry = iterator.next();
			LivingEntity target = find(server, entry.getKey());
			if (target == null || !target.isAlive()) {
				iterator.remove();
				continue;
			}
			if (IceLifecycleRules.expired(now, entry.getValue())) {
				removeModifiers(target);
				iterator.remove();
			}
		}
	}

	/** Clears one entity at death or another explicit lifecycle boundary. */
	public static void clear(LivingEntity target) {
		EXPIRIES.remove(target.getUUID());
		removeModifiers(target);
	}

	/** Clears all loaded modifiers before a server epoch ends. */
	public static void clearAll(MinecraftServer server) {
		for (UUID targetId : java.util.List.copyOf(EXPIRIES.keySet())) {
			LivingEntity target = find(server, targetId);
			if (target != null) removeModifiers(target);
		}
		EXPIRIES.clear();
	}

	/** Diagnostics expose ownership without leaking mutable state. */
	public static boolean active(UUID targetId) {
		return EXPIRIES.containsKey(targetId);
	}

	private static void applyModifier(AttributeInstance attribute, Identifier id) {
		if (attribute == null || attribute.hasModifier(id)) return;
		attribute.addTransientModifier(new AttributeModifier(id,
				IceLifecycleRules.armourMultiplier(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void removeModifiers(LivingEntity target) {
		AttributeInstance armour = target.getAttribute(Attributes.ARMOR);
		AttributeInstance toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
		if (armour != null) armour.removeModifier(ARMOUR_ID);
		if (toughness != null) toughness.removeModifier(TOUGHNESS_ID);
	}

	private static LivingEntity find(MinecraftServer server, UUID targetId) {
		for (var level : server.getAllLevels()) {
			var entity = level.getEntity(targetId);
			if (entity instanceof LivingEntity living) return living;
		}
		return null;
	}
}
