package com.powers;

import com.powers.command.PowerCommand;
import com.powers.companion.ShadowCompanionStore;
import com.powers.config.PowersConfigLoader;
import com.powers.entity.EntityRuntimeLifecycle;
import com.powers.force.LivingForceManager;
import com.powers.forge.CrucibleWeaponRuntime;
import com.powers.item.ArtifactWeaponManager;
import com.powers.knowledge.KnowledgeEntryReloadListener;
import com.powers.loot.PowersLoot;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.state.PowerEntityState;
import com.powers.progression.RankGraphRegistry;

/** Registers immutable content and one-time Fabric hooks in dependency order. */
final class PowersBootstrap {
	private PowersBootstrap() {
	}

	static void initialize() {
		PlayerPowers.initialize();
		PowersConfigLoader.initialize();
		com.powers.command.PermissionNodes.installFabricAdapterIfPresent();
		KnowledgeEntryReloadListener.initialize();
		PowerEntityState.initialize();
		ShadowCompanionStore.initialize();
		RankGraphRegistry.initialize();
		PowersEffects.initialize();
		PowersSounds.initialize();
		PowersParticles.initialize();
		PowersDataComponents.initialize();
		PowersEntities.initialize();
		PowerRegistry.initialize();
		PowersItems.initialize();
		PowersWeapons.initialize();
		PowersBlocks.initialize();
		PowersBlockEntities.initialize();
		PowersMenus.initialize();
		LivingForceManager.initialize();
		ImportedPackItems.initialize();
		PowersLoot.initialize();
		PowersCreativeTab.initialize();
		CrystalPowerRegistry.initialize();
		ArtifactWeaponManager.initialize();
		CrucibleWeaponRuntime.initialize();
		PowersPackets.initialize();
		PowerCommand.register();
		PowerCombatEvents.register();
		EntityRuntimeLifecycle.initialize();
	}
}
