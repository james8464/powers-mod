package com.powers.audio;

import java.util.Map;
import java.util.Optional;

/** Finite semantic vocabulary for every original POWERS magic sound. */
public enum LayeredAudioCue {
	RUNE_HUM(0, "rune_hum", LayeredAudioProfile.INTIMATE, MixGroup.RITUAL, false),
	CRYSTAL_RESONATE(1, "crystal_resonate", LayeredAudioProfile.INTIMATE, MixGroup.CRYSTAL, false),
	AMETHYST_FRACTURE(2, "amethyst_fracture", LayeredAudioProfile.STANDARD, MixGroup.IMPACT, false),
	TIME_SUSPEND(3, "time_suspend", LayeredAudioProfile.STANDARD, MixGroup.TEMPORAL, false),
	CELESTIAL_RING(4, "celestial_ring", LayeredAudioProfile.WORLD, MixGroup.CATASTROPHE, true),
	BEAM_RING(5, "beam_ring", LayeredAudioProfile.STANDARD, MixGroup.IMPACT, false),
	BOSS_IMPACT_RING(6, "boss_impact_ring", LayeredAudioProfile.WORLD, MixGroup.IMPACT, false),
	TIME_RELEASE(7, "time_release", LayeredAudioProfile.STANDARD, MixGroup.TEMPORAL, false),
	RIFT_OPEN(8, "rift_open", LayeredAudioProfile.STANDARD, MixGroup.TRAVEL, false),
	RIFT_CLOSE(9, "rift_close", LayeredAudioProfile.STANDARD, MixGroup.TRAVEL, false),
	SOUL_TETHER(10, "soul_tether", LayeredAudioProfile.STANDARD, MixGroup.RITUAL, false),
	LIGHT_CHORUS(11, "light_chorus", LayeredAudioProfile.WORLD, MixGroup.AMBIENT, false),
	DARK_WHISPER(12, "dark_whisper", LayeredAudioProfile.WORLD, MixGroup.AMBIENT, false),
	WARD_IMPACT(13, "ward_impact", LayeredAudioProfile.STANDARD, MixGroup.IMPACT, false),
	RANK_AWAKEN(14, "rank_awaken", LayeredAudioProfile.WORLD, MixGroup.RITUAL, false),
	INTERACTION_CLASH(15, "interaction_clash", LayeredAudioProfile.STANDARD, MixGroup.IMPACT, false);

	private static final Map<String, LayeredAudioCue> BY_NAME = java.util.Arrays.stream(values())
			.collect(java.util.stream.Collectors.toUnmodifiableMap(
					LayeredAudioCue::semanticName, cue -> cue));
	private final int networkId;
	private final String semanticName;
	private final LayeredAudioProfile profile;
	private final MixGroup group;
	private final boolean tinnitusSensitive;

	LayeredAudioCue(int networkId, String semanticName, LayeredAudioProfile profile,
			MixGroup group, boolean tinnitusSensitive) {
		this.networkId = networkId;
		this.semanticName = semanticName;
		this.profile = profile;
		this.group = group;
		this.tinnitusSensitive = tinnitusSensitive;
	}

	public int networkId() {
		return networkId;
	}

	public String semanticName() {
		return semanticName;
	}

	public String subtitleKey() {
		return "subtitles.powers." + semanticName;
	}

	public LayeredAudioProfile profile() {
		return profile;
	}

	public MixGroup group() {
		return group;
	}

	public boolean tinnitusSensitive() {
		return tinnitusSensitive;
	}

	/** Exact registered event path for one listener layer and comfort state. */
	public String eventPath(LayeredAudioLayer layer, boolean reducedTinnitus) {
		java.util.Objects.requireNonNull(layer, "layer");
		String comfort = reducedTinnitus && tinnitusSensitive ? ".reduced" : "";
		return semanticName + comfort + "." + layer.serializedName();
	}

	public static Optional<LayeredAudioCue> fromNetworkId(int networkId) {
		return networkId >= 0 && networkId < values().length
				? Optional.of(values()[networkId]) : Optional.empty();
	}

	public static Optional<LayeredAudioCue> forSemanticName(String semanticName) {
		if (semanticName == null) return Optional.empty();
		return Optional.ofNullable(BY_NAME.get(semanticName));
	}

	/** Independent mixer lanes prevent quiet ambience from consuming impact headroom. */
	public enum MixGroup {
		RITUAL, CRYSTAL, IMPACT, TEMPORAL, CATASTROPHE, TRAVEL, AMBIENT
	}
}
