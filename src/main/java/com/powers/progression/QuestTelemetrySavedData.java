package com.powers.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.PowersMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;

/** World-owned persistence for anonymous real-player quest timing samples. */
public final class QuestTelemetrySavedData extends SavedData {
	private static final int MAXIMUM_SAMPLES = 10_000;
	public static final Codec<QuestTelemetrySavedData> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Codec.STRING.listOf().optionalFieldOf("starts", List.of())
							.forGetter(data -> data.ledger.encodedStarts()),
					Codec.STRING.listOf().optionalFieldOf("samples", List.of())
							.forGetter(data -> data.ledger.encodedSamples())
			).apply(instance, QuestTelemetrySavedData::new));
	public static final SavedDataType<QuestTelemetrySavedData> TYPE = new SavedDataType<>(
			PowersMod.id("quest_completion_telemetry"), QuestTelemetrySavedData::new,
			CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final QuestTelemetryLedger ledger;

	public QuestTelemetrySavedData() {
		ledger = new QuestTelemetryLedger(MAXIMUM_SAMPLES);
	}

	private QuestTelemetrySavedData(List<String> starts, List<String> samples) {
		ledger = QuestTelemetryLedger.decode(MAXIMUM_SAMPLES, starts, samples);
	}

	public QuestTelemetryLedger ledger() {
		return ledger;
	}

	public void changed() {
		setDirty();
	}
}
