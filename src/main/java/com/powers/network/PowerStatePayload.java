package com.powers.network;

import com.powers.PowersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable server-authoritative snapshot used by the power HUD and rank maze. */
public record PowerStatePayload(List<String> powerIds, List<String> activeToggles,
		List<Integer> cooldownTicks, List<Integer> cooldownMaximums, List<Integer> reactivationTicks,
		EnergySnapshot energySnapshot, boolean canSeeDarkRealm, boolean darkness,
		boolean projection, int sizeMorphOption, SizeMorphSnapshot sizeMorph,
		RankSnapshot rank) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PowerStatePayload> TYPE =
			new CustomPacketPayload.Type<>(PowersMod.id("power_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PowerStatePayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
					PowerStatePayload::powerIds,
					ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
					PowerStatePayload::activeToggles,
					ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT),
					PowerStatePayload::cooldownTicks,
					ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT),
					PowerStatePayload::cooldownMaximums,
					ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT),
					PowerStatePayload::reactivationTicks,
					EnergySnapshot.STREAM_CODEC,
					PowerStatePayload::energySnapshot,
					ByteBufCodecs.BOOL,
					PowerStatePayload::canSeeDarkRealm,
					ByteBufCodecs.BOOL,
					PowerStatePayload::darkness,
					ByteBufCodecs.BOOL,
					PowerStatePayload::projection,
					ByteBufCodecs.VAR_INT,
					PowerStatePayload::sizeMorphOption,
					SizeMorphSnapshot.STREAM_CODEC,
					PowerStatePayload::sizeMorph,
					RankSnapshot.STREAM_CODEC,
					PowerStatePayload::rank,
					PowerStatePayload::new);

	/** Copies collection values so sync deduplication cannot observe later attachment mutations. */
	public PowerStatePayload {
		powerIds = List.copyOf(powerIds);
		activeToggles = List.copyOf(activeToggles);
		cooldownTicks = List.copyOf(cooldownTicks);
		cooldownMaximums = List.copyOf(cooldownMaximums);
		reactivationTicks = normalizedTimers(reactivationTicks, powerIds.size());
		energySnapshot = Objects.requireNonNull(energySnapshot, "energySnapshot");
		sizeMorph = Objects.requireNonNull(sizeMorph, "sizeMorph");
		rank = Objects.requireNonNull(rank, "rank");
	}

	private static List<Integer> normalizedTimers(List<Integer> timers, int slots) {
		Objects.requireNonNull(timers, "timers");
		List<Integer> normalized = new ArrayList<>(slots);
		for (int slot = 0; slot < slots; slot++) {
			Integer value = slot < timers.size() ? timers.get(slot) : null;
			normalized.add(value == null ? 0 : Math.max(0, value));
		}
		return List.copyOf(normalized);
	}

	/** Convenience constructor keeps state assembly independent of the wire grouping. */
	public PowerStatePayload(List<String> powerIds, List<String> activeToggles,
			List<Integer> cooldownTicks, List<Integer> cooldownMaximums, List<Integer> reactivationTicks,
			int energy, int energyCapacity, boolean canSeeDarkRealm, boolean darkness,
			boolean projection, int sizeMorphOption,
			List<String> rankNodes, String rankFocus,
			int rankDepth) {
		this(powerIds, activeToggles, cooldownTicks, cooldownMaximums, reactivationTicks,
				new EnergySnapshot(energy, energyCapacity, 0L, 0L, List.of()),
				canSeeDarkRealm, darkness, projection, sizeMorphOption,
				SizeMorphSnapshot.forSelection(sizeMorphOption, rankDepth),
				new RankSnapshot(rankNodes, rankFocus, rankDepth));
	}

	public PowerStatePayload(List<String> powerIds, List<String> activeToggles,
			List<Integer> cooldownTicks, List<Integer> cooldownMaximums, List<Integer> reactivationTicks,
			int energy, int energyCapacity, boolean canSeeDarkRealm, boolean darkness,
			boolean projection, int sizeMorphOption, List<String> rankNodes, String rankFocus,
			int rankDepth, long consumed, long restored, List<Long> sources) {
		this(powerIds, activeToggles, cooldownTicks, cooldownMaximums, reactivationTicks,
				new EnergySnapshot(energy, energyCapacity, consumed, restored, sources),
				canSeeDarkRealm, darkness, projection, sizeMorphOption,
				SizeMorphSnapshot.forSelection(sizeMorphOption, rankDepth),
				new RankSnapshot(rankNodes, rankFocus, rankDepth));
	}

	/** Full server-authored constructor used by live sync when collision fallback is active. */
	public PowerStatePayload(List<String> powerIds, List<String> activeToggles,
			List<Integer> cooldownTicks, List<Integer> cooldownMaximums, List<Integer> reactivationTicks,
			int energy, int energyCapacity, boolean canSeeDarkRealm, boolean darkness,
			boolean projection, int sizeMorphOption, List<String> rankNodes, String rankFocus,
			int rankDepth, long consumed, long restored, List<Long> sources,
			double appliedScale, double blockReach, double entityReach, int drainPerSecond) {
		this(powerIds, activeToggles, cooldownTicks, cooldownMaximums, reactivationTicks,
				new EnergySnapshot(energy, energyCapacity, consumed, restored, sources),
				canSeeDarkRealm, darkness, projection, sizeMorphOption,
				new SizeMorphSnapshot(appliedScale, blockReach, entityReach, drainPerSecond),
				new RankSnapshot(rankNodes, rankFocus, rankDepth));
	}

	public int energy() { return energySnapshot.energy(); }
	public int energyCapacity() { return energySnapshot.capacity(); }
	public long energyConsumed() { return energySnapshot.consumed(); }
	public long energyRestored() { return energySnapshot.restored(); }
	public List<Long> energySources() { return energySnapshot.sources(); }
	public double sizeMorphAppliedScale() { return sizeMorph.appliedScale(); }
	public double sizeMorphBlockReach() { return sizeMorph.blockReach(); }
	public double sizeMorphEntityReach() { return sizeMorph.entityReach(); }
	public int sizeMorphDrainPerSecond() { return sizeMorph.drainPerSecond(); }

	public List<String> rankNodes() {
		return rank.nodes();
	}

	public String rankFocus() {
		return rank.focus();
	}

	public int rankDepth() {
		return rank.depth();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** Groups rank-maze state into one value to stay within the codec's 12-field limit. */
	public record RankSnapshot(List<String> nodes, String focus, int depth) {
		private static final StreamCodec<RegistryFriendlyByteBuf, RankSnapshot> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
						RankSnapshot::nodes,
						ByteBufCodecs.STRING_UTF8,
						RankSnapshot::focus,
						ByteBufCodecs.VAR_INT,
						RankSnapshot::depth,
						RankSnapshot::new);

		public RankSnapshot {
			nodes = List.copyOf(nodes);
			focus = Objects.requireNonNull(focus, "focus");
		}
	}

	/** Authoritative applied Size Morphing values; selection alone cannot reveal fallback state. */
	public record SizeMorphSnapshot(double appliedScale, double blockReach,
			double entityReach, int drainPerSecond) {
		private static final StreamCodec<RegistryFriendlyByteBuf, SizeMorphSnapshot> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.DOUBLE, SizeMorphSnapshot::appliedScale,
						ByteBufCodecs.DOUBLE, SizeMorphSnapshot::blockReach,
						ByteBufCodecs.DOUBLE, SizeMorphSnapshot::entityReach,
						ByteBufCodecs.VAR_INT, SizeMorphSnapshot::drainPerSecond,
						SizeMorphSnapshot::new);

		public SizeMorphSnapshot {
			appliedScale = Double.isFinite(appliedScale) && appliedScale > 0.0 ? appliedScale : 1.0;
			blockReach = Double.isFinite(blockReach) ? Math.clamp(blockReach, 0.0, 64.0) : 4.5;
			entityReach = Double.isFinite(entityReach) ? Math.clamp(entityReach, 0.0, 64.0) : 3.0;
			drainPerSecond = Math.max(0, drainPerSecond);
		}

		private static SizeMorphSnapshot forSelection(int option, int rankDepth) {
			double scale = com.powers.power.abilities.SizeMorphRules.isValidOption(option)
					? com.powers.power.abilities.SizeMorphRules.scale(option) : 1.0;
			var profile = com.powers.power.abilities.SizeMorphRules.profile(scale);
			double reduction = Math.min(0.30, Math.clamp(rankDepth, 0, 10) * 0.025);
			int drain = profile.drainPerSecond() == 0 ? 0
					: Math.max(1, (int) Math.ceil(profile.drainPerSecond() * (1.0 - reduction)));
			return new SizeMorphSnapshot(scale, profile.blockReach(), profile.entityReach(), drain);
		}
	}

	/** Bounded energy state and aggregate history; no player identity is carried. */
	public record EnergySnapshot(int energy, int capacity, long consumed, long restored,
			List<Long> sources) {
		private static final StreamCodec<RegistryFriendlyByteBuf, EnergySnapshot> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, EnergySnapshot::energy,
						ByteBufCodecs.VAR_INT, EnergySnapshot::capacity,
						ByteBufCodecs.VAR_LONG, EnergySnapshot::consumed,
						ByteBufCodecs.VAR_LONG, EnergySnapshot::restored,
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_LONG),
						EnergySnapshot::sources,
						EnergySnapshot::new);

		public EnergySnapshot {
			energy = Math.max(0, energy);
			capacity = Math.max(0, capacity);
			consumed = Math.max(0L, consumed);
			restored = Math.max(0L, restored);
			sources = List.copyOf(sources.stream().limit(16).map(value ->
					value == null ? 0L : Math.max(0L, value)).toList());
		}
	}

}
