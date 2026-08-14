package com.powers.config;

import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ConcurrentHashMap;

/** Revision-bound cache preventing scoped policy allocation on block/entity hot paths. */
final class PowerPolicyResolver {
	private static final int MAX_SCOPES = 512;
	private static volatile Cache cache = new Cache(-1L, null);

	private PowerPolicyResolver() {
	}

	static ResolvedPowerPolicy resolve(ServerLevel level) {
		PowersConfig config = PowersConfigLoader.get();
		return resolve(config, PowersConfigLoader.validationReport().revision(),
				level.getServer().getWorldData().getLevelName(),
				level.dimension().identifier().toString());
	}

	static ResolvedPowerPolicy resolve(PowersConfig config, long revision,
			String world, String dimension) {
		Cache current = cache;
		if (current.config != config || current.revision != revision) {
			synchronized (PowerPolicyResolver.class) {
				current = cache;
				if (current.config != config || current.revision != revision) {
					current = new Cache(revision, config);
					cache = current;
				}
			}
		}
		String safeWorld = world == null ? "" : world;
		String safeDimension = dimension == null ? "" : dimension;
		Key key = new Key(safeWorld, safeDimension);
		ResolvedPowerPolicy existing = current.scopes.get(key);
		if (existing != null) return existing;
		if (current.scopes.size() >= MAX_SCOPES) {
			return ResolvedPowerPolicy.resolve(config, safeWorld, safeDimension);
		}
		return current.scopes.computeIfAbsent(key,
				ignored -> ResolvedPowerPolicy.resolve(config, safeWorld, safeDimension));
	}

	private record Key(String world, String dimension) { }

	private static final class Cache {
		private final long revision;
		private final PowersConfig config;
		private final ConcurrentHashMap<Key, ResolvedPowerPolicy> scopes = new ConcurrentHashMap<>();

		private Cache(long revision, PowersConfig config) {
			this.revision = revision;
			this.config = config;
		}
	}
}
