package com.powers.client;

/** Latest authoritative action revision observed in a server-authored menu snapshot. */
public final class ClientActionRegistry {
	private static long revision;
	private static String artifactActionKey = "";
	private ClientActionRegistry() { }
	public static long revision() { return revision; }
	public static boolean accept(long current) {
		if (current < revision) return false;
		revision = Math.max(0L, current);
		return true;
	}
	public static boolean acceptArtifact(long current, String actionKey) {
		if (!accept(current)) return false;
		artifactActionKey = actionKey == null ? "" : actionKey;
		return true;
	}
	public static String artifactActionKey() { return artifactActionKey; }
	public static void reset() { revision = 0L; artifactActionKey = ""; }
}
