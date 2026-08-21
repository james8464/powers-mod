package com.powers.testing.network;

import com.powers.network.BodyProxyPackets;
import com.powers.network.ActionSubmissionService;
import com.powers.network.CelestialRuinPackets;
import com.powers.network.CompanionPackets;
import com.powers.network.GrimoirePackets;
import com.powers.network.PowersPackets;
import com.powers.network.RankPackets;
import com.powers.network.RelicPackets;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.VesselControlPackets;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Derives a bounded logical state-owner key without exposing payload contents in diagnostics. */
public final class PacketFaultStreams {
	public static final int MAX_KEY_LENGTH = 64;

	private PacketFaultStreams() {
	}

	public static String key(CustomPacketPayload payload) {
		String type = payload.type().id().getPath();
		if (payload instanceof PowersPackets.SelectAbilityOptionPayload value) {
			return compact(type, Integer.toString(value.slot()));
		}
		if (payload instanceof ShadowSwordPackets.SelectPayload value) {
			return compact(type, value.alignment());
		}
		if (payload instanceof ShadowSwordPackets.BindFavouritePayload value) {
			return compact(type, value.alignment() + ':' + value.slot());
		}
		if (payload instanceof GrimoirePackets.SelectSpellPayload value) {
			return compact(type, value.grimoireKey());
		}
		if (payload instanceof GrimoirePackets.OpenIndexPayload value) {
			return compact(type, value.grimoireKey());
		}
		if (payload instanceof ShadowSwordPackets.OpenMenuPayload value) {
			return compact(type, value.alignment());
		}
		if (payload instanceof ShadowSwordPackets.OpenTeleportPayload value) {
			return compact(type, value.alignment());
		}
		if (payload instanceof ActionSubmissionService.RefreshPayload value) {
			return compact(type, value.surface());
		}
		if (payload instanceof RelicPackets.OpenReservoirPayload value) {
			return compact(type, Integer.toString(value.slot()));
		}
		if (payload instanceof BodyProxyPackets.BodySnapshotPayload value) {
			return compact(type, Integer.toString(value.entityId()));
		}
		if (payload instanceof CompanionPackets.StatePayload value) {
			return compact(type, owner(value.ownerId()) + ':' + value.sessionId());
		}
		if (payload instanceof CompanionPackets.StatusPayload value) {
			return compact(type, owner(value.ownerId()));
		}
		if (payload instanceof VesselControlPackets.StatePayload) {
			return type;
		}
		if (payload instanceof CelestialRuinPackets.Payload value) {
			long position = mix(Double.doubleToLongBits(value.x()), Double.doubleToLongBits(value.y()),
					Double.doubleToLongBits(value.z()));
			return compact(type, Long.toUnsignedString(position, 16));
		}
		if (payload instanceof RankPackets.RankActionPayload value) {
			return compact(type, value.nodeId());
		}
		return type.length() <= MAX_KEY_LENGTH ? type : compact(type, type);
	}

	private static String owner(UUID owner) {
		return Long.toUnsignedString(mix(owner.getMostSignificantBits(), owner.getLeastSignificantBits(), 0L), 16);
	}

	private static String compact(String type, String discriminator) {
		String candidate = type + ':' + discriminator;
		if (candidate.length() <= MAX_KEY_LENGTH) return candidate;
		long hash = 0xcbf29ce484222325L;
		for (byte value : candidate.getBytes(StandardCharsets.UTF_8)) {
			hash = (hash ^ (value & 0xffL)) * 0x100000001b3L;
		}
		String prefix = type.substring(0, Math.min(type.length(), 44));
		return prefix + ':' + Long.toUnsignedString(hash, 16);
	}

	private static long mix(long first, long second, long third) {
		long value = first ^ Long.rotateLeft(second, 21) ^ Long.rotateLeft(third, 42);
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53L;
		return value ^ value >>> 33;
	}
}
