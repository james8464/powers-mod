package com.powers.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powers.PowersEntities;
import com.powers.entity.PowerTestActor;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.testing.GameplayAcceptanceCatalogue;
import com.powers.testing.TestingArenaLayout;
import com.powers.testing.TestingOverrides;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Operator-only, session-scoped controls for rapid manual gameplay testing. */
final class TestingCommand {
	private static final String ARENA_TAG = "powers_testing_arena";
	private static final double ARENA_CLEANUP_RADIUS = 24.0;

	private TestingCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> create() {
		return Commands.literal("testing")
				.requires(PowerCommand::isAdmin)
				.executes(TestingCommand::status)
				.then(Commands.literal("status").executes(TestingCommand::status))
				.then(Commands.literal("on").executes(context -> setAll(context, true)))
				.then(Commands.literal("off").executes(context -> setAll(context, false)))
				.then(Commands.literal("reset").executes(TestingCommand::reset))
				.then(Commands.literal("energy")
						.then(Commands.literal("on").executes(context -> setEnergy(context, true)))
						.then(Commands.literal("off").executes(context -> setEnergy(context, false))))
				.then(Commands.literal("cooldowns")
						.then(Commands.literal("on").executes(context -> setCooldowns(context, true)))
						.then(Commands.literal("off").executes(context -> setCooldowns(context, false))))
				.then(Commands.literal("refill").executes(TestingCommand::refill))
				.then(Commands.literal("coverage").executes(TestingCommand::coverage))
				.then(Commands.literal("quest-telemetry")
						.executes(TestingCommand::questTelemetry))
				.then(Commands.literal("profile")
						.executes(TestingCommand::profileStatus)
						.then(Commands.literal("status").executes(TestingCommand::profileStatus))
						.then(Commands.literal("start")
								.then(Commands.argument("minutes", IntegerArgumentType.integer(1, 180))
										.then(Commands.argument("expectedPlayers",
												IntegerArgumentType.integer(0, 1_000))
												.executes(TestingCommand::profileStart)))))
				.then(Commands.literal("arena")
						.executes(TestingCommand::spawnArena)
						.then(Commands.literal("spawn").executes(TestingCommand::spawnArena))
						.then(Commands.literal("clear").executes(TestingCommand::clearArena)))
				.then(Commands.literal("actor")
						.then(Commands.literal("spawn")
								.executes(context -> spawnActor(context, null))
								.then(Commands.argument("username", StringArgumentType.word())
										.executes(context -> spawnActor(context,
												StringArgumentType.getString(context, "username"))))));
	}

	private static int coverage(CommandContext<CommandSourceStack> context) {
		int count = GameplayAcceptanceCatalogue.entries().size();
		context.getSource().sendSuccess(() -> Component.literal("Acceptance catalogue — ")
				.append(Component.literal(GameplayAcceptanceCatalogue.summary())
						.withStyle(ChatFormatting.AQUA)), false);
		return count;
	}

	private static int questTelemetry(CommandContext<CommandSourceStack> context) {
		var source = context.getSource();
		source.sendSuccess(() -> Component.literal(
				com.powers.progression.QuestCompletionTelemetry.diagnosticLine(source.getServer()))
				.withStyle(ChatFormatting.AQUA), false);
		for (String row : com.powers.progression.QuestCompletionTelemetry.reportRows(
				source.getServer())) {
			source.sendSuccess(() -> Component.literal(row).withStyle(ChatFormatting.GRAY), false);
		}
		return 1;
	}

