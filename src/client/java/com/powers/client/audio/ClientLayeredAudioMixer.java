package com.powers.client.audio;

import com.powers.PowersSounds;
import com.powers.PowersMod;
import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioRules;
import com.powers.network.LayeredAudioPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Client-thread boundary for validating, classifying, and playing one semantic sound layer. */
public final class ClientLayeredAudioMixer {
	private static final ClientLayeredAudioState STATE = new ClientLayeredAudioState();
	private static Identifier lastDimension;

	private ClientLayeredAudioMixer() {
	}

	public static void initialize() {
		ClientAudioComfortConfig.initialize();
		resetConnectionEpoch();
	}

	public static void handle(LayeredAudioPackets.Payload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		if (payload == null || minecraft.player == null || minecraft.level == null) return;
		Vec3 origin = new Vec3(payload.x(), payload.y(), payload.z());
		Vec3 camera = minecraft.gameRenderer.mainCamera().position();
		double distance = camera.distanceTo(origin);
		var rawLayer = payload.cue().profile().layer(distance).orElse(
				com.powers.audio.LayeredAudioLayer.FAR);
		Identifier dimension = minecraft.level.dimension().identifier();
		if (!dimension.equals(payload.dimension())) {
			record(payload, rawLayer, distance, false, 0.0F, false, "dropped");
			return;
		}
		long gameTime = minecraft.level.getGameTime();
		long age = gameTime - payload.emittedGameTime();
		if (age < 0 || age > 100 || !STATE.acceptEvent(payload.eventId())) {
			record(payload, rawLayer, distance, false, 0.0F, false, "dropped");
			return;
		}

		if (payload.cue().profile().layer(distance).isEmpty()) {
			record(payload, rawLayer, distance, false, 0.0F, false, "dropped");
			return;
		}
		Spatial spatial = spatial(minecraft, payload.cue(), origin);
		boolean obstructed = spatial.obstructed();
		ClientLayeredAudioState.Admission admission = STATE.admit(payload.cue(),
				payload.x(), payload.y(), payload.z(), gameTime);
		if (admission.result() != ClientLayeredAudioState.AdmissionResult.ADMITTED) {
			record(payload, rawLayer, distance, obstructed, 0.0F, false, "dropped");
			return;
		}
		LayeredAudioRules.ResolvedLayer resolved = LayeredAudioRules.resolve(payload.cue(), spatial.distance(),
				obstructed, ClientAudioComfortConfig.reducedTinnitus(), payload.gain(),
				admission.concurrentGlobal()).orElse(null);
		if (resolved == null) {
			record(payload, rawLayer, distance, obstructed, 0.0F, false, "dropped");
			return;
		}
		play(minecraft, payload.cue(), resolved, payload.pitch(), origin);
		record(payload, resolved.layer(), distance, obstructed, resolved.gain(),
				resolved.reducedTinnitus(), "admitted");
	}

	private static void record(LayeredAudioPackets.Payload payload,
			com.powers.audio.LayeredAudioLayer layer, double distance, boolean obstructed,
			float effectiveGain, boolean reducedTinnitus, String result) {
		String sha = System.getProperty("powers.vfx007.implementationSha",
				System.getProperty("powers.qa.implementationSha", ""));
		var row = new ClientLayeredAudioAudit.Row(payload.cue(), layer, distance, obstructed,
				effectiveGain, result, reducedTinnitus, payload.dimension(), payload.eventId(), sha);
		ClientLayeredAudioAudit.record(row);
		PowersMod.LOGGER.info("powers_layered_audio_audit {}", row.json());
	}

	/** Plays the existing client-authoritative Celestial cadence at its server-authored origin. */
	public static void playLocalCelestial(Vec3 origin, float gain, float pitch) {
		Minecraft minecraft = Minecraft.getInstance();
		if (origin == null || minecraft.player == null || minecraft.level == null || !Float.isFinite(pitch)
				|| pitch < 0.25F || pitch > 4.0F) return;
		Spatial spatial = spatial(minecraft, LayeredAudioCue.CELESTIAL_RING, origin);
		if (LayeredAudioCue.CELESTIAL_RING.profile().layer(spatial.distance()).isEmpty()) return;
		long gameTime = minecraft.level.getGameTime();
		ClientLayeredAudioState.Admission admission = STATE.admit(LayeredAudioCue.CELESTIAL_RING,
				origin.x, origin.y, origin.z, gameTime);
		if (admission.result() != ClientLayeredAudioState.AdmissionResult.ADMITTED) return;
		LayeredAudioRules.ResolvedLayer resolved = LayeredAudioRules.resolve(
				LayeredAudioCue.CELESTIAL_RING, spatial.distance(), spatial.obstructed(),
				ClientAudioComfortConfig.reducedTinnitus(), gain,
				admission.concurrentGlobal()).orElse(null);
		if (resolved != null) play(minecraft, LayeredAudioCue.CELESTIAL_RING, resolved, pitch, origin);
	}

	private static Spatial spatial(Minecraft minecraft, LayeredAudioCue cue, Vec3 origin) {
		Vec3 camera = minecraft.gameRenderer.mainCamera().position();
		double distance = camera.distanceTo(origin);
		HitResult obstruction = minecraft.level.clip(new ClipContext(camera, origin,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
		return new Spatial(distance, obstruction.getType() != HitResult.Type.MISS);
	}

	private static void play(Minecraft minecraft, LayeredAudioCue cue,
			LayeredAudioRules.ResolvedLayer resolved, float pitch, Vec3 origin) {
		var sound = PowersSounds.layer(cue, resolved.layer(), resolved.reducedTinnitus());
		minecraft.getSoundManager().play(new PositionalLayeredSound(sound, resolved.gain(),
				pitch, origin.x, origin.y, origin.z));
	}

	/** Clears all packet IDs and burst metrics at connection and world boundaries. */
	public static void resetConnectionEpoch() {
		STATE.reset();
		ClientLayeredAudioAudit.reset();
		ClientAudioComfortConfig.clearAcceptanceOverride();
		lastDimension = null;
	}

	/** Reloads the local comfort choice and clears all resource-dependent bookkeeping. */
	public static void reload() {
		ClientAudioComfortConfig.reload();
		resetConnectionEpoch();
	}

	/** Detects dimension replacement even when no semantic sound packet arrives. */
	public static void tick(Minecraft minecraft) {
		Identifier dimension = minecraft.level == null ? null : minecraft.level.dimension().identifier();
		if (lastDimension != null && !lastDimension.equals(dimension)) resetConnectionEpoch();
		lastDimension = dimension;
	}

	public static ClientLayeredAudioState.Metrics metrics() {
		return STATE.metrics();
	}

	private record Spatial(double distance, boolean obstructed) { }
}
