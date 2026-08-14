package com.powers.api.v1;

/** Fabric entrypoint contract discovered under {@code powers:v1} once per server epoch. */
public interface PowersExtension {
	/** Stable lowercase identity used for deterministic ordering and duplicate rejection. */
	String id();
	/** Registers bounded API contributions before SERVER_STARTED; exceptions reject this extension. */
	void register(PowersApiV1 api);
}
