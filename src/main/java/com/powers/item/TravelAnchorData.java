package com.powers.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Persistent named lodestone destination copied with the relic's item stack. */
public record TravelAnchorData(Identifier dimension, int x, int y, int z, String name) {
	public static final Codec<TravelAnchorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("dimension").forGetter(TravelAnchorData::dimension),
			Codec.INT.fieldOf("x").forGetter(TravelAnchorData::x),
			Codec.INT.fieldOf("y").forGetter(TravelAnchorData::y),
			Codec.INT.fieldOf("z").forGetter(TravelAnchorData::z),
			Codec.STRING.optionalFieldOf("name", "").forGetter(TravelAnchorData::name))
			.apply(instance, TravelAnchorData::new));

	public TravelAnchorData {
		if (dimension == null) throw new IllegalArgumentException("Travel anchor needs a dimension");
		name = MiniportalRules.anchorName(name,
				dimension.getPath() + " " + x + ", " + y + ", " + z);
	}

	public TravelAnchorData(Identifier dimension, int x, int y, int z) {
		this(dimension, x, y, z, "");
	}

	public BlockPos position() {
		return new BlockPos(x, y, z);
	}
}
