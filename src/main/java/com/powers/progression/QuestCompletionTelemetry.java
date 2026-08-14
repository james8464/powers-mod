package com.powers.progression;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** Server-thread facade for persistent, anonymous Light/Dark quest telemetry. */
public final class QuestCompletionTelemetry {
	/** Approved independent-session sample floor for every alignment/level threshold. */
	public static final int PUBLICATION_SAMPLE_MINIMUM = 10;

	private QuestCompletionTelemetry() {
	}

	public static void noteActivity(ServerPlayer player, QuestTelemetryLedger.Alignment alignment) {
		if (player == null || alignment == null) return;
		QuestTelemetrySavedData saved = data(player.level().getServer());
		if (saved.ledger().noteActivity(player.getUUID(), alignment,
				player.level().getGameTime())) saved.changed();
	}

	public static void complete(ServerPlayer player, QuestTelemetryLedger.Alignment alignment,
			int level, String route) {
		if (player == null || alignment == null || level < 1 || level > 10) return;
		QuestTelemetrySavedData saved = data(player.level().getServer());
		saved.ledger().complete(player.getUUID(), alignment, level, route,
				player.level().getGameTime());
		saved.changed();
	}

	/** Records one authoritative deed that unlocked a contiguous range of levels. */
	public static void completeRange(ServerPlayer player, QuestTelemetryLedger.Alignment alignment,
			int firstLevel, int lastLevel, IntFunction<String> route) {
		if (player == null || alignment == null || route == null
				|| firstLevel < 1 || lastLevel > 10 || firstLevel > lastLevel) return;
		List<QuestTelemetryLedger.Completion> completions = new ArrayList<>(lastLevel - firstLevel + 1);
		for (int level = firstLevel; level <= lastLevel; level++) {
			completions.add(new QuestTelemetryLedger.Completion(level, route.apply(level)));
		}
		QuestTelemetrySavedData saved = data(player.level().getServer());
		saved.ledger().completeBatch(player.getUUID(), alignment, completions,
				player.level().getGameTime());
		saved.changed();
	}

	public static QuestTelemetryLedger.Summary summary(MinecraftServer server,
			QuestTelemetryLedger.Alignment alignment, int level) {
		return data(server).ledger().summary(alignment, level);
	}

	/** Compact aggregate for operators; no UUID or individual sample is emitted. */
	public static String diagnosticLine(MinecraftServer server) {
		int samples = data(server).ledger().samples().size();
		int sufficientLevels = 0;
		for (QuestTelemetryLedger.Alignment alignment : QuestTelemetryLedger.Alignment.values()) {
			for (int level = 1; level <= 10; level++) {
				if (summary(server, alignment, level).sufficient(PUBLICATION_SAMPLE_MINIMUM)) {
					sufficientLevels++;
				}
			}
		}
		return "questTelemetrySamples=" + samples + "; publicationReadyLevels="
				+ sufficientLevels + "/20; minimumPerLevel=" + PUBLICATION_SAMPLE_MINIMUM;
	}

	/** Complete anonymous report rows for an operator export/document generator. */
	public static List<String> reportRows(MinecraftServer server) {
		List<String> rows = new ArrayList<>(20);
		for (QuestTelemetryLedger.Alignment alignment : QuestTelemetryLedger.Alignment.values()) {
			for (int level = 1; level <= 10; level++) {
				QuestTelemetryLedger.Summary summary = summary(server, alignment, level);
				rows.add(alignment + ";" + level + ";" + summary.samples() + ";"
						+ summary.medianTicks() + ";" + summary.p90Ticks() + ";"
						+ String.join(",", summary.routes()));
			}
		}
		return List.copyOf(rows);
	}

	private static QuestTelemetrySavedData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(QuestTelemetrySavedData.TYPE);
	}
}
