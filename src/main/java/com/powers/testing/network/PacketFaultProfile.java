package com.powers.testing.network;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Immutable deterministic packet-fault configuration, expressed entirely in game ticks. */
public record PacketFaultProfile(String id, long seed, Set<PacketFaultDirection> directions,
		Set<PacketFaultFamily> families, int delayTicks, int lossPerTenThousand,
		int duplicatePerTenThousand, int reorderWindowTicks, int queueLimit,
		int lifetimeTicks, int workPerTick) {
	private static final int DEFAULT_QUEUE_LIMIT = 256;
	private static final int DEFAULT_LIFETIME = 40;
	private static final int DEFAULT_WORK = 64;

	public PacketFaultProfile {
		id = Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
		if (id.isEmpty() || id.length() > 24) throw new IllegalArgumentException("invalid profile id");
		directions = Set.copyOf(Objects.requireNonNull(directions, "directions"));
		families = Set.copyOf(Objects.requireNonNull(families, "families"));
		if (delayTicks < 0 || reorderWindowTicks < 0 || queueLimit < 1
				|| lifetimeTicks < 1 || workPerTick < 1
				|| lossPerTenThousand < 0 || lossPerTenThousand > 10_000
				|| duplicatePerTenThousand < 0 || duplicatePerTenThousand > 10_000) {
			throw new IllegalArgumentException("invalid packet-fault bounds");
		}
	}

	public static PacketFaultProfile disabled() {
		return new PacketFaultProfile("disabled", 0L, Set.of(), Set.of(), 0, 0, 0, 0,
				DEFAULT_QUEUE_LIMIT, DEFAULT_LIFETIME, DEFAULT_WORK);
	}

	public static PacketFaultProfile named(String id, long seed) {
		String normalized = Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
		EnumSet<PacketFaultDirection> directions = EnumSet.allOf(PacketFaultDirection.class);
		EnumSet<PacketFaultFamily> families = EnumSet.allOf(PacketFaultFamily.class);
		return switch (normalized) {
			case "disabled", "off" -> disabled();
			case "delay150" -> profile(normalized, seed, directions, families, 3, 0, 0, 0);
			case "delay300" -> profile(normalized, seed, directions, families, 6, 0, 0, 0);
			case "loss1" -> profile(normalized, seed, directions, families, 0, 100, 0, 0);
			case "loss5" -> profile(normalized, seed, directions, families, 0, 500, 0, 0);
			case "duplicate" -> profile(normalized, seed, directions, families, 0, 0, 10_000, 0);
			case "reorder" -> profile(normalized, seed, directions, families, 0, 0, 0, 2);
			default -> throw new IllegalArgumentException("unknown packet-fault profile: " + id);
		};
	}

	private static PacketFaultProfile profile(String id, long seed,
			Set<PacketFaultDirection> directions, Set<PacketFaultFamily> families,
			int delay, int loss, int duplicate, int reorder) {
		return new PacketFaultProfile(id, seed, directions, families, delay, loss, duplicate,
				reorder, DEFAULT_QUEUE_LIMIT, DEFAULT_LIFETIME, DEFAULT_WORK);
	}

	public boolean enabled() {
		return !"disabled".equals(id) && (!directions.isEmpty() && !families.isEmpty());
	}

	public boolean targets(PacketFaultDirection direction, PacketFaultFamily family) {
		return enabled() && directions.contains(direction) && families.contains(family);
	}
}
