package com.powers.spell;

import com.powers.fx.PowerFx;
import com.powers.item.GrimoireItem;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Owns ritual sessions, spell cooldowns, payments and server-side held-book validation. */
public final class SpellCastingManager {
	private static final SpellRegistry REGISTRY = SpellRegistry.defaults();
	private static final Map<UUID, Session> CHANNELS = new HashMap<>();
	private static final Map<UUID, Long> AMPLIFIED_UNTIL = new HashMap<>();

	private record Session(ChannelState state, SpellDefinition spell, String grimoireKey, int energyCost) {
	}

	private SpellCastingManager() {
	}

	public static SpellRegistry registry() {
		return REGISTRY;
	}

	public static void use(ServerPlayer player, String texture) {
		GrimoireDefinition grimoire = REGISTRY.forTexture(texture);
		if (grimoire == null) return;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (player.isShiftKeyDown()) {
			int selected = data.cycleSpell(grimoire.key(), grimoire.spells().size());
			showSelection(player, grimoire.spells().get(selected));
			return;
		}
		SpellDefinition spell = selectedSpell(player, grimoire);
		if (spell.effect() == SpellEffect.SOUL_COMPASS) {
			if (commonChecks(player, spell, false)) PowersPackets.openLocator(player);
			return;
		}
		if (!commonChecks(player, spell, true)) return;
		if (!SpellEffects.canBegin(player, spell.effect())) {
			failed(player, "spell.powers.no_target");
			return;
		}
		if (!payAndCool(player, spell)) return;
		boolean amplified = consumeAmplification(player, spell.effect());
		if (spell.channelTicks() == 0) {
			finish(player, spell, amplified);
			return;
		}
		long end = player.level().getGameTime() + spell.channelTicks();
		ChannelState state = new ChannelState(end, player.getX(), player.getY(), player.getZ(), grimoire.key(), false);
		CHANNELS.put(player.getUUID(), new Session(state, spell, grimoire.key(), spell.energyCost()));
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.rune(level, player.position().add(0, 0.08, 0), 1.7, 0x7455A8, 20, 0);
		PowerFx.sound(level, player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.9f, 0.7f);
		player.sendSystemMessage(Component.translatable("spell.powers.channeling", spellName(spell)));
		if (amplified) AMPLIFIED_UNTIL.put(player.getUUID(), Long.MAX_VALUE);
	}

	public static boolean commitSoulCompass(ServerPlayer player) {
		GrimoireDefinition grimoire = heldGrimoire(player);
		if (grimoire == null) return false;
		SpellDefinition spell = selectedSpell(player, grimoire);
		return spell.effect() == SpellEffect.SOUL_COMPASS && commonChecks(player, spell, true)
				&& payAndCool(player, spell);
	}

	private static boolean commonChecks(ServerPlayer player, SpellDefinition spell, boolean punishDampening) {
		if (CHANNELS.containsKey(player.getUUID())) {
			failed(player, "spell.powers.already_channeling");
			return false;
		}
		if (SpaceTimeAbility.isFrozen(player)) {
			SpaceTimeAbility.reject(player);
			return false;
		}
		AmethystDampening.update(player);
		if (AmethystDampening.isDampened(player)) {
			if (punishDampening) AmethystDampening.punish(player);
			return false;
		}
		long remaining = PlayerPowers.get(player).cooldownReadyAt(cooldownId(spell)) - player.level().getGameTime();
		if (remaining > 0) {
			player.sendSystemMessage(Component.translatable("spell.powers.cooldown", (remaining + 19) / 20));
			return false;
		}
		return true;
	}

