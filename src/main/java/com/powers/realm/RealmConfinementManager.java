package com.powers.realm;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.util.PowerMessages;
import com.powers.power.travel.TravelChunkLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;

import java.util.Set;

/** Enforces realm confinement across vanilla death and respawn transitions. */
public final class RealmConfinementManager {
	private RealmConfinementManager() {
	}

	/** Returns an under-qualified dead player to the realm that still owns them. */
	public static boolean restoreAfterDeath(ServerPlayer oldPlayer, ServerPlayer respawned) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(respawned);
		String required = RealmConfinementRules.requiredRespawnRealm(
				oldPlayer.level().dimension().identifier().toString(),
				SkillSystem.hasDarknessTag(respawned), data.skillLevel(), data.darknessLevel());
		if (required == null) return false;
		Identifier id = Identifier.tryParse(required);
		if (id == null) return false;
		ServerLevel realm = respawned.level().getServer().getLevel(
				ResourceKey.create(Registries.DIMENSION, id));
		if (realm == null) {
			PowersMod.LOGGER.error("Cannot enforce realm confinement: missing dimension {}", id);
			return false;
		}
		respawned.setGameMode(GameType.SPECTATOR);
		requestConfinement(respawned.level().getServer(), respawned.getUUID(), realm.dimension(), id);
		return true;
	}

	private static void requestConfinement(MinecraftServer server, java.util.UUID playerId,
			ResourceKey<Level> realmKey,
			Identifier realmId) {
		ServerLevel realm = server.getLevel(realmKey);
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (realm == null || player == null) return;
		BlockPos entry = BlockPos.containing(RealmLayout.ENTRY_X, realm.getMinY() + 1, RealmLayout.ENTRY_Z);
		TravelChunkLoader.request(playerId, realm, entry, () -> {
			ServerPlayer captive = server.getPlayerList().getPlayer(playerId);
			if (captive == null) return;
			captive.teleportTo(realm, RealmLayout.ENTRY_X, realm.getMinY() + 1,
					RealmLayout.ENTRY_Z, Set.of(), captive.getYRot(), captive.getXRot(), false);
			captive.setGameMode(GameType.ADVENTURE);
			PowerFx.rune(realm, captive.position(), 2.0,
					realmId.equals(PowersMod.id("dark_realm")) ? 0x2A143D : 0xFFFFFF, 28, Math.PI);
			PowerMessages.sendImportant(captive, "realm.powers.death_confined", 1);
		}, () -> scheduleRetry(server, playerId, realmKey, realmId));
	}

	private static void scheduleRetry(MinecraftServer server, java.util.UUID playerId,
			ResourceKey<Level> realmKey, Identifier realmId) {
		PowersMod.scheduleDelayed(server, 100,
				() -> requestConfinement(server, playerId, realmKey, realmId));
	}
}
