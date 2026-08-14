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
		if (id == null) {
			lockForRecovery(respawned, required);
			return true;
		}
		ServerLevel realm = respawned.level().getServer().getLevel(
				ResourceKey.create(Registries.DIMENSION, id));
		if (RealmConfinementRules.enforcement(required, realm != null)
				== RealmConfinementRules.Enforcement.LOCKED_HOLD) {
			lockForRecovery(respawned, id.toString());
			return true;
		}
		requestConfinement(respawned.level().getServer(), respawned.getUUID(), realm.dimension(), id, 0);
		return true;
	}

	private static void lockForRecovery(ServerPlayer player, String realmId) {
		player.setGameMode(GameType.SPECTATOR);
		PowersMod.LOGGER.error("Realm confinement entered locked spectator hold: player={}, missing realm={}",
				player.getUUID(), realmId);
		PowerMessages.sendImportant(player, "realm.powers.confinement_recovery_required", 1);
	}

	private static void requestConfinement(MinecraftServer server, java.util.UUID playerId,
			ResourceKey<Level> realmKey,
			Identifier realmId, int failures) {
		ServerLevel realm = server.getLevel(realmKey);
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (realm == null || player == null) return;
		BlockPos entry = BlockPos.containing(RealmLayout.ENTRY_X,
				RealmTerrain.provisionalArrivalY(realm), RealmLayout.ENTRY_Z);
		TravelChunkLoader.request(playerId, realm, entry, "realm_confinement", (current, owner) -> {
			ServerPlayer captive = current.getPlayerList().getPlayer(owner);
			ServerLevel currentRealm = current.getLevel(realmKey);
			if (captive == null || currentRealm == null) return;
			captive.teleportTo(currentRealm, RealmLayout.ENTRY_X,
					RealmTerrain.arrivalY(currentRealm, (int) RealmLayout.ENTRY_X, (int) RealmLayout.ENTRY_Z),
					RealmLayout.ENTRY_Z, Set.of(), captive.getYRot(), captive.getXRot(), false);
			PowerFx.rune(currentRealm, captive.position(), 2.0,
					realmId.equals(PowersMod.id("dark_realm")) ? 0x2A143D : 0xFFFFFF, 28, Math.PI);
			PowerMessages.sendImportant(captive, "realm.powers.death_confined", 1);
		}, (current, owner) -> scheduleRetry(current, owner, realmKey, realmId, failures + 1));
	}

	private static void scheduleRetry(MinecraftServer server, java.util.UUID playerId,
			ResourceKey<Level> realmKey, Identifier realmId, int failures) {
		if (!RealmConfinementRetryPolicy.shouldRetry(failures)) {
			ServerPlayer captive = server.getPlayerList().getPlayer(playerId);
			PowersMod.LOGGER.error("Realm confinement entered locked spectator hold after {} failures: player={}, realm={}",
					failures, playerId, realmId);
			if (captive != null) {
				captive.setGameMode(GameType.SPECTATOR);
				PowerMessages.sendImportant(captive, "realm.powers.confinement_recovery_required", 1);
			}
			return;
		}
		PowersMod.scheduleDelayed(server, RealmConfinementRetryPolicy.delayTicks(failures),
				playerId, realmKey, playerId, "realm_confinement_retry",
				(current, task) -> requestConfinement(current, task.subjectId(), realmKey,
						realmId, failures));
	}
}
