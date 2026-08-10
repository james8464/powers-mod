package com.powers.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Persistent lodestone destination copied with the relic's item stack. */
public record TravelAnchorData(Identifier dimension, int x, int y, int z) {
	public static final Codec<TravelAnchorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("dimension").forGetter(TravelAnchorData::dimension),
			Codec.INT.fieldOf("x").forGetter(TravelAnchorData::x),
			Codec.INT.fieldOf("y").forGetter(TravelAnchorData::y),
			Codec.INT.fieldOf("z").forGetter(TravelAnchorData::z))
			.apply(instance, TravelAnchorData::new));

	public TravelAnchorData {
		if (dimension == null) throw new IllegalArgumentException("Travel anchor needs a dimension");
	}

	public BlockPos position() {
		return new BlockPos(x, y, z);
	}
}
