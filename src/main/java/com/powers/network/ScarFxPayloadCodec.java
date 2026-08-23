package com.powers.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

/** Encodes the exact fixed-field visual-scar wire contract without owning delivery state. */
final class ScarFxPayloadCodec {
	private ScarFxPayloadCodec() {
	}

	static void encode(RegistryFriendlyByteBuf buffer, MagicFxPackets.ScarFxPayload payload) {
		buffer.writeVarInt(payload.operation());
		buffer.writeLong(payload.position());
		buffer.writeByte(payload.face());
		buffer.writeByte(payload.impact());
		buffer.writeByte(payload.material());
		buffer.writeInt(payload.visualSeed());
		buffer.writeVarLong(payload.generation());
		buffer.writeVarInt(payload.leaseTicks());
	}

	static MagicFxPackets.ScarFxPayload decode(RegistryFriendlyByteBuf buffer) {
		return new MagicFxPackets.ScarFxPayload(buffer.readVarInt(), buffer.readLong(),
				buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(),
				buffer.readInt(), buffer.readVarLong(), buffer.readVarInt());
	}
}
