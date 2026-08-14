package com.powers.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powers.audit.OperatorAudit;
import com.powers.audit.OperatorAuditAction;
import com.powers.audit.OperatorAuditResult;
import com.powers.mind.BodyProxyManager;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.realm.RealmLayout;
import com.powers.realm.RealmTerrain;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

/** Performs bounded administrator recovery travel to a registered dimension. */
final class PowerTravelCommand {
	private PowerTravelCommand() { }

	static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String dimensionName = StringArgumentType.getString(context, "dimension").strip();
		Identifier id = Identifier.tryParse(dimensionName);
		if (id == null) return reject(context, player, dimensionName, "invalid_dimension");
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
		MinecraftServer server = player.level().getServer();
		var target = server.getLevel(key);
		if (target == null) return reject(context, player, dimensionName, "unknown_dimension");

		UUID ownerId = player.getUUID();
		String actor = context.getSource().getDisplayName().getString();
		BlockPos arrivalChunk = BlockPos.containing(RealmLayout.ENTRY_X,
				RealmTerrain.provisionalArrivalY(target), RealmLayout.ENTRY_Z);
		boolean accepted = TravelChunkLoader.request(ownerId, target, arrivalChunk, "admin_travel",
				() -> complete(server, ownerId, key, dimensionName, actor),
				() -> fail(server, ownerId, dimensionName, actor));
		if (!accepted) return 0;
		context.getSource().sendSuccess(() -> Component.literal(
				"Preparing travel to " + dimensionName + "…").withStyle(ChatFormatting.GRAY), false);
		return 1;
	}

	private static int reject(CommandContext<CommandSourceStack> context, ServerPlayer player,
			String dimensionName, String reason) {
		OperatorCommandAudit.record(context.getSource(), OperatorAuditAction.FORCED_TRAVEL,
				OperatorAuditResult.DENIED, player.getScoreboardName(), reason);
		context.getSource().sendFailure(Component.literal((reason.equals("invalid_dimension")
				? "Invalid" : "Unknown") + " dimension: " + dimensionName));
		return 0;
	}

	private static void complete(MinecraftServer server, UUID ownerId, ResourceKey<Level> key,
			String dimensionName, String actor) {
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		var target = server.getLevel(key);
		if (player == null || target == null) {
			OperatorAudit.record(OperatorAuditAction.FORCED_TRAVEL, OperatorAuditResult.DENIED,
					actor, ownerId.toString(), "destination_disappeared");
			return;
		}
		int y = RealmTerrain.arrivalY(target, (int) RealmLayout.ENTRY_X, (int) RealmLayout.ENTRY_Z);
		BodyProxyManager.finish(player);
		player.teleportTo(target, RealmLayout.ENTRY_X, y, RealmLayout.ENTRY_Z,
				Set.of(), player.getYRot(), player.getXRot(), false);
		OperatorAudit.record(OperatorAuditAction.FORCED_TRAVEL, OperatorAuditResult.SUCCESS,
				actor, player.getScoreboardName(), "dimension_travel");
		player.sendSystemMessage(Component.literal("Traveled to " + dimensionName)
				.withStyle(ChatFormatting.GREEN));
	}

	private static void fail(MinecraftServer server, UUID ownerId, String dimensionName, String actor) {
		OperatorAudit.record(OperatorAuditAction.FORCED_TRAVEL, OperatorAuditResult.DENIED,
				actor, ownerId.toString(), "destination_load_timeout");
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		if (player != null) player.sendSystemMessage(Component.literal(
				"Travel to " + dimensionName + " timed out.").withStyle(ChatFormatting.RED));
	}
}
