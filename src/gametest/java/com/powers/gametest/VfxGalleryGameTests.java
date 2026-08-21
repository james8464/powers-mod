package com.powers.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Server half of the bounded VFX-011 gallery fixture. */
public final class VfxGalleryGameTests {
	@GameTest(environment = "powers:vfx_gallery", maxTicks = 40)
	@SuppressWarnings("removal")
	public void publishesEveryRendererFamily(GameTestHelper helper) {
		helper.assertTrue(VfxGalleryFixture.entityFamilies().size() == 8,
				"VFX gallery entity inventory drifted");
		helper.assertTrue(VfxGalleryFixture.spawnEggIds().size() == 6,
				"VFX gallery spawn-egg inventory drifted");
		var viewer = helper.makeMockServerPlayerInLevel();
		var ownedIds = new java.util.ArrayList<Integer>();
		var cleaned = new java.util.concurrent.atomic.AtomicBoolean();
		Runnable cleanup = () -> {
			if (!cleaned.compareAndSet(false, true)) return;
			for (int id : ownedIds) {
				var entity = helper.getLevel().getEntity(id);
				if (entity != null) entity.discard();
			}
			viewer.discard();
		};
		helper.runBeforeTestEnd(cleanup);
		var centre = net.minecraft.world.phys.Vec3.atCenterOf(
				helper.absolutePos(new net.minecraft.core.BlockPos(4, 2, 4)));
		viewer.teleportTo(centre.x, centre.y, centre.z);
		var ids = VfxGalleryFixture.spawnRendererEntities(viewer,
				new java.util.UUID(0x565846303131L, 1L), new java.util.UUID(0x565846303131L, 2L));
		ownedIds.addAll(ids);
		helper.assertTrue(ids.size() == 10 && ids.stream().distinct().count() == 10,
				"VFX gallery did not publish six mobs plus wide/slim Shadow and Echo instances");
		helper.runAfterDelay(1, () -> {
			var missing = java.util.stream.IntStream.range(0, ids.size())
					.filter(index -> helper.getLevel().getEntity(ids.get(index)) == null).boxed().toList();
			helper.assertTrue(missing.isEmpty(),
					"VFX gallery entity indices did not survive authoritative admission: " + missing);
			cleanup.run();
			helper.succeed();
		});
	}
}
