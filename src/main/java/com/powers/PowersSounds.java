package com.powers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.Map;

/** Registers the original compact sound bank used by semantic magic cues. */
public final class PowersSounds {
	public static final SoundEvent RUNE_HUM = register("rune_hum");
	public static final SoundEvent CRYSTAL_RESONATE = register("crystal_resonate");
	public static final SoundEvent AMETHYST_FRACTURE = register("amethyst_fracture");
	public static final SoundEvent TIME_SUSPEND = register("time_suspend");
	public static final SoundEvent CELESTIAL_RING = register("celestial_ring");
	public static final SoundEvent BEAM_RING = register("beam_ring");
	public static final SoundEvent BOSS_IMPACT_RING = register("boss_impact_ring");
	public static final SoundEvent TIME_RELEASE = register("time_release");
	public static final SoundEvent RIFT_OPEN = register("rift_open");
	public static final SoundEvent RIFT_CLOSE = register("rift_close");
	public static final SoundEvent SOUL_TETHER = register("soul_tether");
	public static final SoundEvent LIGHT_CHORUS = register("light_chorus");
	public static final SoundEvent DARK_WHISPER = register("dark_whisper");
	public static final SoundEvent WARD_IMPACT = register("ward_impact");
	public static final SoundEvent RANK_AWAKEN = register("rank_awaken");
	public static final SoundEvent INTERACTION_CLASH = register("interaction_clash");

	private static final Map<String, SoundEvent> BY_CUE = Map.ofEntries(
			Map.entry("rune_hum", RUNE_HUM), Map.entry("crystal_resonate", CRYSTAL_RESONATE),
			Map.entry("amethyst_fracture", AMETHYST_FRACTURE), Map.entry("time_suspend", TIME_SUSPEND),
			Map.entry("celestial_ring", CELESTIAL_RING),
			Map.entry("beam_ring", BEAM_RING), Map.entry("boss_impact_ring", BOSS_IMPACT_RING),
			Map.entry("time_release", TIME_RELEASE), Map.entry("rift_open", RIFT_OPEN),
			Map.entry("rift_close", RIFT_CLOSE), Map.entry("soul_tether", SOUL_TETHER),
			Map.entry("light_chorus", LIGHT_CHORUS), Map.entry("dark_whisper", DARK_WHISPER),
			Map.entry("ward_impact", WARD_IMPACT), Map.entry("rank_awaken", RANK_AWAKEN),
			Map.entry("interaction_clash", INTERACTION_CLASH));

	private PowersSounds() {
	}

	/** Forces class initialization from the common entry point. */
	public static void initialize() {
		// Static field initialization performs registration exactly once.
	}

	/** Resolves an untrusted semantic cue to a safe registered fallback. */
	public static SoundEvent forCue(String cue) {
		return BY_CUE.getOrDefault(cue, INTERACTION_CLASH);
	}

	private static SoundEvent register(String path) {
		Identifier id = PowersMod.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
