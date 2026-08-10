package com.powers.companion;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Asynchronous transport boundary used by the optional fictional-lore provider. */
@FunctionalInterface
public interface DialogueTransport {
	CompletableFuture<String> request(URI endpoint, String model, String credential,
			String prompt, Duration timeout);
}
