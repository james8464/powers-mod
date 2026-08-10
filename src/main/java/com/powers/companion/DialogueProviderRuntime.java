package com.powers.companion;

import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Lazily rebuilds the optional provider after a configuration reload. */
public final class DialogueProviderRuntime {
	private static PowersConfig.DialogueProvider activeSettings;
	private static BoundedDialogueProvider provider;
	private static JdkDialogueTransport transport;

	private DialogueProviderRuntime() {
	}

	public static CompletableFuture<String> request(UUID speaker, LoreDialogueContext context,
			boolean bossVoice, String fallback) {
		return current().request(speaker, context, bossVoice, fallback, System.currentTimeMillis());
	}

	public static boolean enabled() {
		return current().enabled();
	}

	public static synchronized void forget(UUID speaker) {
		if (provider != null) provider.forget(speaker);
	}

	public static synchronized void clear() {
		if (provider != null) provider.clear();
		if (transport != null) transport.close();
		provider = null;
		transport = null;
		activeSettings = null;
	}

	private static synchronized BoundedDialogueProvider current() {
		PowersConfig.DialogueProvider settings = PowersConfigLoader.get().dialogueProvider();
		if (provider != null && settings.equals(activeSettings)) return provider;
		clear();
		activeSettings = settings;
		transport = new JdkDialogueTransport(settings.maxGlobalRequests());
		String credential = settings.enabled()
				? System.getenv(settings.credentialEnvironmentVariable()) : "";
		provider = new BoundedDialogueProvider(settings, transport, credential);
		return provider;
	}
}
