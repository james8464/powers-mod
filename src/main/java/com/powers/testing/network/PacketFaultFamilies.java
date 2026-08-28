package com.powers.testing.network;

import com.powers.PowersMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Exhaustive semantic classifier for project-owned play payload identifiers. */
public final class PacketFaultFamilies {
	private PacketFaultFamilies() {
	}

	public static PacketFaultFamily classify(CustomPacketPayload payload) {
		return classify(payload.type());
	}

	public static PacketFaultFamily classify(CustomPacketPayload.Type<?> type) {
		if (!isProjectOwned(type)) {
			return PacketFaultFamily.OTHER;
		}
		return switch (type.id().getPath()) {
			case "activate_ability" -> PacketFaultFamily.ABILITY_ACTIVATION;
			case "select_ability_option" -> PacketFaultFamily.ABILITY_SELECTION;
			case "teleport_request", "teleport_mark" -> PacketFaultFamily.TELEPORT_REQUEST;
			case "select_shadow_sword" -> PacketFaultFamily.ARTIFACT_SELECTION;
			case "cycle_artifact_action" -> PacketFaultFamily.ARTIFACT_CYCLE;
			case "bind_artifact_favourite" -> PacketFaultFamily.ARTIFACT_BINDING;
			case "commit_artifact_wheel", "shadow_sword_teleport" -> PacketFaultFamily.ARTIFACT_CAST;
			case "select_grimoire_spell" -> PacketFaultFamily.GRIMOIRE_SELECTION;
			case "select_crystal_mode" -> PacketFaultFamily.CRYSTAL_SELECTION;
			case "rank_action" -> PacketFaultFamily.RANK_ACTION;
			case "transfer_reservoir" -> PacketFaultFamily.RELIC_TRANSFER;
			case "locate_target" -> PacketFaultFamily.LOCATOR_REQUEST;
			case "companion_interact" -> PacketFaultFamily.COMPANION_REQUEST;
			case "vessel_control_input" -> PacketFaultFamily.VESSEL_INPUT;
			case "vessel_control_release" -> PacketFaultFamily.VESSEL_RELEASE;
			case "open_shadow_sword", "open_shadow_teleport", "open_grimoire_index",
					"open_crystal_selector", "open_reservoir", "open_locator", "action_refresh" ->
					PacketFaultFamily.MENU_SNAPSHOT;
			case "power_state" -> PacketFaultFamily.POWER_STATE;
			case "body_snapshot" -> PacketFaultFamily.BODY_SNAPSHOT;
			case "companion_state", "companion_status", "vessel_control_state" ->
					PacketFaultFamily.COMPANION_SNAPSHOT;
			case "magic_fx" -> PacketFaultFamily.MAGIC_FX;
			case "scar_fx", "scar_resync" -> PacketFaultFamily.SCAR_FX;
			case "beam_fx" -> PacketFaultFamily.BEAM_FX;
			case "shape_fx" -> PacketFaultFamily.SHAPE_FX;
			case "semantic_fx_batch" -> PacketFaultFamily.SEMANTIC_BATCH;
			case "celestial_ruin_fx" -> PacketFaultFamily.CELESTIAL_RUIN;
			case "event_audio", "layered_audio" -> PacketFaultFamily.EVENT_AUDIO;
			default -> PacketFaultFamily.OTHER;
		};
	}

	public static boolean isProjectOwned(CustomPacketPayload.Type<?> type) {
		return type != null && PowersMod.MOD_ID.equals(type.id().getNamespace());
	}
}
