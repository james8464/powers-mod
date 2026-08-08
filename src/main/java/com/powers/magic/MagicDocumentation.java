package com.powers.magic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Deterministically renders the canonical action and exhaustive interaction evidence. */
public final class MagicDocumentation {
	private static final MagicActionCatalogue CATALOGUE = MagicActionCatalogue.defaults();
	private static final MagicInteractionResolver RESOLVER = MagicInteractionResolver.defaults(CATALOGUE);
	private static final String CATALOGUE_PATH = "docs/interactions/action-catalogue.md";
	private static final String RULES_PATH = "docs/interactions/interaction-rules.md";
	private static final String MATRIX_PATH = "docs/interactions/interaction-matrix.csv";

	private MagicDocumentation() {
	}

	/** Renders all 63 definitions with their numerical and audiovisual baseline. */
	public static String renderCatalogue() {
		StringBuilder output = new StringBuilder("""
				# Canonical magic action catalogue

				This is the server-authoritative set of every innate power, crystal action, grimoire spell, and amethyst counterforce that may collide. Rank scaling creates separate values and never mutates these baselines.

				| ID | Origin | Aspects | Delivery | Intent | Potency | Range | Duration | Energy | Cooldown | Residue | Priority | Motif | Sound | Colours |
				|---|---|---|---|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|
				""");
		for (MagicActionDefinition action : sortedActions()) {
			String aspects = action.aspects().stream().map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("");
			output.append("| `").append(action.id()).append("` | ").append(action.origin())
					.append(" | ").append(aspects).append(" | ").append(action.delivery())
					.append(" | ").append(action.intent()).append(" | ").append(action.basePotency())
					.append(" | ").append(format(action.baseRange())).append(" | ").append(action.baseDurationTicks())
					.append(" | ").append(action.baseEnergy()).append(" | ").append(action.baseCooldownTicks())
					.append(" | ").append(action.residueTicks()).append(" | ").append(action.priority())
					.append(" | `").append(action.signature().motif()).append("` | `")
					.append(action.signature().sound()).append("` | `#")
					.append(String.format(Locale.ROOT, "%06X", action.signature().primaryColor()))
					.append(" / #").append(String.format(Locale.ROOT, "%06X", action.signature().secondaryColor()))
					.append("` |\n");
		}
		return output.toString();
	}

	/** Renders resolver order and the named exceptional mechanical families. */
	public static String renderRules() {
		return """
				# Magic interaction rules

				Every unordered pair—including two copies of the same action—has a deterministic mechanical outcome and a shape, sound, colour pair, glyph seed, and bounded intensity. The exhaustive concrete results are in `interaction-matrix.csv`.

				## Resolution priority

				1. Exact action-pair rules.
				2. Suppression and dimensional anchors.
				3. Opposed aspects and environment grounding.
				4. Shared-aspect resonance.
				5. Delivery/intent contests.
				6. Safe coexistence with a restrained harmonic weave.

				## Exceptional families

				| Family | Mechanics | Signature |
				|---|---|---|
				| Flame × Frost | Both forces lose persistence and become an obscuring pressure-wave steam transformation. | Steam billow and interaction clash. |
				| Light × Darkness | Near-equal priorities form a revealing eclipse; a decisive priority consumes the weaker force. | Eclipse disc or breaking corona. |
				| Void × Light | Both forces collapse into a short projectile-consuming spatial star rift. | Star-rift fracture. |
				| Life × Darkness | Healing and corruption contest without deleting unrelated status effects. | Withered bloom. |
				| Grounded storm | Environmental grounding visibly diverts storm power into the earth. | Forks terminating in ground runes. |
				| Amethyst item | Portable amethyst dampens magical potency and duration without blanket immunity. | Low amethyst hum. |
				| Amethyst block / ward | Anchored amethyst cancels the incoming presence; wards use the stronger fracture cue. | Grounding lattice or crystal fracture. |
				| Anchor × travel/projection | The attempted route is cancelled before energy or cooldown commits. | Dimensional chains closing a gate. |
				| Purification × hostile/darkness | Hostile residue is unmade while unrelated effects remain owned by their source. | Cleansing rain. |
				| Soul Link × Purification | The owned soul tether is severed symmetrically. | Purifying severance. |
				| Clone Swarm × Banishment | Only POWERS-owned ephemeral clones return; natural entities are untouched. | Return seal. |
				| Shared aspect | Potency, range, and duration resonate within fixed multipliers. | Aspect-specific harmonic resonance. |
				| Defence × harm | Ward integrity and hostile potency contest rather than granting invulnerability. | Ward clash. |
				| Harm × harm | Impacts intensify slightly while duration destabilizes. | Violent interference. |
				| Support × support | Compatible aid receives capped cooperative amplification. | Concordant bloom. |
				| Concealment × detection | Detection contests the veil without leaking protected target information. | Revealed veil. |
				| Suppression × suppression | Seals reinforce but dampen each other to prevent runaway fields. | Sealed resonance. |
				| Fallback coexistence | Independent forces retain full mechanics and receive a low-intensity deterministic cue. | Harmonic weave. |

				## Runtime guarantees

				The cast transaction resolves nearby presences before payment or cooldown, commits residue only after gameplay success, deduplicates a pair/cell/tick cue, bounds spatial cells and residue lifetime, and clears owner state on disconnect, respawn, and shutdown. Server-derived action IDs prevent clients from selecting a stronger rule directly.
				""";
	}

