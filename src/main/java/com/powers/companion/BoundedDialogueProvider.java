package com.powers.companion;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

/**
 * Applies privacy, concurrency, timeout, cooldown, and output bounds around an
 * optional remote lore generator. Gameplay always has the supplied offline
 * fallback and therefore never waits on this provider.
 */
public final class BoundedDialogueProvider {
	private static final int MAX_OUTPUT_CHARACTERS = 256;
	private final PowersConfig.DialogueProvider settings;
	private final DialogueTransport transport;
	private final String credential;
	private final Object lock = new Object();
	private final Set<UUID> inFlightOwners = new HashSet<>();
	private final Map<UUID, Long> lastRequestAt = new HashMap<>();
	private int globalInFlight;

	public BoundedDialogueProvider(PowersConfig.DialogueProvider settings,
			DialogueTransport transport, String credential) {
		this.settings = settings.sanitized();
		this.transport = transport;
		this.credential = credential == null ? "" : credential;
	}

	public boolean enabled() {
		return settings.enabled() && !settings.endpoint().isBlank()
				&& !settings.model().isBlank() && !credential.isBlank();
	}

	/** Returns immediately with either a pending future or the offline fallback. */
	public CompletableFuture<String> request(UUID owner, LoreDialogueContext context,
			boolean bossVoice, String fallback, long nowMillis) {
		String safeFallback = sanitizeOutput(fallback, "...");
		URI endpoint = endpoint();
		if (!enabled() || endpoint == null) return CompletableFuture.completedFuture(safeFallback);
		synchronized (lock) {
			long cooldownMillis = settings.ownerCooldownSeconds() * 1_000L;
			long previous = lastRequestAt.getOrDefault(owner, Long.MIN_VALUE / 2L);
			if (inFlightOwners.contains(owner) || globalInFlight >= settings.maxGlobalRequests()
					|| nowMillis - previous < cooldownMillis) {
				return CompletableFuture.completedFuture(safeFallback);
			}
			inFlightOwners.add(owner);
			lastRequestAt.put(owner, nowMillis);
			globalInFlight++;
		}

		CompletableFuture<String> pending;
		try {
			pending = transport.request(endpoint, settings.model(), credential,
					prompt(context, bossVoice), Duration.ofMillis(settings.timeoutMillis()));
		} catch (RuntimeException error) {
			release(owner);
			return CompletableFuture.completedFuture(safeFallback);
		}
		return pending.completeOnTimeout("", settings.timeoutMillis(), TimeUnit.MILLISECONDS)
				.handle((body, error) -> error == null ? response(body, safeFallback) : safeFallback)
				.whenComplete((ignored, error) -> release(owner));
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

	private static String prompt(LoreDialogueContext context, boolean bossVoice) {
		return "Write one short line (maximum 35 words) of ancient Minecraft fantasy lore. "
				+ "Voice=" + (bossVoice ? "First Vessel boss" : "owner-private shadow companion")
				+ "; realm=" + field(context.realm())
				+ "; low_health=" + context.lowHealth()
				+ "; low_energy=" + context.lowEnergy()
				+ "; rank=" + Math.clamp(context.rank(), 0, 10)
				+ "; nearby_force=" + field(context.nearbyAlignment())
				+ "; selected_rite=" + field(context.artifactAction())
				+ "; boss_nearby=" + context.bossNearby()
				+ "; recent_death=" + context.recentDeath()
				+ "; milestone=" + field(context.milestone())
				+ ". Stay fictional, atmospheric, non-instructional, and output only the line.";
	}

	private static String field(String value) {
		if (value == null) return "none";
		String safe = value.replaceAll("[^A-Za-z0-9_:-]", "_");
		return safe.substring(0, Math.min(48, safe.length()));
	}

	private static String response(String body, String fallback) {
		try {
			JsonObject root = JsonParser.parseString(body).getAsJsonObject();
			String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
					.getAsJsonObject("message").get("content").getAsString();
			return sanitizeOutput(content, fallback);
		} catch (RuntimeException error) {
			return fallback;
		}
	}

	private static String sanitizeOutput(String value, String fallback) {
		return DialogueTextSanitizer.sanitize(value, MAX_OUTPUT_CHARACTERS, fallback);
	}
}
