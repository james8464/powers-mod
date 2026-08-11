package com.powers.magic.runtime;

import com.powers.magic.ActionPair;
import com.powers.magic.InteractionContext;
import com.powers.magic.InteractionResolution;
import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionId;
import com.powers.magic.MagicInteractionResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-thread transaction coordinator for spatial magic interactions. The
 * global instance is safe because a Minecraft server executes these methods on
 * its server thread; tests may construct isolated instances with real pure
 * dependencies. No ability presence is registered until its execution reports
 * success.
 */
public final class MagicRuntime {
	private record CueKey(ActionPair pair, String dimension, int x, int y, int z, long gameTime) {
	}

	private static final MagicActionCatalogue GLOBAL_CATALOGUE = MagicActionCatalogue.defaults();
	private static final MagicRuntime GLOBAL = new MagicRuntime(GLOBAL_CATALOGUE,
			MagicInteractionResolver.defaults(GLOBAL_CATALOGUE), new ActiveMagicIndex(16));

	private final MagicActionCatalogue catalogue;
	private final MagicInteractionResolver resolver;
	private final ActiveMagicIndex index;
	private final Set<CueKey> emittedCues = new HashSet<>();

	/** Creates an isolated runtime; production callers normally use {@link #global()}. */
	public MagicRuntime(MagicActionCatalogue catalogue, MagicInteractionResolver resolver,
			ActiveMagicIndex index) {
		this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
		this.resolver = Objects.requireNonNull(resolver, "resolver");
		this.index = Objects.requireNonNull(index, "index");
	}

	/** Returns the one server-thread-owned production runtime. */
	public static MagicRuntime global() {
		return GLOBAL;
	}

	/** Returns the canonical catalogue used by the production cast adapters. */
	public static MagicActionCatalogue catalogue() {
		return GLOBAL_CATALOGUE;
	}

	/** Resolves two trusted runtime action IDs through the canonical matrix. */
	public InteractionResolution resolveInteraction(String firstAction, String secondAction) {
		var first = catalogue.definition(new MagicActionId(firstAction));
		var second = catalogue.definition(new MagicActionId(secondAction));
		if (first == null || second == null) {
			throw new IllegalArgumentException("Runtime ray action is not registered");
		}
		return resolver.resolve(first, second, InteractionContext.DEFAULT);
	}

	/**
	 * Resolves all nearby magic before payment/cooldown commit without emitting
	 * presentation or gameplay reactions.
	 */
	public MagicCastPreview previewCast(MagicCastContext cast) {
		Objects.requireNonNull(cast, "cast");
		if (catalogue.definition(cast.definition().id()) == null) {
			throw new IllegalArgumentException("Cast action is not registered: " + cast.definition().id());
		}
		List<MagicPresence> nearby = index.nearby(cast.dimension(), cast.anchor().x(), cast.anchor().y(),
				cast.anchor().z(), cast.queryRadius(), cast.gameTime());
		List<MagicReactionEvent> reactions = new ArrayList<>(nearby.size());
		for (MagicPresence presence : nearby) {
			var existingDefinition = catalogue.definition(presence.action());
			if (existingDefinition == null) continue;
			InteractionResolution resolution = resolver.resolve(cast.definition(), existingDefinition,
					cast.interactionContext());
			reactions.add(new MagicReactionEvent(cast, presence, resolution));
		}
		List<MagicReactionEvent> ordered = MagicInteractionArbitrator.orderEvents(reactions);
		List<InteractionResolution> orderedResolutions = ordered.stream()
				.map(MagicReactionEvent::resolution).toList();
		return new MagicCastPreview(CastAdjustment.combine(orderedResolutions), ordered);
	}

	/** Emits a preview's reactions at most once per action pair, spatial cell, and tick. */
	public void emitReactions(MagicCastPreview preview, MagicReactionSink sink) {
		emitMatchingReactions(preview, sink, ignored -> true);
	}

	/** Emits only reactions that reject the attempted cast, excluding unrelated nearby fields. */
	public void emitBlockingReactions(MagicCastPreview preview, MagicReactionSink sink) {
		emitMatchingReactions(preview, sink, event -> event.resolution().blocksFirst());
	}

	private void emitMatchingReactions(MagicCastPreview preview, MagicReactionSink sink,
			Predicate<MagicReactionEvent> filter) {
		Objects.requireNonNull(preview, "preview");
		Objects.requireNonNull(sink, "sink");
		Objects.requireNonNull(filter, "filter");
		for (MagicReactionEvent event : preview.reactions()) {
			if (!filter.test(event)) continue;
			CueKey key = cueKey(event.cast(), event.existing());
			if (emittedCues.add(key)) sink.emit(event);
		}
	}

