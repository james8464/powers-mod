package com.powers.magic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure deterministic resolver for all same-action and cross-action collisions.
 * Rules use a strict priority: exact action overrides, suppression, opposed
 * aspects, resonance, delivery/intent, then safe coexistence. The last branch
 * is deliberate mechanics and still carries a generated presentation cue.
 */
public final class MagicInteractionResolver {
	private static final Set<String> ANCHORS = Set.of("dimensional_anchor");
	private static final Set<MagicDelivery> TRAVEL = EnumSet.of(MagicDelivery.TRAVEL, MagicDelivery.PROJECTION);

	private final MagicActionCatalogue catalogue;
	private final Map<ActionPair, MagicInteractionRule> exactRules;

	private MagicInteractionResolver(MagicActionCatalogue catalogue,
			Map<ActionPair, MagicInteractionRule> exactRules) {
		this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
		this.exactRules = Map.copyOf(exactRules);
	}

	/** Creates the production resolver with named high-impact action overrides. */
	public static MagicInteractionResolver defaults(MagicActionCatalogue catalogue) {
		Map<ActionPair, MagicInteractionRule> exact = new HashMap<>();
		register(exact, "light_crystal", "dark_crystal",
				(first, second, context) -> symmetric(InteractionOutcome.CONTEST, 0.70, 0.70,
						cue("eclipse", first, second, 5),
						"Equal light and darkness form a revealing eclipse field."));
		register(exact, "soul_link", "purification_circle",
				(first, second, context) -> cancellation(first, second, "purifying_severance",
						"Purification severs the active soul tether."));
		register(exact, "darkness_block", "pure_light_block",
				(first, second, context) -> cancellation(first, second, "realm_annihilation",
						"Opposed realm matter annihilates in a staged power-100-equivalent wave."));
		return new MagicInteractionResolver(catalogue, exact);
	}

	/**
	 * Resolves a caller-ordered pair. Definitions must come from the canonical
	 * catalogue or carry an ID present in it; this rejects unregistered magic
	 * before it can bypass interaction policy.
	 */
	public InteractionResolution resolve(MagicActionDefinition first, MagicActionDefinition second,
			InteractionContext context) {
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(second, "second");
		Objects.requireNonNull(context, "context");
		if (catalogue.definition(first.id()) == null || catalogue.definition(second.id()) == null) {
			throw new IllegalArgumentException("Interaction action is not registered");
		}
		MagicInteractionRule exact = exactRules.get(ActionPair.of(first.id(), second.id()));
		if (exact != null) return exact.resolve(first, second, context);
		return suppression(first, second, context)
				.or(() -> opposition(first, second, context))
				.or(() -> resonance(first, second))
				.or(() -> deliveryIntent(first, second))
				.orElseGet(() -> symmetric(InteractionOutcome.COEXIST, 1.0, 1.0,
						cue("harmonic_weave", first, second, 1),
						"Independent forces coexist with a restrained harmonic weave."));
	}

	/** Enumerates every unordered pair in stable ID order. */
	public List<ResolvedPair> allPairs() {
		List<MagicActionDefinition> definitions = catalogue.definitions().stream()
				.sorted(java.util.Comparator.comparing(MagicActionDefinition::id)).toList();
		List<ResolvedPair> result = new ArrayList<>((definitions.size() * (definitions.size() + 1)) / 2);
		for (int first = 0; first < definitions.size(); first++) {
			for (int second = first; second < definitions.size(); second++) {
				MagicActionDefinition a = definitions.get(first);
				MagicActionDefinition b = definitions.get(second);
				result.add(new ResolvedPair(ActionPair.of(a.id(), b.id()), resolve(a, b, InteractionContext.DEFAULT)));
			}
		}
		return List.copyOf(result);
	}

	private Optional<InteractionResolution> suppression(MagicActionDefinition first,
			MagicActionDefinition second, InteractionContext context) {
		boolean firstSuppresses = has(first, MagicAspect.SUPPRESSION);
		boolean secondSuppresses = has(second, MagicAspect.SUPPRESSION);
		if (!firstSuppresses && !secondSuppresses) return Optional.empty();
		if (firstSuppresses && secondSuppresses) {
			return Optional.of(symmetric(InteractionOutcome.RESONATE, 0.85, 0.85,
					cue("sealed_resonance", first, second, 2),
					"Suppression fields reinforce while slightly damping one another."));
		}

		MagicActionDefinition suppressor = firstSuppresses ? first : second;
		MagicActionDefinition subject = firstSuppresses ? second : first;
		boolean blocksFirst = !firstSuppresses;
		boolean blocksSecond = firstSuppresses;

		if (ANCHORS.contains(suppressor.id().value()) && TRAVEL.contains(subject.delivery())) {
			return Optional.of(blocked(InteractionOutcome.CANCEL, first, second, blocksFirst, blocksSecond,
					"anchor_chains", "Dimensional chains close the attempted route."));
		}
		if (suppressor.origin() == MagicOrigin.AMETHYST) {
			if (suppressor.id().value().equals("amethyst_item")) {
				return Optional.of(asymmetricDampen(first, second, firstSuppresses, "amethyst_hum",
						"Carried amethyst dampens but does not fully erase the force."));
			}
			String motif = suppressor.id().value().equals("amethyst_ward")
					? "amethyst_fracture" : "amethyst_grounding";
			return Optional.of(blocked(InteractionOutcome.CANCEL, first, second, blocksFirst, blocksSecond,
					motif, "Anchored amethyst overcomes the incoming magical presence."));
		}
		if (suppressor.id().value().equals("purification_circle")
				&& (subject.intent() == MagicIntent.HARM || has(subject, MagicAspect.DARKNESS))) {
			return Optional.of(blocked(InteractionOutcome.CANCEL, first, second, blocksFirst, blocksSecond,
					"cleansing_rain", "Purification unweaves the hostile residue."));
		}
		return Optional.of(asymmetricDampen(first, second, firstSuppresses, "counter_sigils",
				"Suppression and action contest according to interaction priority."));
	}

