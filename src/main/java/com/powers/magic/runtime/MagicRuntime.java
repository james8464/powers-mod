package com.powers.magic.runtime;

import com.powers.magic.ActionPair;
import com.powers.magic.InteractionResolution;
import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicInteractionResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

	/**
	 * Resolves all nearby magic before payment/cooldown commit. The sink is a
	 * presentation-only boundary and is invoked at most once per pair/cell/tick.
	 */
	public CastAdjustment beforeCast(MagicCastContext cast, MagicReactionSink sink) {
		Objects.requireNonNull(cast, "cast");
		Objects.requireNonNull(sink, "sink");
		if (catalogue.definition(cast.definition().id()) == null) {
			throw new IllegalArgumentException("Cast action is not registered: " + cast.definition().id());
		}
		List<MagicPresence> nearby = index.nearby(cast.dimension(), cast.anchor().x(), cast.anchor().y(),
				cast.anchor().z(), cast.queryRadius(), cast.gameTime());
		List<InteractionResolution> resolutions = new ArrayList<>(nearby.size());
		for (MagicPresence presence : nearby) {
			var existingDefinition = catalogue.definition(presence.action());
			if (existingDefinition == null) continue;
			InteractionResolution resolution = resolver.resolve(cast.definition(), existingDefinition,
					cast.interactionContext());
			resolutions.add(resolution);
			CueKey key = cueKey(cast, presence);
			if (emittedCues.add(key)) sink.emit(new MagicReactionEvent(cast, presence, resolution));
		}
		return CastAdjustment.combine(resolutions);
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

	private static CueKey cueKey(MagicCastContext cast, MagicPresence presence) {
		return new CueKey(ActionPair.of(cast.definition().id(), presence.action()), cast.dimension(),
				floorCell(cast.anchor().x()), floorCell(cast.anchor().y()), floorCell(cast.anchor().z()),
				cast.gameTime());
	}

	private static int floorCell(double coordinate) {
		return (int) Math.floor(coordinate / 8.0);
	}
}
