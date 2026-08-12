package com.powers.gametest;

import com.powers.magic.InteractionOutcome;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import com.powers.magic.MagicOrigin;
import com.powers.PowersEntities;
import com.powers.PowersWeapons;
import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.ShadowCompanionRules;
import com.powers.companion.ShadowCompanionStore;
import com.powers.companion.ShadowChatContext;
import com.powers.companion.ShadowChatRuntime;
import com.powers.companion.ShadowMagicState;
import com.powers.companion.ShadowStance;
import com.powers.companion.combat.ShadowPowerAction;
import com.powers.companion.combat.ShadowPowerCatalogue;
import com.powers.companion.combat.ShadowPowerRuntime;
import com.powers.entity.DarknessFireballProjectile;
import com.powers.entity.DarknessCreature;
import com.powers.entity.PowerTestActor;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.MagicPresence;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PhysicalCollisionFamily;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.power.AmethystDampening;
import com.powers.spell.SpellFieldKind;
import com.powers.spell.SpellFieldManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** High-volume live-server interaction proof using distinct embedded connections. */
public final class MultiplayerInteractionGameTests {
	private static final int ADAPTER_CASES_PER_TICK = 2;
	private static final int PHYSICAL_CASES_PER_TICK = 1;
	private static final Map<InteractionOutcome, Integer> REVIEWED_OUTCOME_TOTALS = Map.of(
			InteractionOutcome.COEXIST, 884,
			InteractionOutcome.RESONATE, 533,
			InteractionOutcome.DAMPEN, 261,
			InteractionOutcome.CONTEST, 144,
			InteractionOutcome.CANCEL, 139,
			InteractionOutcome.CONSUME, 94,
			InteractionOutcome.DESTABILIZE, 16,
			InteractionOutcome.AMPLIFY, 6,
			InteractionOutcome.TRANSFORM, 3);

	@GameTest(maxTicks = 1_500, padding = 128)
	@SuppressWarnings("removal")
	public void everyMagicPairTraversesTheProductionAdapterBetweenDistinctPlayers(
			GameTestHelper helper) {
		ServerPlayer existingOwner = helper.makeMockServerPlayerInLevel();
		ServerPlayer castingOwner = helper.makeMockServerPlayerInLevel();
		helper.assertFalse(existingOwner.getUUID().equals(castingOwner.getUUID()),
				"Embedded multiplayer fixtures reused one authoritative UUID");
		helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(
				existingOwner.getUUID()) == existingOwner
				&& helper.getLevel().getServer().getPlayerList().getPlayer(
				castingOwner.getUUID()) == castingOwner,
				"Both multiplayer fixtures were not connected to the live player list");

