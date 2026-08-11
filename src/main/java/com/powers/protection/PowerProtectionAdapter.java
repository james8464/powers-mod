package com.powers.protection;

/**
 * Optional integration boundary for claim mods. Implementations return false
 * to deny; they must be fast, side-effect free, and safe on the server thread.
 */
@FunctionalInterface
public interface PowerProtectionAdapter {
	boolean allows(ProtectionQuery query);
}
