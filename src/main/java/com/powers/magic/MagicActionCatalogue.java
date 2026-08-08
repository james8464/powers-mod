package com.powers.magic;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical registry of every action that can participate in a magical
 * collision. Construction is fail-fast so an incomplete or duplicate action
 * prevents server startup instead of silently falling through interaction
 * logic.
 */
public final class MagicActionCatalogue {
	private static final Map<MagicAspect, Integer> COLORS = Map.ofEntries(
			Map.entry(MagicAspect.FLAME, 0xFF5A24),
			Map.entry(MagicAspect.FROST, 0x82E9FF),
			Map.entry(MagicAspect.STORM, 0xFFF59D),
			Map.entry(MagicAspect.FORCE, 0xB9E7FF),
			Map.entry(MagicAspect.MOTION, 0xD7F8FF),
			Map.entry(MagicAspect.GRAVITY, 0x8C66FF),
			Map.entry(MagicAspect.TIME, 0x68E0D5),
			Map.entry(MagicAspect.SPACE, 0x5267D8),
			Map.entry(MagicAspect.MIND, 0xC27CFF),
			Map.entry(MagicAspect.SOUL, 0x8FE9FF),
			Map.entry(MagicAspect.LIFE, 0x78E06B),
			Map.entry(MagicAspect.LIGHT, 0xFFF2B0),
			Map.entry(MagicAspect.DARKNESS, 0x55265F),
			Map.entry(MagicAspect.VOID, 0x241044),
			Map.entry(MagicAspect.PROTECTION, 0x58C7FF),
			Map.entry(MagicAspect.CONCEALMENT, 0x6E7180),
			Map.entry(MagicAspect.CREATION, 0xFF9D42),
			Map.entry(MagicAspect.SUPPRESSION, 0xB36BFF));

	private final Map<MagicActionId, MagicActionDefinition> definitions;

	private MagicActionCatalogue(Collection<MagicActionDefinition> definitions) {
		Map<MagicActionId, MagicActionDefinition> indexed = new LinkedHashMap<>();
		for (MagicActionDefinition definition : definitions) {
			if (indexed.putIfAbsent(definition.id(), definition) != null) {
				throw new IllegalArgumentException("Duplicate magic action: " + definition.id());
			}
		}
		this.definitions = Collections.unmodifiableMap(indexed);
	}

