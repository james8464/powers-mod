package com.powers;

import com.powers.item.artifact.ArtifactIdentity;
import com.powers.forge.CrucibleWeaponData;
import com.powers.item.TravelAnchorData;
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
	public static final DataComponentType<CrucibleWeaponData> CRUCIBLE_WEAPON = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, PowersMod.id("crucible_weapon"),
			DataComponentType.<CrucibleWeaponData>builder().persistent(CrucibleWeaponData.CODEC)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CrucibleWeaponData.CODEC)).build());
	public static final DataComponentType<TravelAnchorData> TRAVEL_ANCHOR = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, PowersMod.id("travel_anchor"),
			DataComponentType.<TravelAnchorData>builder().persistent(TravelAnchorData.CODEC)
					.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(TravelAnchorData.CODEC)).build());

	private PowersDataComponents() {
	}

	public static void initialize() {
		// Class loading performs the registry insertion before any artifact item is constructed.
	}
}
