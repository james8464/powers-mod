package com.powers.magic.runtime;

import com.powers.magic.InteractionContext;
import com.powers.magic.InteractionOutcome;
import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionId;
import com.powers.magic.MagicInteractionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicRuntimeTest {
	private static final UUID CASTER = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private MagicActionCatalogue catalogue;
	private ActiveMagicIndex index;
	private MagicRuntime runtime;
	private List<MagicReactionEvent> cues;

	@BeforeEach
	void createRuntime() {
		catalogue = MagicActionCatalogue.defaults();
		index = new ActiveMagicIndex(16);
		runtime = new MagicRuntime(catalogue, MagicInteractionResolver.defaults(catalogue), index);
		cues = new ArrayList<>();
	}

	@Test
	void anchorCancelsTravelBeforeAResidueCanCommit() {
		index.register(presence("00000000-0000-0000-0000-000000000001", OTHER,
				"dimensional_anchor", 0, 64, 0, 12, 200));
		MagicCastContext travel = cast("time_shift", CASTER, 0, 64, 0, 40, 100);

		MagicCastPreview preview = runtime.previewCast(travel);
		CastAdjustment adjustment = preview.adjustment();
		runtime.emitReactions(preview, cues::add);

		assertFalse(adjustment.allowed());
		assertEquals(InteractionOutcome.CANCEL, adjustment.resolutions().getFirst().outcome());
		assertEquals(1, index.size(), "the existing anchor remains; the blocked cast adds nothing");
		assertEquals(1, cues.size());
	}

	@Test
	void successfulCommitRegistersAdjustedResidueOnlyAfterExecution() {
		MagicCastContext fire = cast("fireball", CASTER, 0, 64, 0, 32, 100);
		CastAdjustment adjustment = runtime.previewCast(fire).adjustment();

		assertTrue(adjustment.allowed());
		assertEquals(0, index.size());
		MagicPresenceId id = runtime.commitCast(fire, adjustment);

		assertEquals(1, index.size());
		assertEquals(List.of(id), index.nearby("overworld", 0, 64, 0, 40, 100).stream()
				.map(MagicPresence::id).toList());
	}

	@Test
	void delayedExecutionRebasesResidueAtTheActualCompletionPlaceAndTick() {
		MagicCastContext started = cast("controlled_hellfire", CASTER, 0, 64, 0, 32, 100);
		CastAdjustment adjustment = runtime.previewCast(started).adjustment();
		MagicCastContext completed = started.rebased("overworld", PresenceAnchor.fixed(24, 70, 24), 220);

		runtime.commitCast(completed, adjustment);

		assertTrue(index.nearby("overworld", 0, 64, 0, 2, 220).isEmpty());
		assertEquals(1, index.nearby("overworld", 24, 70, 24, 2, 220).size());
		assertEquals(1, index.nearby("overworld", 24, 70, 24, 2, 249).size());
		assertTrue(index.nearby("overworld", 24, 70, 24, 2, 250).isEmpty());
	}

	@Test
	void repeatedPairInOneCellAndTickEmitsOneReactionCue() {
		index.register(presence("00000000-0000-0000-0000-000000000002", OTHER,
				"frost_nova", 1, 64, 1, 12, 200));
		MagicCastContext fire = cast("fireball", CASTER, 0, 64, 0, 32, 100);

		runtime.emitReactions(runtime.previewCast(fire), cues::add);
		runtime.emitReactions(runtime.previewCast(fire), cues::add);

		assertEquals(1, cues.size());
		assertEquals("steam", cues.getFirst().resolution().cue().motif());
	}

	@Test
	void allowedPreviewHasNoReactionSideEffectsUntilSuccessfulCommitPathEmitsIt() {
		index.register(presence("00000000-0000-0000-0000-000000000004", OTHER,
				"frost_nova", 1, 64, 1, 12, 200));
		MagicCastContext fire = cast("fireball", CASTER, 0, 64, 0, 32, 100);

		MagicCastPreview preview = runtime.previewCast(fire);

		assertTrue(preview.allowed());
		assertTrue(cues.isEmpty());
		assertEquals(0, runtime.pendingCueKeys());
		runtime.emitReactions(preview, cues::add);
		assertEquals(1, cues.size());
		assertEquals(1, runtime.pendingCueKeys());
	}

	@Test
	void rejectedPreviewEmitsOnlyTheReactionThatActuallyBlockedTheCast() {
		index.register(presence("00000000-0000-0000-0000-000000000005", OTHER,
				"dimensional_anchor", 0, 64, 0, 12, 200));
		index.register(presence("00000000-0000-0000-0000-000000000006", OTHER,
				"frost_nova", 1, 64, 1, 12, 200));
		MagicCastPreview preview = runtime.previewCast(cast("time_shift", CASTER, 0, 64, 0, 40, 100));

		runtime.emitBlockingReactions(preview, cues::add);

		assertFalse(preview.allowed());
		assertEquals(1, cues.size());
		assertTrue(cues.getFirst().resolution().blocksFirst());
	}

	@Test
	void ownerAndServerCleanupRemoveRuntimeStateAndDedupeKeys() {
		index.register(presence("00000000-0000-0000-0000-000000000003", CASTER,
				"fireball", 0, 64, 0, 4, 200));
		assertEquals(1, runtime.clearOwner(CASTER));
		assertEquals(0, index.size());

		runtime.clearAll();
		assertEquals(0, runtime.pendingCueKeys());
	}

	@Test
	void explicitlyRemovedImpactPresenceCannotCollideAgain() {
		MagicPresence presence = presence("00000000-0000-0000-0000-000000000099", OTHER,
				"void_beam", 0, 64, 0, 4, 200);
		runtime.registerPresence(presence);

		assertTrue(runtime.removePresence(presence.id()));
		assertFalse(runtime.removePresence(presence.id()));
		assertTrue(runtime.previewCast(cast("starfall", CASTER, 0, 64, 0, 8, 100))
				.reactions().isEmpty());
	}

	private MagicCastContext cast(String action, UUID owner, double x, double y, double z,
			double queryRadius, long gameTime) {
		return new MagicCastContext(catalogue.definition(new MagicActionId(action)), owner,
				"overworld", PresenceAnchor.fixed(x, y, z), queryRadius, gameTime,
				InteractionContext.DEFAULT);
	}

	private static MagicPresence presence(String id, UUID owner, String action,
			double x, double y, double z, double radius, long expiresAt) {
		return new MagicPresence(new MagicPresenceId(UUID.fromString(id)), new MagicActionId(action), owner,
				"overworld", PresenceAnchor.fixed(x, y, z), radius, expiresAt);
	}
}
