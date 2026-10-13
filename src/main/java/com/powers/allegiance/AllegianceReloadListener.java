package com.powers.allegiance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.powers.PowersMod;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Datapack entrypoint for the single atomic allegiance relationship policy. */
public final class AllegianceReloadListener
		extends SimpleReloadListener<List<AllegianceReloadListener.Document>> {
	private static final FileToIdConverter CONVERTER = FileToIdConverter.json("allegiance");
	private static final AllegianceRegistry REGISTRY = new AllegianceRegistry();

	public record Document(String id, JsonObject json) { }

	public static void initialize() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				PowersMod.id("allegiance"), new AllegianceReloadListener());
	}

	public static AllegianceRegistry.Snapshot snapshot() {
		return REGISTRY.snapshot();
	}

	@Override
	protected List<Document> prepare(PreparableReloadListener.SharedState state) {
		ResourceManager manager = state.resourceManager();
		List<Document> documents = new ArrayList<>();
		CONVERTER.listMatchingResources(manager).entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().toString()))
				.forEach(entry -> {
					try (Reader reader = entry.getValue().openAsReader()) {
						documents.add(new Document(CONVERTER.fileToId(entry.getKey()).toString(),
								JsonParser.parseReader(reader).getAsJsonObject()));
					} catch (Exception error) {
						throw new IllegalArgumentException("Invalid allegiance resource "
								+ entry.getKey() + ": " + error.getMessage(), error);
					}
				});
		return List.copyOf(documents);
	}

	@Override
	protected void apply(List<Document> documents, PreparableReloadListener.SharedState state) {
		if (documents.size() != 1) {
			throw new IllegalArgumentException("Expected exactly one allegiance policy, found "
					+ documents.size());
		}
		if (!reloadDocument(REGISTRY, documents.getFirst().json())) {
			throw new IllegalArgumentException("Rejected incomplete allegiance policy "
					+ documents.getFirst().id());
		}
		PowersMod.LOGGER.info("Published allegiance policy revision {}",
				REGISTRY.snapshot().revision());
	}

	/** Runs the same validation and atomic publication boundary as the registered listener. */
	public static boolean reloadDocument(AllegianceRegistry registry, JsonObject json) {
		return registry.reload(json);
	}
}