	private static int profileStart(CommandContext<CommandSourceStack> context) {
		int minutes = IntegerArgumentType.getInteger(context, "minutes");
		int expectedPlayers = IntegerArgumentType.getInteger(context, "expectedPlayers");
		boolean started = com.powers.performance.ServerTickProfiler.start(
				context.getSource().getServer(), expectedPlayers, minutes * 60 * 20,
				expectedPlayers + "p-" + minutes + "m");
		if (!started) {
			context.getSource().sendFailure(Component.literal("A server tick profile is already active."));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal("Started a " + minutes
				+ " minute profile for the " + expectedPlayers + "-player scenario; JFR and JSON"
				+ " will be written under the server's profiles directory.")
				.withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	private static int profileStatus(CommandContext<CommandSourceStack> context) {
		var status = com.powers.performance.ServerTickProfiler.status(context.getSource().getServer());
		context.getSource().sendSuccess(() -> Component.literal("Profile active=" + status.active()
				+ "; expectedPlayers=" + status.expectedPlayers()
				+ "; connectedPlayers=" + status.connectedPlayers()
				+ "; ticks=" + status.sampledTicks() + "/" + status.requestedTicks()
				+ "; label=" + status.label()).withStyle(ChatFormatting.AQUA), false);
		return status.active() ? 1 : 0;
	}

	private static int spawnArena(CommandContext<CommandSourceStack> context) {
		clearArenaEntities(context.getSource());
		var level = context.getSource().getLevel();
		Vec3 origin = context.getSource().getPosition();
		int spawned = 0;
		for (TestingArenaLayout.Target target : TestingArenaLayout.targets()) {
			LivingEntity entity = createArenaTarget(level, target);
			if (entity == null) continue;
			entity.setPos(origin.x + target.x(), origin.y, origin.z + target.z());
			entity.addTag(ARENA_TAG);
			entity.setCustomName(Component.literal(target.name()));
			entity.setCustomNameVisible(true);
			if (entity instanceof Mob mob) {
				mob.setNoAi(true);
				mob.setPersistenceRequired();
			}
			if (level.addFreshEntity(entity)) spawned++;
		}
		int result = spawned;
		context.getSource().sendSuccess(() -> Component.literal("Spawned " + result
				+ " bounded acceptance targets; use /powers testing arena clear when finished.")
				.withStyle(ChatFormatting.AQUA), false);
		return result;
	}

	private static int clearArena(CommandContext<CommandSourceStack> context) {
		int removed = clearArenaEntities(context.getSource());
		context.getSource().sendSuccess(() -> Component.literal("Cleared " + removed
				+ " nearby acceptance targets.").withStyle(ChatFormatting.GRAY), false);
		return removed;
	}

	private static int clearArenaEntities(CommandSourceStack source) {
		Vec3 center = source.getPosition();
		AABB bounds = AABB.ofSize(center, ARENA_CLEANUP_RADIUS * 2.0,
				ARENA_CLEANUP_RADIUS * 2.0, ARENA_CLEANUP_RADIUS * 2.0);
		var targets = BoundedEntityCandidates.living(source.getLevel(), bounds, 64,
				entity -> entity.entityTags().contains(ARENA_TAG));
		targets.forEach(LivingEntity::discard);
		return targets.size();
	}

	private static LivingEntity createArenaTarget(net.minecraft.server.level.ServerLevel level,
			TestingArenaLayout.Target target) {
		LivingEntity entity = switch (target.kind()) {
			case NEUTRAL_ACTOR, RADIANT_ACTOR, DARKNESS_ACTOR ->
					PowersEntities.POWER_TEST_ACTOR.create(level, EntitySpawnReason.COMMAND);
			case ZOMBIE -> EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			case IRON_GOLEM -> EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
			case DARKNESS_CREATURE -> PowersEntities.DARKNESS_CREATURE.create(
					level, EntitySpawnReason.COMMAND);
			case RADIANT_SENTINEL -> PowersEntities.RADIANT_SENTINEL.create(
					level, EntitySpawnReason.COMMAND);
		};
		if (entity instanceof PowerTestActor actor) actor.setTestingUsername(target.name());
		if (target.kind() == TestingArenaLayout.TargetKind.DARKNESS_ACTOR && entity != null) {
			entity.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		}
		return entity;
	}

	private static int reset(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		TestingOverrides.clear(context.getSource().getPlayerOrException().getUUID());
		return status(context);
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
