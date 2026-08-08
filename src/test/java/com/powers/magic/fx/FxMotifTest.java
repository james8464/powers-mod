package com.powers.magic.fx;

import com.powers.magic.MagicAspect;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ensures every canonical aspect has a deliberate, shape-distinct language. */
class FxMotifTest {
	@Test
	void everyMagicAspectMapsToItsAuthoredGeometryFamily() {
		Map<MagicAspect, FxMotif> expected = Map.ofEntries(
				Map.entry(MagicAspect.FLAME, FxMotif.FRACTURE),
				Map.entry(MagicAspect.FROST, FxMotif.SHARD),
				Map.entry(MagicAspect.STORM, FxMotif.FORK),
				Map.entry(MagicAspect.FORCE, FxMotif.RING),
				Map.entry(MagicAspect.MOTION, FxMotif.SPIRAL),
				Map.entry(MagicAspect.GRAVITY, FxMotif.TETHER),
				Map.entry(MagicAspect.TIME, FxMotif.SPIRAL),
				Map.entry(MagicAspect.SPACE, FxMotif.TETHER),
				Map.entry(MagicAspect.MIND, FxMotif.TETHER),
				Map.entry(MagicAspect.SOUL, FxMotif.TETHER),
				Map.entry(MagicAspect.LIFE, FxMotif.ROOT),
				Map.entry(MagicAspect.LIGHT, FxMotif.GLYPH),
				Map.entry(MagicAspect.DARKNESS, FxMotif.ECLIPSE),
				Map.entry(MagicAspect.VOID, FxMotif.ECLIPSE),
				Map.entry(MagicAspect.PROTECTION, FxMotif.RING),
				Map.entry(MagicAspect.CONCEALMENT, FxMotif.ECLIPSE),
				Map.entry(MagicAspect.CREATION, FxMotif.GLYPH),
				Map.entry(MagicAspect.SUPPRESSION, FxMotif.FRACTURE));

		for (Map.Entry<MagicAspect, FxMotif> entry : expected.entrySet()) {
			assertEquals(entry.getValue(), FxMotif.fromCue(entry.getKey().name()), entry.getKey().name());
		}
	}
}