	private static boolean payAndCool(ServerPlayer player, SpellDefinition spell) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (!data.consumeEnergy(spell.energyCost())) {
			failed(player, "energy.powers.empty.1");
			return false;
		}
		data.setCooldown(cooldownId(spell), player.level().getGameTime() + spell.cooldownTicks());
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
				iterator.remove();
				continue;
			}
			ChannelStatus status = ChannelRules.status(session.state(), player.level().getGameTime(),
					player.getX(), player.getY(), player.getZ(), holding(player, session.grimoireKey()),
					AmethystDampening.isDampened(player));
			if (status == ChannelStatus.CHANNELING) {
				if (server.getTickCount() % 5 == 0) channelFx(player, session);
				continue;
			}
			iterator.remove();
			boolean amplified = AMPLIFIED_UNTIL.remove(player.getUUID()) != null;
			if (status == ChannelStatus.INTERRUPTED) {
				PlayerPowers.get(player).refundEnergy(session.energyCost() / 2);
				PowersPackets.syncTo(player);
				failed(player, "spell.powers.interrupted");
				continue;
			}
			finish(player, session.spell(), amplified);
		}
		AMPLIFIED_UNTIL.entrySet().removeIf(entry -> entry.getValue() != Long.MAX_VALUE
				&& entry.getValue() <= server.overworld().getGameTime());
	}

	private static void channelFx(ServerPlayer player, Session session) {
		ServerLevel level = (ServerLevel) player.level();
		double phase = level.getGameTime() * 0.08;
		PowerFx.ring(level, player.position().add(0, 0.08, 0), 1.8, 0x7455A8, 14, phase);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.ENCHANT, 3, 0.45, 0.01);
	}

	private static void finish(ServerPlayer player, SpellDefinition spell, boolean amplified) {
		if (!SpellEffects.execute(player, spell, amplified)) {
			failed(player, "spell.powers.failed");
			return;
		}
		player.sendSystemMessage(Component.translatable("spell.powers.cast", spellName(spell)));
	}

	public static void markDamaged(LivingEntity entity) {
		Session session = CHANNELS.get(entity.getUUID());
		if (session != null) CHANNELS.put(entity.getUUID(), new Session(
				session.state().withDamaged(true), session.spell(), session.grimoireKey(), session.energyCost()));
	}

	public static boolean counterspell(ServerPlayer caster, double range) {
		UUID nearest = null;
		double best = range * range;
		for (UUID uuid : CHANNELS.keySet()) {
			if (uuid.equals(caster.getUUID())) continue;
			ServerPlayer target = caster.level().getServer().getPlayerList().getPlayer(uuid);
			if (target == null || target.level() != caster.level()) continue;
			double distance = target.distanceToSqr(caster);
			if (distance <= best) {
				best = distance;
				nearest = uuid;
			}
		}
		if (nearest == null) return EnergyDrainAbility.counterNearest(caster, range);
		ServerPlayer target = caster.level().getServer().getPlayerList().getPlayer(nearest);
		CHANNELS.remove(nearest);
		AMPLIFIED_UNTIL.remove(nearest);
		if (target != null) {
			PowerFx.clash((ServerLevel) caster.level(), caster.getEyePosition(), target.getEyePosition(),
					0x7455A8, 0xA66CFF);
			target.sendSystemMessage(Component.translatable("spell.powers.countered"));
		}
		return true;
	}

	public static void amplify(ServerPlayer player, int duration) {
		AMPLIFIED_UNTIL.put(player.getUUID(), player.level().getGameTime() + duration);
	}

	private static boolean consumeAmplification(ServerPlayer player, SpellEffect effect) {
		if (effect == SpellEffect.RITUAL_AMPLIFICATION) return false;
		Long expiry = AMPLIFIED_UNTIL.get(player.getUUID());
		if (expiry == null || expiry <= player.level().getGameTime()) return false;
		AMPLIFIED_UNTIL.remove(player.getUUID());
		return true;
	}

	private static GrimoireDefinition heldGrimoire(ServerPlayer player) {
		GrimoireDefinition main = definition(player.getMainHandItem());
		return main != null ? main : definition(player.getOffhandItem());
	}

	private static GrimoireDefinition definition(ItemStack stack) {
		return stack.getItem() instanceof GrimoireItem item ? REGISTRY.forTexture(item.key()) : null;
	}

	private static boolean holding(ServerPlayer player, String canonicalKey) {
		GrimoireDefinition held = heldGrimoire(player);
		return held != null && held.key().equals(canonicalKey);
	}

	private static SpellDefinition selectedSpell(ServerPlayer player, GrimoireDefinition grimoire) {
		int selected = PlayerPowers.get(player).selectedSpell(grimoire.key(), grimoire.spells().size());
		return grimoire.spells().get(selected);
	}

	private static String cooldownId(SpellDefinition spell) {
		return "spell:" + spell.id();
	}

	private static Component spellName(SpellDefinition spell) {
		return Component.translatable("spell.powers." + spell.id());
	}

	private static void showSelection(ServerPlayer player, SpellDefinition spell) {
		player.sendSystemMessage(Component.translatable("spell.powers.selected", spellName(spell)));
	}

	private static void failed(ServerPlayer player, String key) {
		PowerFx.cancelled((ServerLevel) player.level(), player.position().add(0, 1, 0), 0x7455A8);
		player.sendSystemMessage(Component.translatable(key));
	}

	public static void clear(ServerPlayer player) {
		CHANNELS.remove(player.getUUID());
		AMPLIFIED_UNTIL.remove(player.getUUID());
	}

	public static void clearAll() {
		CHANNELS.clear();
		AMPLIFIED_UNTIL.clear();
	}
}