	/** Renders all 2,016 unordered resolutions as RFC 4180-compatible CSV. */
	public static String renderMatrix() {
		StringBuilder output = new StringBuilder(
				"first,second,first_origin,second_origin,outcome,motif,sound,intensity,first_potency,second_potency,first_duration,second_duration,first_range,second_range,replacement_aspect,blocks_first,blocks_second,mechanics\n");
		for (ResolvedPair pair : RESOLVER.allPairs()) {
			MagicActionDefinition first = CATALOGUE.definition(pair.pair().first());
			MagicActionDefinition second = CATALOGUE.definition(pair.pair().second());
			InteractionResolution resolution = pair.resolution();
			appendCsv(output, first.id().value(), second.id().value(), first.origin().name(), second.origin().name(),
					resolution.outcome().name(), resolution.cue().motif(), resolution.cue().sound(),
					Integer.toString(resolution.cue().intensity()), format(resolution.firstPotencyMultiplier()),
					format(resolution.secondPotencyMultiplier()), format(resolution.firstDurationMultiplier()),
					format(resolution.secondDurationMultiplier()), format(resolution.firstRangeMultiplier()),
					format(resolution.secondRangeMultiplier()),
					resolution.replacementAspect() == null ? "" : resolution.replacementAspect().name(),
					Boolean.toString(resolution.blocksFirst()), Boolean.toString(resolution.blocksSecond()),
					resolution.mechanics());
		}
		return output.toString();
	}

	/** Writes documents, or verifies them with a second {@code --check} argument. */
	public static void main(String[] args) throws IOException {
		Path root = args.length == 0 ? Path.of("").toAbsolutePath() : Path.of(args[0]).toAbsolutePath();
		boolean check = args.length > 1 && args[1].equals("--check");
		writeOrCheck(root.resolve(CATALOGUE_PATH), renderCatalogue(), check);
		writeOrCheck(root.resolve(RULES_PATH), renderRules(), check);
		writeOrCheck(root.resolve(MATRIX_PATH), renderMatrix(), check);
	}

	private static List<MagicActionDefinition> sortedActions() {
		return CATALOGUE.definitions().stream().sorted(Comparator.comparing(MagicActionDefinition::id)).toList();
	}

	private static void writeOrCheck(Path destination, String expected, boolean check) throws IOException {
		if (check) {
			if (!Files.exists(destination) || !Files.readString(destination).equals(expected)) {
				throw new IllegalStateException("Generated magic document is stale: " + destination);
			}
			return;
		}
		Files.createDirectories(destination.getParent());
		Files.writeString(destination, expected);
	}

	private static void appendCsv(StringBuilder output, String... values) {
		for (int index = 0; index < values.length; index++) {
			if (index > 0) output.append(',');
			String value = values[index];
			if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
				output.append('"').append(value.replace("\"", "\"\"")).append('"');
			} else {
				output.append(value);
			}
		}
		output.append('\n');
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}
}
