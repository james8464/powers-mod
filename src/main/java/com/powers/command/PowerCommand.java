package com.powers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * /powers list
 * /powers slots [player]
 * /powers assign <player> <power> <slot>
 * /powers reroll [player]
 *
 * <p>The Rainbow Crystal item is the friendly path; the command is for server
 * admins.
 */
public final class PowerCommand {
	private PowerCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("powers")
				.then(Commands.literal("list")
						.executes(PowerCommand::list))
				.then(Commands.literal("slots")
						.executes(PowerCommand::slotsSelf)
						.then(Commands.argument("player", EntityArgument.player())
								.requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
								.executes(PowerCommand::slotsOther)))
				.then(Commands.literal("assign")
						.requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("power", StringArgumentType.word())
										.then(Commands.argument("slot", IntegerArgumentType.integer(0, PlayerPowers.SLOT_COUNT - 1))
												.executes(PowerCommand::assign)))))
				.then(Commands.literal("reroll")
						.executes(PowerCommand::rerollSelf)
						.then(Commands.argument("player", EntityArgument.player())
								.requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
								.executes(PowerCommand::rerollOther)))
				.then(Commands.literal("travel")
						.requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.argument("dimension", StringArgumentType.word())
								.executes(PowerCommand::travel))));
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		StringBuilder sb = new StringBuilder();
		for (Power power : PowerRegistry.getAll()) {
			if (!sb.isEmpty()) {
				sb.append(", ");
			}
			sb.append(power.id().getPath());
		}
		context.getSource().sendSuccess(() -> Component.literal("Available powers: " + sb), false);
		return PowerRegistry.getAll().size();
	}

	private static int slotsSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return showSlots(context.getSource(), context.getSource().getPlayerOrException());
	}

	private static int slotsOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return showSlots(context.getSource(), EntityArgument.getPlayer(context, "player"));
	}

	private static int showSlots(CommandSourceStack source, ServerPlayer player) {
		List<String> ids = PlayerPowers.get(player).getSlotIds();
		if (ids.isEmpty()) {
			source.sendFailure(Component.literal(player.getScoreboardName() + " has no powers assigned."));
			return 0;
		}
		for (int slot = 0; slot < ids.size(); slot++) {
			Power power = PowerRegistry.get(ids.get(slot));
			Ability ability = power.ability();
			int slotNum = slot + 1;
			String powerName = power.name().getString();
			String abilityName = ability.name().getString();
			source.sendSuccess(() -> Component.literal("  [" + slotNum + "] " + powerName
					+ " - " + abilityName), false);
		}
		return ids.size();
	}

	private static int assign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		String powerId = StringArgumentType.getString(context, "power");
		int slot = IntegerArgumentType.getInteger(context, "slot");

		Power power = PowerRegistry.get(powerId);
		if (power == null) {
			context.getSource().sendFailure(Component.literal("Unknown power: " + powerId));
			return 0;
		}
		String canonicalPowerId = power.id().toString();

		List<String> ids = new java.util.ArrayList<>(PlayerPowers.get(player).getSlotIds());
		if (ids.size() != PlayerPowers.SLOT_COUNT) {
			ids = new java.util.ArrayList<>();
			for (Power p : PowerRegistry.randomDistinct(PlayerPowers.SLOT_COUNT, new java.util.Random())) {
				ids.add(p.id().toString());
			}
		}
		if (ids.contains(canonicalPowerId) && !ids.get(slot).equals(canonicalPowerId)) {
			context.getSource().sendFailure(Component.literal("That power is already assigned to another slot."));
			return 0;
		}
		ids.set(slot, canonicalPowerId);
		PlayerPowers.get(player).setSlots(player, ids);

		context.getSource().sendSuccess(() -> Component.literal("Assigned " + power.name().getString()
				+ " to slot " + (slot + 1) + " of " + player.getScoreboardName()).withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private static int rerollSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return reroll(context.getSource(), context.getSource().getPlayerOrException());
	}

	private static int rerollOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return reroll(context.getSource(), EntityArgument.getPlayer(context, "player"));
	}

	private static int reroll(CommandSourceStack source, ServerPlayer player) {
		PlayerPowers.get(player).assignRandom(player, true);
		source.sendSuccess(() -> Component.literal("Rerolled powers for " + player.getScoreboardName())
				.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	/** Teleports the executing player to a registered dimension (e.g. powers:dark_realm). */
	private static int travel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String dimName = StringArgumentType.getString(context, "dimension");
		Identifier id = Identifier.tryParse(dimName);
		if (id == null) {
			context.getSource().sendFailure(Component.literal("Invalid dimension: " + dimName));
			return 0;
		}
		ServerLevel target = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
		if (target == null) {
			context.getSource().sendFailure(Component.literal("Unknown dimension: " + dimName));
			return 0;
		}
		player.teleportTo(target, 8.0, -58.0, 8.0, Set.of(), player.getYRot(), player.getXRot(), false);
		context.getSource().sendSuccess(() -> Component.literal("Traveled to " + dimName)
				.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}
}