	/** Builds the complete immutable catalogue in stable registration order. */
	public static MagicActionCatalogue defaults() {
		List<MagicActionDefinition> actions = new java.util.ArrayList<>();

		// Innate powers: the identifiers match PowerRegistry and player attachments.
		add(actions, "slow_world", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.TIME, MagicAspect.MOTION);
		add(actions, "time_shift", MagicOrigin.INNATE, MagicDelivery.TRAVEL, MagicIntent.MOVEMENT,
				MagicAspect.SPACE, MagicAspect.TIME);
		add(actions, "shadow_step", MagicOrigin.INNATE, MagicDelivery.TRAVEL, MagicIntent.MOVEMENT,
				MagicAspect.DARKNESS, MagicAspect.MOTION);
		add(actions, "flight", MagicOrigin.INNATE, MagicDelivery.TOGGLE, MagicIntent.MOVEMENT,
				MagicAspect.MOTION);
		add(actions, "elemental_blast", MagicOrigin.INNATE, MagicDelivery.PROJECTILE, MagicIntent.HARM,
				MagicAspect.FLAME, MagicAspect.FROST, MagicAspect.STORM, MagicAspect.GRAVITY);
		add(actions, "starfall", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.HARM,
				MagicAspect.STORM, MagicAspect.LIGHT, MagicAspect.FORCE);
		add(actions, "void_beam", MagicOrigin.INNATE, MagicDelivery.BEAM, MagicIntent.HARM,
				MagicAspect.VOID, MagicAspect.DARKNESS);
		add(actions, "fireball", MagicOrigin.INNATE, MagicDelivery.PROJECTILE, MagicIntent.HARM,
				MagicAspect.FLAME, MagicAspect.FORCE);
		add(actions, "frost_nova", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.FROST, MagicAspect.FORCE);
		add(actions, "lightning_strike", MagicOrigin.INNATE, MagicDelivery.INSTANT, MagicIntent.HARM,
				MagicAspect.STORM);
		add(actions, "ground_slam", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.HARM,
				MagicAspect.FORCE, MagicAspect.GRAVITY);
		add(actions, "speed_burst", MagicOrigin.INNATE, MagicDelivery.INSTANT, MagicIntent.MOVEMENT,
				MagicAspect.MOTION, MagicAspect.FORCE);
		add(actions, "telekinesis", MagicOrigin.INNATE, MagicDelivery.CHANNEL, MagicIntent.CONTROL,
				MagicAspect.FORCE, MagicAspect.MIND);
		add(actions, "energy_beam", MagicOrigin.INNATE, MagicDelivery.BEAM, MagicIntent.HARM,
				MagicAspect.FORCE, MagicAspect.FLAME);
		add(actions, "super_speed", MagicOrigin.INNATE, MagicDelivery.INSTANT, MagicIntent.MOVEMENT,
				MagicAspect.MOTION, MagicAspect.TIME);
		add(actions, "breezy_bash", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.MOTION, MagicAspect.FORCE);
		add(actions, "cozy_campfire", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.FLAME, MagicAspect.LIFE, MagicAspect.PROTECTION);
		add(actions, "invisibility", MagicOrigin.INNATE, MagicDelivery.TOGGLE, MagicIntent.DEFENCE,
				MagicAspect.CONCEALMENT, MagicAspect.MIND);
		add(actions, "time_freeze", MagicOrigin.INNATE, MagicDelivery.TOGGLE, MagicIntent.CONTROL,
				MagicAspect.TIME);
		add(actions, "forcefield", MagicOrigin.INNATE, MagicDelivery.AURA, MagicIntent.DEFENCE,
				MagicAspect.PROTECTION, MagicAspect.FORCE);
		add(actions, "gravity_displacement", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.GRAVITY, MagicAspect.FORCE);
		add(actions, "vessel_possession", MagicOrigin.INNATE, MagicDelivery.PROJECTION, MagicIntent.CONTROL,
				MagicAspect.MIND, MagicAspect.SOUL);
		add(actions, "astral_projection", MagicOrigin.INNATE, MagicDelivery.PROJECTION, MagicIntent.INFORMATION,
				MagicAspect.SOUL, MagicAspect.MIND);
		add(actions, "energy_drain", MagicOrigin.INNATE, MagicDelivery.CHANNEL, MagicIntent.HARM,
				MagicAspect.SOUL, MagicAspect.DARKNESS);
		add(actions, "ice_manipulation", MagicOrigin.INNATE, MagicDelivery.PROJECTILE, MagicIntent.CONTROL,
				MagicAspect.FROST, MagicAspect.CREATION);
		add(actions, "plant_healing_acceleration", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIFE);
		add(actions, "double_health", MagicOrigin.INNATE, MagicDelivery.AURA, MagicIntent.DEFENCE,
				MagicAspect.LIFE, MagicAspect.PROTECTION);

		// Crystal actions: convergence items reuse these underlying action IDs.
		add(actions, "inferno", MagicOrigin.CRYSTAL, MagicDelivery.FIELD, MagicIntent.HARM,
				MagicAspect.FLAME, MagicAspect.STORM);
		add(actions, "clone_swarm", MagicOrigin.CRYSTAL, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.CREATION, MagicAspect.MIND);
		add(actions, "creativity_manifestation", MagicOrigin.CRYSTAL, MagicDelivery.INSTANT,
				MagicIntent.WORLD_INTERACTION, MagicAspect.CREATION, MagicAspect.LIGHT);
		add(actions, "size_shift", MagicOrigin.CRYSTAL, MagicDelivery.AURA, MagicIntent.CONTROL,
				MagicAspect.CREATION, MagicAspect.GRAVITY);
		add(actions, "life_bloom", MagicOrigin.CRYSTAL, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIFE, MagicAspect.LIGHT);
		add(actions, "space_time", MagicOrigin.CRYSTAL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.SPACE, MagicAspect.TIME);
		add(actions, "chrono_stop", MagicOrigin.CRYSTAL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.TIME, MagicAspect.FORCE);
		add(actions, "dreamwalking", MagicOrigin.CRYSTAL, MagicDelivery.PROJECTION, MagicIntent.CONTROL,
				MagicAspect.MIND, MagicAspect.SOUL);
		add(actions, "portal_rift", MagicOrigin.CRYSTAL, MagicDelivery.TRAVEL, MagicIntent.HARM,
				MagicAspect.SPACE, MagicAspect.FORCE);
		add(actions, "middleworld", MagicOrigin.CRYSTAL, MagicDelivery.TRAVEL, MagicIntent.MOVEMENT,
				MagicAspect.SPACE, MagicAspect.MIND);
		add(actions, "soul_link", MagicOrigin.CRYSTAL, MagicDelivery.CHANNEL, MagicIntent.CONTROL,
				MagicAspect.SOUL, MagicAspect.LIFE);
		add(actions, "light_crystal", MagicOrigin.CRYSTAL, MagicDelivery.PROJECTION, MagicIntent.MOVEMENT,
				MagicAspect.LIGHT, MagicAspect.MIND);
		add(actions, "dark_crystal", MagicOrigin.CRYSTAL, MagicDelivery.PROJECTION, MagicIntent.MOVEMENT,
				MagicAspect.DARKNESS, MagicAspect.MIND);

		// Grimoire spells: identifiers match SpellRegistry selections and cooldowns.
		add(actions, "soul_compass", MagicOrigin.SPELL, MagicDelivery.INSTANT, MagicIntent.INFORMATION,
				MagicAspect.SOUL, MagicAspect.MIND);
		add(actions, "tracking_mark", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.INFORMATION,
				MagicAspect.LIGHT, MagicAspect.SOUL);
		add(actions, "weather_sigil", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.STORM, MagicAspect.CREATION);
		add(actions, "dimensional_anchor", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.CONTROL,
				MagicAspect.SPACE, MagicAspect.SUPPRESSION);
		add(actions, "binding_sigil", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.FORCE, MagicAspect.SUPPRESSION);
		add(actions, "anti_portal_field", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.SPACE, MagicAspect.SUPPRESSION);
		add(actions, "kinetic_ward", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.PROTECTION, MagicAspect.MOTION);
		add(actions, "vitality_transfer", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.SUPPORT,
				MagicAspect.LIFE, MagicAspect.SOUL);
		add(actions, "hex", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.HARM,
				MagicAspect.DARKNESS, MagicAspect.MIND);
		add(actions, "concealment_veil", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.CONCEALMENT, MagicAspect.MIND);
		add(actions, "purification_circle", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIFE, MagicAspect.LIGHT, MagicAspect.SUPPRESSION);
		add(actions, "root_binding", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.LIFE, MagicAspect.FORCE);
		add(actions, "sanctuary_growth", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.LIFE, MagicAspect.PROTECTION);
		add(actions, "infernal_seal", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.FLAME, MagicAspect.SUPPRESSION);
		add(actions, "banishment_circle", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.SPACE, MagicAspect.SUPPRESSION);
		add(actions, "controlled_hellfire", MagicOrigin.SPELL, MagicDelivery.PROJECTILE, MagicIntent.HARM,
				MagicAspect.FLAME, MagicAspect.DARKNESS);
		add(actions, "ward_breaking_ritual", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.HARM,
				MagicAspect.SUPPRESSION, MagicAspect.VOID);
		add(actions, "counterspell", MagicOrigin.SPELL, MagicDelivery.INSTANT, MagicIntent.CONTROL,
				MagicAspect.SUPPRESSION, MagicAspect.MIND);
		add(actions, "dispel", MagicOrigin.SPELL, MagicDelivery.INSTANT, MagicIntent.SUPPORT,
				MagicAspect.SUPPRESSION, MagicAspect.LIGHT);
		add(actions, "ritual_amplification", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.SUPPORT,
				MagicAspect.CREATION, MagicAspect.SOUL);

		// Suppression sources remain separate because their priorities differ.
		add(actions, "amethyst_item", MagicOrigin.AMETHYST, MagicDelivery.AURA, MagicIntent.DEFENCE,
				MagicAspect.SUPPRESSION, MagicAspect.PROTECTION);
		add(actions, "amethyst_block", MagicOrigin.AMETHYST, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.SUPPRESSION, MagicAspect.PROTECTION);
		add(actions, "amethyst_ward", MagicOrigin.AMETHYST, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.SUPPRESSION, MagicAspect.PROTECTION);

		return new MagicActionCatalogue(actions);
	}

