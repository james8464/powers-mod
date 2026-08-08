package com.powers.progression;

import com.powers.PowersMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reconciles the small set of vanilla attributes owned exclusively by rank perks. */
public final class RankAttributeManager {
	private static final Identifier MOVEMENT_ID = PowersMod.id("rank_movement");
	private static final Identifier RESILIENCE_ID = PowersMod.id("rank_resilience");
	private static final Identifier WARD_STABILITY_ID = PowersMod.id("rank_ward_stability");
	private static final List<OwnedAttribute> OWNED = List.of(
			new OwnedAttribute(MOVEMENT_ID, AttributeKind.MOVEMENT_SPEED),
			new OwnedAttribute(RESILIENCE_ID, AttributeKind.MAX_HEALTH),
			new OwnedAttribute(WARD_STABILITY_ID, AttributeKind.KNOCKBACK_RESISTANCE));
	private static final Map<UUID, List<ModifierSpec>> APPLIED = new HashMap<>();

	private RankAttributeManager() {
	}

	/** Pure desired-state calculation used by reconciliation and regression tests. */
	public static List<ModifierSpec> specifications(RankProfile profile) {
		List<ModifierSpec> result = new ArrayList<>();
		add(result, MOVEMENT_ID, AttributeKind.MOVEMENT_SPEED,
				Math.min(0.15, profile.value(RankPerkType.MOVEMENT) * 0.5));
		add(result, RESILIENCE_ID, AttributeKind.MAX_HEALTH,
				Math.min(0.10, profile.value(RankPerkType.RESISTANCE) * 0.5));
		add(result, WARD_STABILITY_ID, AttributeKind.KNOCKBACK_RESISTANCE,
				Math.min(0.20, profile.value(RankPerkType.WARD_INTEGRITY) * 0.4));
		return List.copyOf(result);
	}

	/** Applies only changed POWERS-owned modifiers and preserves current health ratio. */
	public static void reconcile(ServerPlayer player, RankProfile profile) {
		List<ModifierSpec> desired = specifications(profile);
		if (desired.equals(APPLIED.get(player.getUUID())) && allPresent(player, desired)) return;
		float healthRatio = player.getMaxHealth() <= 0 ? 1.0f : player.getHealth() / player.getMaxHealth();
		removeOwned(player);
		for (ModifierSpec spec : desired) {
			AttributeInstance instance = player.getAttribute(attribute(spec.attribute()));
			if (instance != null) {
				instance.addTransientModifier(new AttributeModifier(
						spec.id(), spec.amount(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
		APPLIED.put(player.getUUID(), desired);
		player.setHealth(Math.min(player.getMaxHealth(), Math.max(0.0f, player.getMaxHealth() * healthRatio)));
	}

	/** Removes only rank-owned modifiers from a live player. */
	public static void clear(ServerPlayer player) {
		removeOwned(player);
		APPLIED.remove(player.getUUID());
	}

	/** Forgets cached desired state after an entity lifecycle transition. */
	public static void forget(UUID playerId) {
		APPLIED.remove(playerId);
	}

	/** Clears all desired-state cache entries during server shutdown. */
	public static void clearAll() {
		APPLIED.clear();
	}

	private static void add(List<ModifierSpec> result, Identifier id, AttributeKind attribute, double amount) {
		if (amount > 0) result.add(new ModifierSpec(id, attribute, amount));
	}

	private static boolean allPresent(ServerPlayer player, List<ModifierSpec> desired) {
		for (ModifierSpec spec : desired) {
			AttributeInstance instance = player.getAttribute(attribute(spec.attribute()));
			if (instance == null || !instance.hasModifier(spec.id())) return false;
		}
		return true;
	}

	private static void removeOwned(ServerPlayer player) {
		for (OwnedAttribute owned : OWNED) {
			AttributeInstance instance = player.getAttribute(attribute(owned.attribute()));
			if (instance != null) instance.removeModifier(owned.id());
		}
	}

	/** Stable identifier, vanilla attribute, and finite multiplier owned by ranks. */
	public record ModifierSpec(Identifier id, AttributeKind attribute, double amount) {
		public ModifierSpec {
			if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid rank modifier");
		}
	}

	/** Registry-independent identity used by pure desired-state calculations. */
	public enum AttributeKind {
		MOVEMENT_SPEED,
		MAX_HEALTH,
		KNOCKBACK_RESISTANCE
	}

	private static Holder<Attribute> attribute(AttributeKind kind) {
		return switch (kind) {
			case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
			case MAX_HEALTH -> Attributes.MAX_HEALTH;
			case KNOCKBACK_RESISTANCE -> Attributes.KNOCKBACK_RESISTANCE;
		};
	}

	private record OwnedAttribute(Identifier id, AttributeKind attribute) {
	}
}
