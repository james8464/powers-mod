package com.powers.realm;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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
		BlockPos entry = BlockPos.containing(RealmLayout.ENTRY_X, realm.getMinY() + 1, RealmLayout.ENTRY_Z);
		// Death is rare and the player must never spend even one playable tick
		// outside the realm, so load the single entry chunk synchronously here.
		realm.getChunk(entry.getX() >> 4, entry.getZ() >> 4);
		respawned.teleportTo(realm, RealmLayout.ENTRY_X, realm.getMinY() + 1,
				RealmLayout.ENTRY_Z, Set.of(), respawned.getYRot(), respawned.getXRot(), false);
		PowerFx.rune(realm, respawned.position(), 2.0,
				id.equals(PowersMod.id("dark_realm")) ? 0x2A143D : 0xFFFFFF, 28, Math.PI);
		PowerMessages.sendImportant(respawned, "realm.powers.death_confined", 1);
		return true;
	}
}
