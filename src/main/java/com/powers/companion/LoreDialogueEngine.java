package com.powers.companion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bounded offline lore voice that never consumes chat or another player's data. */
public final class LoreDialogueEngine {
	private record Line(String topic, String companion, String boss) {
	}

	private static final List<Line> LINES = List.of(
			new Line("realm", "This place is a thought pretending to have horizons.",
					"Even this realm remembers the shape of my first command."),
			new Line("health", "Your pulse stutters. Let the dark carry the next step.",
					"Your failing heart keeps excellent time."),
			new Line("energy", "The well is low, but the abyss below it has no floor.",
					"Spend your final spark. I have buried brighter suns."),
			new Line("rank", "A title is only a door that learned your name.",
					"Your titles are fragments of the crown you stole from me."),
			new Line("alignment", "The old forces are close. They taste each other through the stone.",
					"Light and darkness were quieter before mortals taught them hatred."),
			new Line("action", "That rite has teeth. Aim it where the world is weakest.",
					"I wrote that gesture before your blood had a language."),
			new Line("death", "Death noticed you. It will look more carefully next time.",
					"You returned. Good. Repetition sharpens the lesson."),
			new Line("boss", "The First Vessel is near. Do not mistake patience for mercy.",
					"Come closer. I would see what my echo became."),
			new Line("milestone", "Another seal opens behind your eyes.",
					"You have reached the place where victories become debts."),
			new Line("calm", "I walk where your shadow cannot quite reach.",
					"The world calls this silence. I remember it as obedience."));

	private final Map<UUID, ArrayDeque<String>> history = new HashMap<>();

	public String line(UUID speaker, LoreDialogueContext context, boolean bossVoice) {
		ArrayDeque<String> recent = history.computeIfAbsent(speaker, ignored -> new ArrayDeque<>(8));
		List<String> preferred = topics(context);
		Line chosen = LINES.stream().filter(line -> preferred.contains(line.topic())
				&& !recent.contains(line.topic())).findFirst().orElseGet(() -> LINES.stream()
				.filter(line -> !recent.contains(line.topic())).findFirst().orElse(LINES.getLast()));
		if (recent.size() == 8) recent.removeFirst();
		recent.addLast(chosen.topic());
		return bossVoice ? chosen.boss() : chosen.companion();
	}

	private static List<String> topics(LoreDialogueContext context) {
		List<String> result = new ArrayList<>(10);
		if (context.bossNearby()) result.add("boss");
		if (context.lowHealth()) result.add("health");
		if (context.lowEnergy()) result.add("energy");
		if (context.recentDeath()) result.add("death");
		if (!context.milestone().equals("none")) result.add("milestone");
		if (!context.realm().equals("overworld")) result.add("realm");
		if (!context.nearbyAlignment().equals("none")) result.add("alignment");
		if (!context.artifactAction().equals("none")) result.add("action");
		if (context.rank() > 0) result.add("rank");
		result.add("calm");
		return result;
	}

	public void forget(UUID speaker) {
		history.remove(speaker);
	}

	public void clear() {
		history.clear();
	}
}