	/** Returns the definition for an action or {@code null} when the ID is unknown. */
	public MagicActionDefinition definition(MagicActionId id) {
		return definitions.get(Objects.requireNonNull(id, "id"));
	}

	/** Returns all definitions in stable registration order. */
	public Collection<MagicActionDefinition> definitions() {
		return definitions.values();
	}

	/** Returns the immutable subset owned by the requested origin. */
	public List<MagicActionDefinition> byOrigin(MagicOrigin origin) {
		return definitions.values().stream().filter(definition -> definition.origin() == origin).toList();
	}

	private static void add(List<MagicActionDefinition> actions, String id, MagicOrigin origin,
			MagicDelivery delivery, MagicIntent intent, MagicAspect first, MagicAspect... remaining) {
		EnumSet<MagicAspect> aspects = EnumSet.of(first, remaining);
		int primary = COLORS.get(first);
		MagicAspect secondaryAspect = remaining.length == 0 ? first : remaining[0];
		int secondary = COLORS.get(secondaryAspect);
		actions.add(new MagicActionDefinition(new MagicActionId(id), origin, aspects, delivery, intent,
				basePotency(origin, intent), baseRange(delivery), baseDuration(delivery), baseEnergy(origin, intent),
				baseCooldown(origin, delivery), residueTicks(delivery), basePriority(origin),
				new MagicSignature(primary, secondary, id.hashCode(), motif(first), sound(origin, first))));
	}

