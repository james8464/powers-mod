package com.powers.client.audio;

import com.google.gson.JsonObject;
import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Bounded, identity-free acceptance ledger for production client audio decisions. */
public final class ClientLayeredAudioAudit {
	private static final int CAPACITY = 128;
	private static final ArrayDeque<Row> ROWS = new ArrayDeque<>(CAPACITY);

	private ClientLayeredAudioAudit() {
	}

	public static synchronized void record(Row row) {
		Objects.requireNonNull(row, "row");
		if (ROWS.size() == CAPACITY) ROWS.removeFirst();
		ROWS.addLast(row);
	}

	public static synchronized List<Row> rows() {
		return List.copyOf(ROWS);
	}

	public static synchronized Row last() {
		return ROWS.peekLast();
	}

	public static synchronized void reset() {
		ROWS.clear();
	}

	/** One privacy-safe JSONL decision row with a fixed field vocabulary. */
	public record Row(LayeredAudioCue cue, LayeredAudioLayer layer, double distance,
			boolean obstructed, float effectiveGain, String result, boolean reducedTinnitus,
			Identifier dimension, long eventId, String implementationSha) {
		public Row {
			Objects.requireNonNull(cue, "cue");
			Objects.requireNonNull(layer, "layer");
			Objects.requireNonNull(dimension, "dimension");
			if (!Double.isFinite(distance) || distance < 0.0) distance = 0.0;
			if (!Float.isFinite(effectiveGain) || effectiveGain < 0.0F) effectiveGain = 0.0F;
			result = "admitted".equals(result) ? "admitted" : "dropped";
			implementationSha = implementationSha != null
					&& implementationSha.matches("[0-9a-f]{7,40}")
					? implementationSha : "unknown";
		}

		public String json() {
			JsonObject object = new JsonObject();
			object.addProperty("schemaVersion", 1);
			object.addProperty("cue", cue.semanticName());
			object.addProperty("layer", layer.serializedName());
			object.addProperty("distance", distance);
			object.addProperty("obstructed", obstructed);
			object.addProperty("effectiveGain", effectiveGain);
			object.addProperty("result", result.toLowerCase(Locale.ROOT));
			object.addProperty("subtitleKey", cue.subtitleKey());
			object.addProperty("reducedTinnitus", reducedTinnitus);
			object.addProperty("dimension", dimension.toString());
			object.addProperty("eventId", eventId);
			object.addProperty("implementationSha", implementationSha);
			return object.toString();
		}
	}
}
