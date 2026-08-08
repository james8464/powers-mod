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
		List<Integer> cooldownTicks, List<Integer> cooldownMaximums,
		int energy, int energyCapacity, boolean canSeeDarkRealm, boolean darkness,
		boolean projection, int elementalPhase, RankSnapshot rank) implements CustomPacketPayload {
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
					ByteBufCodecs.VAR_INT,
					PowerStatePayload::energy,
					ByteBufCodecs.VAR_INT,
					PowerStatePayload::energyCapacity,
					ByteBufCodecs.BOOL,
					PowerStatePayload::canSeeDarkRealm,
					ByteBufCodecs.BOOL,
					PowerStatePayload::darkness,
					ByteBufCodecs.BOOL,
					PowerStatePayload::projection,
					ByteBufCodecs.VAR_INT,
					PowerStatePayload::elementalPhase,
					RankSnapshot.STREAM_CODEC,
					PowerStatePayload::rank,
					PowerStatePayload::new);

	/** Copies collection values so sync deduplication cannot observe later attachment mutations. */
	public PowerStatePayload {
		powerIds = List.copyOf(powerIds);
		activeToggles = List.copyOf(activeToggles);
		cooldownTicks = List.copyOf(cooldownTicks);
		cooldownMaximums = List.copyOf(cooldownMaximums);
		rank = Objects.requireNonNull(rank, "rank");
	}

	/** Convenience constructor keeps state assembly independent of the wire grouping. */
	public PowerStatePayload(List<String> powerIds, List<String> activeToggles,
			List<Integer> cooldownTicks, List<Integer> cooldownMaximums,
			int energy, int energyCapacity, boolean canSeeDarkRealm, boolean darkness,
			boolean projection, int elementalPhase, List<String> rankNodes, String rankFocus,
			int rankDepth) {
		this(powerIds, activeToggles, cooldownTicks, cooldownMaximums, energy, energyCapacity,
				canSeeDarkRealm, darkness, projection, elementalPhase,
				new RankSnapshot(rankNodes, rankFocus, rankDepth));
	}

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
}