	/**
	 * Registers a short interaction residue after successful ability execution.
	 * Calling this for a blocked adjustment is a programming error.
	 */
	public MagicPresenceId commitCast(MagicCastContext cast, CastAdjustment adjustment) {
		Objects.requireNonNull(cast, "cast");
		Objects.requireNonNull(adjustment, "adjustment");
		if (!adjustment.allowed()) throw new IllegalStateException("Blocked magic cannot commit");
		MagicPresenceId id = MagicPresenceId.random();
		double radius = Math.max(1.0, Math.min(128.0,
				cast.definition().baseRange() * adjustment.rangeMultiplier()));
		long duration = Math.max(1L, Math.round(cast.definition().residueTicks()
				* adjustment.durationMultiplier()));
		index.register(new MagicPresence(id, cast.definition().id(), cast.owner(), cast.dimension(),
				cast.anchor(), radius, cast.gameTime() + duration));
		return id;
	}

	/** Registers an explicitly constructed long-lived field or entity presence. */
	public void registerPresence(MagicPresence presence) {
		if (catalogue.definition(presence.action()) == null) {
			throw new IllegalArgumentException("Presence action is not registered: " + presence.action());
		}
		index.register(presence);
	}

	/** Removes one explicitly managed field or impact presence before expiry. */
	public boolean removePresence(MagicPresenceId id) {
		return index.remove(Objects.requireNonNull(id, "id"));
	}

	/** Rebinds a committed residue to its physical object and authoritative lifetime. */
	public boolean rebindPresence(MagicPresenceId id, String dimension,
			PresenceAnchor anchor, long expiresAt) {
		return index.rebind(Objects.requireNonNull(id, "id"),
				Objects.requireNonNull(dimension, "dimension"),
				Objects.requireNonNull(anchor, "anchor"), expiresAt);
	}

	/** Moves an already-bound entity or projectile without changing its lifetime. */
	public boolean movePresence(MagicPresenceId id, String dimension, PresenceAnchor anchor) {
		return index.move(Objects.requireNonNull(id, "id"),
				Objects.requireNonNull(dimension, "dimension"), Objects.requireNonNull(anchor, "anchor"));
	}

	/** Expires old presences and reaction-deduplication keys at the current tick. */
	public int tick(long gameTime) {
		emittedCues.removeIf(key -> key.gameTime() < gameTime);
		return index.expire(gameTime);
	}

	/** Removes all active magic belonging to one player lifecycle. */
	public int clearOwner(UUID owner) {
		return index.removeOwner(owner);
	}

	/** Clears every runtime map during server stop or full reload. */
	public void clearAll() {
		index.clear();
		emittedCues.clear();
	}

	/** Returns the current deduplication-key count for diagnostics. */
	public int pendingCueKeys() {
		return emittedCues.size();
	}

	/** Returns the exhaustive same-or-cross-action pair count for diagnostics. */
	public int interactionCount() {
		return resolver.allPairs().size();
	}

	/** Active physical/residue presence count for live server diagnostics. */
	public int activePresenceCount() {
		return index.size();
	}

	/** Allocated chunk-sized cells in the active-magic spatial index. */
	public int activePresenceCellCount() {
		return index.cellCount();
	}

	/** Exact physical presence lookup; returns null after cleanup or expiry. */
	MagicPresence presence(MagicPresenceId id) {
		return index.get(Objects.requireNonNull(id, "id"));
	}

	/** Bounded indexed physical overlap candidates for the live handle bridge. */
	List<MagicPresence> nearbyPhysical(MagicPresence presence, double radius, long gameTime) {
		return index.nearby(presence.dimension(), presence.anchor().x(), presence.anchor().y(),
				presence.anchor().z(), radius, gameTime);
	}

	/** Bounded indexed query used by exact physical geometry adapters. */
	List<MagicPresence> indexedNearby(String dimension, net.minecraft.world.phys.Vec3 point,
			double radius, long gameTime) {
		return index.nearby(dimension, point.x, point.y, point.z, radius, gameTime);
	}

	private static CueKey cueKey(MagicCastContext cast, MagicPresence presence) {
		return new CueKey(ActionPair.of(cast.definition().id(), presence.action()), cast.dimension(),
				floorCell(cast.anchor().x()), floorCell(cast.anchor().y()), floorCell(cast.anchor().z()),
				cast.gameTime());
	}

	private static int floorCell(double coordinate) {
		return (int) Math.floor(coordinate / 8.0);
	}
}