	private Optional<InteractionResolution> opposition(MagicActionDefinition first,
			MagicActionDefinition second, InteractionContext context) {
		if (opposed(first, second, MagicAspect.FLAME, MagicAspect.FROST)) {
			return Optional.of(new InteractionResolution(InteractionOutcome.TRANSFORM,
					0.75, 0.75, 0.60, 0.60, 0.90, 0.90, MagicAspect.MOTION,
					false, false, cue("steam", first, second, 4),
					"Flame and frost become a pressure wave of obscuring steam."));
		}
		if (opposed(first, second, MagicAspect.LIGHT, MagicAspect.DARKNESS)) {
			double firstStrength = first.priority() + context.firstRankPriority();
			double secondStrength = second.priority() + context.secondRankPriority();
			if (Math.abs(firstStrength - secondStrength) < 2.0) {
				return Optional.of(symmetric(InteractionOutcome.CONTEST, 0.70, 0.70,
						cue("eclipse", first, second, 5),
						"Balanced light and darkness become a revealing eclipse."));
			}
			boolean firstWins = firstStrength > secondStrength;
			return Optional.of(dominance(first, second, firstWins, "eclipse_break",
					"The higher-priority force breaks the opposed aspect."));
		}
		if (opposed(first, second, MagicAspect.VOID, MagicAspect.LIGHT)) {
			return Optional.of(new InteractionResolution(InteractionOutcome.TRANSFORM,
					0.65, 0.65, 0.75, 0.75, 0.80, 0.80, MagicAspect.SPACE,
					false, false, cue("star_rift", first, second, 5),
					"Void and light tear a brief projectile-consuming star rift."));
		}
		if (opposed(first, second, MagicAspect.LIFE, MagicAspect.DARKNESS)) {
			return Optional.of(symmetric(InteractionOutcome.CONTEST, 0.80, 0.80,
					cue("withered_bloom", first, second, 3),
					"Life and corruption contest without deleting unrelated effects."));
		}
		if (context.grounded() && (has(first, MagicAspect.STORM) || has(second, MagicAspect.STORM))) {
			return Optional.of(symmetric(InteractionOutcome.DAMPEN, 0.55, 0.90,
					cue("grounded_storm", first, second, 3),
					"Grounding diverts storm energy visibly into the earth."));
		}
		return Optional.empty();
	}

	private Optional<InteractionResolution> resonance(MagicActionDefinition first,
			MagicActionDefinition second) {
		Set<MagicAspect> shared = EnumSet.copyOf(first.aspects());
		shared.retainAll(second.aspects());
		if (shared.isEmpty()) return Optional.empty();
		MagicAspect resonant = shared.iterator().next();
		return Optional.of(new InteractionResolution(InteractionOutcome.RESONATE,
				1.12, 1.12, 1.10, 1.10, 1.05, 1.05, resonant,
				false, false, cue(resonant.name().toLowerCase(java.util.Locale.ROOT) + "_resonance",
						first, second, 3),
				"Shared aspects resonate with bounded potency and duration."));
	}

	private Optional<InteractionResolution> deliveryIntent(MagicActionDefinition first,
			MagicActionDefinition second) {
		if (defendsAgainst(first, second) || defendsAgainst(second, first)) {
			return Optional.of(symmetric(InteractionOutcome.CONTEST, 0.80, 0.80,
					cue("ward_clash", first, second, 3),
					"Defensive and harmful magic contest shield integrity and potency."));
		}
		if (first.intent() == MagicIntent.HARM && second.intent() == MagicIntent.HARM) {
			return Optional.of(symmetric(InteractionOutcome.DESTABILIZE, 1.05, 0.75,
					cue("violent_interference", first, second, 4),
					"Colliding hostile forces intensify impact while shortening persistence."));
		}
		if (first.intent() == MagicIntent.SUPPORT && second.intent() == MagicIntent.SUPPORT) {
			return Optional.of(symmetric(InteractionOutcome.AMPLIFY, 1.08, 1.12,
					cue("concordant_bloom", first, second, 2),
					"Compatible support magic gains a capped cooperative resonance."));
		}
		if (opposed(first, second, MagicAspect.CONCEALMENT, MagicAspect.MIND)
				|| opposed(first, second, MagicAspect.CONCEALMENT, MagicAspect.LIGHT)) {
			return Optional.of(symmetric(InteractionOutcome.CONTEST, 0.85, 0.85,
					cue("revealed_veil", first, second, 3),
					"Concealment and detection contest without leaking hidden information."));
		}
		return Optional.empty();
	}

