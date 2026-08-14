package com.powers.spell;

import com.powers.cooldown.CooldownPresentation;
import com.powers.fx.PowerFx;
import com.powers.item.GrimoireItem;
import com.powers.network.PowersPackets;
import com.powers.network.PacketRateLimiter;
import com.powers.network.GrimoirePackets;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.magic.runtime.PreparedMagicCast;
import com.powers.magic.runtime.ServerMagicCasts;
import com.powers.magic.runtime.CastSource;
import com.powers.progression.PowerScalingService;
import com.powers.util.PowerMessages;
import com.powers.testing.TestingOverrides;
import com.powers.item.ArtifactEnergyModifiers;
import com.powers.item.ArtifactEnergyReservoir;
import com.powers.knowledge.MagicAttemptReporter;
import com.powers.knowledge.MagicFailureReason;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Owns ritual sessions, spell cooldowns, payments and server-side held-book validation. */
public final class SpellCastingManager {
	private static final SpellRegistry REGISTRY = SpellRegistry.defaults();
	private static final Map<UUID, Session> CHANNELS = new HashMap<>();

	private record Session(ChannelState state, SpellDefinition spell, String grimoireKey,
			SpellCastTransaction transaction, SpellTarget target, ResourceKey<Level> dimension) {
	}

	private SpellCastingManager() {
	}

	public static SpellRegistry registry() {
		return REGISTRY;
	}

	/** Returns whether the player currently owns a live ritual channel. */
	public static boolean isChanneling(UUID playerId) {
		return CHANNELS.containsKey(playerId);
	}

