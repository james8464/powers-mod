package com.powers;

import com.powers.item.artifact.ArtifactIdentity;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/** Registers hidden, persistent item-stack identity used by mythic artifacts. */
public final class PowersDataComponents {
	public static final DataComponentType<ArtifactIdentity> ARTIFACT_IDENTITY = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, PowersMod.id("artifact_identity"),
			DataComponentType.<ArtifactIdentity>builder().persistent(ArtifactIdentity.CODEC)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(ArtifactIdentity.CODEC)).build());

	private PowersDataComponents() {
	}

	public static void initialize() {
		// Class loading performs the registry insertion before any artifact item is constructed.
	}
}
