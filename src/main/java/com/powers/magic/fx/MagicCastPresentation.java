package com.powers.magic.fx;

import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicAspect;
import com.powers.magic.MagicDelivery;
import com.powers.magic.MagicOrigin;

import java.util.Objects;
import java.util.Set;

/**
 * Safe audiovisual profile derived exclusively from canonical server action
 * metadata. It prevents catalogue-internal signature names from being treated
 * as registered sound-event identifiers.
 */
public record MagicCastPresentation(String soundCue, int intensity, int genericBeatCount) {
	private static final Set<String> AUTHORED_CUES = Set.of(
			"rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
			"rift_open", "soul_tether", "light_chorus", "dark_whisper", "ward_impact");

	/** Validates profiles before they reach sound registration or packet code. */
	public MagicCastPresentation {
		Objects.requireNonNull(soundCue, "soundCue");
		if (!AUTHORED_CUES.contains(soundCue)) {
			throw new IllegalArgumentException("Unsupported cast sound cue: " + soundCue);
		}
		if (intensity < 1 || intensity > MagicFxEvent.MAX_INTENSITY) {
			throw new IllegalArgumentException("Cast intensity must be within 1..5");
		}
		if (genericBeatCount != 0 && genericBeatCount != 1 && genericBeatCount != 2
				&& genericBeatCount != 4 && genericBeatCount != 6) {
			throw new IllegalArgumentException("Unsupported generic ceremony beat count");
		}
	}

	/** Resolves a bounded profile from immutable action mechanics. */
	public static MagicCastPresentation forAction(MagicActionDefinition action) {
		return forAction(action, 0, Set.of());
	}

	/** Resolves a profile amplified by authoritative progression state. */
	public static MagicCastPresentation forAction(MagicActionDefinition action, int legacyLevel,
			Set<String> unlockedVariants) {
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(unlockedVariants, "unlockedVariants");
		int level = Math.clamp(legacyLevel, 0, 10);
		int masteryBonus = level >= 8 ? 2 : level >= 4 ? 1 : 0;
		if (unlockedVariants.contains("ancient_mastery")) masteryBonus++;
		int intensity = Math.clamp(baseIntensity(action) + masteryBonus,
				1, MagicFxEvent.MAX_INTENSITY);
		int beats = action.genericCeremony() ? action.significance().genericBeatCount() : 0;
		return new MagicCastPresentation(soundCue(action), intensity, beats);
	}

	private static String soundCue(MagicActionDefinition action) {
		Set<MagicAspect> aspects = action.aspects();
		if (action.origin() == MagicOrigin.AMETHYST) return "amethyst_fracture";
		if (aspects.contains(MagicAspect.SUPPRESSION)) return "ward_impact";
		if (aspects.contains(MagicAspect.TIME)) return "time_suspend";
		if (aspects.contains(MagicAspect.SPACE) || action.delivery() == MagicDelivery.TRAVEL) {
			return "rift_open";
		}
		if (aspects.contains(MagicAspect.SOUL) || aspects.contains(MagicAspect.MIND)) {
			return "soul_tether";
		}
		if (aspects.contains(MagicAspect.LIGHT)) return "light_chorus";
		if (aspects.contains(MagicAspect.DARKNESS) || aspects.contains(MagicAspect.VOID)) {
			return "dark_whisper";
		}
		if (aspects.contains(MagicAspect.PROTECTION) || aspects.contains(MagicAspect.FORCE)
				|| aspects.contains(MagicAspect.STORM)) return "ward_impact";
		if (action.origin() == MagicOrigin.CRYSTAL) return "crystal_resonate";
		return "rune_hum";
	}

	private static int baseIntensity(MagicActionDefinition action) {
		int value = switch (action.origin()) {
			case INNATE -> 1;
			case SPELL -> 2;
			case CRYSTAL, AMETHYST -> 3;
			case ARTIFACT -> 4;
			case REALM -> 5;
		};
		if (action.delivery() == MagicDelivery.FIELD || action.delivery() == MagicDelivery.PROJECTION
				|| action.delivery() == MagicDelivery.BEAM) value++;
		if (action.basePotency() >= 18) value++;
		return Math.clamp(value, 1, MagicFxEvent.MAX_INTENSITY);
	}
}
