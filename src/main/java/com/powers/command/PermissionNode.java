package com.powers.command;

/** Independently grantable server controls exposed to optional permission adapters. */
public enum PermissionNode {
	DIAGNOSE("powers.command.diagnose"),
	TESTING("powers.command.testing"),
	TRAVEL("powers.command.travel"),
	ASSIGN("powers.command.assign"),
	RECOVER("powers.command.recover"),
	BOSS("powers.command.boss");

	private final String id;

	PermissionNode(String id) { this.id = id; }
	public String id() { return id; }
}