	public static void use(ServerPlayer player, String texture) {
		if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.RITUAL)) {
			MagicAttemptReporter.failure(player, "spell", MagicFailureReason.SERVER_BUDGET);
			return;
		}
		GrimoireDefinition grimoire = REGISTRY.forTexture(texture);
		if (grimoire == null) {
			if (REGISTRY.isDormantTexture(texture)) {
				PowerMessages.overlay(player, Component.translatable("spell.powers.infernal_dormant"));
			}
			return;
		}
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (player.isShiftKeyDown()) {
			GrimoirePackets.open(player, grimoire);
			return;
		}
		SpellDefinition spell = selectedSpell(player, grimoire);
		if (spell.effect() == SpellEffect.SOUL_COMPASS
				|| spell.effect() == SpellEffect.CARTOGRAPHERS_STAR) {
			if (commonChecks(player, spell, false)) PowersPackets.openLocator(player,
					spell.effect() == SpellEffect.SOUL_COMPASS
							? CelestialSearchMode.ENTITY : CelestialSearchMode.WORLD);
			return;
		}
		if (!commonChecks(player, spell, true)) return;
		SpellTarget target = SpellEffects.acquireTarget(player, spell);
		if (!target.available()) {
			MagicAttemptReporter.failure(player, spell.id(), MagicFailureReason.NO_TARGET);
			failed(player, "spell.powers.no_target");
			return;
		}
		PreparedMagicCast magic = ServerMagicCasts.prepare(player, spell.id(), CastSource.SPELL);
		if (!magic.allowed()) return;
		int energyCost = spellEnergyCost(player, spell);
		SpellCastTransaction transaction = new SpellCastTransaction(player, spell, magic, energyCost);
		if (!begin(player, spell, transaction)) return;
		int channelTicks = SpellCastValues.from(PowerScalingService.unranked(spell.id()))
				.channelTicks(spell.channelTicks());
		if (channelTicks == 0) {
			finish(player, spell, transaction, target);
			return;
		}
		long end = player.level().getGameTime() + channelTicks;
		ChannelState state = new ChannelState(end, player.getX(), player.getY(), player.getZ(), grimoire.key(), false);
		CHANNELS.put(player.getUUID(), new Session(state, spell, grimoire.key(), transaction,
				target, player.level().dimension()));
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.rune(level, player.position().add(0, 0.08, 0), 1.7, 0x7455A8, 20, 0);
		PowerFx.sound(level, player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.9f, 0.7f);
		PowerMessages.overlay(player, Component.translatable("spell.powers.channeling", spellName(spell)));
	}

	public static boolean commitSoulCompass(ServerPlayer player) {
		return commitLocator(player, SpellEffect.SOUL_COMPASS);
	}

	/** Revalidates and commits the selected non-channeled celestial search spell. */
	public static boolean commitLocator(ServerPlayer player, SpellEffect expectedEffect) {
		if (expectedEffect != SpellEffect.SOUL_COMPASS
				&& expectedEffect != SpellEffect.CARTOGRAPHERS_STAR) return false;
		GrimoireDefinition grimoire = heldGrimoire(player);
		if (grimoire == null) return false;
		SpellDefinition spell = selectedSpell(player, grimoire);
		if (spell.effect() != expectedEffect || !commonChecks(player, spell, true)) return false;
		PreparedMagicCast magic = ServerMagicCasts.prepare(player, spell.id(), CastSource.SPELL);
		if (!magic.allowed()) return false;
		int energyCost = spellEnergyCost(player, spell);
		SpellCastTransaction transaction = new SpellCastTransaction(player, spell, magic, energyCost);
		if (!begin(player, spell, transaction)) return false;
		boolean committed = transaction.complete(() -> true);
		PowersPackets.syncTo(player);
		if (!committed) MagicAttemptReporter.executionFailure(player, spell.id());
		return committed;
	}

	private static boolean commonChecks(ServerPlayer player, SpellDefinition spell, boolean punishDampening) {
		if (CHANNELS.containsKey(player.getUUID())) {
			MagicAttemptReporter.failure(player, spell.id(), MagicFailureReason.ALREADY_CHANNELING);
			failed(player, "spell.powers.already_channeling");
			return false;
		}
		if (!MagicUseGate.passes(player, punishDampening, spell.id())) return false;
		long remaining = TestingOverrides.cooldownsDisabled(player.getUUID()) ? 0L
				: PlayerPowers.get(player).cooldownReadyAt(cooldownId(spell))
						- player.level().getGameTime();
		if (remaining > 0) {
			MagicAttemptReporter.failure(player, spell.id(), MagicFailureReason.COOLDOWN,
					Map.of("remaining_ticks", remaining));
			PowerMessages.overlay(player, Component.translatable(
					"spell.powers.cooldown", CooldownPresentation.wholeSeconds(remaining)));
			return false;
		}
		return true;
	}

	private static int spellEnergyCost(ServerPlayer player, SpellDefinition spell) {
		return ArtifactEnergyModifiers.forPlayer(player, spell.id(), spell.energyCost());
	}

	private static boolean begin(ServerPlayer player, SpellDefinition spell,
			SpellCastTransaction transaction) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (!transaction.begin()) {
			long available = (long) data.energy() + ArtifactEnergyReservoir.totalStored(player);
			MagicAttemptReporter.failure(player, spell.id(), MagicFailureReason.INSUFFICIENT_ENERGY,
					Map.of("required", (long) transaction.energyCost(), "available", available));
			failed(player, "energy.powers.empty.1");
			return false;
		}
		PowersPackets.syncTo(player);
		return true;
	}

	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Session>> iterator = CHANNELS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Session> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Session session = entry.getValue();
			if (player == null || !player.isAlive()) {
				session.transaction().rollbackFull();
				iterator.remove();
				continue;
			}
			ChannelStatus status = MagicUseGate.ongoingAllowed(player)
					&& player.level().dimension().equals(session.dimension())
					? ChannelRules.status(session.state(), player.level().getGameTime(),
					player.getX(), player.getY(), player.getZ(), holding(player, session.grimoireKey()),
					AmethystDampening.isDampened(player)) : ChannelStatus.INTERRUPTED;
			if (status == ChannelStatus.CHANNELING) {
				if (server.getTickCount() % 5 == 0) channelFx(player, session);
				continue;
			}
			iterator.remove();
			if (status == ChannelStatus.INTERRUPTED) {
				MagicAttemptReporter.failure(player, session.spell().id(),
						MagicFailureReason.CHANNEL_INTERRUPTED);
				session.transaction().interrupt();
				PowersPackets.syncTo(player);
				failed(player, "spell.powers.interrupted");
				continue;
			}
			finish(player, session.spell(), session.transaction(), session.target());
		}
	}

	private static void channelFx(ServerPlayer player, Session session) {
		ServerLevel level = (ServerLevel) player.level();
		double phase = level.getGameTime() * 0.08;
		PowerFx.ring(level, player.position().add(0, 0.08, 0), 1.8, 0x7455A8, 14, phase);
		PowerFx.burst(level, player.position().add(0, 1, 0),
				PowerFx.dust(0xBCA7FF, 0.85F), 3, 0.45, 0.0);
	}

	private static void finish(ServerPlayer player, SpellDefinition spell,
			SpellCastTransaction transaction, SpellTarget target) {
		if (!transaction.complete(() -> SpellEffects.execute(player, spell, target))) {
			PowersPackets.syncTo(player);
			failed(player, "spell.powers.failed");
			MagicAttemptReporter.executionFailure(player, spell.id());
			return;
		}
		PowersPackets.syncTo(player);
		PowerMessages.overlay(player, Component.translatable("spell.powers.cast", spellName(spell)));
	}

	public static void markDamaged(LivingEntity entity) {
		Session session = CHANNELS.get(entity.getUUID());
		if (session != null) CHANNELS.put(entity.getUUID(), new Session(
				session.state().withDamaged(true), session.spell(), session.grimoireKey(),
				session.transaction(), session.target(), session.dimension()));
	}

	private static GrimoireDefinition heldGrimoire(ServerPlayer player) {
		GrimoireDefinition main = definition(player.getMainHandItem());
		return main != null ? main : definition(player.getOffhandItem());
	}

	/** Resolves the currently held canonical grimoire for server packet revalidation. */
	public static GrimoireDefinition heldDefinition(ServerPlayer player) {
		return heldGrimoire(player);
	}

	private static GrimoireDefinition definition(ItemStack stack) {
		return stack.getItem() instanceof GrimoireItem item ? REGISTRY.forTexture(item.key()) : null;
	}

	private static boolean holding(ServerPlayer player, String canonicalKey) {
		GrimoireDefinition held = heldGrimoire(player);
		return held != null && held.key().equals(canonicalKey);
	}

	private static SpellDefinition selectedSpell(ServerPlayer player, GrimoireDefinition grimoire) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int raw = data.rawSelectedSpell(grimoire.key());
		int migrated = SpellSelectionMigration.canonicalIndex(grimoire.key(), raw);
		if (migrated != raw) data.setSelectedSpell(grimoire.key(), migrated);
		int selected = data.selectedSpell(grimoire.key(),
				grimoire.spells().stream().map(SpellDefinition::id).toList());
		return grimoire.spells().get(selected);
	}

	private static String cooldownId(SpellDefinition spell) {
		return "spell:" + spell.id();
	}

	private static Component spellName(SpellDefinition spell) {
		return Component.translatable("spell.powers." + spell.id());
	}

	private static void showSelection(ServerPlayer player, SpellDefinition spell) {
		PowerMessages.overlay(player, Component.translatable("spell.powers.selected", spellName(spell)));
	}

	private static void failed(ServerPlayer player, String key) {
		PowerFx.cancelled((ServerLevel) player.level(), player.position().add(0, 1, 0), 0x7455A8);
		PowerMessages.overlay(player, Component.translatable(key));
	}

	public static void clear(ServerPlayer player) {
		Session session = CHANNELS.remove(player.getUUID());
		if (session != null) session.transaction().rollbackFull();
	}

	public static void clearAll() {
		CHANNELS.values().forEach(session -> session.transaction().rollbackFull());
		CHANNELS.clear();
	}
}
