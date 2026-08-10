package com.powers.knowledge;

import com.google.gson.JsonParser;
import com.powers.PowersMod;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Datapack reload listener for the {@code knowledge_entries} directory. */
public final class KnowledgeEntryReloadListener extends SimpleReloadListener<List<KnowledgeEntry>> {
	private static final FileToIdConverter CONVERTER = FileToIdConverter.json("knowledge_entries");

	public static void initialize() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				PowersMod.id("knowledge_entries"), new KnowledgeEntryReloadListener());
	}

	@Override
	protected List<KnowledgeEntry> prepare(PreparableReloadListener.SharedState state) {
		ResourceManager manager = state.resourceManager();
		List<KnowledgeEntry> loaded = new ArrayList<>();
		CONVERTER.listMatchingResources(manager).entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().toString()))
				.forEach(entry -> {
					Identifier id = CONVERTER.fileToId(entry.getKey());
					try (Reader reader = entry.getValue().openAsReader()) {
						loaded.add(KnowledgeEntryParser.parse(id.toString(),
								JsonParser.parseReader(reader).getAsJsonObject()));
					} catch (Exception error) {
						PowersMod.LOGGER.warn("Ignoring malformed knowledge entry {}: {}",
								id, error.getMessage());
					}
				});
		return List.copyOf(loaded);
	}

	@Override
	protected void apply(List<KnowledgeEntry> loaded, PreparableReloadListener.SharedState state) {
		KnowledgeService.replaceEntries(loaded);
		PowersMod.LOGGER.info("Loaded {} Knowledge Book entries", loaded.size());
	}
}
