package com.powers.client.audio;

import com.powers.PowersSounds;
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
		Identifier dimension = minecraft.level.dimension().identifier();
		if (!dimension.equals(payload.dimension())) return;
		long gameTime = minecraft.level.getGameTime();
		long age = gameTime - payload.emittedGameTime();
		if (age < 0 || age > 100 || !STATE.acceptEvent(payload.eventId())) return;

		Vec3 origin = new Vec3(payload.x(), payload.y(), payload.z());
		Vec3 camera = minecraft.gameRenderer.mainCamera().position();
		double distance = camera.distanceTo(origin);
		if (payload.cue().profile().layer(distance).isEmpty()) return;
		HitResult obstruction = minecraft.level.clip(new ClipContext(camera, origin,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
		boolean obstructed = obstruction.getType() != HitResult.Type.MISS;
		ClientLayeredAudioState.Admission admission = STATE.admit(payload.cue(),
				payload.x(), payload.y(), payload.z(), gameTime);
		if (admission.result() != ClientLayeredAudioState.AdmissionResult.ADMITTED) return;
		LayeredAudioRules.ResolvedLayer resolved = LayeredAudioRules.resolve(payload.cue(), distance,
				obstructed, ClientAudioComfortConfig.reducedTinnitus(), payload.gain(),
				admission.concurrentInGroup()).orElse(null);
		if (resolved == null) return;
		var sound = PowersSounds.layer(payload.cue(), resolved.layer(), resolved.reducedTinnitus());
		minecraft.getSoundManager().play(new PositionalLayeredSound(sound, resolved.gain(),
				payload.pitch(), payload.x(), payload.y(), payload.z()));
	}

	/** Clears all packet IDs and burst metrics at connection and world boundaries. */
	public static void resetConnectionEpoch() {
		STATE.reset();
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
		if (lastDimension != null && !lastDimension.equals(dimension)) STATE.reset();
		lastDimension = dimension;
	}

	public static ClientLayeredAudioState.Metrics metrics() {
		return STATE.metrics();
	}
}
