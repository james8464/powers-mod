package com.powers.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-thread history containing only bounded numeric damage/healing samples. */
public final class BloodReadingHistoryService {
	private static final int MAX_ENTITIES = 512;
	private static final Map<UUID, List<BloodReadingTrendRules.Sample>> HISTORY =
			new LinkedHashMap<>(16, 0.75F, true);

	private BloodReadingHistoryService() {
	}

	public static void recordDamage(LivingEntity entity, float amount) {
		record(entity, BloodReadingTrendRules.Kind.DAMAGE, amount);
	}

	public static void recordHealing(LivingEntity entity, float amount) {
		record(entity, BloodReadingTrendRules.Kind.HEALING, amount);
	}

	public static BloodReadingTrendRules.Summary summary(LivingEntity entity) {
		if (entity == null || !(entity.level() instanceof ServerLevel level)) return empty();
		long tick = level.getServer().getTickCount();
		UUID id = entity.getUUID();
		List<BloodReadingTrendRules.Sample> samples = HISTORY.get(id);
		BloodReadingTrendRules.Summary summary = BloodReadingTrendRules.summarize(samples, tick);
		if (summary.sampleCount() == 0) HISTORY.remove(id);
		return summary;
	}

	public static void clear(LivingEntity entity) {
		if (entity != null) HISTORY.remove(entity.getUUID());
	}

	public static void clearAll() {
		HISTORY.clear();
	}

	static int trackedEntities() {
		return HISTORY.size();
	}

	private static void record(LivingEntity entity, BloodReadingTrendRules.Kind kind, float amount) {
		if (entity == null || !(entity.level() instanceof ServerLevel level)
				|| !Float.isFinite(amount) || amount <= 0.0F) return;
		UUID id = entity.getUUID();
		HISTORY.put(id, BloodReadingTrendRules.record(HISTORY.get(id),
				level.getServer().getTickCount(), kind, amount));
		while (HISTORY.size() > MAX_ENTITIES) {
			HISTORY.remove(HISTORY.keySet().iterator().next());
		}
	}

	private static BloodReadingTrendRules.Summary empty() {
		return new BloodReadingTrendRules.Summary(0.0F, 0.0F,
				BloodReadingTrendRules.Trend.STABLE, 0);
	}
}