		Vec3 center = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 3, 8)));
		List<MagicActionDefinition> definitions = MagicRuntime.catalogue().definitions().stream()
				.sorted(Comparator.comparing(MagicActionDefinition::id)).toList();
		EnumMap<InteractionOutcome, Integer> outcomes = new EnumMap<>(InteractionOutcome.class);
		int[] firstIndex = {0};
		int[] secondIndex = {0};
		int[] scenarios = {0};
		int[] invocations = {0};
		int[] blocked = {0};
		String dimension = helper.getLevel().dimension().identifier().toString();

		helper.onEachTick(() -> {
			for (int work = 0; work < ADAPTER_CASES_PER_TICK
					&& firstIndex[0] < definitions.size(); work++) {
				MagicActionDefinition existing = definitions.get(firstIndex[0]);
				MagicActionDefinition casting = definitions.get(secondIndex[0]);
				exerciseAdapterPair(helper, existingOwner, castingOwner, center, dimension,
						existing, casting, outcomes, blocked);
				invocations[0]++;
				if (!existing.id().equals(casting.id())) {
					exerciseAdapterPair(helper, castingOwner, existingOwner, center, dimension,
							casting, existing, null, null);
					invocations[0]++;
				}
				scenarios[0]++;
				secondIndex[0]++;
				if (secondIndex[0] >= definitions.size()) {
					firstIndex[0]++;
					secondIndex[0] = firstIndex[0];
				}
			}
			if (firstIndex[0] >= definitions.size()) {
				MagicRuntime.global().clearOwner(existingOwner.getUUID());
				MagicRuntime.global().clearOwner(castingOwner.getUUID());
				assertAdapterTotals(helper, outcomes, scenarios[0], invocations[0], blocked[0]);
				helper.succeed();
			}
		});
	}

	@GameTest(maxTicks = 1_500, padding = 128)
	@SuppressWarnings("removal")
	public void everySupportedPhysicalPairCollidesOnceBetweenDistinctPlayers(GameTestHelper helper) {
		ServerPlayer firstOwner = helper.makeMockServerPlayerInLevel();
		ServerPlayer secondOwner = helper.makeMockServerPlayerInLevel();
		helper.assertFalse(firstOwner.getUUID().equals(secondOwner.getUUID()),
				"Physical multiplayer fixtures reused one authoritative UUID");
		List<MagicActionDefinition> definitions = MagicRuntime.catalogue().definitions().stream()
				.sorted(Comparator.comparing(MagicActionDefinition::id)).toList();
		Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 4, 8)));
		int[] firstIndex = {0};
		int[] secondIndex = {0};
		int[] scenarios = {0};

		helper.onEachTick(() -> {
			int work = 0;
			while (work < PHYSICAL_CASES_PER_TICK && firstIndex[0] < definitions.size()) {
				MagicActionDefinition first = definitions.get(firstIndex[0]);
				MagicActionDefinition second = definitions.get(secondIndex[0]);
				MagicPresenceHandle.Kind firstKind = kind(first);
				MagicPresenceHandle.Kind secondKind = kind(second);
				if (PhysicalCollisionFamily.of(firstKind, secondKind)
						!= PhysicalCollisionFamily.UNSUPPORTED) {
					exercisePhysicalPair(helper, firstOwner, secondOwner, center,
							first, second, firstKind, secondKind);
					scenarios[0]++;
					work++;
				}
				secondIndex[0]++;
				if (secondIndex[0] >= definitions.size()) {
					firstIndex[0]++;
					secondIndex[0] = firstIndex[0];
				}
			}
			if (firstIndex[0] >= definitions.size()) {
				helper.assertTrue(scenarios[0] == 1_069,
						"Physical live matrix covered " + scenarios[0] + " of 1,069 supported pairs");
				MagicRuntime.global().clearOwner(firstOwner.getUUID());
				MagicRuntime.global().clearOwner(secondOwner.getUUID());
				helper.succeed();
			}
		});
	}

	@GameTest(maxTicks = 160, padding = 48)
	@SuppressWarnings("removal")
	public void shadowChatExecutesAndStopsItsCompleteTwentySixPowerArsenal(
			GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 3, 12)));
		owner.snapTo(origin, 0.0F, 0.0F);
		owner.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		try {
			helper.assertTrue(PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself"),
					"Shadow reveal chat was not consumed");
			PrivateCompanionManager.tickPlayer(owner, 0);
			ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
			helper.assertTrue(shadow.ownerId().equals(owner.getUUID()) && shadow.revealed(),
					"Manifested Shadow was not bound and visible to its owner");
			List<ShadowPowerAction> actions = ShadowPowerCatalogue.actions().stream()
					.sorted(Comparator.comparing(ShadowPowerAction::id)).toList();
			helper.assertTrue(actions.size() == 26,
					"Live Shadow arsenal contained " + actions.size() + " rather than 26 powers");
			for (ShadowPowerAction action : actions) {
				prepareShadowArena(helper.getLevel(), shadow, origin);
				PowerTestActor target = PowersEntities.POWER_TEST_ACTOR.create(
						helper.getLevel(), EntitySpawnReason.COMMAND);
				helper.assertTrue(target != null, "Could not create target for " + action.id());
				target.setNoAi(true);
				target.setPos(origin.add(4.0, 0.0, 0.0));
				target.setHealth(target.getMaxHealth());
				helper.getLevel().addFreshEntity(target);
				shadow.setTarget(target);
				shadow.setEnergy(ShadowCompanionRules.MAX_ENERGY);
				long before = ShadowPowerRuntime.diagnostics().casts();
				helper.assertTrue(PrivateCompanionManager.handleChat(owner,
						"shadow, use " + action.id().replace('_', ' ')),
						"Shadow power chat leaked for " + action.id());
				helper.assertTrue(ShadowPowerRuntime.diagnostics().casts() == before + 1,
						"Shadow chat did not execute " + action.id());
				if (action.toggle()) {
					helper.assertTrue(PrivateCompanionManager.handleChat(owner,
							"shadow, stop " + action.id().replace('_', ' ')),
							"Shadow stop chat leaked for " + action.id());
					helper.assertFalse(ShadowPowerRuntime.active(owner.getUUID(), action.id()),
							"Shadow toggle remained active after stop: " + action.id());
				}
				if (action.id().equals("double_health")) {
					helper.assertFalse(shadow.hasEffect(MobEffects.ABSORPTION)
							|| shadow.hasEffect(MobEffects.RESISTANCE),
							"Stopping Double Health left defensive effects on Shadow");
				}
				discardTransientEntities(helper.getLevel(), shadow, target, origin);
			}
			helper.assertFalse(helper.getLevel().getServer().tickRateManager().isFrozen(),
					"Shadow Time Freeze left the live server clock frozen");
			helper.succeed();
		} finally {
			PrivateCompanionManager.forget(owner);
		}
	}

	@GameTest(maxTicks = 160, padding = 96)
	@SuppressWarnings("removal")
	public void threeShadowOwnersRemainIsolatedAcrossChatCombatCounterplayAndDeath(
			GameTestHelper helper) {
		ServerPlayer first = eligibleShadowOwner(helper, new BlockPos(12, 3, 12));
		ServerPlayer second = eligibleShadowOwner(helper, new BlockPos(44, 3, 12));
		ServerPlayer third = eligibleShadowOwner(helper, new BlockPos(76, 3, 12));
		try {
			for (ServerPlayer owner : List.of(first, second, third)) {
				helper.assertTrue(PrivateCompanionManager.handleChat(owner,
						"shadow, reveal yourself"), "Shadow reveal was not consumed");
				PrivateCompanionManager.tickPlayer(owner, 0);
			}
			ShadowCompanionEntity firstShadow = PrivateCompanionManager.body(
					first.getUUID()).orElseThrow();
			ShadowCompanionEntity secondShadow = PrivateCompanionManager.body(
					second.getUUID()).orElseThrow();
			ShadowCompanionEntity thirdShadow = PrivateCompanionManager.body(
					third.getUUID()).orElseThrow();
			helper.assertTrue(PrivateCompanionManager.activeSessionCount() >= 3
					&& PrivateCompanionManager.activeRevealedBodyCount() >= 3,
					"Three owners did not receive three independent revealed sessions");
			helper.assertTrue(java.util.Set.of(firstShadow.getUUID(), secondShadow.getUUID(),
					thirdShadow.getUUID()).size() == 3,
					"Different owners shared one Shadow body identity");
			helper.assertTrue(firstShadow.ownerId().equals(first.getUUID())
					&& secondShadow.ownerId().equals(second.getUUID())
					&& thirdShadow.ownerId().equals(third.getUUID()),
					"A Shadow body crossed its authoritative owner boundary");

			PrivateCompanionManager.handleChat(first, "shadow, stay here");
			PrivateCompanionManager.handleChat(second, "shadow, fight at far range");
			PrivateCompanionManager.handleChat(third, "shadow, follow me");
			helper.assertTrue(ShadowCompanionStore.get(first).stance() == ShadowStance.STAY
					&& ShadowCompanionStore.get(third).stance() == ShadowStance.FOLLOW,
					"Owner-local conversation changed another Shadow's stance");
			helper.assertTrue(ShadowCompanionStore.get(second).preferredCombatRange().name()
					.equals("FAR"), "Spoken combat-range preference was not retained");
			PrivateCompanionManager.handleChat(first,
					"shadow, how does amethyst interfere with magic?");
			PrivateCompanionManager.handleChat(second,
					"shadow, explain the darkness to me");
			helper.assertTrue(ShadowCompanionStore.get(first).memory().turns().stream()
					.anyMatch(turn -> turn.owner().contains("amethyst"))
					&& ShadowCompanionStore.get(second).memory().turns().stream()
					.anyMatch(turn -> turn.owner().contains("darkness")),
					"Independent conversations were not retained in owner-local memory");

			PowerTestActor firstTarget = target(helper, firstShadow.position().add(4.0, 0.0, 0.0));
			PowerTestActor secondTarget = target(helper, secondShadow.position().add(4.0, 0.0, 0.0));
			firstShadow.setTarget(firstTarget);
			secondShadow.setTarget(secondTarget);
			long beforeCombat = ShadowPowerRuntime.diagnostics().casts();
			PrivateCompanionManager.handleChat(first, "shadow, use lightning strike");
			PrivateCompanionManager.handleChat(second, "shadow, use energy beam");
			helper.assertTrue(ShadowPowerRuntime.diagnostics().casts() == beforeCombat + 2,
					"Two owners could not issue independent live combat orders");
			helper.assertTrue(!firstTarget.isAlive() || firstTarget.getHealth() < firstTarget.getMaxHealth(),
					"First owner's Shadow did not damage its assigned target");
			helper.assertTrue(!secondTarget.isAlive() || secondTarget.getHealth() < secondTarget.getMaxHealth(),
					"Second owner's Shadow did not damage its assigned target");

			BlockPos amethyst = secondShadow.blockPosition().offset(1, -1, 0);
			helper.getLevel().setBlock(amethyst, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
			AmethystDampening.blockChanged(helper.getLevel(), amethyst,
					Blocks.AMETHYST_BLOCK.defaultBlockState());
			ShadowMagicState.tick(second, secondShadow);
			PowerTestActor dampenedTarget = target(helper,
					secondShadow.position().add(4.0, 0.0, 0.0));
			secondShadow.setTarget(dampenedTarget);
			long beforeDampened = ShadowPowerRuntime.diagnostics().casts();
			PrivateCompanionManager.handleChat(second, "shadow, use lightning strike");
			helper.assertTrue(ShadowPowerRuntime.diagnostics().casts() == beforeDampened,
					"Amethyst did not suppress Shadow like another player-like caster");
			helper.getLevel().setBlock(amethyst, Blocks.AIR.defaultBlockState(), 3);
			AmethystDampening.blockChanged(helper.getLevel(), amethyst, Blocks.AIR.defaultBlockState());
			AmethystDampening.update(secondShadow);

			PowerTestActor sanctuaryTarget = target(helper,
					third.position().add(2.0, 0.0, 0.0));
			thirdShadow.snapTo(third.position().add(-4.0, 0.0, 0.0), 0.0F, 0.0F);
			thirdShadow.setTarget(sanctuaryTarget);
			SpellFieldManager.add(SpellFieldKind.SANCTUARY, third, 200, 6.0, 2);
			long beforeSanctuary = ShadowPowerRuntime.diagnostics().casts();
			PrivateCompanionManager.handleChat(third, "shadow, use void beam");
			helper.assertTrue(ShadowPowerRuntime.diagnostics().casts() == beforeSanctuary
					&& sanctuaryTarget.getHealth() == sanctuaryTarget.getMaxHealth(),
					"Sanctuary failed to counter a different caster's Shadow attack");

			PrivateCompanionManager.handleChat(second, "shadow, hide yourself");
			helper.assertTrue(secondShadow.isInvisible() && !secondShadow.revealed()
					&& firstShadow.revealed() && thirdShadow.revealed(),
					"One owner's hide request changed another owner's presentation");
			helper.assertTrue(firstShadow.hurtServer(helper.getLevel(),
					firstShadow.damageSources().generic(), 10_000.0F),
					"Revealed first Shadow could not be killed");
			helper.assertTrue(PrivateCompanionManager.body(first.getUUID()).isEmpty()
					&& PrivateCompanionManager.body(second.getUUID()).orElseThrow().isAlive()
					&& PrivateCompanionManager.body(third.getUUID()).orElseThrow().isAlive(),
					"One Shadow death corrupted another owner's session");
			helper.succeed();
		} finally {
			PrivateCompanionManager.forget(first);
			PrivateCompanionManager.forget(second);
			PrivateCompanionManager.forget(third);
		}
	}

	@GameTest(maxTicks = 100, padding = 96)
	@SuppressWarnings("removal")
	public void shadowsFollowIndependentHumanLikeConversationsInsideBusyGlobalChat(
			GameTestHelper helper) {
		ServerPlayer first = eligibleShadowOwner(helper, new BlockPos(12, 3, 12));
		ServerPlayer second = eligibleShadowOwner(helper, new BlockPos(44, 3, 12));
		ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
		bystander.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(76, 3, 12))),
				0.0F, 0.0F);
		PrivateCompanionManager.handleChat(first, "shadow, explain darkness");
		PrivateCompanionManager.handleChat(second, "shadow, explain amethyst");
		PrivateCompanionManager.tickPlayer(first, 0);
		PrivateCompanionManager.tickPlayer(second, 0);
		helper.assertFalse(PrivateCompanionManager.handleChat(first,
					"I wonder whether it spreads through ordinary stone"),
					"Ordinary conversation was incorrectly consumed as a command");
		helper.assertTrue(ShadowChatRuntime.observe(first,
					"I wonder whether it spreads through ordinary stone"),
					"First owner's active dialogue did not accept an unprefixed follow-up");
		helper.assertFalse(ShadowChatRuntime.observe(bystander,
					"I am discussing diamonds, not amethyst"),
					"A bystander without Shadow dialogue received an unsolicited reply");
		helper.assertTrue(ShadowChatRuntime.observe(second,
					"Would it suppress lightning as well?"),
					"Second owner's active dialogue did not accept its own follow-up");
		helper.assertFalse(ShadowChatRuntime.observe(bystander,
					"The first player means darkness and the second means amethyst"),
					"Busy public chat opened an unrelated dialogue focus");
		helper.runAfterDelay(4, () -> {
				var visibleContext = ShadowChatContext.snapshot(first,
						first.level().getServer().getTickCount());
				helper.assertTrue(visibleContext.stream().map(ShadowChatContext.Entry::speaker)
						.collect(java.util.stream.Collectors.toSet()).containsAll(
								java.util.Set.of(first.getUUID(), second.getUUID(), bystander.getUUID())),
						"Shadow did not read the speaker-labelled global conversation");
				var firstTurns = ShadowCompanionStore.get(first).memory().turns();
				var secondTurns = ShadowCompanionStore.get(second).memory().turns();
				helper.assertTrue(firstTurns.stream().anyMatch(turn ->
						turn.owner().contains("ordinary stone")),
						"First Shadow forgot its owner's natural follow-up");
				helper.assertFalse(firstTurns.stream().anyMatch(turn ->
						turn.owner().contains("suppress lightning")),
						"Second conversation leaked into first owner's persistent dialogue");
				helper.assertTrue(secondTurns.stream().anyMatch(turn ->
						turn.owner().contains("suppress lightning")),
						"Second Shadow forgot its owner's natural follow-up");
				helper.assertFalse(secondTurns.stream().anyMatch(turn ->
						turn.owner().contains("ordinary stone")),
						"First conversation leaked into second owner's persistent dialogue");
				PrivateCompanionManager.forget(first);
				PrivateCompanionManager.forget(second);
				helper.succeed();
			});
	}

	private static void exerciseAdapterPair(GameTestHelper helper, ServerPlayer existingOwner,
			ServerPlayer castingOwner, Vec3 center, String dimension,
			MagicActionDefinition existing, MagicActionDefinition casting,
			EnumMap<InteractionOutcome, Integer> outcomes, int[] blocked) {
		existingOwner.snapTo(center.add(-2.0, 0.0, 0.0), 0.0F, 0.0F);
		castingOwner.snapTo(center, 0.0F, 0.0F);
		existingOwner.setDeltaMovement(Vec3.ZERO);
		castingOwner.setDeltaMovement(Vec3.ZERO);
		existingOwner.removeAllEffects();
		castingOwner.removeAllEffects();
		long tick = helper.getLevel().getServer().getTickCount();
		MagicPresenceId existingId = MagicPresenceId.random();
		MagicRuntime.global().registerPresence(new MagicPresence(existingId,
				existing.id(), existingOwner.getUUID(), dimension,
				PresenceAnchor.fixed(center.x, center.y + 1.0, center.z),
				1.0, tick + 10_000L));
		MagicPresenceId committedId = null;
		try {
			PreparedMagicCast prepared = ServerMagicCasts.prepare(castingOwner,
					casting.id().value(), source(casting.origin()));
			helper.assertTrue(prepared.preview().reactions().size() == 1,
					"Live adapter did not resolve exactly one presence for "
							+ existing.id() + " x " + casting.id());
			var reaction = prepared.preview().reactions().getFirst();
			helper.assertTrue(reaction.existing().id().equals(existingId)
					&& reaction.existing().owner().equals(existingOwner.getUUID())
					&& reaction.cast().owner().equals(castingOwner.getUUID()),
					"Live interaction crossed multiplayer ownership for "
							+ existing.id() + " x " + casting.id());
			helper.assertTrue(prepared.allowed() == !reaction.resolution().blocksFirst(),
					"Production preflight disagreed with its collision resolution for "
							+ existing.id() + " x " + casting.id());
			if (outcomes != null) outcomes.merge(reaction.resolution().outcome(), 1, Integer::sum);
			if (prepared.allowed()) committedId = ServerMagicCasts.commit(prepared, castingOwner);
			else if (blocked != null) blocked[0]++;
		} finally {
			if (committedId != null) {
				helper.assertTrue(MagicRuntime.global().removePresence(committedId),
						"Committed multiplayer residue leaked for " + casting.id());
			}
			helper.assertTrue(MagicRuntime.global().removePresence(existingId),
					"Existing multiplayer residue leaked for " + existing.id());
		}
	}

	private static void prepareShadowArena(ServerLevel level, ShadowCompanionEntity shadow,
			Vec3 origin) {
		shadow.snapTo(origin, 0.0F, 0.0F);
		shadow.setDeltaMovement(Vec3.ZERO);
		shadow.removeAllEffects();
		shadow.setNoGravity(false);
		shadow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)
				.setBaseValue(1.0);
		BlockPos floor = BlockPos.containing(origin).below();
		for (int x = -9; x <= 9; x++) {
			for (int z = -9; z <= 9; z++) {
				level.setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}
	}

	@SuppressWarnings("removal")
	private static ServerPlayer eligibleShadowOwner(GameTestHelper helper, BlockPos localPosition) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(localPosition)), 0.0F, 0.0F);
		owner.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		return owner;
	}

	private static PowerTestActor target(GameTestHelper helper, Vec3 position) {
		PowerTestActor target = PowersEntities.POWER_TEST_ACTOR.create(
				helper.getLevel(), EntitySpawnReason.COMMAND);
		if (target == null) throw new IllegalStateException("Could not create live Shadow target");
		target.setNoAi(true);
		target.setPos(position);
		target.setHealth(target.getMaxHealth());
		helper.getLevel().addFreshEntity(target);
		return target;
	}

	private static void discardTransientEntities(ServerLevel level, ShadowCompanionEntity shadow,
			PowerTestActor target, Vec3 origin) {
		if (!target.isRemoved()) target.discard();
		for (Entity entity : level.getEntities(shadow,
				AABB.ofSize(origin, 48.0, 32.0, 48.0), entity ->
						entity instanceof DarknessFireballProjectile
								|| entity instanceof DarknessCreature)) {
			entity.discard();
		}
	}

	private static void assertAdapterTotals(GameTestHelper helper,
			EnumMap<InteractionOutcome, Integer> outcomes, int scenarios, int invocations,
			int blocked) {
		helper.assertTrue(scenarios == 2_080,
				"Production adapter covered " + scenarios + " of 2,080 action pairs");
		helper.assertTrue(invocations == 4_096,
				"Production adapter covered " + invocations + " of 4,096 cast orientations");
		helper.assertTrue(blocked == 179,
				"Caller-order blocking total drifted from the reviewed matrix: " + blocked);
		for (var expected : REVIEWED_OUTCOME_TOTALS.entrySet()) {
			helper.assertTrue(outcomes.getOrDefault(expected.getKey(), 0).equals(expected.getValue()),
					"Live outcome total drifted for " + expected.getKey() + ": "
							+ outcomes.getOrDefault(expected.getKey(), 0));
		}
	}

	private static void exercisePhysicalPair(GameTestHelper helper, ServerPlayer firstOwner,
			ServerPlayer secondOwner, Vec3 center, MagicActionDefinition first,
			MagicActionDefinition second, MagicPresenceHandle.Kind firstKind,
			MagicPresenceHandle.Kind secondKind) {
		ServerLevel level = helper.getLevel();
		long tick = level.getServer().getTickCount();
		PhysicalFixture firstFixture = physical(level, firstOwner, first, firstKind,
				center.add(96.0, 0.0, 0.0), tick);
		PhysicalFixture secondFixture = physical(level, secondOwner, second, secondKind,
				center, tick);
		try {
			helper.assertTrue(MagicRuntime.global().movePresence(firstFixture.handle().presenceId(),
					level.dimension().identifier().toString(),
					PresenceAnchor.fixed(center.x, center.y, center.z)),
					"Could not move physical presence for " + first.id());
			helper.assertTrue(PhysicalMagicPresences.collideNearby(firstFixture.handle(), level,
					center, tick) == 1,
					"Physical pair did not resolve exactly once: " + first.id() + " x " + second.id());
			helper.assertTrue(PhysicalMagicPresences.collideNearby(firstFixture.handle(), level,
					center, tick) == 0,
					"Physical pair repeated inside its collision window: "
							+ first.id() + " x " + second.id());
		} finally {
			remove(firstFixture);
			remove(secondFixture);
		}
	}

	private static PhysicalFixture physical(ServerLevel level, ServerPlayer owner,
			MagicActionDefinition action, MagicPresenceHandle.Kind kind, Vec3 position, long tick) {
		if (kind != MagicPresenceHandle.Kind.ENTITY
				&& kind != MagicPresenceHandle.Kind.PROJECTILE) {
			return new PhysicalFixture(PhysicalMagicPresences.registerFixed(action.id(), owner.getUUID(),
					level, position, 1.0, tick + 200L, kind), null);
		}
		Entity entity;
		if (kind == MagicPresenceHandle.Kind.PROJECTILE) {
			entity = new DarknessFireballProjectile(level, owner, Vec3.ZERO);
		} else {
			entity = PowersEntities.POWER_TEST_ACTOR.create(level, EntitySpawnReason.COMMAND);
			if (entity == null) throw new IllegalStateException("Could not create physical test body");
		}
		entity.setPos(position);
		level.addFreshEntity(entity);
		MagicPresenceId id = MagicPresenceId.random();
		MagicRuntime.global().registerPresence(new MagicPresence(id, action.id(), owner.getUUID(),
				level.dimension().identifier().toString(),
				PresenceAnchor.fixed(position.x, position.y, position.z), 1.0, tick + 200L));
		MagicPresenceHandle handle = PhysicalMagicPresences.bindExistingEntity(
				id, entity, kind, tick + 200L);
		if (handle == null) throw new IllegalStateException("Could not bind physical test presence");
		return new PhysicalFixture(handle, entity);
	}

	private static void remove(PhysicalFixture fixture) {
		PhysicalMagicPresences.remove(fixture.handle());
		if (fixture.entity() != null && !fixture.entity().isRemoved()) fixture.entity().discard();
	}

	private static MagicPresenceHandle.Kind kind(MagicActionDefinition action) {
		if (action.id().equals(new MagicActionId("darkness_block"))
				|| action.id().equals(new MagicActionId("pure_light_block"))) {
			return MagicPresenceHandle.Kind.FORCE_BLOCK;
		}
		return switch (action.delivery()) {
			case PROJECTILE -> MagicPresenceHandle.Kind.PROJECTILE;
			case BEAM -> MagicPresenceHandle.Kind.BEAM;
			case FIELD, AURA, CHANNEL -> MagicPresenceHandle.Kind.FIELD;
			case INSTANT, TOGGLE, TRAVEL, PROJECTION -> MagicPresenceHandle.Kind.ENTITY;
		};
	}

	private record PhysicalFixture(MagicPresenceHandle handle, Entity entity) { }

	private static CastSource source(MagicOrigin origin) {
		return switch (origin) {
			case INNATE -> CastSource.INNATE;
			case CRYSTAL -> CastSource.CRYSTAL;
			case SPELL -> CastSource.SPELL;
			default -> CastSource.ARTIFACT;
		};
	}
}
