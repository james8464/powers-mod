package com.powers.magic;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical registry of every action that can participate in a magical
 * collision. Construction is fail-fast so an incomplete or duplicate action
 * prevents server startup instead of silently falling through interaction
 * logic.
 */
public final class MagicActionCatalogue {
	private static final Map<String, MagicSignificance> SIGNIFICANCE = significanceAssignments();
	private static final Set<String> BESPOKE_PRESENTATION = Set.of(
			"fireball", "lightning_strike", "starbound_dark_lightning", "starbound_light_lightning");
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

		// Innate powers. Size Morph keeps its legacy saved power ID but has a
		// distinct collision identity from the fixed-strength Yellow Crystal rite.
		add(actions, "size_morph", MagicOrigin.INNATE, MagicDelivery.TOGGLE, MagicIntent.CONTROL,
				MagicAspect.CREATION, MagicAspect.GRAVITY);
		add(actions, "time_shift", MagicOrigin.INNATE, MagicDelivery.TRAVEL, MagicIntent.MOVEMENT,
				MagicAspect.SPACE, MagicAspect.TIME);
		add(actions, "flight", MagicOrigin.INNATE, MagicDelivery.TOGGLE, MagicIntent.MOVEMENT,
				MagicAspect.MOTION);
		add(actions, "starfall", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.HARM,
				MagicAspect.STORM, MagicAspect.LIGHT, MagicAspect.FORCE);
		add(actions, "void_beam", MagicOrigin.INNATE, MagicDelivery.BEAM, MagicIntent.HARM,
				MagicAspect.VOID, MagicAspect.DARKNESS);
		add(actions, "fireball", MagicOrigin.INNATE, MagicDelivery.PROJECTILE, MagicIntent.HARM,
				MagicAspect.FLAME, MagicAspect.FORCE);
		add(actions, "lightning_strike", MagicOrigin.INNATE, MagicDelivery.INSTANT, MagicIntent.HARM,
				MagicAspect.STORM);
		add(actions, "thunderclap", MagicOrigin.INNATE, MagicDelivery.FIELD, MagicIntent.HARM,
				MagicAspect.FORCE, MagicAspect.MOTION, MagicAspect.STORM);
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
		add(actions, "chrono_stop", MagicOrigin.CRYSTAL, MagicDelivery.FIELD, MagicIntent.CONTROL,
				MagicAspect.TIME, MagicAspect.FORCE);
		add(actions, "dreamwalking", MagicOrigin.CRYSTAL, MagicDelivery.PROJECTION, MagicIntent.CONTROL,
				MagicAspect.MIND, MagicAspect.SOUL);
		add(actions, "middleworld", MagicOrigin.CRYSTAL, MagicDelivery.TRAVEL, MagicIntent.MOVEMENT,
				MagicAspect.SPACE, MagicAspect.MIND);
		add(actions, "soul_link", MagicOrigin.CRYSTAL, MagicDelivery.CHANNEL, MagicIntent.CONTROL,
				MagicAspect.SOUL, MagicAspect.LIFE);
		add(actions, "light_crystal", MagicOrigin.CRYSTAL, MagicDelivery.PROJECTION, MagicIntent.MOVEMENT,
				MagicAspect.LIGHT, MagicAspect.MIND);
		add(actions, "dark_crystal", MagicOrigin.CRYSTAL, MagicDelivery.PROJECTION, MagicIntent.MOVEMENT,
				MagicAspect.DARKNESS, MagicAspect.MIND);

		// The Shadow Sword's own rites remain artifacts even when its menu invokes
		// an innate or crystal action from the authoritative registries above.
		add(actions, "nightfall_dominion", MagicOrigin.ARTIFACT, MagicDelivery.AURA, MagicIntent.DEFENCE,
				MagicAspect.DARKNESS, MagicAspect.SOUL, MagicAspect.PROTECTION);
		add(actions, "call_hollowed", MagicOrigin.ARTIFACT, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.DARKNESS, MagicAspect.CREATION);
		add(actions, "blight_ground", MagicOrigin.ARTIFACT, MagicDelivery.FIELD,
				MagicIntent.WORLD_INTERACTION, MagicAspect.DARKNESS, MagicAspect.CREATION);
		add(actions, "call_radiant", MagicOrigin.ARTIFACT, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIGHT, MagicAspect.CREATION);
		add(actions, "consecrate_ground", MagicOrigin.ARTIFACT, MagicDelivery.FIELD,
				MagicIntent.WORLD_INTERACTION, MagicAspect.LIGHT, MagicAspect.CREATION);
		add(actions, "covenant_chain", MagicOrigin.ARTIFACT, MagicDelivery.CHANNEL, MagicIntent.SUPPORT,
				MagicAspect.LIGHT, MagicAspect.LIFE, MagicAspect.PROTECTION);
		add(actions, "daybreak_wave", MagicOrigin.ARTIFACT, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIGHT, MagicAspect.LIFE, MagicAspect.FORCE);
		add(actions, "heaven_gate", MagicOrigin.ARTIFACT, MagicDelivery.TRAVEL, MagicIntent.MOVEMENT,
				MagicAspect.LIGHT, MagicAspect.SPACE);
		add(actions, "solar_firmament", MagicOrigin.ARTIFACT, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.LIGHT, MagicAspect.FORCE, MagicAspect.PROTECTION);
		add(actions, "second_dawn", MagicOrigin.ARTIFACT, MagicDelivery.AURA, MagicIntent.DEFENCE,
				MagicAspect.LIGHT, MagicAspect.LIFE, MagicAspect.PROTECTION);
		add(actions, "host_heaven", MagicOrigin.ARTIFACT, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIGHT, MagicAspect.CREATION, MagicAspect.SOUL);
		add(actions, "starbound_dark_lightning", MagicOrigin.ARTIFACT, MagicDelivery.INSTANT,
				MagicIntent.HARM, MagicAspect.STORM, MagicAspect.DARKNESS);
		add(actions, "starbound_light_lightning", MagicOrigin.ARTIFACT, MagicDelivery.INSTANT,
				MagicIntent.HARM, MagicAspect.STORM, MagicAspect.LIGHT);

