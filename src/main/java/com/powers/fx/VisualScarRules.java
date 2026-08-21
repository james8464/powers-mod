package com.powers.fx;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Defines immutable admission, classification, and hard-limit rules for transient visual scars. */
public final class VisualScarRules {
	private VisualScarRules() {
	}

	/** Returns the exact denial or approval for already-observed support and origin facts. */
	public static Admission admit(SupportFacts facts) {
		Objects.requireNonNull(facts, "facts");
		if (!facts.supportLoaded()) return Admission.DENY_SUPPORT_UNLOADED;
		if (!facts.originLoaded()) return Admission.DENY_ORIGIN_UNLOADED;
		if (!facts.supportPolicy()) return Admission.DENY_SUPPORT_PROTECTED;
		if (facts.supportBlockEntity()) return Admission.DENY_SUPPORT_BLOCK_ENTITY;
		if (facts.supportFluid()) return Admission.DENY_SUPPORT_FLUID;
		if (!facts.sturdyFace()) return Admission.DENY_SUPPORT_FACE;
		if (!facts.classifiable()) return Admission.DENY_SUPPORT_MATERIAL;
		if (!facts.originOpen()) return Admission.DENY_ORIGIN_OCCLUDED;
		if (facts.originBlockEntity()) return Admission.DENY_ORIGIN_BLOCK_ENTITY;
		if (facts.originFluid()) return Admission.DENY_ORIGIN_FLUID;
		return Admission.ALLOW;
	}

	/** Returns the one matching closed material class, rejecting ambiguous or absent facts. */
	public static Optional<Material> classify(MaterialFacts facts) {
		Objects.requireNonNull(facts, "facts");
		boolean[] values = {facts.stone(), facts.earth(), facts.wood(), facts.metal(),
				facts.sand(), facts.cold()};
		int match = -1;
		for (int index = 0; index < values.length; index++) {
			if (!values[index]) continue;
			if (match >= 0) return Optional.empty();
			match = index;
		}
		return match < 0 ? Optional.empty() : Optional.of(Material.values()[match]);
	}

	/** Coalesces same-owner requests for one surface key, rejecting cross-owner laundering. */
	public static Optional<Request> coalesce(Request current, Request latest) {
		Objects.requireNonNull(current, "current");
		Objects.requireNonNull(latest, "latest");
		if (!current.dimension().equals(latest.dimension())
				|| current.position() != latest.position() || current.face() != latest.face()
				|| !current.owner().equals(latest.owner())) return Optional.empty();
		return Optional.of(latest);
	}

	/** Returns the immutable presentation-only authority boundary for this feature. */
	public static PresentationContract presentationOnly() {
		return new PresentationContract(false, false, false, false);
	}

	public enum Admission {
		ALLOW,
		DENY_SUPPORT_UNLOADED,
		DENY_ORIGIN_UNLOADED,
		DENY_SUPPORT_PROTECTED,
		DENY_SUPPORT_BLOCK_ENTITY,
		DENY_SUPPORT_FLUID,
		DENY_SUPPORT_FACE,
		DENY_SUPPORT_MATERIAL,
		DENY_ORIGIN_OCCLUDED,
		DENY_ORIGIN_BLOCK_ENTITY,
		DENY_ORIGIN_FLUID
	}

	public enum Material { STONE, EARTH, WOOD, METAL, SAND, COLD }

	public enum Impact { BEAM, SLAM, THUNDERCLAP, ICE, FIRE }

	public enum Face { DOWN, UP, NORTH, SOUTH, WEST, EAST }

	public record SupportFacts(boolean supportLoaded, boolean originLoaded,
			boolean supportPolicy, boolean supportBlockEntity, boolean supportFluid,
			boolean sturdyFace, boolean classifiable, boolean originOpen,
			boolean originBlockEntity, boolean originFluid) {
	}

	public record MaterialFacts(boolean stone, boolean earth, boolean wood,
			boolean metal, boolean sand, boolean cold) {
	}

	public record Request(String dimension, long position, Face face, UUID owner,
			Impact impact, int visualSeed, long requestedAt) {
		public Request {
			dimension = Objects.requireNonNull(dimension, "dimension");
			face = Objects.requireNonNull(face, "face");
			owner = Objects.requireNonNull(owner, "owner");
			impact = Objects.requireNonNull(impact, "impact");
			if (dimension.isBlank() || requestedAt < 0) {
				throw new IllegalArgumentException("invalid visual scar request");
			}
		}
	}

	public record PresentationContract(boolean mutatesTerrain, boolean loadsChunks,
			boolean persists, boolean registersWorldObjects) {
	}

	public record Limits(int activePerOwner, int activeGlobal, int queuedPerOwner,
			int queuedGlobal, int requestsPerTick, int revalidationsPerTick,
			int sendsPerTick, int pendingPerObserver, int pendingGlobal,
			int maximumLease) {
		private static final int ACTIVE_OWNER_HARD = 128;
		private static final int ACTIVE_GLOBAL_HARD = 2_048;
		private static final int QUEUED_OWNER_HARD = 128;
		private static final int QUEUED_GLOBAL_HARD = 2_048;
		private static final int REQUEST_HARD = 64;
		private static final int REVALIDATION_HARD = 64;
		private static final int SEND_HARD = 256;
		private static final int PENDING_OBSERVER_HARD = 2_048;
		private static final int PENDING_GLOBAL_HARD = 32_768;
		private static final int LEASE_HARD = 1_200;
		private static final Limits HARD = new Limits(ACTIVE_OWNER_HARD, ACTIVE_GLOBAL_HARD,
				QUEUED_OWNER_HARD, QUEUED_GLOBAL_HARD, REQUEST_HARD, REVALIDATION_HARD,
				SEND_HARD, PENDING_OBSERVER_HARD, PENDING_GLOBAL_HARD, LEASE_HARD);

		public Limits {
			int[] values = {activePerOwner, activeGlobal, queuedPerOwner, queuedGlobal,
					requestsPerTick, revalidationsPerTick, sendsPerTick, pendingPerObserver,
					pendingGlobal, maximumLease};
			int[] ceilings = {ACTIVE_OWNER_HARD, ACTIVE_GLOBAL_HARD, QUEUED_OWNER_HARD,
					QUEUED_GLOBAL_HARD, REQUEST_HARD, REVALIDATION_HARD, SEND_HARD,
					PENDING_OBSERVER_HARD, PENDING_GLOBAL_HARD, LEASE_HARD};
			for (int index = 0; index < values.length; index++) {
				if (values[index] < 1 || values[index] > ceilings[index]) {
					throw new IllegalArgumentException("visual scar limit exceeds hard bounds");
				}
			}
			if (maximumLease < 40) throw new IllegalArgumentException("lease is shorter than minimum scar life");
		}

		/** Returns the immutable hard ceilings that configuration may only lower. */
		public static Limits hardCeilings() {
			return HARD;
		}

		/** Returns a lowered owner queue ceiling and rejects values above the hard cap. */
		public Limits withQueuedPerOwner(int value) {
			if (value < 1 || value > HARD.queuedPerOwner) {
				throw new IllegalArgumentException("queued owner cap exceeds hard bounds");
			}
			return new Limits(activePerOwner, activeGlobal, value, queuedGlobal,
					requestsPerTick, revalidationsPerTick, sendsPerTick,
					pendingPerObserver, pendingGlobal, maximumLease);
		}
	}
}
