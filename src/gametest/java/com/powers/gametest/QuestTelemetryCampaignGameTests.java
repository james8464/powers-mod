package com.powers.gametest;

import com.powers.player.PlayerPowers;
import com.powers.progression.QuestCompletionTelemetry;
import com.powers.progression.QuestTelemetryLedger;
import com.powers.testing.QuestTelemetryCampaignScenario;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Live server proof that the campaign drives real player attachments and telemetry. */
public final class QuestTelemetryCampaignGameTests {
	public QuestTelemetryCampaignGameTests() {
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void connectedPlayersProduceIndependentAuthoritativeQuestSamples(GameTestHelper helper) {
		List<ServerPlayer> players = new ArrayList<>();
		for (int index = 0; index < QuestCompletionTelemetry.PUBLICATION_SAMPLE_MINIMUM; index++) {
			players.add(helper.makeMockServerPlayerInLevel());
		}
		helper.assertTrue(QuestTelemetryCampaignScenario.start(players,
				QuestTelemetryLedger.Alignment.LIGHT, 10_000).passed(),
				"Campaign rejected ten connected server players");
		helper.runAfterDelay(8, () -> {
			for (ServerPlayer player : players) {
				helper.assertTrue(PlayerPowers.get(player).skillLevel() >= 1,
						"Campaign did not reach the real progression tracker");
			}
			helper.assertTrue(QuestCompletionTelemetry.summary(helper.getLevel().getServer(),
					QuestTelemetryLedger.Alignment.LIGHT, 1).samples()
					>= QuestCompletionTelemetry.PUBLICATION_SAMPLE_MINIMUM,
					"Campaign did not publish ten independent level-one samples");
			QuestTelemetryCampaignScenario.clear(helper.getLevel().getServer());
			helper.succeed();
		});
	}
}
