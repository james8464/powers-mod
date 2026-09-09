package com.powers.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Owner-local, explicitly authored place names; performs no discovery or world scan. */
public record ShadowPlaceMemory(List<Place> places) {
	public static final int MAX_PLACES = 16;
	public static final int MAX_NAME_LENGTH = 32;
	public static final Codec<ShadowPlaceMemory> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(Place.CODEC.listOf().optionalFieldOf("places", List.of())
					.forGetter(ShadowPlaceMemory::places)).apply(instance, ShadowPlaceMemory::new));

	public record Place(String name, String dimension, long packedPosition) {
		private static final Codec<Place> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.STRING.fieldOf("name").forGetter(Place::name),
						Codec.STRING.fieldOf("dimension").forGetter(Place::dimension),
						Codec.LONG.fieldOf("position").forGetter(Place::packedPosition))
						.apply(instance, Place::new));

		public BlockPos position() {
			return BlockPos.of(packedPosition);
		}
	}

	public record RememberResult(ShadowPlaceMemory memory, boolean accepted, boolean updated) { }
	public record ForgetResult(ShadowPlaceMemory memory, boolean removed) { }

	public ShadowPlaceMemory {
		List<Place> bounded = new ArrayList<>(MAX_PLACES);
		for (Place place : places == null ? List.<Place>of() : places) {
			String name = normalize(place.name());
			if (name.isEmpty() || Identifier.tryParse(place.dimension()) == null
					|| bounded.stream().anyMatch(existing -> existing.name().equals(name))) continue;
			bounded.add(new Place(name, place.dimension(), place.packedPosition()));
			if (bounded.size() == MAX_PLACES) break;
		}
		places = List.copyOf(bounded);
	}

	public static ShadowPlaceMemory empty() {
		return new ShadowPlaceMemory(List.of());
	}

	public int size() {
		return places.size();
	}

	public Optional<Place> find(String name) {
		String key = normalize(name);
		return places.stream().filter(place -> place.name().equals(key)).findFirst();
	}

	public RememberResult remember(String name, Identifier dimension, BlockPos position) {
		String key = normalize(name);
		if (key.isEmpty() || dimension == null || position == null) {
			return new RememberResult(this, false, false);
		}
		List<Place> updated = new ArrayList<>(places);
		for (int index = 0; index < updated.size(); index++) {
			if (updated.get(index).name().equals(key)) {
				updated.set(index, new Place(key, dimension.toString(), position.asLong()));
				return new RememberResult(new ShadowPlaceMemory(updated), true, true);
			}
		}
		if (updated.size() >= MAX_PLACES) return new RememberResult(this, false, false);
		updated.add(new Place(key, dimension.toString(), position.asLong()));
		return new RememberResult(new ShadowPlaceMemory(updated), true, false);
	}

	public ForgetResult forget(String name) {
		String key = normalize(name);
		List<Place> updated = places.stream().filter(place -> !place.name().equals(key)).toList();
		return new ForgetResult(updated.size() == places.size() ? this : new ShadowPlaceMemory(updated),
				updated.size() != places.size());
	}

	private static String normalize(String value) {
		if (value == null) return "";
		String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 _-]", "")
				.replaceAll("\\s+", " ").strip();
		return normalized.substring(0, Math.min(MAX_NAME_LENGTH, normalized.length()));
	}
}
