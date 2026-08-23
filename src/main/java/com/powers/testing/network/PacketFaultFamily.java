package com.powers.testing.network;

/** Logical POWERS payload family; stateful streams reject older scheduled frames. */
public enum PacketFaultFamily {
	ABILITY_ACTIVATION(Semantics.ONCE),
	ABILITY_SELECTION(Semantics.CURRENT_ONLY),
	TELEPORT_REQUEST(Semantics.ONCE),
	ARTIFACT_SELECTION(Semantics.CURRENT_ONLY),
	ARTIFACT_CYCLE(Semantics.ONCE),
	ARTIFACT_BINDING(Semantics.CURRENT_ONLY),
	ARTIFACT_CAST(Semantics.ONCE),
	GRIMOIRE_SELECTION(Semantics.CURRENT_ONLY),
	CRYSTAL_SELECTION(Semantics.CURRENT_ONLY),
	RANK_ACTION(Semantics.ONCE),
	RELIC_TRANSFER(Semantics.ONCE),
	LOCATOR_REQUEST(Semantics.ONCE),
	COMPANION_REQUEST(Semantics.ONCE),
	VESSEL_INPUT(Semantics.CURRENT_ONLY),
	VESSEL_RELEASE(Semantics.ONCE),
	MENU_SNAPSHOT(Semantics.CURRENT_ONLY),
	POWER_STATE(Semantics.CURRENT_ONLY),
	BODY_SNAPSHOT(Semantics.CURRENT_ONLY),
	COMPANION_SNAPSHOT(Semantics.CURRENT_ONLY),
	MAGIC_FX(Semantics.EVERY),
	SCAR_FX(Semantics.CURRENT_ONLY),
	BEAM_FX(Semantics.EVERY),
	SHAPE_FX(Semantics.EVERY),
	SEMANTIC_BATCH(Semantics.EVERY),
	CELESTIAL_RUIN(Semantics.EVERY),
	EVENT_AUDIO(Semantics.EVERY),
	OTHER(Semantics.EVERY);

	public enum Semantics { EVERY, ONCE, CURRENT_ONLY }

	private final Semantics semantics;

	PacketFaultFamily(Semantics semantics) {
		this.semantics = semantics;
	}

	public Semantics semantics() {
		return semantics;
	}
}
