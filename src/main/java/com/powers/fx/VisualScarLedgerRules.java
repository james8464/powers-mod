package com.powers.fx;

import java.util.Objects;
import java.util.UUID;

/** Defines bounded, deterministic server-ledger decisions without reading Minecraft state. */
public final class VisualScarLedgerRules {
	private VisualScarLedgerRules() {
	}

	/** Returns the first hard-cap denial for an attempted active and queued reservation. */
	public static Admission reserve(int ownerActive, int globalActive,
			int ownerQueued, int globalQueued, VisualScarRules.Limits limits) {
		Objects.requireNonNull(limits, "limits");
		if (ownerActive < 0 || globalActive < 0 || ownerQueued < 0 || globalQueued < 0) {
			throw new IllegalArgumentException("ledger counts cannot be negative");
		}
		if (ownerActive >= limits.activePerOwner()) return Admission.DENY_ACTIVE_OWNER;
		if (globalActive >= limits.activeGlobal()) return Admission.DENY_ACTIVE_GLOBAL;
		if (ownerQueued >= limits.queuedPerOwner()) return Admission.DENY_QUEUE_OWNER;
		if (globalQueued >= limits.queuedGlobal()) return Admission.DENY_QUEUE_GLOBAL;
		return Admission.ALLOW;
	}

	/** Returns a retain/remove decision using only independently loaded support observations. */
	public static Revalidation revalidate(Record record, boolean supportLoaded,
			boolean originLoaded, boolean supportValid, long currentFingerprint) {
		Objects.requireNonNull(record, "record");
		if (!supportLoaded || !originLoaded) return Revalidation.RETAIN_UNLOADED;
		return supportValid && record.supportFingerprint() == currentFingerprint
				? Revalidation.RETAIN : Revalidation.REMOVE_STALE;
	}

	/** Starts a cursor when bounded movement or teleport observation requires active-state resync. */
	public static MovementObservation observeMovement(ObserverSession session,
			double oldX, double oldY, double oldZ, double newX, double newY, double newZ,
			boolean forced) {
		Objects.requireNonNull(session, "session");
		double dx = newX - oldX;
		double dy = newY - oldY;
		double dz = newZ - oldZ;
		boolean moved = forced || !Double.isFinite(dx + dy + dz)
				|| dx * dx + dy * dy + dz * dz >= 1.0;
		return new MovementObservation(session, moved, 0);
	}

	/** Reports whether UUID, connection identity, dimension, and session generation still match. */
	public static boolean sessionCurrent(ObserverSession captured, ObserverSession current) {
		return Objects.equals(captured, current);
	}

	public enum Admission {
		ALLOW, DENY_ACTIVE_OWNER, DENY_ACTIVE_GLOBAL, DENY_QUEUE_OWNER, DENY_QUEUE_GLOBAL
	}

	public enum Revalidation { RETAIN_UNLOADED, RETAIN, REMOVE_STALE }

	public record Request(String dimension, long providerPolicyId, UUID owner,
			VisualScarRules.Impact impact) {
		public Request {
			dimension = Objects.requireNonNull(dimension, "dimension");
			owner = Objects.requireNonNull(owner, "owner");
			impact = Objects.requireNonNull(impact, "impact");
			if (dimension.isBlank() || providerPolicyId < 0) {
				throw new IllegalArgumentException("invalid request lane");
			}
		}
	}

	public record Record(String dimension, long position, VisualScarRules.Face face,
			UUID owner, VisualScarRules.Impact impact, VisualScarRules.Material material,
			int visualSeed, long generation, long supportFingerprint,
			long createdAt, long expiresAt) {
		public Record {
			dimension = Objects.requireNonNull(dimension, "dimension");
			face = Objects.requireNonNull(face, "face");
			owner = Objects.requireNonNull(owner, "owner");
			impact = Objects.requireNonNull(impact, "impact");
			material = Objects.requireNonNull(material, "material");
			if (dimension.isBlank() || generation <= 0 || createdAt < 0 || expiresAt < createdAt
					|| expiresAt - createdAt < 40 || expiresAt - createdAt > 1_200) {
				throw new IllegalArgumentException("invalid scar lifetime or generation");
			}
		}
	}

	public record ObserverSession(UUID player, long connectionIdentity,
			String dimension, long sessionGeneration) {
		public ObserverSession {
			player = Objects.requireNonNull(player, "player");
			dimension = Objects.requireNonNull(dimension, "dimension");
			if (dimension.isBlank() || connectionIdentity < 0 || sessionGeneration < 0) {
				throw new IllegalArgumentException("invalid observer session");
			}
		}
	}

	public record MovementObservation(ObserverSession session,
			boolean needsResync, int materializedRecords) {
		public MovementObservation {
			session = Objects.requireNonNull(session, "session");
			if (materializedRecords < 0) {
				throw new IllegalArgumentException("invalid movement observation");
			}
		}
	}
}
