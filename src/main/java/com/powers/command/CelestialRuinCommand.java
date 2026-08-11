package com.powers.command;

import com.mojang.brigadier.context.CommandContext;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.CelestialRuinCancellation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Presents Heavenfall operator staging and cancellation results. */
final class CelestialRuinCommand {
	private CelestialRuinCommand() {
	}

	static int preview(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		var preview = CelestialRuinManager.preview(source.getLevel(),
				BlockPos.containing(source.getPosition()));
		source.sendSuccess(() -> Component.literal("Heavenfall dry run at " + preview.center().toShortString()
				+ " in " + preview.dimension() + ": crater=" + preview.craterChunks()
				+ " chunks, shockwave=" + preview.shockwaveChunks() + " chunks, loaded entities="
				+ preview.loadedEntityCandidates()
				+ (preview.entityLimitReached() ? "+ (scan cap reached)" : "") + "."), false);
		source.sendSuccess(() -> Component.literal("Protected regions="
				+ preview.intersectingProtectedRegions() + ", center permitted=" + preview.centerPermitted()
				+ ", terrain=" + preview.terrainDamage() + ", block entities="
				+ preview.blockEntityDamage() + ". No world state was changed."), false);
		return preview.centerPermitted() ? 1 : 0;
	}

	static int cancel(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		var result = CelestialRuinManager.cancelNearest(source.getLevel(), source.getPosition());
		Component message = switch (result) {
			case CANCELLED -> Component.literal("The nearest staged Heavenfall was cancelled before lock.");
			case LOCKED -> Component.literal("The nearest Heavenfall has begun preload and is irreversible.");
			case NONE -> Component.literal("No Heavenfall exists in this dimension.");
		};
		if (result == CelestialRuinCancellation.CANCELLED) {
			source.sendSuccess(() -> message, true);
			return 1;
		}
		source.sendFailure(message);
		return 0;
	}
}
