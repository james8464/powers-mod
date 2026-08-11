package com.powers.knowledge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.powers.companion.DialogueTransport;
import com.powers.companion.DialogueTextSanitizer;
import com.powers.config.PowersConfig;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Privacy, timeout, rate, concurrency, and output boundary for Shadow's optional remote fallback. */
public final class BoundedKnowledgeProvider {
	private static final int MAX_OUTPUT_CHARACTERS = 1_024;
	private final PowersConfig.DialogueProvider settings;
	private final DialogueTransport transport;
	private final String credential;
	private final Object lock = new Object();
	private final Set<UUID> inFlightOwners = new HashSet<>();
	private final Map<UUID, Long> lastRequestAt = new HashMap<>();
	private int globalInFlight;

	public BoundedKnowledgeProvider(PowersConfig.DialogueProvider settings,
			DialogueTransport transport, String credential) {
		this.settings = settings.sanitized();
		this.transport = transport;
		this.credential = credential == null ? "" : credential;
	}

	/** Returns a future immediately; every rejected, failed, or late request uses offline truth. */
	public CompletableFuture<KnowledgeAnswer> request(UUID owner, KnowledgeQuery query,
			KnowledgeAnswer offline, long nowMillis) {
		URI endpoint = endpoint();
		if (!enabled() || endpoint == null
				|| !KnowledgeRemoteRules.mayFallback(query.question(), offline.confidence())) {
			return CompletableFuture.completedFuture(offline);
		}
		synchronized (lock) {
			long cooldown = settings.ownerCooldownSeconds() * 1_000L;
			long previous = lastRequestAt.getOrDefault(owner, Long.MIN_VALUE / 2L);
			if (inFlightOwners.contains(owner) || globalInFlight >= settings.maxGlobalRequests()
					|| nowMillis - previous < cooldown) {
				return CompletableFuture.completedFuture(offline);
			}
			inFlightOwners.add(owner);
			lastRequestAt.put(owner, nowMillis);
			globalInFlight++;
		}
		CompletableFuture<String> pending;
		try {
			pending = transport.request(endpoint, settings.model(), credential,
					prompt(query), Duration.ofMillis(settings.timeoutMillis()));
		} catch (RuntimeException error) {
			release(owner);
			return CompletableFuture.completedFuture(offline);
		}
		return pending.completeOnTimeout("", settings.timeoutMillis(), TimeUnit.MILLISECONDS)
				.handle((body, error) -> error == null ? response(body, query, offline) : offline)
				.whenComplete((ignored, error) -> release(owner));
	}

	public boolean enabled() {
		return settings.enabled() && !settings.endpoint().isBlank()
				&& !settings.model().isBlank() && !credential.isBlank();
	}

	public void forget(UUID owner) {
		synchronized (lock) {
			lastRequestAt.remove(owner);
		}
	}

	public void clear() {
		synchronized (lock) {
			lastRequestAt.clear();
			inFlightOwners.clear();
			globalInFlight = 0;
		}
	}

	private URI endpoint() {
		try {
			URI uri = URI.create(settings.endpoint());
			boolean secure = "https".equalsIgnoreCase(uri.getScheme());
			boolean local = "http".equalsIgnoreCase(uri.getScheme())
					&& ("localhost".equalsIgnoreCase(uri.getHost())
					|| "127.0.0.1".equals(uri.getHost()) || "::1".equals(uri.getHost()));
			return uri.isAbsolute() && (secure || local) ? uri : null;
		} catch (IllegalArgumentException error) {
			return null;
		}
	}

	private void release(UUID owner) {
		synchronized (lock) {
			if (inFlightOwners.remove(owner)) globalInFlight = Math.max(0, globalInFlight - 1);
		}
	}

	private static String prompt(KnowledgeQuery query) {
		String question = query.question().replaceAll("[\\p{Cntrl}]", " ")
				.replaceAll("\\s+", " ").strip();
		String context = query.contextRegistryIds().stream()
				.map(BoundedKnowledgeProvider::registryField).limit(8)
				.collect(java.util.stream.Collectors.joining(","));
		return "Speak as Shadow, a concise ancient companion. Answer one Minecraft/POWERS question in at most 120 words. "
				+ "Never invent recipes, commands, registry IDs, or implemented features. "
				+ "If an authoritative diagnosis is supplied, repeat it verbatim before any advice. "
				+ "Clearly say when uncertain. No coordinates, player identity, chat, IP, or world data is supplied. "
				+ "Progression reveal rank=" + query.revealRank() + "; registry context=" + context
				+ "; authoritative diagnosis=" + query.authoritativeDiagnostic()
				+ "; question=" + question;
	}

	private static String registryField(String value) {
		if (value == null) return "";
		String safe = value.replaceAll("[^A-Za-z0-9_=:/.-]", "_");
		return safe.substring(0, Math.min(96, safe.length()));
	}

	private static KnowledgeAnswer response(String body, KnowledgeQuery query,
			KnowledgeAnswer offline) {
		try {
			JsonObject root = JsonParser.parseString(body).getAsJsonObject();
			String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
					.getAsJsonObject("message").get("content").getAsString();
			String safe = DialogueTextSanitizer.sanitize(content,
					MAX_OUTPUT_CHARACTERS, offline.answer());
			if (!query.authoritativeDiagnostic().isEmpty()
					&& !safe.startsWith(query.authoritativeDiagnostic())) {
				safe = query.authoritativeDiagnostic() + " " + safe;
				safe = safe.substring(0, Math.min(MAX_OUTPUT_CHARACTERS, safe.length()));
			}
			return new KnowledgeAnswer("remote_fallback", safe, 0.55,
					java.util.List.of("opt-in remote knowledge provider"),
					query.contextRegistryIds());
		} catch (RuntimeException error) {
			return offline;
		}
	}
}