		// Grimoire spells: identifiers match SpellRegistry selections and cooldowns.
		add(actions, "soul_compass", MagicOrigin.SPELL, MagicDelivery.INSTANT, MagicIntent.INFORMATION,
				MagicAspect.SOUL, MagicAspect.MIND);
		add(actions, "augury", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.INFORMATION,
				MagicAspect.LIGHT, MagicAspect.MIND);
		add(actions, "cartographers_star", MagicOrigin.SPELL, MagicDelivery.INSTANT,
				MagicIntent.INFORMATION, MagicAspect.SPACE, MagicAspect.LIGHT);
		add(actions, "celestial_ruin", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.WORLD_INTERACTION,
				MagicAspect.LIGHT, MagicAspect.CREATION, MagicAspect.FORCE);
		add(actions, "dimensional_anchor", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.CONTROL,
				MagicAspect.SPACE, MagicAspect.SUPPRESSION);
		add(actions, "blood_reading", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.INFORMATION,
				MagicAspect.LIFE, MagicAspect.SOUL);
		add(actions, "grave_recall", MagicOrigin.SPELL, MagicDelivery.INSTANT, MagicIntent.INFORMATION,
				MagicAspect.SOUL, MagicAspect.MIND);
		add(actions, "purification_circle", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.SUPPORT,
				MagicAspect.LIFE, MagicAspect.LIGHT, MagicAspect.SUPPRESSION);
		add(actions, "verdant_tending", MagicOrigin.SPELL, MagicDelivery.FIELD,
				MagicIntent.WORLD_INTERACTION, MagicAspect.LIFE, MagicAspect.CREATION);
		add(actions, "hearth_sanctuary", MagicOrigin.SPELL, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.LIFE, MagicAspect.PROTECTION);
		add(actions, "ward_breaking_ritual", MagicOrigin.SPELL, MagicDelivery.CHANNEL, MagicIntent.HARM,
				MagicAspect.SUPPRESSION, MagicAspect.VOID);
		add(actions, "dispel", MagicOrigin.SPELL, MagicDelivery.INSTANT, MagicIntent.SUPPORT,
				MagicAspect.SUPPRESSION, MagicAspect.LIGHT);

		// Suppression sources remain separate because their priorities differ.
		add(actions, "amethyst_item", MagicOrigin.AMETHYST, MagicDelivery.AURA, MagicIntent.DEFENCE,
				MagicAspect.SUPPRESSION, MagicAspect.PROTECTION);
		add(actions, "amethyst_block", MagicOrigin.AMETHYST, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.SUPPRESSION, MagicAspect.PROTECTION);
		add(actions, "amethyst_ward", MagicOrigin.AMETHYST, MagicDelivery.FIELD, MagicIntent.DEFENCE,
				MagicAspect.SUPPRESSION, MagicAspect.PROTECTION);

