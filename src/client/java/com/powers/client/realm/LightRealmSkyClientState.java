package com.powers.client.realm;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.powers.PowersMod;
import com.powers.visual.LightRealmSkyProfile;

/** Per-SkyRenderer owner that keeps enhancement construction and failure outside vanilla fallback state. */
public final class LightRealmSkyClientState implements AutoCloseable {
	private LightRealmSkyRenderer renderer;
	private LightRealmSkyProfile profile;
	private boolean closed;

	public LightRealmSkyClientState(RenderTarget renderTarget) {
		try {
			renderer = new LightRealmSkyRenderer(renderTarget);
		} catch (RuntimeException | LinkageError failure) {
			PowersMod.LOGGER.warn("Light Realm sky enhancement unavailable; using static white fallback", failure);
		}
	}

	public boolean enhancedAvailable() {
		return !closed && renderer != null && renderer.available();
	}

	public void update(LightRealmSkyProfile nextProfile) {
		profile = nextProfile;
	}

	public boolean tryRender() {
		return profile != null && renderer != null && renderer.tryRender(profile);
	}

	@Override
	public void close() {
		if (closed) return;
		closed = true;
		profile = null;
		try {
			if (renderer != null) renderer.close();
		} finally {
			renderer = null;
		}
	}
}
