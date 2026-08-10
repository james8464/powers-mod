package com.powers.knowledge;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Deterministic token/phrase index for curated offline knowledge entries. */
public final class KnowledgeIndex implements KnowledgeProvider {
	private record Match(KnowledgeEntry entry, int score) {
	}

	private final List<KnowledgeEntry> entries;

	public KnowledgeIndex(List<KnowledgeEntry> entries) {
		this.entries = entries == null ? List.of() : List.copyOf(entries);
	}

	public KnowledgeAnswer answer(String question, int revealRank) {
		return answer(new KnowledgeQuery(question, revealRank, List.of()));
	}

	@Override
	public KnowledgeAnswer answer(KnowledgeQuery query) {
		String normalized = normalize(query.question());
		Set<String> tokens = tokens(normalized);
		List<Match> matches = entries.stream().map(entry -> new Match(entry,
				score(entry, normalized, tokens))).filter(match -> match.score() > 0)
				.sorted(Comparator.comparingInt(Match::score).reversed()
						.thenComparing(match -> match.entry().id())).toList();
		Match visible = matches.stream()
				.filter(match -> query.revealRank() >= match.entry().revealRank()).findFirst().orElse(null);
		if (visible == null) {
			boolean withheld = matches.stream().anyMatch(match -> match.score() >= 3);
			String answer = withheld
					? "The book recognises this subject, but its pages have not yet opened to your rank."
					: "The book could not verify an answer from loaded registries, recipes, or authored lore."
						+ " It will not invent a recipe.";
			return new KnowledgeAnswer("", answer, 0.0, List.of("offline knowledge index"),
					query.contextRegistryIds());
		}
		double confidence = Math.min(1.0, visible.score() / 8.0);
		return new KnowledgeAnswer(visible.entry().id(), visible.entry().answer(), confidence,
				visible.entry().sources(), query.contextRegistryIds());
	}

	public List<KnowledgeEntry> entries() {
		return entries;
	}

	private static int score(KnowledgeEntry entry, String question, Set<String> queryTokens) {
		String title = normalize(entry.title());
		int score = question.contains(title) ? 6 : 0;
		Set<String> authoredTokens = new LinkedHashSet<>(tokens(title));
		for (String keyword : entry.keywords()) {
			String normalizedKeyword = normalize(keyword);
			if (!normalizedKeyword.isBlank() && question.contains(normalizedKeyword)) score += 4;
			authoredTokens.addAll(tokens(normalizedKeyword));
		}
		for (String token : queryTokens) if (authoredTokens.contains(token)) score++;
		return score;
	}

	private static Set<String> tokens(String value) {
		Set<String> result = new LinkedHashSet<>();
		for (String token : value.split(" ")) {
			if (token.length() >= 3) result.add(token);
		}
		return result;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9_:]+", " ").strip().replaceAll(" +", " ");
	}
}
