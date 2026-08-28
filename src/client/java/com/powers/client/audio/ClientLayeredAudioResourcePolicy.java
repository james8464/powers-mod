package com.powers.client.audio;

import com.powers.PowersMod;
import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Deterministic resource-failure policy for the finite layered-audio bank. */
final class ClientLayeredAudioResourcePolicy {
	private static final float RESTRAINED_BASE_GAIN = 0.35F;
	private static final Set<MissingKey> WARNED = new HashSet<>();

	private ClientLayeredAudioResourcePolicy() {
	}

	static Decision resolve(LayeredAudioCue cue, LayeredAudioLayer layer,
			boolean reducedTinnitus, Predicate<Identifier> available) {
		Objects.requireNonNull(cue, "cue");
		Objects.requireNonNull(layer, "layer");
		Objects.requireNonNull(available, "available");
		boolean reduced = reducedTinnitus && cue.tinnitusSensitive();
		MissingKey key = new MissingKey(cue, layer, reduced);
		Identifier requested = resource(cue, layer, reduced);
		if (available.test(requested)) return new Decision(Mode.LAYERED, 1.0F, false, requested);

		boolean logMissing = WARNED.add(key);
		if (reduced) return new Decision(Mode.SILENT, 0.0F, logMissing, requested);
		Identifier base = PowersMod.id("sounds/magic/" + cue.semanticName() + ".ogg");
		if (available.test(base)) return new Decision(Mode.BASE, RESTRAINED_BASE_GAIN, logMissing, requested);
		return new Decision(Mode.SILENT, 0.0F, logMissing, requested);
	}

	static void reset() {
		WARNED.clear();
	}

	private static Identifier resource(LayeredAudioCue cue, LayeredAudioLayer layer, boolean reduced) {
		String suffix = reduced ? "_reduced" : "";
		return PowersMod.id("sounds/magic/layered/" + cue.semanticName() + suffix + "_"
				+ layer.serializedName() + ".ogg");
	}

	enum Mode {
		LAYERED,
		BASE,
		SILENT
	}

	record Decision(Mode mode, float gainScale, boolean logMissing, Identifier missingResource) { }

	private record MissingKey(LayeredAudioCue cue, LayeredAudioLayer layer, boolean reduced) { }
}
