package com.powers.force;

import com.mojang.serialization.Codec;
import com.powers.PowersMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compact, world-owned surface frontier for living-force chunks. Only blocks
 * capable of reaching a non-identical neighbour are persisted, avoiding a
 * complete chunk block scan on every restart.
 */
public final class LivingForceFrontierSavedData extends SavedData {
	private record Key(String dimension, long chunk) { }

	record DecodedEntry(String dimension, long chunk, Map<Long, LivingForceKind> positions) { }

	public static final Codec<LivingForceFrontierSavedData> CODEC = Codec.STRING.listOf()
			.optionalFieldOf("frontiers", List.of())
			.xmap(LivingForceFrontierSavedData::new, LivingForceFrontierSavedData::snapshot).codec();
	public static final SavedDataType<LivingForceFrontierSavedData> TYPE = new SavedDataType<>(
			PowersMod.id("living_force_frontiers"), LivingForceFrontierSavedData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<Key, Map<Long, LivingForceKind>> chunks = new LinkedHashMap<>();

	public LivingForceFrontierSavedData() { }

	LivingForceFrontierSavedData(List<String> encoded) {
		for (String row : encoded == null ? List.<String>of() : encoded) {
			decodeEntry(row).ifPresent(entry -> chunks.put(
					new Key(entry.dimension(), entry.chunk()), new LinkedHashMap<>(entry.positions())));
		}
	}

	public boolean hasChunk(String dimension, long chunk) {
		return chunks.containsKey(new Key(dimension, chunk));
	}

	public Map<Long, LivingForceKind> frontier(String dimension, long chunk) {
		Map<Long, LivingForceKind> frontier = chunks.get(new Key(dimension, chunk));
		return frontier == null ? Map.of() : Map.copyOf(frontier);
	}

	public void replaceChunk(String dimension, long chunk, Map<Long, LivingForceKind> positions) {
		Key key = new Key(dimension, chunk);
		Map<Long, LivingForceKind> replacement = new LinkedHashMap<>();
		if (positions != null) positions.entrySet().stream()
				.filter(entry -> entry.getKey() != null && entry.getValue() != null)
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> replacement.put(entry.getKey(), entry.getValue()));
		if (replacement.equals(chunks.get(key))) return;
		chunks.put(key, replacement);
		setDirty();
	}

	public void update(String dimension, long chunk, long position, LivingForceKind kind) {
		Key key = new Key(dimension, chunk);
		Map<Long, LivingForceKind> frontier = chunks.get(key);
		// An unseen chunk must first be indexed as one unit; accepting one mutation
		// here would falsely mark a partial frontier as complete.
		if (frontier == null) return;
		LivingForceKind previous = kind == null ? frontier.remove(position) : frontier.put(position, kind);
		if (previous != kind) setDirty();
	}

	private List<String> snapshot() {
		return chunks.entrySet().stream()
				.sorted(Comparator.comparing((Map.Entry<Key, ?> entry) -> entry.getKey().dimension())
						.thenComparingLong(entry -> entry.getKey().chunk()))
				.map(entry -> encodeEntry(entry.getKey().dimension(), entry.getKey().chunk(), entry.getValue()))
				.toList();
	}

	static String encodeEntry(String dimension, long chunk, Map<Long, LivingForceKind> positions) {
		List<Long> darkness = new ArrayList<>();
		List<Long> light = new ArrayList<>();
		positions.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			if (entry.getValue() == LivingForceKind.DARKNESS) darkness.add(entry.getKey());
			else if (entry.getValue() == LivingForceKind.PURE_LIGHT) light.add(entry.getKey());
		});
		return dimension + ";" + chunk + ";" + encodeLongs(darkness) + ";" + encodeLongs(light);
	}

	static Optional<DecodedEntry> decodeEntry(String encoded) {
		try {
			String[] parts = encoded.split(";", -1);
			if (parts.length != 4 || parts[0].isBlank()) return Optional.empty();
			long chunk = Long.parseLong(parts[1]);
			Map<Long, LivingForceKind> positions = new LinkedHashMap<>();
			for (long position : decodeLongs(parts[2])) positions.put(position, LivingForceKind.DARKNESS);
			for (long position : decodeLongs(parts[3])) positions.put(position, LivingForceKind.PURE_LIGHT);
			return Optional.of(new DecodedEntry(parts[0], chunk, Map.copyOf(positions)));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private static String encodeLongs(List<Long> values) {
		if (values.isEmpty()) return "-";
		ByteBuffer bytes = ByteBuffer.allocate(values.size() * Long.BYTES);
		values.forEach(bytes::putLong);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
	}

	private static List<Long> decodeLongs(String encoded) {
		if (encoded.equals("-") || encoded.isEmpty()) return List.of();
		byte[] bytes = Base64.getUrlDecoder().decode(encoded);
		if (bytes.length % Long.BYTES != 0) throw new IllegalArgumentException("Invalid frontier length");
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		List<Long> result = new ArrayList<>(bytes.length / Long.BYTES);
		while (buffer.hasRemaining()) result.add(buffer.getLong());
		return List.copyOf(result);
	}
}
