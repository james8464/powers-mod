package com.powers.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powers.PowersEntities;
import com.powers.entity.PowerTestActor;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.testing.TestingOverrides;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/** Operator-only, session-scoped controls for rapid manual gameplay testing. */
final class TestingCommand {
	private TestingCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> create() {
		return Commands.literal("testing")
				.requires(PowerCommand::isAdmin)
				.executes(TestingCommand::status)
				.then(Commands.literal("status").executes(TestingCommand::status))
				.then(Commands.literal("on").executes(context -> setAll(context, true)))
				.then(Commands.literal("off").executes(context -> setAll(context, false)))
				.then(Commands.literal("energy")
						.then(Commands.literal("on").executes(context -> setEnergy(context, true)))
						.then(Commands.literal("off").executes(context -> setEnergy(context, false))))
				.then(Commands.literal("cooldowns")
						.then(Commands.literal("on").executes(context -> setCooldowns(context, true)))
						.then(Commands.literal("off").executes(context -> setCooldowns(context, false))))
				.then(Commands.literal("refill").executes(TestingCommand::refill))
				.then(Commands.literal("actor")
						.then(Commands.literal("spawn")
								.executes(context -> spawnActor(context, null))
								.then(Commands.argument("username", StringArgumentType.word())
										.executes(context -> spawnActor(context,
												StringArgumentType.getString(context, "username"))))));
	}

	private static int setAll(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TestingOverrides.setAll(player.getUUID(), enabled);
		if (enabled) refillPlayer(player);
		else PlayerPowers.get(player).clearCooldowns();
		return status(context);
	}

	private static int setEnergy(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TestingOverrides.setEnergyDisabled(player.getUUID(), enabled);
		if (enabled) {
			PlayerPowers.get(player).forceRestoreEnergy();
			PowersPackets.syncTo(player);
		}
		return status(context);
	}

	private static int setCooldowns(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TestingOverrides.setCooldownsDisabled(player.getUUID(), enabled);
		if (enabled) PlayerPowers.get(player).clearCooldowns();
		return status(context);
	}

	private static int refill(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		refillPlayer(context.getSource().getPlayerOrException());
		context.getSource().sendSuccess(() -> Component.literal(
				"Energy refilled and saved power cooldowns cleared.")
				.withStyle(ChatFormatting.AQUA), false);
		return 1;
	}

	private static void refillPlayer(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.forceRestoreEnergy();
		data.clearCooldowns();
		PowersPackets.syncTo(player);
	}

	private static int status(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TestingOverrides.State state = TestingOverrides.state(player.getUUID());
		context.getSource().sendSuccess(() -> Component.literal("Testing limits — energy: ")
				.append(toggle(state.energyDisabled())).append(Component.literal(", cooldowns: "))
				.append(toggle(state.cooldownsDisabled())), false);
		return 1;
	}

	private static Component toggle(boolean disabled) {
		return Component.literal(disabled ? "DISABLED" : "normal")
				.withStyle(disabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
	}

	private static int spawnActor(CommandContext<CommandSourceStack> context, String username) {
		CommandSourceStack source = context.getSource();
		PowerTestActor actor = PowersEntities.POWER_TEST_ACTOR.create(
				source.getLevel(), EntitySpawnReason.COMMAND);
		if (actor == null) {
			source.sendFailure(Component.literal("A Power Test Actor could not be created."));
			return 0;
		}
		if (username != null) actor.setTestingUsername(username);
		var position = source.getPosition();
		actor.setPos(position.x, position.y, position.z);
		source.getLevel().addFreshEntity(actor);
		source.sendSuccess(() -> Component.literal("Spawned test actor: ")
				.append(Component.literal(actor.testingUsername()).withStyle(ChatFormatting.AQUA)), true);
		return 1;
	}
}