		// Persistent realm matter participates even though no player actively casts it.
		add(actions, "darkness_block", MagicOrigin.REALM, MagicDelivery.FIELD, MagicIntent.WORLD_INTERACTION,
				MagicAspect.DARKNESS);
		add(actions, "pure_light_block", MagicOrigin.REALM, MagicDelivery.FIELD, MagicIntent.WORLD_INTERACTION,
				MagicAspect.LIGHT);

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
		MagicSignificance significance = SIGNIFICANCE.get(id);
		if (significance == null) {
			throw new IllegalStateException("Magic action lacks explicit significance: " + id);
		}
		actions.add(new MagicActionDefinition(new MagicActionId(id), origin, aspects, delivery, intent,
				basePotency(origin, intent), baseRange(delivery), baseDuration(delivery), baseEnergy(origin, intent),
				baseCooldown(origin, delivery), residueTicks(delivery), basePriority(origin),
				new MagicSignature(primary, secondary, id.hashCode(), motif(first), sound(origin, first)),
				significance, significance != MagicSignificance.NONE && !BESPOKE_PRESENTATION.contains(id),
				targetContract(id)));
	}

	private static ActionTargetContract targetContract(String id) {
		if (Set.of("soul_link", "light_crystal", "dark_crystal").contains(id)) {
			return ActionTargetContract.PLAYER_PARTICIPANT;
		}
		if (id.equals("energy_drain")) return ActionTargetContract.PLAYER_OR_MOB_FALLBACK;
		if (Set.of("starfall", "void_beam", "fireball", "lightning_strike", "thunderclap",
				"telekinesis", "energy_beam", "breezy_bash", "gravity_displacement",
				"vessel_possession", "ice_manipulation", "plant_healing_acceleration", "inferno",
				"life_bloom", "chrono_stop", "dreamwalking", "covenant_chain", "daybreak_wave",
				"starbound_dark_lightning", "starbound_light_lightning", "dimensional_anchor",
				"blood_reading", "ward_breaking_ritual", "dispel").contains(id)) {
			return ActionTargetContract.ANY_LIVING;
		}
		return ActionTargetContract.NONE;
	}

	private static Map<String, MagicSignificance> significanceAssignments() {
		Map<String, MagicSignificance> values = new LinkedHashMap<>();
		assign(values, MagicSignificance.NONE,
				"amethyst_item", "amethyst_block", "amethyst_ward",
				"darkness_block", "pure_light_block");
		assign(values, MagicSignificance.MINIMAL,
				"fireball", "lightning_strike",
				"starbound_dark_lightning", "starbound_light_lightning");
		assign(values, MagicSignificance.COSMIC,
				"time_freeze", "chrono_stop", "celestial_ruin", "nightfall_dominion",
				"solar_firmament", "host_heaven");
		assign(values, MagicSignificance.RITUAL,
				"soul_compass", "augury", "cartographers_star", "dimensional_anchor",
				"blood_reading", "grave_recall", "purification_circle", "verdant_tending",
				"hearth_sanctuary", "ward_breaking_ritual", "dispel",
				"daybreak_wave", "heaven_gate", "second_dawn");
		assign(values, MagicSignificance.STANDARD,
				"size_morph", "time_shift", "flight", "starfall", "void_beam", "thunderclap", "speed_burst", "telekinesis",
				"energy_beam", "super_speed", "breezy_bash", "invisibility",
				"forcefield", "gravity_displacement", "vessel_possession", "astral_projection",
				"energy_drain", "ice_manipulation", "plant_healing_acceleration", "double_health",
				"inferno", "clone_swarm", "creativity_manifestation", "size_shift", "life_bloom",
				"dreamwalking", "middleworld", "soul_link",
				"light_crystal", "dark_crystal", "call_hollowed", "blight_ground",
				"call_radiant", "consecrate_ground", "covenant_chain");
		return Map.copyOf(values);
	}

	private static void assign(Map<String, MagicSignificance> values, MagicSignificance significance,
			String... ids) {
		for (String id : ids) {
			if (values.putIfAbsent(id, significance) != null) {
				throw new IllegalStateException("Duplicate magic significance assignment: " + id);
			}
		}
	}

	private static int basePotency(MagicOrigin origin, MagicIntent intent) {
		int intentValue = switch (intent) {
			case HARM -> 10;
			case CONTROL -> 8;
			case DEFENCE, SUPPORT -> 7;
			case MOVEMENT, INFORMATION, WORLD_INTERACTION -> 5;
		};
		int originBonus = switch (origin) {
			case INNATE, AMETHYST -> 0;
			case SPELL -> 3;
			case CRYSTAL -> 8;
			case ARTIFACT -> 14;
			case REALM -> 20;
		};
		return intentValue + originBonus;
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
		if (origin == MagicOrigin.AMETHYST || origin == MagicOrigin.REALM) return 0;
		int base = origin == MagicOrigin.CRYSTAL ? 55
				: origin == MagicOrigin.ARTIFACT ? 40 : origin == MagicOrigin.SPELL ? 20 : 18;
		return base + (intent == MagicIntent.HARM || intent == MagicIntent.CONTROL ? 8 : 0);
	}

	private static int baseCooldown(MagicOrigin origin, MagicDelivery delivery) {
		if (origin == MagicOrigin.AMETHYST || origin == MagicOrigin.REALM
				|| delivery == MagicDelivery.TOGGLE) return 0;
		return origin == MagicOrigin.CRYSTAL ? 1200
				: origin == MagicOrigin.ARTIFACT ? 800 : origin == MagicOrigin.SPELL ? 600 : 200;
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
			case ARTIFACT -> 22;
			case AMETHYST -> 25;
			case REALM -> 30;
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
