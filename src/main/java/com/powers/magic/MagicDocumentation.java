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

	/** Renders all 65 definitions with their numerical and audiovisual baseline. */
	public static String renderCatalogue() {
		StringBuilder output = new StringBuilder("""
				# Canonical magic action catalogue

				This is the server-authoritative set of every innate power, crystal action, grimoire spell, amethyst counterforce, and persistent realm force that may collide. Rank scaling creates separate values and never mutates these baselines.

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
				| Pure-light block × Darkness block | Contact cancels both forces and starts a deduplicated 48-block annihilation wave that removes only loaded realm matter under a per-tick budget. | Expanding opposed coronas, fracture storm, layered light/dark impact, and visual lightning. |
				| Void × Light | Both forces collapse into a short projectile-consuming spatial star rift. | Star-rift fracture. |
				| Life × Darkness | Healing and corruption contest without deleting unrelated status effects. | Withered bloom. |
				| Grounded storm | Environmental grounding visibly diverts storm power into the earth. | Forks terminating in ground runes. |
				| Amethyst item | Portable amethyst dampens magical potency and duration without blanket immunity. | Low amethyst hum. |
				| Amethyst block / ward | Anchored amethyst cancels the incoming presence; wards use the stronger fracture cue. | Grounding lattice or crystal fracture. |
				| Anchor × travel/projection | The attempted route is cancelled before energy or cooldown commits. | Dimensional chains closing a gate. |
				| Purification × hostile/darkness | Hostile residue is unmade while unrelated effects remain owned by their source. | Cleansing rain. |
				| Soul Link × Purification | The owned soul tether is severed symmetrically. | Purifying severance. |
				| Clone Swarm × Banishment | Only POWERS-owned ephemeral clones return; natural entities are untouched. | Return seal. |
				| Gravity orrery × protected body | Consent/safe zones, amethyst, soul-anchored projection bodies, personal forcefields, Sanctuary/Kinetic Ward, and time locks resist capture without consuming or moving the target. | Privacy-blue boundary, amethyst fracture, soul tether, cyan shield, green-gold seal, or pale time fracture. |
				| Gravity orrery × gravity orrery | Each shared body belongs to the nearer field after a hysteresis margin; ownership hands off once with no competing velocity writes. | Violet-cyan twin tether and resonance fracture. |
				| Sunfire Energy Beam × water | The nearest sampled water boundary converts only that damage beat into a radius-three, eight-target steam pulse at 65% base damage, with bounded consent-safe motion and no ignition. | Pale pressure rune, cloud bloom, extinguish hiss, and interaction clash. |
				| Sunfire Energy Beam × terminal ward or matter | Ordinary matter, amethyst, Pure Light, Darkness, safe zones, Sanctuary, Kinetic Ward, personal forcefields, and invulnerable bodies stop the ray before protected effects; shields alone consume integrity through the damage bridge. | Material-specific double corona, fracture language, and semantic ward sound. |
				| Sunfire Energy Beam × ranked body sequence | Three consecutive hits escalate scorch and unlock at most one Empowered solar flare; Ancient Mastery adds at most two visible 45% forks that never chain or cross protection. | Rising ember coronas, compact solar disc, and twin white-gold arcs. |
				| Tempest Rite × protected body | Consent/safe zones, amethyst, projection bodies, forcefields, Sanctuary/Kinetic Ward, time locks, and blocked launch volumes resist before velocity writes; later protection changes trigger Slow Falling release. | Privacy blue, amethyst violet, soul lavender, shield cyan, ritual green-gold, time white, or terrain-grey double corona. |
				| Tempest Rite × Tempest Rite | A body belongs to its first active gust owner until slam or safe release; a competing rite cannot write velocity and receives a teal wind-resonance fracture. | Twin teal-white rings and interaction clash. |
				| Mastered Tempest Rite × hostile projectile | At most sixteen projectiles curve radially away once, under a speed cap, without reflection or ownership transfer. | Sky-blue ribbon bend, tight projectile rune, and gust mote. |
				| Chronal Overdrive × water | The owned movement bonus falls to 35% while submerged, updates without touching any foreign modifier, and returns immediately on exit. | Deep-cyan grounding clock, splash wake, and pale release seal. |
				| Chronal Overdrive × collision / rank | Every new wall contact fractures a time seal; Motion may spend one collision-checked backward Second Step, Might may spend one non-damaging eight-body pressure corona, Veil clears at most eight visible hostile target memories per beat, and Dominion bends at most sixteen approaching hostile projectiles once. | Cyan clock fracture, pale rewind ribbon, gold pressure rune, fading memory motes, or gold-white projectile bend. |
				| Chronal pressure × protected body | Consent/safe zones, amethyst, projection bodies, forcefields, Sanctuary/Kinetic Ward, time locks, and blocked body volumes independently refuse velocity before any write. | Privacy blue, amethyst violet, soul lavender, shield cyan, ritual green-gold, time white, or terrain-grey double corona. |
				| Chronal Overdrive × suppression / lifecycle | Amethyst, time freeze, death, power loss, dimension change, respawn, disconnect, expiry, and shutdown remove only the POWERS-owned modifier and leave a finite safe-fall release. | Counter-coloured closing clock, temporal release, or fractured interruption. |
				| Cinderheart × repeated Fireball cast | One hovering heart per original caster advances through three paid tiers, or four with Ancient Mastery; charges extend but never exceed a 360-tick creation window. | Nested coal-red, ember, gold, and white flame seals with rising heartbeat pitch. |
				| Cinderheart × reflection | The first player attack launches freely; only two later controller transfers are legal, plus one each from Reflective Ward and Ancient Mastery. Current player control owns attribution while the original caster owns lifecycle. | White-gold handoff rune, directional fracture ribbon, or sealed coal corona after the cap. |
				| Cinderheart × water / frost | Water, ice, or snow consumes flame persistence and converts the impact into a 55%-damage no-ignition pressure cloud whose movement still respects consent, anchors, wards, shields, time locks, and body collision. | Pale or frost-blue steam rune, cloud bloom, gust spokes, extinguish hiss, and interaction clash. |
				| Cinderheart × protected terminal | Safe zones, tagged amethyst, Sanctuary, Kinetic Ward, personal forcefields, invalid controllers, and per-body protection stop or contain effects before forbidden damage, ignition, or movement. A Reflective Ward may spend its existing finite reflection. | Privacy blue, amethyst violet, ritual green, shield cyan, or ownerless grey double seal. |
				| Cinderheart × terrain / lifecycle | Vanilla explosion grief is always replaced. Terrain-disabled impacts mutate nothing; enabled servers receive at most eight policy-approved surface fires and no destroyed blocks. Death, disconnect, respawn, dimension change, power loss, suppression, time locks, expiry, unload, and shutdown discard both indexes and the ephemeral entity. | Bounded flame sigils, individual scorch runes, or counter-coloured extinguishing heart. |
				| Astral Convergence × authored timeline | One storm per caster opens through 20 omen ticks, then resolves eight deterministic golden-angle strikes six ticks apart. Might and Dominion add at most four regular beats; Dominion alone authors one final crown. | Indigo-gold astrolabe, contracting omen clock, three-tick strike constellations, descending ribbons, and white-gold crown. |
				| Astral Convergence × environment | Loaded sky paths resolve before bodies: ordinary roofs catch a strike, water conducts a wider 70%-damage pulse, Pure Light amplifies it, Darkness consumes it, and tagged amethyst or powered wards fracture it. No blocks, fire, or harmful vanilla lightning are created. | Ceiling-grey fracture, cyan water lattice, white-gold resonance, eclipse seal, or amethyst grounding. |
				| Astral Convergence × protected body | Safe zones, body amethyst, Sanctuary, Kinetic Ward, personal forcefields, body proxies, time locks, movement consent, collision, repeat intervals, and total-hit caps independently stop forbidden damage or pressure. | Privacy blue, amethyst violet, ritual green-gold, shield cyan, soul lavender, time white, or resisted-grey double seals. |
				| Astral Convergence × rank maze | Might expands and pressures; Motion leashes the eye to the original target; Insight reveals only after a successful hit; Wardcraft diverts at most sixteen hostile projectiles without owner transfer; Communion mirrors each third strike at 45%; Veil enriches the omen residue; Dominion authors the crown. | Gold pressure corona, cyan tracking ribbon, third-eye glyph, constellation ward, soul echo, lingering star ribbons, or triple crown. |
				| Astral Convergence × lifecycle | Death, disconnect, respawn, dimension change, power loss, suppression, time freeze, expiry, unload, and shutdown remove identifier-only state exactly once. | Counter-coloured collapsing astrolabe with no damaging residue. |
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

	/** Renders all 2,145 unordered resolutions as RFC 4180-compatible CSV. */
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
