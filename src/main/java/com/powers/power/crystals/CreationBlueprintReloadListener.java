package com.powers.power.crystals;

import com.google.gson.JsonParser;
import com.powers.PowersMod;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-data reload entrypoint for bounded creation blueprints. */
public final class CreationBlueprintReloadListener
		extends SimpleReloadListener<List<CreationBlueprintRegistry.Document>> {
	private static final FileToIdConverter CONVERTER = FileToIdConverter.json("creation_blueprints");
	private static final CreationBlueprintRegistry REGISTRY = new CreationBlueprintRegistry();

	public static void initialize() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				PowersMod.id("creation_blueprints"), new CreationBlueprintReloadListener());
	}

	public static CreationBlueprintRegistry registry() {
		return REGISTRY;
	}

	@Override
	protected List<CreationBlueprintRegistry.Document> prepare(PreparableReloadListener.SharedState state) {
		List<CreationBlueprintRegistry.Document> documents = new ArrayList<>();
		CONVERTER.listMatchingResources(state.resourceManager()).entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().toString()))
				.forEach(entry -> {
					try (Reader reader = entry.getValue().openAsReader()) {
						documents.add(new CreationBlueprintRegistry.Document(
								CONVERTER.fileToId(entry.getKey()).toString(),
								JsonParser.parseReader(reader).getAsJsonObject()));
					} catch (Exception error) {
						throw new IllegalArgumentException("Invalid creation blueprint " + entry.getKey(), error);
					}
				});
		return List.copyOf(documents);
	}

	@Override
	protected void apply(List<CreationBlueprintRegistry.Document> documents,
			PreparableReloadListener.SharedState state) {
		if (!REGISTRY.reload(documents)) {
			throw new IllegalStateException("Creation blueprint validation rejected the prepared reload");
		}
		PowersMod.LOGGER.info("Published {} validated creation blueprints", REGISTRY.ids().size());
	}
}