	private static boolean defendsAgainst(MagicActionDefinition defender, MagicActionDefinition subject) {
		return defender.intent() == MagicIntent.DEFENCE && subject.intent() == MagicIntent.HARM;
	}

	private static boolean opposed(MagicActionDefinition first, MagicActionDefinition second,
			MagicAspect a, MagicAspect b) {
		return has(first, a) && has(second, b) || has(first, b) && has(second, a);
	}

	private static boolean has(MagicActionDefinition definition, MagicAspect aspect) {
		return definition.aspects().contains(aspect);
	}

	private static InteractionResolution symmetric(InteractionOutcome outcome, double potency,
			double duration, InteractionCue cue, String mechanics) {
		return new InteractionResolution(outcome, potency, potency, duration, duration,
				1.0, 1.0, null, false, false, cue, mechanics);
	}

	private static InteractionResolution cancellation(MagicActionDefinition first,
			MagicActionDefinition second, String motif, String mechanics) {
		return new InteractionResolution(InteractionOutcome.CANCEL, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, null, true, true,
				cue(motif, first, second, 4), mechanics);
	}

	private static InteractionResolution blocked(InteractionOutcome outcome, MagicActionDefinition first,
			MagicActionDefinition second, boolean blocksFirst, boolean blocksSecond,
			String motif, String mechanics) {
		return new InteractionResolution(outcome,
				blocksFirst ? 0.0 : 1.0, blocksSecond ? 0.0 : 1.0,
				blocksFirst ? 0.0 : 1.0, blocksSecond ? 0.0 : 1.0,
				blocksFirst ? 0.0 : 1.0, blocksSecond ? 0.0 : 1.0,
				null, blocksFirst, blocksSecond, cue(motif, first, second, 4), mechanics);
	}

	private static InteractionResolution asymmetricDampen(MagicActionDefinition first,
			MagicActionDefinition second, boolean firstSuppresses, String motif, String mechanics) {
		double firstPotency = firstSuppresses ? 1.0 : 0.55;
		double secondPotency = firstSuppresses ? 0.55 : 1.0;
		return new InteractionResolution(InteractionOutcome.DAMPEN, firstPotency, secondPotency,
				firstPotency, secondPotency, 1.0, 1.0, null, false, false,
				cue(motif, first, second, 2), mechanics);
	}

	private static InteractionResolution dominance(MagicActionDefinition first,
			MagicActionDefinition second, boolean firstWins, String motif, String mechanics) {
		return new InteractionResolution(InteractionOutcome.CONSUME,
				firstWins ? 1.15 : 0.35, firstWins ? 0.35 : 1.15,
				firstWins ? 1.0 : 0.5, firstWins ? 0.5 : 1.0,
				1.0, 1.0, null, !firstWins, firstWins,
				cue(motif, first, second, 5), mechanics);
	}

	private static InteractionCue cue(String motif, MagicActionDefinition first,
			MagicActionDefinition second, int intensity) {
		int seed = 31 * first.signature().glyphSeed() + second.signature().glyphSeed();
		return new InteractionCue(motif, soundFor(motif), first.signature().primaryColor(),
				second.signature().primaryColor(), seed, intensity);
	}

	/** Gives collision families an audible identity instead of one generic cue. */
	private static String soundFor(String motif) {
		if (motif.contains("amethyst")) return "amethyst_fracture";
		if (motif.contains("rift") || motif.contains("anchor") || motif.contains("return")) return "rift_open";
		if (motif.contains("soul") || motif.contains("severance")) return "soul_tether";
		if (motif.contains("eclipse") || motif.contains("veil")) return "light_chorus";
		if (motif.contains("ward")) return "ward_impact";
		if (motif.contains("resonance") || motif.contains("bloom") || motif.contains("weave")) {
			return "crystal_resonate";
		}
		if (motif.contains("cleansing") || motif.contains("sigil")) return "rune_hum";
		return "interaction_clash";
	}

	private static void register(Map<ActionPair, MagicInteractionRule> rules, String first,
			String second, MagicInteractionRule rule) {
		ActionPair pair = ActionPair.of(new MagicActionId(first), new MagicActionId(second));
		if (rules.putIfAbsent(pair, rule) != null) {
			throw new IllegalArgumentException("Duplicate exact interaction rule: " + pair);
		}
	}
}
