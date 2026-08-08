package com.powers.network;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.Power;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.protection.PowerProtection;
import com.powers.power.abilities.TeleportAbility;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// the mod's packets: ability activation, teleport requests and marks from the
// client, plus the power-state snapshot sent to each player
public final class PowersPackets {
	private static final CastNonceTracker LOCATOR_NONCES = new CastNonceTracker(20 * 30);
	private PowersPackets() {
	}

	public record ActivateAbilityPayload(int slot) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ActivateAbilityPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("activate_ability"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, ActivateAbilityPayload::slot,
						ActivateAbilityPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportRequestPayload(int slot, double x, double y, double z,
			ResourceKey<Level> dimension, String targetName, boolean toPlayer) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TeleportRequestPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("teleport_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, TeleportRequestPayload::slot,
						ByteBufCodecs.DOUBLE, TeleportRequestPayload::x,
						ByteBufCodecs.DOUBLE, TeleportRequestPayload::y,
						ByteBufCodecs.DOUBLE, TeleportRequestPayload::z,
						ResourceKey.streamCodec(Registries.DIMENSION), TeleportRequestPayload::dimension,
						ByteBufCodecs.STRING_UTF8, TeleportRequestPayload::targetName,
						ByteBufCodecs.BOOL, TeleportRequestPayload::toPlayer,
						TeleportRequestPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportMarkPayload(int slot, double x, double y, double z) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TeleportMarkPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("teleport_mark"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportMarkPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, TeleportMarkPayload::slot,
						ByteBufCodecs.DOUBLE, TeleportMarkPayload::x,
						ByteBufCodecs.DOUBLE, TeleportMarkPayload::y,
						ByteBufCodecs.DOUBLE, TeleportMarkPayload::z,
						TeleportMarkPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// the server's go-ahead for the celestial grimoire: open the locator screen
	public record OpenLocatorScreenPayload(UUID nonce) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<OpenLocatorScreenPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("open_locator"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenLocatorScreenPayload> STREAM_CODEC =
				StreamCodec.composite(UUID_CODEC, OpenLocatorScreenPayload::nonce, OpenLocatorScreenPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// the scry choice: the uuid of the online player the grimoire should locate
	public record LocatePlayerPayload(UUID targetUuid, UUID nonce) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<LocatePlayerPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("locate_player"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LocatePlayerPayload> STREAM_CODEC =
				StreamCodec.composite(
						UUID_CODEC, LocatePlayerPayload::targetUuid,
						UUID_CODEC, LocatePlayerPayload::nonce,
						LocatePlayerPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void openLocator(ServerPlayer player) {
		UUID nonce = LOCATOR_NONCES.issue(player.getUUID(), player.level().getServer().getTickCount());
		ServerPlayNetworking.send(player, new OpenLocatorScreenPayload(nonce));
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
			(buf, uuid) -> {
				buf.writeLong(uuid.getMostSignificantBits());
				buf.writeLong(uuid.getLeastSignificantBits());
			},
			buf -> new UUID(buf.readLong(), buf.readLong()));

	public record PowerStatePayload(List<String> powerIds, List<String> activeToggles, int energy,
			int energyCapacity, boolean canSeeDarkRealm) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<PowerStatePayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("power_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PowerStatePayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
						PowerStatePayload::powerIds,
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
						PowerStatePayload::activeToggles,
						ByteBufCodecs.VAR_INT,
						PowerStatePayload::energy,
						ByteBufCodecs.VAR_INT,
						PowerStatePayload::energyCapacity,
						ByteBufCodecs.BOOL,
						PowerStatePayload::canSeeDarkRealm,
						PowerStatePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportRequestPayload.TYPE, TeleportRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportMarkPayload.TYPE, TeleportMarkPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LocatePlayerPayload.TYPE, LocatePlayerPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PowerStatePayload.TYPE, PowerStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenLocatorScreenPayload.TYPE, OpenLocatorScreenPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.TYPE, PowersPackets::handleActivate);
		ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPayload.TYPE, PowersPackets::handleTeleport);
		ServerPlayNetworking.registerGlobalReceiver(TeleportMarkPayload.TYPE, PowersPackets::handleMark);
		ServerPlayNetworking.registerGlobalReceiver(LocatePlayerPayload.TYPE, PowersPackets::handleLocate);
	}

	private static void handleActivate(ActivateAbilityPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			// dampened or time-frozen players can't use powers; trying gets punished
			AmethystDampening.update(player);
			if (AmethystDampening.isDampened(player)) {
				AmethystDampening.punish(player);
				return;
			}
			if (SpaceTimeAbility.isFrozen(player)) {
				SpaceTimeAbility.reject(player);
				return;
			}
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			Power power = data.getPower(payload.slot());
			if (power == null) return;
			Ability ability = power.ability();
			if (ability == null || ability.requiresInput()) return;
			String powerId = power.id().toString();

			if (ability.isToggle()) {
				// toggling pays energy up front; if activation fails the cost is refunded
				if (data.isToggleActive(powerId)) {
					ability.activateToggleOff(player, data);
					data.setToggleActive(player, powerId, false);
				} else {
					boolean paid = data.spendEnergy(player, ability);
					if (paid && ability.activateToggleOn(player, data)) {
						data.setToggleActive(player, powerId, true);
					} else if (paid) {
						data.refundEnergy(ability);
					}
				}
				return;
			}

			// abilities can't be spammed: the client gets the remaining cooldown in seconds
			if (!ActivationCooldowns.isReady(player, ability)) {
				PowerMessages.send(player, "ability.powers.cooldown", 4,
						seconds(ActivationCooldowns.remainingTicks(player, ability)));
				return;
			}
			if (!data.spendEnergy(player, ability)) return;
			if (!ability.activate(player, data)) {
				data.refundEnergy(ability);
			} else {
				ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
			}
			syncTo(player);
		});
	}

	private static void handleTeleport(TeleportRequestPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			// dampened or frozen players can't teleport either
			AmethystDampening.update(player);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			if (AmethystDampening.isDampened(player)) {
				AmethystDampening.punish(player);
				return;
			}
			if (SpaceTimeAbility.isFrozen(player)) {
				SpaceTimeAbility.reject(player);
				return;
			}
			// guards against malformed packets: names cap at 16 chars and
			// NaN coordinates must never reach the teleport code
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT
					|| payload.targetName().length() > 16
					|| !Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			Power power = data.getPower(payload.slot());
			// only targeted teleports arrive here; one-shot casts come through activate
			if (power == null || power.ability() == null || !power.ability().requiresInput()) return;
			Ability ability = power.ability();

			if (payload.toPlayer()) {
				// warping to a player drops you next to them in marking mode (spectator) to pick the exact landing spot
				ServerPlayer target = findPlayer(player, payload.targetName());
				if (target == null) return;
				if (!ActivationCooldowns.isReady(player, ability)) {
					PowerMessages.send(player, "ability.powers.cooldown", 4,
							seconds(ActivationCooldowns.remainingTicks(player, ability)));
					return;
				}
				if (!data.spendEnergy(player, ability)) return;
				if (!TeleportAbility.startMarking(player, target, payload.slot())) {
					data.refundEnergy(ability);
					syncTo(player);
					return;
				}
				ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
				syncTo(player);
				return;
			}

			ServerPlayer subject = payload.targetName().isEmpty()
					? player : findPlayer(player, payload.targetName());
			if (subject == null) return;
			if (!PowerProtection.mayForceMove(player, subject)) {
				PowerMessages.send(player, "powers.packet.consent_denied", 1, subject.getName().getString());
				return;
			}
			// a dampened target is protected from being yanked away
			if (AmethystDampening.isDampened(subject)) {
				PowerMessages.send(player, "amethyst.powers.target_protected", 4);
				return;
			}
			if (!ActivationCooldowns.isReady(player, ability)) {
				PowerMessages.send(player, "ability.powers.cooldown", 4,
						seconds(ActivationCooldowns.remainingTicks(player, ability)));
				return;
			}
			if (!data.spendEnergy(player, ability)) return;
			if (!ability.activateTeleport(player, subject, data, payload.dimension(), payload.x(), payload.y(), payload.z())) {
				data.refundEnergy(ability);
			} else {
				ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
			}
			syncTo(player);
		});
	}

	private static void handleMark(TeleportMarkPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			// reject garbage: NaN coordinates would corrupt the stored mark
			if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			AmethystDampening.update(player);
			// the same counterplay applies to marking a teleport spot
			if (AmethystDampening.isDampened(player)) {
				TeleportAbility.clearMarking(player);
				AmethystDampening.punish(player);
				return;
			}
			TeleportAbility.completeMarking(player, payload.slot(),
					new Vec3(payload.x(), payload.y(), payload.z()));
		});
	}

	private static ServerPlayer findPlayer(ServerPlayer caster, String name) {
		for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) caster.level()).getServer().getPlayerList().getPlayers()) {
			if (p.getName().getString().equalsIgnoreCase(name)) {
				return p;
			}
		}
		PowerMessages.send(caster, "powers.packet.player_not_found", 3, name);
		return null;
	}

	private static void handleLocate(LocatePlayerPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			long tick = context.server().getTickCount();
			if (!LOCATOR_NONCES.consume(player.getUUID(), payload.nonce(), tick)) return;
			boolean holdingGrimoire = (player.getMainHandItem().getItem() instanceof com.powers.item.GrimoireItem main
						&& com.powers.spell.SpellCastingManager.registry().forTexture(main.key()) != null
						&& com.powers.spell.SpellCastingManager.registry().forTexture(main.key()).key().equals("book_grimoire_celestial"))
					|| (player.getOffhandItem().getItem() instanceof com.powers.item.GrimoireItem off
						&& com.powers.spell.SpellCastingManager.registry().forTexture(off.key()) != null
						&& com.powers.spell.SpellCastingManager.registry().forTexture(off.key()).key().equals("book_grimoire_celestial"));
			if (!holdingGrimoire) return;
			// frozen time stalls the grimoire too, and the payment never lands
			if (SpaceTimeAbility.isFrozen(player)) {
				SpaceTimeAbility.reject(player);
				return;
			}
			ServerPlayer target = findOnlinePlayer(player, payload.targetUuid());
			if (target == null) {
				// the chosen soul left the world between the screen and the cast
				PowerMessages.send(player, "grimoire.celestial.offline", 3);
				return;
			}
			if (!PowerProtection.mayLocate(player, target)) {
				PowerMessages.send(player, "grimoire.celestial.consent_denied", 1);
				return;
			}

			ServerLevel level = (ServerLevel) player.level();
			Vec3 pos = player.position().add(0, 1, 0);

			// the amethyst curse grounds the celestial words and bites back
			AmethystDampening.update(player);
			if (AmethystDampening.isDampened(player)) {
				retaliate(player, level, pos, "grimoire.celestial.amethyst", 3);
				return;
			}

			// realms veil themselves from the unmastered: the light realm only
			// answers to a maxed light path, the dark realm only to a maxed
			// darkness user - anyone else gets the backlash
			ResourceKey<Level> targetDim = target.level().dimension();
			PlayerPowers.PlayerPowersData casterData = PlayerPowers.get(player);
			if (isLightRealm(targetDim)) {
				if (SkillSystem.hasDarknessTag(player) || casterData.skillLevel() < SkillSystem.MAX_LEVEL) {
					retaliate(player, level, pos, "grimoire.celestial.light_gate", 3);
					return;
				}
			} else if (SkillSystem.isDarkRealm(targetDim)) {
				if (!SkillSystem.hasDarknessTag(player) || casterData.darknessLevel() < SkillSystem.DARKNESS_MAX_LEVEL) {
					retaliate(player, level, pos, "grimoire.celestial.dark_gate", 3);
					return;
				}
			}

			// The selection packet only names a target. The server revalidates the
			// held celestial book, selected spell, cooldown and energy immediately
			// before committing the cast.
			if (!com.powers.spell.SpellCastingManager.commitSoulCompass(player)) return;
			cast(player, level, pos, target);
		});
	}

	// a wasted cast stings: nausea for twenty seconds while the power recoils
	private static void retaliate(ServerPlayer player, ServerLevel level, Vec3 pos, String messageKey, int variants) {
		PowerFx.cancelled(level, pos, 0xFF8C6FD8);
		PowerFx.coloredBurst(level, pos, 0xFF4B2E50, 26, 1.0);
		PowerFx.burst(level, pos, ParticleTypes.REVERSE_PORTAL, 14, 0.5, 0.05);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.8f);
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.NAUSEA, 400, 0));
		PowerMessages.send(player, messageKey, variants);
	}

	// the locator's cast: a short celestial ritual, then the stars answer or the void laughs
	private static void cast(ServerPlayer player, ServerLevel level, Vec3 pos, ServerPlayer target) {
		final int CELESTIAL = 0xFFD9E9FF;
		final int GOLD = 0xFFFFE08A;
		PowerFx.sound(level, pos, SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		PowerFx.rune(level, pos, 2.2, CELESTIAL, 26, 0.0);
		PowerFx.spiral(level, pos.add(0, 0.1, 0), 0.7, 3.4, CELESTIAL, 20, 0.0);
		PowerFx.burst(level, pos, ParticleTypes.END_ROD, 24, 0.6, 0.04);

		MinecraftServer server = level.getServer();
		// mid-cast: the ritual swells with widening rings and a beacon's hum
		PowersMod.scheduleDelayed(server, 16, () -> {
			if (player.isRemoved()) return;
			PowerFx.ring(level, pos, 4.2, CELESTIAL, 34, 0.4);
			PowerFx.ring(level, pos, 2.8, 0xFFE8F4FF, 26, 1.1);
			PowerFx.sound(level, pos, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.25f);
		});
		// the heavens open: a pillar of starlight climbs from the book
		PowersMod.scheduleDelayed(server, 32, () -> {
			if (player.isRemoved()) return;
			PowerFx.beam(level, pos, pos.add(0, 36, 0),
					ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFD9E9FF), 18);
			PowerFx.burst(level, pos.add(0, 0.2, 0), ParticleTypes.END_ROD, 18, 0.4, 0.05);
			PowerFx.sound(level, pos, SoundEvents.CONDUIT_ACTIVATE, 1.0f, 1.15f);
		});
		// the answer: a golden ring, then the whisper of a place
		PowersMod.scheduleDelayed(server, 48, () -> {
			if (player.isRemoved()) return;
			PowerFx.rune(level, pos, 3.0, GOLD, 34, Math.PI);
			PowerFx.coloredBurst(level, pos.add(0, 1.2, 0), GOLD, 40, 1.6);
			PowerFx.burst(level, pos.add(0, 1.2, 0), ParticleTypes.END_ROD, 26, 1.2, 0.06);
			PowerFx.sound(level, pos, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.4f);

			// the scried soul feels a brief prickling of stars, wherever they are
			if (target.isAlive() && !target.isRemoved()) {
				ServerLevel targetLevel = (ServerLevel) target.level();
				PowerFx.coloredBurst(targetLevel, target.position().add(0, 1, 0), 0xFFFFFFFF, 10, 0.6);
				PowerFx.burst(targetLevel, target.position().add(0, 1, 0), ParticleTypes.END_ROD, 8, 0.4, 0.04);
			}

			// only the caster hears the answer
			Vec3 tPos = target.position();
			PowerMessages.send(player, "grimoire.celestial.reveal", 3, target.getName().getString());
			player.sendSystemMessage(Component.literal("Dimension: ")
					.append(Component.literal(dimensionName(target.level().dimension()))
							.withStyle(style -> style.withColor(0xFFFFE08A))));
			player.sendSystemMessage(Component.literal("Coordinates: ")
					.append(Component.literal((int) Math.floor(tPos.x) + " " + (int) Math.floor(tPos.y) + " " + (int) Math.floor(tPos.z))
							.withStyle(style -> style.withColor(0xFFFFE08A).withBold(true))));
		});
	}

	private static ServerPlayer findOnlinePlayer(ServerPlayer caster, UUID uuid) {
		for (ServerPlayer p : caster.level().getServer().getPlayerList().getPlayers()) {
			if (p.getUUID().equals(uuid)) return p;
		}
		return null;
	}

	private static boolean isLightRealm(ResourceKey<Level> dimension) {
		return dimension.identifier().equals(PowersMod.id("light_realm"));
	}

	private static String dimensionName(ResourceKey<Level> key) {
		return switch (key.identifier().getPath()) {
			case "overworld" -> "The Overworld";
			case "the_nether" -> "The Nether";
			case "the_end" -> "The End";
			case "dark_realm" -> "The Dark Realm";
			case "light_realm" -> "The Light Realm";
			case "middleworld" -> "The Middleworld";
			default -> key.identifier().toString();
		};
	}

	private static String seconds(int ticks) {
		// rounds up so the cooldown message never reads zero too early
		return String.valueOf((ticks + 19) / 20);
	}

	// sends the player's current power state so the client HUD matches the server
	public static void syncTo(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		ServerPlayNetworking.send(player, new PowerStatePayload(
				data.getSlotIds(),
				data.getActiveToggles(),
				data.energy(),
				data.energyCapacity(),
				SkillSystem.canEnterDarkRealm(player)));
	}
}