	private static int basePotency(MagicOrigin origin, MagicIntent intent) {
		int intentValue = switch (intent) {
			case HARM -> 10;
			case CONTROL -> 8;
			case DEFENCE, SUPPORT -> 7;
			case MOVEMENT, INFORMATION, WORLD_INTERACTION -> 5;
		};
		return intentValue + (origin == MagicOrigin.CRYSTAL ? 8 : origin == MagicOrigin.SPELL ? 3 : 0);
	}

	private static double baseRange(MagicDelivery delivery) {
		return switch (delivery) {
			case BEAM, PROJECTILE -> 32.0;
			case FIELD -> 12.0;
			case CHANNEL -> 20.0;
			case TRAVEL, PROJECTION -> 48.0;
			case INSTANT -> 16.0;
			case AURA, TOGGLE -> 8.0;
		};
	}

	private static int baseDuration(MagicDelivery delivery) {
		return switch (delivery) {
			case FIELD, AURA -> 200;
			case CHANNEL -> 80;
			case TOGGLE, PROJECTION -> 600;
			case TRAVEL -> 40;
			case INSTANT, BEAM, PROJECTILE -> 20;
		};
	}

	private static int baseEnergy(MagicOrigin origin, MagicIntent intent) {
		if (origin == MagicOrigin.AMETHYST) return 0;
		int base = origin == MagicOrigin.CRYSTAL ? 55 : origin == MagicOrigin.SPELL ? 20 : 18;
		return base + (intent == MagicIntent.HARM || intent == MagicIntent.CONTROL ? 8 : 0);
	}

	private static int baseCooldown(MagicOrigin origin, MagicDelivery delivery) {
		if (origin == MagicOrigin.AMETHYST || delivery == MagicDelivery.TOGGLE) return 0;
		return origin == MagicOrigin.CRYSTAL ? 1200 : origin == MagicOrigin.SPELL ? 600 : 200;
	}

	private static int residueTicks(MagicDelivery delivery) {
		return switch (delivery) {
			case INSTANT, BEAM, PROJECTILE -> 30;
			case TRAVEL -> 20;
			case CHANNEL -> 60;
			case FIELD, AURA, TOGGLE, PROJECTION -> 100;
		};
	}

	private static int basePriority(MagicOrigin origin) {
		return switch (origin) {
			case INNATE -> 10;
			case SPELL -> 15;
			case CRYSTAL -> 20;
			case AMETHYST -> 25;
		};
	}

	private static String motif(MagicAspect aspect) {
		return aspect.name().toLowerCase(java.util.Locale.ROOT);
	}

	private static String sound(MagicOrigin origin, MagicAspect aspect) {
		return origin.name().toLowerCase(java.util.Locale.ROOT) + "_"
				+ aspect.name().toLowerCase(java.util.Locale.ROOT);
	}
}
