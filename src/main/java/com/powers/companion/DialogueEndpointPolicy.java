package com.powers.companion;

import java.net.URI;

/** Validates the narrow network boundary used by optional dialogue providers. */
public final class DialogueEndpointPolicy {
	private DialogueEndpointPolicy() {}

	/** Accepts remote HTTPS and loopback-only HTTP endpoints. */
	public static URI validated(String value) {
		try {
			URI uri = URI.create(value == null ? "" : value);
			boolean secure = "https".equalsIgnoreCase(uri.getScheme());
			boolean local = "http".equalsIgnoreCase(uri.getScheme()) && isLoopback(uri);
			return uri.isAbsolute() && uri.getHost() != null && (secure || local) ? uri : null;
		} catch (IllegalArgumentException error) {
			return null;
		}
	}

	/** Hosted endpoints require a secret; localhost Ollama does not. */
	public static boolean credentialAccepted(URI endpoint, String credential) {
		return endpoint != null && (isLoopback(endpoint)
				|| credential != null && !credential.isBlank());
	}

	public static boolean isLoopback(URI endpoint) {
		String host = endpoint == null ? null : endpoint.getHost();
		return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
				|| "::1".equals(host) || "[::1]".equals(host);
	}
}
