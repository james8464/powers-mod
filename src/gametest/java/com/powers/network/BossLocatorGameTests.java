package com.powers.network;

import com.powers.ImportedPackItems;
import com.powers.PowersEntities;
import com.powers.gametest.BossControlResponseGameTests;
import com.powers.player.CartographerJournalStore;
import com.powers.player.PlayerPowers;
import com.powers.power.crystals.DreamwalkingAbility;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;

/** Real opened nonces and registered selection handlers, not payment-only locator substitutes. */
public final class BossLocatorGameTests {
	@GameTest(maxTicks = 100)
	@SuppressWarnings("removal")
	public void soulCompassPreservesAnExistingBossViewingSession(GameTestHelper helper) {
		var owner = helper.makeMockServerPlayerInLevel();
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		owner.setPos(origin.getX(), origin.getY(), origin.getZ());
		caster.setPos(origin.getX() + 2, origin.getY(), origin.getZ());
		owner.setNoGravity(true);
		caster.setNoGravity(true);
		caster.setGameMode(GameType.SURVIVAL);
		var boss = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setCustomName(net.minecraft.network.chat.Component.literal("Occupied Witness"));
		helper.assertTrue(DreamwalkingAbility.beginRemoteView(owner, boss, 1200), "Existing viewing fixture failed");
		var prior = com.powers.mind.MindSessionManager.activeFor(boss.getUUID()).orElseThrow();
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_celestial", 0);
		data.forceRestoreEnergy();
		int energy = data.energy();
		var payloads = PacketFaultGameTests.capture(caster);
		var messages = BossControlResponseGameTests.capture(caster);
		SpellCastingManager.use(caster, "book_grimoire_celestial");
		var opened = payloads.stream().filter(PowersPackets.OpenLocatorScreenPayload.class::isInstance)
				.map(PowersPackets.OpenLocatorScreenPayload.class::cast).findFirst().orElseThrow();
		caster.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
				new PowersPackets.LocateTargetPayload("Occupied Witness", opened.nonce())));
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(data.energy() == energy && data.cooldownReadyAt("spell:soul_compass") == 0
					&& !LocatorSpellPackets.hasPendingNonce(caster.getUUID()) && caster.getCamera() == caster,
					"Conflicting locator charged, retained nonce, or attached a camera");
			helper.assertTrue(com.powers.mind.MindSessionManager.activeFor(caster.getUUID()).isEmpty()
					&& com.powers.mind.MindSessionManager.activeFor(boss.getUUID()).orElseThrow().equals(prior)
					&& owner.getCamera() == boss, "Refusal disturbed original session ownership/view");
			helper.assertTrue(messages.stream().anyMatch(packet -> packet.content().getContents()
					instanceof net.minecraft.network.chat.contents.TranslatableContents text
					&& text.getKey().equals("ability.powers.mind_session_nested")), "Missing conflict feedback");
			DreamwalkingAbility.clear(owner);
			helper.assertTrue(owner.getCamera() == owner
					&& com.powers.mind.MindSessionManager.activeFor(boss.getUUID()).isEmpty(), "Original view did not clean up");
			helper.succeed();
		});
	}

	@GameTest
	@SuppressWarnings("removal")
	public void soulCompassAdmissionFailureRefundsPayment(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_celestial", 0);
		data.forceRestoreEnergy();
		int energy = data.energy();
		var attempted = new java.util.concurrent.atomic.AtomicBoolean();
		helper.assertFalse(SpellCastingManager.commitSoulCompass(caster, () -> {
			attempted.set(true);
			return false;
		}), "Rejected session admission committed the spell");
		helper.assertTrue(attempted.get() && data.energy() == energy
				&& data.cooldownReadyAt("spell:soul_compass") == 0,
				"Session refusal did not restore reserved energy and cooldown");
		data.consumeEnergy(energy);
		attempted.set(false);
		helper.assertFalse(SpellCastingManager.commitSoulCompass(caster, () -> {
			attempted.set(true);
			return true;
		}), "Insufficient energy committed the spell");
		helper.assertTrue(!attempted.get() && data.energy() == 0
				&& data.cooldownReadyAt("spell:soul_compass") == 0,
				"Unpaid cast reached session acquisition or changed cooldown");
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void soulCompassObservesLightHerald(GameTestHelper helper) { locator(helper, PowersEntities.LIGHT_HERALD, false); }
	@GameTest(maxTicks = 100)
	public void soulCompassObservesDarkHerald(GameTestHelper helper) { locator(helper, PowersEntities.DARK_HERALD, false); }
	@GameTest(maxTicks = 100)
	public void soulCompassObservesFirstVessel(GameTestHelper helper) { locator(helper, PowersEntities.FIRST_VESSEL, false); }
	@GameTest(maxTicks = 100)
	public void cartographerSearchesNearLightHerald(GameTestHelper helper) { locator(helper, PowersEntities.LIGHT_HERALD, true); }
	@GameTest(maxTicks = 100)
	public void cartographerSearchesNearDarkHerald(GameTestHelper helper) { locator(helper, PowersEntities.DARK_HERALD, true); }
	@GameTest(maxTicks = 100)
	public void cartographerSearchesNearFirstVessel(GameTestHelper helper) { locator(helper, PowersEntities.FIRST_VESSEL, true); }

	@SuppressWarnings("removal")
	private static void locator(GameTestHelper helper, EntityType<? extends Mob> type, boolean world) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_celestial", world ? 2 : 0);
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 2, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		// The documented locator contract requires a custom name, not the entity-type title.
		if (!world) boss.setCustomName(net.minecraft.network.chat.Component.literal("Locator Witness"));
		var payloads = PacketFaultGameTests.capture(caster);
		var messages = BossControlResponseGameTests.capture(caster);
		String biome = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME)
				.getKey(helper.getLevel().getBiome(origin).value()).toString();
		String query = world ? "biome " + biome : boss.getName().getString();
		String spell = world ? "cartographers_star" : "soul_compass";
		int energy = data.energy();
		SpellCastingManager.use(caster, "book_grimoire_celestial");
		var opened = payloads.stream().filter(PowersPackets.OpenLocatorScreenPayload.class::isInstance)
				.map(PowersPackets.OpenLocatorScreenPayload.class::cast).findFirst().orElseThrow();
		helper.assertTrue(opened.mode() == (world ? CelestialSearchMode.WORLD : CelestialSearchMode.ENTITY)
				&& data.energy() == energy && data.cooldownReadyAt("spell:" + spell) == 0,
				"Opening the locator charged before selection or opened the wrong mode");
		helper.runAfterDelay(5, () -> {
			long cooldown = caster.level().getGameTime() + (world ? 1200 : 200);
			caster.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
					new PowersPackets.LocateTargetPayload(query, opened.nonce())));
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(data.energy() == energy - (world ? 24 : 14)
						&& data.cooldownReadyAt("spell:" + spell) >= cooldown
						&& data.cooldownReadyAt("spell:" + spell) <= cooldown + 2
						&& !LocatorSpellPackets.hasPendingNonce(caster.getUUID()),
						"Registered locator selection failed: energy=" + data.energy() + "/" + energy
								+ ", cooldown=" + data.cooldownReadyAt("spell:" + spell) + "/" + cooldown
								+ ", pending=" + LocatorSpellPackets.hasPendingNonce(caster.getUUID())
								+ ", query=" + query + ", customName=" + boss.hasCustomName()
								+ ", resolution=" + NamedLivingTargetIndex.resolve(helper.getLevel().getServer(), query).status()
								+ ", messages=" + messages.stream().map(packet -> packet.content().getString()).toList());
			});
			helper.runAfterDelay(55, () -> {
				if (world) {
					var site = CartographerJournalStore.find(caster, helper.getLevel().dimension().identifier().toString(),
							"biome", biome).orElseThrow();
					helper.assertTrue(site.registryId().equals(biome)
							&& messages.stream().anyMatch(packet -> packet.content().getContents()
							instanceof net.minecraft.network.chat.contents.TranslatableContents text
							&& text.getKey().equals("grimoire.celestial.world.result")
							&& text.getArgs().length == 3 && text.getArgs()[0].equals(biome)),
							"Cartographer selection did not discover and report the requested biome");
				} else {
					helper.assertTrue(caster.getCamera() == boss,
							"Soul Compass selection did not reveal the named boss through the viewing session");
					String coordinates = "Coordinates: " + boss.blockPosition().getX() + " "
							+ boss.blockPosition().getY() + " " + boss.blockPosition().getZ();
					helper.assertTrue(messages.stream().anyMatch(packet -> packet.content().getString().equals(coordinates)),
							"Soul Compass did not report the boss's actual coordinates");
					DreamwalkingAbility.clear(caster);
					helper.assertTrue(caster.getCamera() == caster, "Locator view cleanup retained the boss camera");
				}
				helper.assertTrue(boss.getHealth() == 37F, "Locator changed boss health");
				helper.succeed();
			});
		});
	}
}
