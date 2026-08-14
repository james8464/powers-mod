package com.powers.magic;

import com.google.gson.JsonElement;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-data reload entrypoint for bounded retired action and menu key aliases. */
public final class ActionRegistryReloadListener extends SimpleReloadListener<List<ActionRegistryReloadListener.Document>> {
	private static final FileToIdConverter CONVERTER = FileToIdConverter.json("powers_actions");
	private static Map<String, String> pendingInitialAliases;
	private static boolean serverRegistrationComplete;

	public record Document(String id, JsonObject json) { }

	public static void initialize() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				PowersMod.id("action_registry"), new ActionRegistryReloadListener());
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
						throw new IllegalArgumentException("Invalid action registry resource "
								+ entry.getKey() + ": " + error.getMessage(), error);
					}
				});
		return List.copyOf(documents);
	}

	@Override
	protected void apply(List<Document> documents, PreparableReloadListener.SharedState state) {
		Map<String, String> aliases = parseDocuments(documents);
		synchronized (ActionRegistryReloadListener.class) {
			if (!serverRegistrationComplete) {
				pendingInitialAliases = aliases;
				PowersMod.LOGGER.info("Staged {} initial action aliases until extension registration",
						aliases.size());
				return;
			}
		}
		MagicActionCatalogue catalogue = com.powers.magic.runtime.MagicRuntime.catalogue();
		if (!catalogue.reloadAliases(aliases)) {
			throw new IllegalStateException("Action registry validation rejected the prepared reload");
		}
		logPublished(catalogue);
	}

	/** Publishes the cold-start resource snapshot after installed extensions register. */
	public static synchronized void completeInitialRegistration() {
		if (serverRegistrationComplete) return;
		MagicActionCatalogue catalogue = com.powers.magic.runtime.MagicRuntime.catalogue();
		Map<String, String> aliases = pendingInitialAliases == null ? Map.of() : pendingInitialAliases;
		if (!catalogue.reloadAliases(aliases)) {
			throw new IllegalStateException("Initial action registry validation failed after extension registration");
		}
		pendingInitialAliases = null;
		serverRegistrationComplete = true;
		logPublished(catalogue);
	}

	/** Reopens cold-start staging for the next server epoch in this process. */
	public static synchronized void serverStopped() {
		serverRegistrationComplete = false;
		pendingInitialAliases = null;
	}

	private static void logPublished(MagicActionCatalogue catalogue) {
		PowersMod.LOGGER.info("Published action registry revision {} with {} canonical keys and {} aliases",
				catalogue.snapshot().revision(), catalogue.snapshot().validation().canonicalKeyCount(),
				catalogue.snapshot().aliases().size());
	}

	/** Runs the exact parser and atomic publication path used by the registered Fabric listener. */
	public static boolean reloadDocuments(MagicActionCatalogue catalogue, List<Document> documents) {
		return catalogue.reloadAliases(parseDocuments(documents));
	}

	static Map<String, String> parseDocuments(List<Document> documents) {
		Map<String, String> aliases = new LinkedHashMap<>();
		documents.stream().sorted(Comparator.comparing(Document::id)).forEach(document -> {
			JsonElement rawAliases = document.json().get("aliases");
			if (rawAliases == null || !rawAliases.isJsonObject()) {
				throw new IllegalArgumentException("Missing aliases object in " + document.id());
			}
			for (Map.Entry<String, JsonElement> entry : rawAliases.getAsJsonObject().entrySet()) {
				if (!entry.getValue().isJsonPrimitive()
						|| !entry.getValue().getAsJsonPrimitive().isString()) {
					throw new IllegalArgumentException("Alias target must be a string in " + document.id());
				}
				if (aliases.putIfAbsent(entry.getKey(), entry.getValue().getAsString()) != null) {
					throw new IllegalArgumentException("Duplicate action alias " + entry.getKey());
				}
			}
		});
		return Map.copyOf(aliases);
	}
}
