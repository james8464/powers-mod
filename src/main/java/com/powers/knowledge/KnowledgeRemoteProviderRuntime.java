package com.powers.knowledge;

import com.powers.companion.JdkDialogueTransport;
import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Lazily shares the opt-in OpenAI-compatible endpoint settings without blocking ticks. */
public final class KnowledgeRemoteProviderRuntime {
	private static PowersConfig.DialogueProvider activeSettings;
	private static BoundedKnowledgeProvider provider;
	private static JdkDialogueTransport transport;

	private KnowledgeRemoteProviderRuntime() {
	}

	public static CompletableFuture<KnowledgeAnswer> request(UUID owner, KnowledgeQuery query,
			KnowledgeAnswer offline) {
		return current().request(owner, query, offline, System.currentTimeMillis());
	}

	public static synchronized void forget(UUID owner) {
		if (provider != null) provider.forget(owner);
	}

	public static synchronized void clear() {
		if (provider != null) provider.clear();
		if (transport != null) transport.close();
		provider = null;
		transport = null;
		activeSettings = null;
	}

	private static synchronized BoundedKnowledgeProvider current() {
		PowersConfig.DialogueProvider settings = PowersConfigLoader.get().dialogueProvider();
		if (provider != null && settings.equals(activeSettings)) return provider;
		clear();
		activeSettings = settings;
		transport = new JdkDialogueTransport(settings.maxGlobalRequests());
		String credential = settings.enabled()
				? System.getenv(settings.credentialEnvironmentVariable()) : "";
		provider = new BoundedKnowledgeProvider(settings, transport, credential);
		return provider;
	}
}
