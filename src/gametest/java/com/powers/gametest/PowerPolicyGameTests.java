package com.powers.config;

import com.powers.protection.PowerProtection;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.protection.ProtectionDecision;
import com.powers.protection.ProtectionAction;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Live-level proof for deterministic scoped policy resolution and denial precedence. */
public final class PowerPolicyGameTests {
	@GameTest(maxTicks = 20)
	public void liveDimensionPolicyOverridesWorldWithoutBypassingDenials(GameTestHelper helper) {
		String world = helper.getLevel().getServer().getWorldData().getLevelName();
		String dimension = helper.getLevel().dimension().identifier().toString();
		PowerPolicyPatch worldPatch = patch(false, false);
		PowerPolicyPatch dimensionPatch = patch(true, null);
		PowersConfig config = PowersConfig.defaults().withPolicyOverrides(
				new PowerPolicyOverrides(Map.of(world, worldPatch),
						Map.of(dimension, dimensionPatch)));

		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(config, helper.getLevel());
		helper.assertTrue(policy.allowTerrainDamage(),
				"The live dimension did not override its matching world policy");
		helper.assertTrue(!policy.requireLocatorConsent(),
				"A field absent from the dimension patch did not fall back to its world policy");
		helper.assertTrue(policy.source(ResolvedPowerPolicy.Field.ALLOW_TERRAIN_DAMAGE).scope()
				== ResolvedPowerPolicy.Scope.DIMENSION,
				"The live resolved source did not identify the dimension override");
		helper.assertTrue(PowerProtection.blockDecision(policy, true, false, true)
				== ProtectionDecision.DENY_SAFE_ZONE,
				"A scoped allow bypassed the absolute safe-zone denial");
		helper.assertTrue(PowerProtection.blockDecision(policy, false, false, false)
				== ProtectionDecision.DENY_ADAPTER,
				"A scoped allow bypassed the external protection denial");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void runtimeConfigDiagnosticsAndObserveAdapterRemainAuthoritative(GameTestHelper helper) {
		String world = helper.getLevel().getServer().getWorldData().getLevelName();
		String dimension = helper.getLevel().dimension().identifier().toString();
		BlockPos adapterTargetPosition = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockPos safeTargetPosition = helper.absolutePos(new BlockPos(6, 2, 2));
		var caster = helper.makeMockServerPlayerInLevel();
		var adapterTarget = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
		var safeTarget = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
		helper.assertTrue(adapterTarget != null && safeTarget != null, "Test targets could not spawn");
		adapterTarget.setPos(Vec3.atCenterOf(adapterTargetPosition));
		safeTarget.setPos(Vec3.atCenterOf(safeTargetPosition));
		helper.getLevel().addFreshEntity(adapterTarget);
		helper.getLevel().addFreshEntity(safeTarget);
		String config = """
				{
				  "schemaVersion": 4,
				  "safeZones": [{"dimension":"%s","x":%d,"y":%d,"z":%d,"radius":3}],
				  "policyOverrides": {
				    "worlds": {"%s":{"allowTerrainDamage":false}},
				    "dimensions": {"%s":{"allowTerrainDamage":true}}
				  }
				}
				""".formatted(dimension, safeTargetPosition.getX(), safeTargetPosition.getY(),
				safeTargetPosition.getZ(), world, dimension);
		AutoCloseable installed = PowersConfigLoader.installForGameTest(config);
		boolean registered = PowerProtectionAdapters.register("net007_observe_fixture", 2_000,
				query -> query.action() != ProtectionAction.OBSERVE
						|| !adapterTarget.getUUID().equals(query.target()));
		try {
			helper.assertTrue(registered, "The live OBSERVE adapter fixture did not register");
			ResolvedPowerPolicy runtime = ResolvedPowerPolicy.resolve(helper.getLevel());
			helper.assertTrue(runtime.allowTerrainDamage(),
					"The runtime resolver did not apply the loaded dimension override");
			helper.assertTrue(!PowerProtection.mayLocate(caster, adapterTarget),
					"A named mob bypassed the registered OBSERVE adapter");
			PowerProtectionAdapters.unregister("net007_observe_fixture");
			helper.assertTrue(!PowerProtection.mayLocate(caster, safeTarget),
					"A named mob bypassed its configured safe zone");
			String diagnostic = String.join("\n", PowerPolicyDiagnostics.lines(helper.getLevel().getServer()));
			helper.assertTrue(diagnostic.contains("terrainDamage=true@dimension:" + dimension),
					"The runtime diagnostic omitted the effective dimension source");
			helper.succeed();
		} finally {
			PowerProtectionAdapters.unregister("net007_observe_fixture");
			try {
				installed.close();
			} catch (Exception failure) {
				throw new IllegalStateException("Could not restore the GameTest config", failure);
			}
		}
	}

	private static PowerPolicyPatch patch(Boolean terrain, Boolean locatorConsent) {
		return new PowerPolicyPatch(terrain, null, null, null, locatorConsent,
				null, null, null, null, null, null);
	}
}
