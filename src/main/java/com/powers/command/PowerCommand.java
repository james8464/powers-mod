package com.powers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powers.player.PlayerPowers;
import com.powers.PowersEntities;
import com.powers.audit.OperatorAuditAction;
import com.powers.audit.OperatorAuditResult;
import com.powers.player.SkillSystem;
import com.powers.config.PowersConfigLoader;
import com.powers.mind.BodyProxyManager;
import com.powers.power.Ability;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PowerAffinity;
import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * Registers the administrator's power-management commands:
 * {@code /powers list}, {@code /powers slots [player]},
 * {@code /powers assign <player> <power> <slot>},
 * {@code /powers reroll [player]}, {@code /powers darkprefix [true|false]},
 * and {@code /powers travel <dimension>}.
 *
 * <p>Mutating branches enforce the configured operator permission on the
 * server; player-facing branches expose only the caller's permitted state.</p>
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
								.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("slots")))
								.executes(PowerCommand::slotsOther)))
				.then(Commands.literal("assign")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("assign")))
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("power", StringArgumentType.word())
										.then(Commands.argument("slot", IntegerArgumentType.integer(0, PlayerPowers.SLOT_COUNT - 1))
												.executes(PowerCommand::assign)))))
				.then(Commands.literal("reroll")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("reroll"))
								|| PowersConfigLoader.get().allowSelfReroll())
						.executes(PowerCommand::rerollSelf)
						.then(Commands.argument("player", EntityArgument.player())
								.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("reroll")))
								.executes(PowerCommand::rerollOther)))
				.then(Commands.literal("consent")
						.then(consentLiteral("teleport", com.powers.protection.ConsentKind.TELEPORT))
						.then(consentLiteral("locator", com.powers.protection.ConsentKind.LOCATOR))
						.then(consentLiteral("companion", com.powers.protection.ConsentKind.COMPANION))
						.then(consentLiteral("dreamwalk", com.powers.protection.ConsentKind.DREAMWALK))
						.then(consentLiteral("possession", com.powers.protection.ConsentKind.POSSESSION)))
				.then(Commands.literal("reload")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("reload")))
						.executes(PowerCommand::reloadConfig))
				.then(Commands.literal("return")
						.executes(PowerCommand::returnToBody))
				.then(Commands.literal("recover")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("recover")))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(PowerCommand::recoverBody)))
				.then(Commands.literal("path")
						.then(Commands.literal("list").executes(PowerCommand::pathList))
						.then(Commands.literal("unlock")
								.then(Commands.argument("node", StringArgumentType.word())
										.executes(PowerCommand::pathUnlock)))
						.then(Commands.literal("focus")
								.then(Commands.argument("node", StringArgumentType.word())
										.executes(PowerCommand::pathFocus)))
						.then(Commands.literal("respec").executes(PowerCommand::pathRespec)))
				.then(Commands.literal("darkprefix")
						.executes(PowerCommand::darkPrefixToggle)
						.then(Commands.literal("true")
								.executes(PowerCommand::darkPrefixShow))
						.then(Commands.literal("false")
								.executes(PowerCommand::darkPrefixHide)))
				.then(Commands.literal("boss")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("boss")))
						.then(Commands.literal("spawn")
								.executes(PowerCommand::spawnFirstVessel)))
				.then(Commands.literal("diagnose")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("diagnose")))
						.executes(PowerDiagnosticsCommand::run)
						.then(Commands.literal("export").executes(PowerDiagnosticsCommand::export)))
				.then(Commands.literal("ruin")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("ruin")))
							.then(Commands.literal("preview").executes(CelestialRuinCommand::preview))
							.then(Commands.literal("cancel").executes(CelestialRuinCommand::cancel)))
				.then(Commands.literal("shadow")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("shadow")))
						.then(Commands.literal("learning")
								.then(Commands.literal("reset")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(PowerCommand::resetShadowLearning)))))
				.then(TestingCommand.create())
				.then(Commands.literal("travel")
						.requires(source -> PermissionNodes.allows(source, PermissionCommandRoutes.forControl("travel")))
						.then(Commands.argument("dimension", dimensionArgument())
								.executes(PowerTravelCommand::run))));
	}

	static StringArgumentType dimensionArgument() { return StringArgumentType.greedyString(); }

	private static int resetShadowLearning(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		com.powers.companion.PrivateCompanionManager.resetLearning(player);
		context.getSource().sendSuccess(() -> Component.literal("Reset bounded Shadow combat learning for "
				+ player.getScoreboardName() + "; conversation memory was preserved."), true);
		return 1;
	}

	private static int spawnFirstVessel(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		var boss = PowersEntities.FIRST_VESSEL.create(source.getLevel(),
				net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		if (boss == null) {
			source.sendFailure(Component.literal("The First Vessel could not manifest."));
			return 0;
		}
		var position = source.getPosition();
		boss.setPos(position.x, position.y, position.z);
		source.getLevel().addFreshEntity(boss);
		source.sendSuccess(() -> Component.literal("The First Vessel has awakened.")
				.withStyle(ChatFormatting.DARK_PURPLE), true);
		return 1;
	}
	private static int returnToBody(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (!BodyProxyManager.returnToBody(player)) {
			context.getSource().sendFailure(Component.literal("You have no reachable mind-body anchor."));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal("Your spirit returned to your body."), false);
		return 1;
	}
	private static int recoverBody(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		if (!BodyProxyManager.recoverToBody(player)) {
			OperatorCommandAudit.record(context.getSource(), OperatorAuditAction.RECOVERY, OperatorAuditResult.DENIED,
					player.getScoreboardName(), "no_anchor");
			context.getSource().sendFailure(Component.literal("That player has no recoverable mind-body anchor."));
			return 0;
		}
		OperatorCommandAudit.record(context.getSource(), OperatorAuditAction.RECOVERY, OperatorAuditResult.SUCCESS,
				player.getScoreboardName(), "body_anchor");
		context.getSource().sendSuccess(() -> Component.literal(
				"Administratively recovered " + player.getName().getString() + "."), true);
		return 1;
	}
	private static int pathList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean darkness = SkillSystem.hasDarknessTag(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		var progress = data.rankProgress(darkness);
		Set<String> choices = graph.unlockable(progress.completed(),
				darkness ? data.darknessLevel() : data.skillLevel());
		context.getSource().sendSuccess(() -> Component.literal("Focused title: " + progress.focus()
				+ " | Unlockable: " + (choices.isEmpty() ? "none" : String.join(", ", choices))), false);
		return choices.size();
	}

	private static int pathUnlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean darkness = SkillSystem.hasDarknessTag(player);
		String nodeId = StringArgumentType.getString(context, "node");
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		RankNode node = graph.node(nodeId);
		if (node == null || !PlayerPowers.get(player).unlockRankNode(darkness, nodeId)) {
			context.getSource().sendFailure(Component.literal("That path is not currently reachable."));
			return 0;
		}
		SkillSystem.refreshPrefix(player);
		context.getSource().sendSuccess(() -> Component.literal("Unlocked and focused: " + node.title())
				.withStyle(ChatFormatting.LIGHT_PURPLE), false);
		return 1;
	}

	private static int pathFocus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean darkness = SkillSystem.hasDarknessTag(player);
		String nodeId = StringArgumentType.getString(context, "node");
		if (!PlayerPowers.get(player).setRankFocus(darkness, nodeId)) {
			context.getSource().sendFailure(Component.literal("You have not unlocked that title."));
			return 0;
		}
		SkillSystem.refreshPrefix(player);
		context.getSource().sendSuccess(() -> Component.literal("Focused title: " + nodeId), false);
		return 1;
	}

	private static int pathRespec(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int cost = PowersConfigLoader.get().rankRespecExperienceLevels();
		if (player.experienceLevel < cost) {
			context.getSource().sendFailure(Component.literal("The respec ritual requires " + cost + " levels."));
			return 0;
		}
		player.giveExperienceLevels(-cost);
		PlayerPowers.get(player).respecRankMaze(SkillSystem.hasDarknessTag(player));
		SkillSystem.refreshPrefix(player);
		context.getSource().sendSuccess(() -> Component.literal("Branch titles reset; earned rank depth was preserved."), false);
		return 1;
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> consentLiteral(
			String name, com.powers.protection.ConsentKind kind) {
		return Commands.literal(name)
				.then(Commands.literal("allow").executes(context -> setConsent(context, kind, true)))
				.then(Commands.literal("deny").executes(context -> setConsent(context, kind, false)));
	}

	private static int setConsent(CommandContext<CommandSourceStack> context,
			com.powers.protection.ConsentKind kind, boolean allowed) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		PlayerPowers.get(player).setConsent(kind, allowed);
		context.getSource().sendSuccess(() -> Component.literal(kind.name().toLowerCase()
				+ " consent " + (allowed ? "allowed" : "denied")).withStyle(ChatFormatting.AQUA), false);
		return 1;
	}

	private static int reloadConfig(CommandContext<CommandSourceStack> context) {
		boolean loaded = PowersConfigLoader.reload();
		if (!loaded) {
			context.getSource().sendFailure(Component.literal("POWERS configuration is invalid; kept the last valid settings."));
			return 0;
		}
		if (!PowersConfigLoader.get().persistCooldowns()) {
			for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
				PlayerPowers.get(player).clearCooldowns();
				com.powers.network.PowersPackets.syncTo(player);
			}
		}
		OperatorCommandAudit.sendConfigReport(context, PowersConfigLoader.validationReport());
		return 1;
	}

	static boolean isAdmin(CommandSourceStack source) {
		return hasVanillaAdmin(source);
	}

	static boolean hasVanillaAdmin(CommandSourceStack source) {
		return switch (CommandPermissionRules.tier(PowersConfigLoader.get().adminPermissionLevel())) {
			case 0 -> true;
			case 1 -> source.permissions().hasPermission(
					net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR);
			case 2 -> source.permissions().hasPermission(
					net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
			case 3 -> source.permissions().hasPermission(
					net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN);
			default -> source.permissions().hasPermission(
					net.minecraft.server.permissions.Permissions.COMMANDS_OWNER);
		};
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
		PowerAffinity allegiance = SkillSystem.hasDarknessTag(player)
				? PowerAffinity.DARKNESS : PowerAffinity.RADIANT;
		if (!power.affinity().permits(allegiance)) {
			context.getSource().sendFailure(Component.literal("That innate power belongs to the other allegiance."));
			return 0;
		}

		List<String> ids = new java.util.ArrayList<>(PlayerPowers.get(player).getSlotIds());
		// a player with no saved loadout yet gets random powers first, so assign works on a full set
		if (ids.size() != PlayerPowers.SLOT_COUNT) {
			ids = new java.util.ArrayList<>();
			for (Power p : PowerRegistry.randomDistinct(PlayerPowers.SLOT_COUNT,
					new java.util.Random(), allegiance)) {
				ids.add(p.id().toString());
			}
		}
		// Reject duplicate slots so one assignment cannot reduce loadout variety.
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

	/**
	 * /powers darkprefix toggles whether a darkness-tagged player's visible
	 * prefix is their real darkness title or the equivalent normal-ladder name.
	 * only matters to darkness users, everyone else is unaffected
	 */
	private static int darkPrefixToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean hidden = data.isDarknessPrefixHidden();
		return applyDarkPrefix(player, data, !hidden);
	}

	private static int darkPrefixShow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return applyDarkPrefix(context.getSource().getPlayerOrException(), PlayerPowers.get(context.getSource().getPlayerOrException()), false);
	}

	private static int darkPrefixHide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return applyDarkPrefix(context.getSource().getPlayerOrException(), PlayerPowers.get(context.getSource().getPlayerOrException()), true);
	}

	private static int applyDarkPrefix(ServerPlayer player, PlayerPowers.PlayerPowersData data, boolean hidden) {
		data.setDarknessPrefixHidden(hidden);
		SkillSystem.refreshPrefix(player);
		// the choice only does anything for darkness-tagged players, others just get a note
		if (SkillSystem.hasDarknessTag(player)) {
			contextMessage(player, hidden);
		} else {
			player.sendSystemMessage(Component.literal("You are not touched by darkness — the prefix choice is idle for you."));
		}
		return 1;
	}

	private static void contextMessage(ServerPlayer player, boolean hidden) {
		if (hidden) {
			player.sendSystemMessage(Component.literal("Your darkness title is hidden — others now see the rank of an ordinary power-wielder.")
					.withStyle(ChatFormatting.GRAY));
		} else {
			player.sendSystemMessage(Component.literal("Your true darkness title is revealed.")
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
