package com.powers.companion;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletionException;

/** Small daemon-backed HTTP transport for an administrator-supplied endpoint. */
public final class JdkDialogueTransport implements DialogueTransport, AutoCloseable {
	private static final Gson GSON = new Gson();
	private final ExecutorService executor;
	private final HttpClient client;

	public JdkDialogueTransport(int maximumThreads) {
		executor = Executors.newFixedThreadPool(Math.clamp(maximumThreads, 1, 4), runnable -> {
			Thread thread = new Thread(runnable, "powers-lore-provider");
			thread.setDaemon(true);
			return thread;
		});
		client = HttpClient.newBuilder().executor(executor)
				.connectTimeout(Duration.ofMillis(2_500)).build();
	}

	@Override
	public CompletableFuture<String> request(URI endpoint, String model, String credential,
			String prompt, Duration timeout) {
		String body = GSON.toJson(Map.of(
				"model", model,
				"messages", List.of(Map.of("role", "user", "content", prompt)),
				"max_tokens", 96));
		HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
				.header("Authorization", "Bearer " + credential)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
				.thenApply(JdkDialogueTransport::boundedBody);
	}

	private static String boundedBody(HttpResponse<java.io.InputStream> response) {
		try (var stream = response.body()) {
			if (response.statusCode() < 200 || response.statusCode() >= 300) return "";
			byte[] bytes = stream.readNBytes(8_193);
			return bytes.length <= 8_192 ? new String(bytes, StandardCharsets.UTF_8) : "";
		} catch (IOException error) {
			throw new CompletionException(error);
		}
	}

	@Override
	public void close() {
		executor.shutdownNow();
	}
}
