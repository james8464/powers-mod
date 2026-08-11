package com.powers.command;

import java.util.Locale;

/** Canonical mapping between sensitive command controls and external node names. */
public final class PermissionCommandRoutes {
	private PermissionCommandRoutes() { }

	public static PermissionNode forControl(String control) {
		return switch (control == null ? "" : control.toLowerCase(Locale.ROOT)) {
			case "diagnose", "reload" -> PermissionNode.DIAGNOSE;
			case "testing" -> PermissionNode.TESTING;
			case "travel" -> PermissionNode.TRAVEL;
			case "assign", "slots", "reroll" -> PermissionNode.ASSIGN;
			case "recover", "shadow" -> PermissionNode.RECOVER;
			case "boss", "ruin" -> PermissionNode.BOSS;
			default -> throw new IllegalArgumentException("Unknown sensitive control");
		};
	}
}
