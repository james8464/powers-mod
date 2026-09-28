package com.powers.command;

import com.mojang.brigadier.context.CommandContext;
import com.powers.PowersEntities;
import com.powers.entity.PowerTestActor;
import com.powers.entity.TestActorProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;

/** Bounded command decoding and spawning for configurable player-like test actors. */
final class TestActorCommandSupport {
	private TestActorCommandSupport() { }

	static TestActorProfile parse(String encoded) {
		String[] values = encoded.split(",", -1);
		if (values.length != 6) throw new IllegalArgumentException(
				"actor profile requires rank,alignment,consent,armored,mobile,action");
		return TestActorProfile.of(Integer.parseInt(values[0]),
				com.powers.item.artifact.ArtifactAlignment.fromSerialized(values[1]),
				Boolean.parseBoolean(values[2]), Boolean.parseBoolean(values[3]),
				Boolean.parseBoolean(values[4]), values[5]);
	}

	static int spawn(CommandContext<CommandSourceStack> context, String username,
			TestActorProfile profile) {
		CommandSourceStack source = context.getSource();
		PowerTestActor actor = PowersEntities.POWER_TEST_ACTOR.create(source.getLevel(),
				EntitySpawnReason.COMMAND);
		if (actor == null) {
			source.sendFailure(Component.literal("A Power Test Actor could not be created."));
			return 0;
		}
		if (username != null) actor.setTestingUsername(username);
		actor.configure(profile);
		var position = source.getPosition();
		actor.setPos(position.x, position.y, position.z);
		source.getLevel().addFreshEntity(actor);
		source.sendSuccess(() -> Component.literal("Spawned test actor: ")
				.append(Component.literal(actor.testingUsername()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(" rank=" + profile.rank() + " alignment="
						+ profile.alignment().serializedName() + " consent=" + profile.consents())), true);
		return 1;
	}
}
