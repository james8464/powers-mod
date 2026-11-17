package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.PowerRegistry;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Actual activation, boss impulse, and clientbound resistance feedback for each boss. */
public final class BossControlResponseGameTests {
	@GameTest(maxTicks = 240)
	@SuppressWarnings("removal")
	public void immuneBossViewOnlyCameraKeepsFullLeaseWithoutHostControl(GameTestHelper helper) {
		for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) {
			helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
		}
		var owners = new java.util.ArrayList<ServerPlayer>();
		var hosts = new java.util.ArrayList<Mob>();
		for (var type : List.of(PowersEntities.LIGHT_HERALD, PowersEntities.DARK_HERALD,
				PowersEntities.FIRST_VESSEL)) {
			var owner = helper.makeMockServerPlayerInLevel();
			owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			var origin = helper.absolutePos(new BlockPos(4, 1, 2));
			owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
			var host = helper.spawn(type, new BlockPos(4, 1, 6));
			host.setNoAi(true);
			host.setNoGravity(true);
			host.addTag(com.powers.power.abilities.ControlResistance.IMMUNE_TAG);
			owners.add(owner);
			hosts.add(host);
		}
		helper.runAfterDelay(2, () -> {
			for (int i = 0; i < owners.size(); i++) {
				var owner = owners.get(i);
				var host = hosts.get(i);
				helper.assertTrue(com.powers.power.crystals.DreamwalkingAbility.beginRemoteView(owner, host, 180),
						"Control immunity incorrectly blocked permitted observation");
				var before = host.position();
				com.powers.power.abilities.VesselPossessionAbility.applyControl(owner,
						new com.powers.network.VesselControlPackets.InputPayload(0L, 1F, 0F, false, false, 0F, 0F, 0, -1));
				helper.assertTrue(host.position().equals(before)
						&& !com.powers.power.abilities.VesselPossessionAbility.isDreamwalking(owner.getUUID()),
						"View-only camera admitted host movement");
			}
			helper.runAfterDelay(170, () -> {
				for (int i = 0; i < owners.size(); i++) helper.assertTrue(owners.get(i).getCamera() == hosts.get(i),
						"View-only camera lost its unmodified observation lease");
			});
			helper.runAfterDelay(185, () -> {
				for (var owner : owners) helper.assertTrue(owner.getCamera() == owner
						&& !com.powers.mind.BodyProxyManager.hasSession(owner, com.powers.mind.BodyProxyKind.DREAMWALK),
						"Expired observation retained camera or body ownership");
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 220)
	public void blueDreamwalkResistsLightHerald(GameTestHelper helper) {
		possession(helper, PowersEntities.LIGHT_HERALD, 180, true, "blue");
	}
	@GameTest(maxTicks = 220)
	public void blueDreamwalkResistsDarkHerald(GameTestHelper helper) {
		possession(helper, PowersEntities.DARK_HERALD, 180, true, "blue");
	}
	@GameTest(maxTicks = 220)
	public void blueDreamwalkResistsFirstVessel(GameTestHelper helper) {
		possession(helper, PowersEntities.FIRST_VESSEL, 180, true, "blue");
	}
	// The shared thirty-second control ceiling applies before 30% boss resistance.
	@GameTest(maxTicks = 220)
	public void artifactDreamwalkResistsLightHerald(GameTestHelper helper) {
		possession(helper, PowersEntities.LIGHT_HERALD, 180, true, "artifact_dreamwalk");
	}
	@GameTest(maxTicks = 220)
	public void artifactDreamwalkResistsDarkHerald(GameTestHelper helper) {
		possession(helper, PowersEntities.DARK_HERALD, 180, true, "artifact_dreamwalk");
	}
	@GameTest(maxTicks = 220)
	public void artifactDreamwalkResistsFirstVessel(GameTestHelper helper) {
		possession(helper, PowersEntities.FIRST_VESSEL, 180, true, "artifact_dreamwalk");
	}
	@GameTest(maxTicks = 100)
	public void artifactPossessionResistsLightHerald(GameTestHelper helper) {
		possession(helper, PowersEntities.LIGHT_HERALD, 84, true, "artifact_possession");
	}
	@GameTest(maxTicks = 100)
	public void artifactPossessionResistsDarkHerald(GameTestHelper helper) {
		possession(helper, PowersEntities.DARK_HERALD, 84, true, "artifact_possession");
	}
	@GameTest(maxTicks = 100)
	public void artifactPossessionResistsFirstVessel(GameTestHelper helper) {
		possession(helper, PowersEntities.FIRST_VESSEL, 84, true, "artifact_possession");
	}

	@GameTest
	@SuppressWarnings("removal")
	public void dreamwalkingRejectsImmuneAndReflectedBossControl(GameTestHelper helper) {
		for (var type : List.of(PowersEntities.LIGHT_HERALD, PowersEntities.DARK_HERALD,
				PowersEntities.FIRST_VESSEL)) {
			for (String tag : List.of(com.powers.power.abilities.ControlResistance.IMMUNE_TAG,
					com.powers.power.abilities.ControlResistance.REFLECTED_TAG)) {
				ServerPlayer caster = helper.makeMockServerPlayerInLevel();
				caster.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				var boss = helper.spawn(type, new BlockPos(4, 2, 6));
				boss.setNoAi(true);
				boss.addTag(tag);
				helper.assertFalse(com.powers.power.abilities.VesselPossessionAbility.beginDreamwalk(
						caster, boss, 600, com.powers.magic.runtime.CastSource.CRYSTAL),
						"Dreamwalking admitted forbidden boss control: " + tag);
				helper.assertFalse(com.powers.mind.BodyProxyManager.hasSession(caster,
						com.powers.mind.BodyProxyKind.DREAMWALK), "Rejected control left a body session");
				helper.assertTrue(caster.getCamera() == caster && boss.isNoAi(),
						"Rejected control changed camera or host AI");
				boss.discard();
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 240)
	public void ordinaryHostRetainsFullPossessionDuration(GameTestHelper helper) {
		possession(helper, PowersEntities.POWER_TEST_ACTOR, 200, false);
	}

	@GameTest(maxTicks = 100)
	public void lightHeraldResistsPossessionDuration(GameTestHelper helper) {
		possession(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 100)
	public void darkHeraldResistsPossessionDuration(GameTestHelper helper) {
		possession(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 100)
	public void firstVesselResistsPossessionDuration(GameTestHelper helper) {
		possession(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void possession(GameTestHelper helper, EntityType<? extends Mob> type) {
		possession(helper, type, 60, true);
	}

	/** Neutral-duration oracle: use the standard fresh-process runner, avoiding nearby mind resonance. */
	@SuppressWarnings("removal")
	private static void possession(GameTestHelper helper, EntityType<? extends Mob> type,
			int expectedTicks, boolean resisted) {
		possession(helper, type, expectedTicks, resisted, "innate");
	}

	@SuppressWarnings("removal")
	private static void possession(GameTestHelper helper, EntityType<? extends Mob> type,
			int expectedTicks, boolean resisted, String route) {
		for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) {
			helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
		}
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		var origin = helper.absolutePos(new BlockPos(4, 1, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0);
		caster.setXRot(0);
		var data = PlayerPowers.get(caster);
		data.setSlots(caster, List.of("powers:vessel_possession", "powers:flight", "powers:forcefield"));
		boolean dreamwalk = route.equals("blue") || route.equals("artifact_dreamwalk");
		var bodyKind = dreamwalk ? com.powers.mind.BodyProxyKind.DREAMWALK : com.powers.mind.BodyProxyKind.POSSESSION;
		if (route.startsWith("artifact")) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS,
					dreamwalk ? "crystal/dreamwalking" : "innate/vessel_possession", -1), "Artifact selection failed");
		} else if (route.equals("blue")) {
			caster.setItemInHand(InteractionHand.MAIN_HAND, com.powers.PowersItems.BLUE_CRYSTAL.getDefaultInstance());
			caster.setShiftKeyDown(true);
			caster.setPose(net.minecraft.world.entity.Pose.CROUCHING);
			helper.assertTrue(com.powers.power.crystals.CrystalPowerRegistry.tryActivate(caster,
					com.powers.PowersItems.BLUE_CRYSTAL), "Blue Crystal selection failed");
			caster.setShiftKeyDown(false);
			caster.setPose(net.minecraft.world.entity.Pose.STANDING);
			helper.assertTrue(((com.powers.power.crystals.ModeCrystalAbility)
					com.powers.power.crystals.CrystalPowerRegistry.get(com.powers.PowersItems.BLUE_CRYSTAL))
					.selectedActionId(caster).equals("dreamwalking"), "Wrong Blue Crystal mode");
		}
		data.forceRestoreEnergy();
		int energy = data.energy();
		var boss = helper.spawn(type, new BlockPos(4, 1, 6));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		var messages = capture(caster);
		var ability = dreamwalk ? com.powers.power.crystals.CrystalPowerRegistry.getAbility("dreamwalking")
				: PowerRegistry.get("vessel_possession").ability();
		boolean activated = route.equals("blue")
				? com.powers.power.crystals.CrystalPowerRegistry.tryActivate(caster, com.powers.PowersItems.BLUE_CRYSTAL)
				: (route.startsWith("artifact") ? ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
						: AbilityActivationService.activate(caster, ability, "powers:vessel_possession"))
						== AbilityActivationService.Result.ACTIVATED;
		helper.assertTrue(activated, "Control activation failed: " + route);
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) > 0,
				"Control bypassed payment or cooldown: " + route);
		helper.assertTrue(caster.getCamera() == boss, "Possession did not own the boss camera");
		int started = helper.getLevel().getServer().getTickCount();
		helper.runAfterDelay(expectedTicks - 10, () -> helper.assertTrue(caster.getCamera() == boss,
				"Possession ended before its authored lease"));
		helper.runAfterDelay(expectedTicks + 5, () -> {
			helper.assertTrue(caster.getCamera() == caster, "Possession exceeded expected duration " + expectedTicks + ": elapsed="
					+ (helper.getLevel().getServer().getTickCount() - started) + ", currentOwner="
					+ (helper.getLevel().getServer().getPlayerList().getPlayer(caster.getUUID()) == caster)
					+ ", body=" + com.powers.mind.BodyProxyManager.hasSession(caster,
							bodyKind));
			helper.assertFalse(com.powers.mind.BodyProxyManager.hasSession(caster,
					bodyKind), "Expired control retained its body session");
			helper.assertTrue(boss.isNoAi(), "Cleanup changed the host's original AI state");
			if (resisted) helper.assertTrue(messages.stream().anyMatch(packet -> packet.overlay()
					&& packet.content().getContents() instanceof TranslatableContents text
					&& text.getKey().equals("ability.powers.control.resisted")), "Missing resistance feedback");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 30)
	public void lightHeraldResistsArtifactTelekinesisWithFeedback(GameTestHelper helper) {
		telekinesis(helper, PowersEntities.LIGHT_HERALD, true);
	}

	@GameTest(maxTicks = 30)
	public void darkHeraldResistsArtifactTelekinesisWithFeedback(GameTestHelper helper) {
		telekinesis(helper, PowersEntities.DARK_HERALD, true);
	}

	@GameTest(maxTicks = 30)
	public void firstVesselResistsArtifactTelekinesisWithFeedback(GameTestHelper helper) {
		telekinesis(helper, PowersEntities.FIRST_VESSEL, true);
	}

	@GameTest(maxTicks = 30)
	public void lightHeraldResistsTelekinesisWithFeedback(GameTestHelper helper) {
		telekinesis(helper, PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 30)
	public void darkHeraldResistsTelekinesisWithFeedback(GameTestHelper helper) {
		telekinesis(helper, PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 30)
	public void firstVesselResistsTelekinesisWithFeedback(GameTestHelper helper) {
		telekinesis(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void telekinesis(GameTestHelper helper, EntityType<? extends Mob> type) {
		telekinesis(helper, type, false);
	}

	@SuppressWarnings("removal")
	private static void telekinesis(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		data.setSlots(caster, List.of("powers:telekinesis", "powers:flight", "powers:forcefield"));
		if (artifact) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
			data.setDarknessLevel(caster, 9);
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS, "innate/telekinesis", -1),
					"Held authorized artifact could not select Telekinesis");
		}
		data.forceRestoreEnergy();
		int energy = data.energy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		var ordinary = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 1));
		boss.setNoAi(true);
		ordinary.setNoAi(true);
		boss.setNoGravity(true);
		ordinary.setNoGravity(true);
		boss.setPos(caster.getX(), caster.getY(), caster.getZ() + 3);
		ordinary.setPos(caster.getX(), caster.getY(), caster.getZ() - 3);
		List<ClientboundSystemChatPacket> messages = capture(caster);
		var ability = PowerRegistry.get("telekinesis").ability();
		helper.assertTrue((artifact ? ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
				: AbilityActivationService.activate(caster, ability, "powers:telekinesis"))
				== AbilityActivationService.Result.ACTIVATED, "Actual Telekinesis activation failed");
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) > 0,
				"Telekinesis bypassed energy or cooldown");
		double normal = ordinary.getDeltaMovement().length();
		helper.assertTrue(normal > 0, "Ordinary control target was not moved");
		helper.assertTrue(Math.abs(boss.getDeltaMovement().length() - normal * 0.30) < 1.0e-6,
				"Boss did not receive exactly 30% of the ordinary Telekinesis impulse");
		helper.runAtTickTime(2, () -> {
			helper.assertTrue(messages.stream().anyMatch(packet -> packet.overlay()
					&& packet.content().getContents() instanceof TranslatableContents text
					&& text.getKey().equals("ability.powers.control.resisted")),
					"Resisted Telekinesis emitted no clientbound resistance feedback");
			helper.succeed();
		});
	}

	public static List<ClientboundSystemChatPacket> capture(ServerPlayer player) {
		try {
			Field listenerConnection = player.connection.getClass().getSuperclass().getDeclaredField("connection");
			listenerConnection.setAccessible(true);
			Connection connection = (Connection) listenerConnection.get(player.connection);
			Field channelField = Connection.class.getDeclaredField("channel");
			channelField.setAccessible(true);
			io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(connection);
			List<ClientboundSystemChatPacket> messages = new CopyOnWriteArrayList<>();
			channel.pipeline().addLast("boss_response_capture", new ChannelDuplexHandler() {
				@Override
				public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
					if (message instanceof ClientboundSystemChatPacket packet) messages.add(packet);
					super.write(context, message, promise);
				}
			});
			return messages;
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Cannot observe actual resistance feedback packets", exception);
		}
	}
}
