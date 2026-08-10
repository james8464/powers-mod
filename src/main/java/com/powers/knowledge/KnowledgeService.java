package com.powers.knowledge;

import com.powers.player.PlayerPowers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Combines curated lore, live registries, recipes, and redacted player context offline. */
public final class KnowledgeService {
	private record RegistryFact(String type, Identifier id) {
	}

	private static final List<RegistryFact> REGISTRY_FACTS = buildRegistryFacts();
	private static volatile KnowledgeIndex INDEX = new KnowledgeIndex(List.of());

	private KnowledgeService() {
	}

	public static void replaceEntries(List<KnowledgeEntry> entries) {
		INDEX = new KnowledgeIndex(entries);
	}

	public static KnowledgeAnswer answer(ServerPlayer player, String question) {
		KnowledgeQuery query = query(player, question);
		return answerOffline(player, query);
	}

	/** Captures server context synchronously, then optionally falls back off-thread. */
	public static CompletableFuture<KnowledgeAnswer> answerAsync(ServerPlayer player, String question) {
		KnowledgeQuery query = query(player, question);
		KnowledgeAnswer offline = answerOffline(player, query);
		return KnowledgeRemoteProviderRuntime.request(player.getUUID(), query, offline);
	}

	private static KnowledgeAnswer answerOffline(ServerPlayer player, KnowledgeQuery query) {
		KnowledgeAnswer curated = INDEX.answer(query);
		if (curated.confidence() >= 0.5) return curated;
		KnowledgeAnswer recipe = recipeAnswer(player, query);
		if (recipe != null) return recipe;
		KnowledgeAnswer registry = registryAnswer(query);
		if (registry != null) return registry;
		if (asksForContext(query.question()) && !query.contextRegistryIds().isEmpty()) {
			return new KnowledgeAnswer("context", "Current registry context: "
					+ String.join(", ", query.contextRegistryIds()) + ".", 0.9,
					List.of("server-authoritative player context"), query.contextRegistryIds());
		}
		return curated;
	}

	private static KnowledgeQuery query(ServerPlayer player, String question) {
		List<String> context = context(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int revealRank = Math.max(data.skillLevel(), data.darknessLevel());
		return new KnowledgeQuery(question, revealRank, context);
	}

	public static int entryCount() {
		return INDEX.entries().size();
	}

	private static KnowledgeAnswer recipeAnswer(ServerPlayer player, KnowledgeQuery query) {
		String normalized = normalize(query.question());
		if (!normalized.contains("recipe") && !normalized.contains("craft")
				&& !normalized.contains("make ")) return null;
		List<String> candidates = player.level().getServer().getRecipeManager().getRecipes().stream()
				.map(holder -> holder.id().identifier().toString())
				.filter(id -> mentions(normalized, id) || query.contextRegistryIds().stream()
						.anyMatch(context -> context.startsWith("held=")
								&& id.contains(context.substring("held=".length()).replace(':', '/'))))
				.sorted().limit(8).toList();
		if (candidates.isEmpty()) return null;
		return new KnowledgeAnswer("recipe", "Verified loaded recipe ID"
				+ (candidates.size() == 1 ? ": " : "s: ") + String.join(", ", candidates)
				+ ". The book reports only server-loaded recipe data and never guesses ingredients.",
				0.72, List.of("Minecraft RecipeManager"), candidates);
	}

	private static KnowledgeAnswer registryAnswer(KnowledgeQuery query) {
		String normalized = normalize(query.question());
		RegistryFact best = REGISTRY_FACTS.stream().filter(fact -> mentions(normalized, fact.id().toString()))
				.max(Comparator.comparingInt(fact -> fact.id().getPath().length())).orElse(null);
		if (best == null) return null;
		String id = best.id().toString();
		return new KnowledgeAnswer("registry", "Verified " + best.type() + " registry entry: " + id + ".",
				0.82, List.of("Minecraft built-in registry"), List.of(id));
	}

	private static List<String> context(ServerPlayer player) {
		List<String> values = new ArrayList<>();
		values.add("dimension=" + player.level().dimension().identifier());
		Identifier held = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem());
		if (held != null && !player.getMainHandItem().isEmpty()) values.add("held=" + held);
		player.level().getBiome(player.blockPosition()).unwrapKey()
				.ifPresent(key -> values.add("biome=" + key.identifier()));
		HitResult hit = player.pick(8.0, 0.0F, false);
		if (hit instanceof BlockHitResult blockHit) {
			Identifier block = BuiltInRegistries.BLOCK.getKey(
					player.level().getBlockState(blockHit.getBlockPos()).getBlock());
			if (block != null) values.add("target_block=" + block);
		} else if (hit instanceof EntityHitResult entityHit) {
			Identifier entity = BuiltInRegistries.ENTITY_TYPE.getKey(entityHit.getEntity().getType());
			if (entity != null) values.add("target_entity=" + entity);
		}
		return List.copyOf(values);
	}

	private static List<RegistryFact> buildRegistryFacts() {
		List<RegistryFact> facts = new ArrayList<>();
		BuiltInRegistries.ITEM.keySet().forEach(id -> facts.add(new RegistryFact("item", id)));
		BuiltInRegistries.BLOCK.keySet().forEach(id -> facts.add(new RegistryFact("block", id)));
		BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> facts.add(new RegistryFact("entity", id)));
		return List.copyOf(facts);
	}

	private static boolean asksForContext(String question) {
		String normalized = normalize(question);
		return normalized.contains("looking at") || normalized.contains("holding")
				|| normalized.contains("where am i") || normalized.contains("this block")
				|| normalized.contains("this item") || normalized.contains("this mob");
	}

	private static boolean mentions(String question, String id) {
		String path = id.substring(id.indexOf(':') + 1);
		return question.contains(id.toLowerCase(Locale.ROOT))
				|| question.contains(path.replace('_', ' ').replace('/', ' '));
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9_:/]+", " ").strip().replaceAll(" +", " ");
	}
}
