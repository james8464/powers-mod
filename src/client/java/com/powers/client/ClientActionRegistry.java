package com.powers.client;

/** Latest authoritative action revision observed in a server-authored menu snapshot. */
public final class ClientActionRegistry {
	private static long revision;
	private static String artifactActionKey = "";
	private ClientActionRegistry() { }
	public static long revision() { return revision; }
	public static void accept(long current) { revision = Math.max(0L, current); }
	public static void acceptArtifact(long current, String actionKey) {
		accept(current);
		artifactActionKey = actionKey == null ? "" : actionKey;
	}
	public static String artifactActionKey() { return artifactActionKey; }
	public static void reset() { revision = 0L; artifactActionKey = ""; }
}
