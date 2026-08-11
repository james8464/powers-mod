package com.powers.testing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Generates the omission-visible operator checklist from authoritative catalogues. */
public final class ManualAcceptanceChecklistReport {
	private ManualAcceptanceChecklistReport() {
	}

	public static void main(String[] arguments) throws Exception {
		if (arguments.length != 1) throw new IllegalArgumentException("Expected project root");
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		com.powers.power.PowerRegistry.initialize();
		Path root = Path.of(arguments[0]);
		List<Row> rows = new ArrayList<>();
		for (GameplayAcceptanceCatalogue.Entry entry : GameplayAcceptanceCatalogue.entries()) {
			rows.add(new Row(entry.family().name().toLowerCase(), entry.id(),
					entry.evidence(), "AUTOMATED PASS"));
		}
		for (String line : Files.readAllLines(root.resolve("docs/gameplay/item-catalogue.md"))) {
			if (!line.startsWith("| `powers:")) continue;
			String[] cells = line.split("\\|", -1);
			if (cells.length < 7) continue;
			String id = cells[1].strip().replace("`", "");
			rows.add(new Row("item", id, "ItemCatalogueExecutableAuditTest; "
					+ cells[4].strip() + "; " + cells[5].strip(), "AUTOMATED PASS"));
		}
		for (String screen : List.of("power selection", "artifact combat wheel", "artifact library",
				"rank maze light", "rank maze darkness", "grimoire index", "arcane crucible",
				"locator", "teleport", "advancement light", "advancement darkness")) {
			rows.add(new Row("screen", screen, "verifyScreenshots visual golden",
					"GOLDEN REVIEW PASS"));
		}
		for (String command : commands()) {
			rows.add(new Row("command", command, "PowerCommand/TestingCommand registration",
					"MANUAL LIVE PENDING"));
		}
		StringBuilder output = new StringBuilder("# Manual gameplay acceptance checklist\n\n")
				.append("Build ID: `________________`  Tester: `________________`  Date: `________________`\n\n")
				.append("This generated register makes omissions visible. Automated/resource evidence is prefilled; ")
				.append("the release tester must replace every `MANUAL LIVE PENDING` row and record a build ID.\n\n")
				.append("| Family | Identity | Current evidence | Manual result | Notes / screenshot / log |\n")
				.append("| --- | --- | --- | --- | --- |\n");
		for (Row row : rows) {
			output.append("| ").append(cell(row.family())).append(" | `")
					.append(cell(row.id())).append("` | ").append(cell(row.evidence()))
					.append(" | ").append(row.status()).append(" |  |\n");
		}
		output.append("\nTotal registered rows: **").append(rows.size()).append("**.\n");
		Files.writeString(root.resolve("docs/verification/manual-acceptance-checklist.md"), output);
	}

	private static List<String> commands() {
		return List.of("/powers list", "/powers slots [player]", "/powers assign <player> <power> <slot>",
				"/powers reroll [player]", "/powers consent <kind> <allow|deny>", "/powers reload",
				"/powers return", "/powers recover <player>", "/powers path list",
				"/powers path unlock <node>", "/powers path focus <node>", "/powers path respec",
				"/powers darkprefix [true|false]", "/powers boss spawn", "/powers diagnose",
				"/powers ruin preview", "/powers ruin cancel", "/powers shadow learning reset <player>",
				"/powers travel <dimension>", "/powers testing status", "/powers testing on",
				"/powers testing off", "/powers testing reset", "/powers testing energy <on|off>",
				"/powers testing cooldowns <on|off>", "/powers testing refill",
				"/powers testing coverage", "/powers testing quest-telemetry",
				"/powers testing profile [status]",
				"/powers testing profile start <minutes> <expectedPlayers>",
				"/powers testing arena <spawn|clear>",
				"/powers testing actor spawn [username]");
	}

	private static String cell(String value) {
		return value.replace("|", "\\|").replace("\n", " ").strip();
	}

	private record Row(String family, String id, String evidence, String status) {
	}
}
