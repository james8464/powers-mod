package com.powers.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.spell.CelestialSearchMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Bounded real-client visual acceptance routes beneath the operator testing command. */
final class VfxTestingCommands {
	private VfxTestingCommands() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> create() {
		return Commands.literal("vfx")
				.then(Commands.literal("locator-entity").executes(VfxTestingCommands::openLocator))
				.then(Commands.literal("advancement-dark").executes(VfxTestingCommands::prepareDarkAdvancement));
	}

	private static int openLocator(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		PowersPackets.openLocator(player, CelestialSearchMode.ENTITY);
		context.getSource().sendSuccess(() -> Component.literal("Opened production entity locator"), false);
		return 1;
	}

	private static int prepareDarkAdvancement(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		player.addTag(SkillSystem.DARKNESS_TAG);
		PlayerPowers.get(player).setDarknessLevel(player, 1);
		SkillSystem.syncPathVisibility(player);
		SkillSystem.awardDarknessRite(player, 1);
		context.getSource().sendSuccess(
				() -> Component.literal("Prepared production Darkness advancement root"), false);
		return 1;
	}
}
