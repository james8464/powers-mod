package com.powers.network;

/** Pure validation shared by the configuration-stage protocol handshake. */
public final class ProtocolHandshakeRules {
	public static final int CURRENT_PROTOCOL = 2;
	public static final int MAX_VERSION_LENGTH = 64;

	private ProtocolHandshakeRules() {
	}

	public record Result(boolean accepted, String message) {
	}

	public static Result validate(int serverProtocol, int clientProtocol, String clientModVersion) {
		String version = clientModVersion == null ? "" : clientModVersion.trim();
		if (serverProtocol < 0 || clientProtocol < 0
				|| version.isEmpty() || version.length() > MAX_VERSION_LENGTH) {
			return new Result(false, "Invalid POWERS configuration handshake.");
		}
		if (serverProtocol != clientProtocol) {
			return new Result(false, "Incompatible POWERS network protocol: server protocol "
					+ serverProtocol + ", client protocol " + clientProtocol + ", client mod "
					+ version + ". Install the same POWERS version as the server.");
		}
		return new Result(true, "POWERS protocol " + serverProtocol + " accepted (client mod "
				+ version + ").");
	}
}
