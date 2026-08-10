package com.powers.forge;

import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.resources.Identifier;

/** Stable server-authored choice shown by the Crucible menu. */
public record CrucibleChoice(String id, CrucibleOperation operation,
		ArtifactAlignment alignment, Identifier targetItem) {
	public CrucibleChoice {
		if (id == null || !id.matches("[a-z0-9_]{1,64}") || operation == null) {
			throw new IllegalArgumentException("Invalid Crucible choice");
		}
		if (operation == CrucibleOperation.CONVERT && (alignment == null || targetItem == null)) {
			throw new IllegalArgumentException("Conversion choice requires alignment and target");
		}
	}
}
